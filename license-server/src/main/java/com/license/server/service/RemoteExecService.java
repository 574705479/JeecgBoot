package com.license.server.service;

import com.jcraft.jsch.*;
import com.license.server.entity.ServerInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class RemoteExecService {

    private static final int DEFAULT_SSH_PORT = 22;
    private static final int DEFAULT_MYSQL_PORT = 3306;
    private static final String DEFAULT_DB = "im_platform";

    public Session createSshSession(ServerInfo serverInfo) throws Exception {
        JSch jsch = new JSch();
        addIdentity(jsch, serverInfo);
        Session session = jsch.getSession(serverInfo.getUsername(), serverInfo.getIp(), getSshPort(serverInfo));
        session.setConfig("StrictHostKeyChecking", "no");
        session.setConfig("PreferredAuthentications", "publickey");
        return session;
    }

    public String testConnection(ServerInfo serverInfo) throws Exception {
        int connectionType = normalizeConnectionType(serverInfo);
        if (connectionType == 2) {
            return testMysqlConnection(serverInfo);
        }
        try (SessionHolder holder = connectSsh(serverInfo, 30_000)) {
            return executeCommand(holder.session(), "echo 'SSH连接测试成功' && date", 10_000);
        }
    }

    public String executeShell(ServerInfo serverInfo, String shellCommand) throws Exception {
        if (normalizeConnectionType(serverInfo) == 2) {
            throw new IllegalArgumentException("云数据库（直连）不支持执行 Shell");
        }
        try (SessionHolder holder = connectSsh(serverInfo, 30_000)) {
            return executeCommand(holder.session(), shellCommand, 60_000);
        }
    }

    public String executeSql(ServerInfo serverInfo, String sqlContent) throws Exception {
        if (normalizeConnectionType(serverInfo) == 2) {
            return executeSqlByJdbc(serverInfo.getIp(), getMysqlPort(serverInfo), serverInfo, sqlContent);
        }

        try (SessionHolder holder = connectSsh(serverInfo, 30_000)) {
            int localPort = 10_000 + (int) (Math.random() * 20_000);
            holder.session().setPortForwardingL(localPort, "127.0.0.1", getMysqlPort(serverInfo));
            return executeSqlByJdbc("127.0.0.1", localPort, serverInfo, sqlContent);
        }
    }

    public Map<String, Integer> queryDockerStatus(ServerInfo serverInfo) throws Exception {
        if (normalizeConnectionType(serverInfo) == 2) {
            throw new IllegalArgumentException("云数据库（直连）不支持 Docker 操作");
        }
        try (SessionHolder holder = connectSsh(serverInfo, 30_000)) {
            String result = executeCommand(holder.session(), "docker ps -a --format \"{{.Names}}\\t{{.Status}}\"", 30_000);
            Map<String, Integer> statusMap = new HashMap<>();
            for (String line : result.split("\n")) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                String[] parts = line.split("\t", 2);
                if (parts.length < 2) {
                    continue;
                }
                String containerName = parts[0].trim();
                String statusText = parts[1].toLowerCase();
                int status = 0;
                if (statusText.startsWith("up")) {
                    status = 1;
                } else if (statusText.contains("unhealthy") || statusText.contains("restarting")) {
                    status = 2;
                }
                statusMap.put(containerName, status);
            }
            return statusMap;
        }
    }

    public String generateAndInstallPublicKey(ServerInfo serverInfo) throws Exception {
        if (serverInfo.getPassword() == null || serverInfo.getPassword().isBlank()) {
            throw new IllegalArgumentException("服务器密码不能为空（配置公钥时需要）");
        }

        KeyPairHolder keyPairHolder = generateKeyPair(serverInfo.getId());
        JSch jsch = new JSch();
        Session session = jsch.getSession(serverInfo.getUsername(), serverInfo.getIp(), getSshPort(serverInfo));
        session.setPassword(serverInfo.getPassword());
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(10_000);
        try {
            executeCommand(session, "mkdir -p ~/.ssh && chmod 700 ~/.ssh && touch ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys", 10_000);
            String safePublicKey = keyPairHolder.publicKey().replace("'", "'\"'\"'");
            executeCommand(session, "echo '" + safePublicKey.trim() + "' >> ~/.ssh/authorized_keys", 10_000);
            return keyPairHolder.publicKey();
        } finally {
            session.disconnect();
        }
    }

    public String executeCommand(Session session, String command, int timeoutMs) throws Exception {
        ChannelExec channel = (ChannelExec) session.openChannel("exec");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        channel.setCommand(command);
        channel.setOutputStream(out);
        channel.setErrStream(err);
        channel.connect();
        long start = System.currentTimeMillis();
        while (!channel.isClosed()) {
            Thread.sleep(100);
            if (System.currentTimeMillis() - start > timeoutMs) {
                channel.disconnect();
                throw new RuntimeException("命令执行超时: " + command);
            }
        }
        int exitCode = channel.getExitStatus();
        channel.disconnect();
        String stdout = out.toString(StandardCharsets.UTF_8);
        String stderr = err.toString(StandardCharsets.UTF_8);
        if (exitCode != 0) {
            throw new RuntimeException(stderr.isBlank() ? stdout : stderr);
        }
        return stdout;
    }

    public String ensureComposeFile(Session session, Long serverId, String content) throws Exception {
        String safeServerId = String.valueOf(serverId).replace("-", "_");
        String dir = "/tmp/license-server-compose-" + safeServerId;
        String filePath = dir + "/docker-compose.yml";
        executeCommand(session, "mkdir -p " + dir, 10_000);
        String writeCmd = "cat > " + filePath + " << 'EOF_COMPOSE'\n" + content + "\nEOF_COMPOSE";
        executeCommand(session, writeCmd, 15_000);
        return filePath;
    }

    public String getComposeDir(String composePath) {
        if (composePath == null || composePath.isBlank()) {
            return "/opt/docker";
        }
        if (composePath.endsWith(".yml") || composePath.endsWith(".yaml")) {
            int i = composePath.lastIndexOf("/");
            return i > 0 ? composePath.substring(0, i) : "/opt/docker";
        }
        return composePath;
    }

    private String executeSqlByJdbc(String host, int port, ServerInfo serverInfo, String sqlContent) throws Exception {
        String db = (serverInfo.getDatabaseName() == null || serverInfo.getDatabaseName().isBlank()) ? DEFAULT_DB : serverInfo.getDatabaseName();
        String user = serverInfo.getMsUser();
        String pwd = serverInfo.getMsPwd();
        String jdbc = "jdbc:mysql://" + host + ":" + port + "/" + db
                + "?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8";

        long begin = System.currentTimeMillis();
        try (Connection conn = DriverManager.getConnection(jdbc, user, pwd);
             Statement stmt = conn.createStatement()) {
            StringBuilder output = new StringBuilder();
            for (String sql : sqlContent.split(";")) {
                String part = sql == null ? "" : sql.trim();
                if (part.isBlank()) {
                    continue;
                }
                boolean hasRs = stmt.execute(part);
                if (hasRs) {
                    try (ResultSet rs = stmt.getResultSet()) {
                        ResultSetMetaData md = rs.getMetaData();
                        int columns = md.getColumnCount();
                        for (int i = 1; i <= columns; i++) {
                            output.append(md.getColumnName(i)).append(i < columns ? "\t" : "\n");
                        }
                        while (rs.next()) {
                            for (int i = 1; i <= columns; i++) {
                                output.append(rs.getString(i)).append(i < columns ? "\t" : "\n");
                            }
                        }
                    }
                } else {
                    output.append("SQL执行完成，影响行数: ").append(stmt.getUpdateCount()).append("\n");
                }
            }
            output.append("耗时: ").append(Duration.ofMillis(System.currentTimeMillis() - begin).toMillis()).append("ms");
            return output.toString();
        }
    }

    private String testMysqlConnection(ServerInfo serverInfo) throws Exception {
        String db = (serverInfo.getDatabaseName() == null || serverInfo.getDatabaseName().isBlank()) ? DEFAULT_DB : serverInfo.getDatabaseName();
        String jdbc = "jdbc:mysql://" + serverInfo.getIp() + ":" + getMysqlPort(serverInfo) + "/" + db
                + "?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8&connectTimeout=5000";
        try (Connection conn = DriverManager.getConnection(jdbc, serverInfo.getMsUser(), serverInfo.getMsPwd());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DATABASE(), VERSION(), NOW()")) {
            if (rs.next()) {
                return "MySQL连接测试成功，数据库: " + rs.getString(1) + "，版本: " + rs.getString(2) + "，时间: " + rs.getString(3);
            }
            return "MySQL连接测试成功";
        }
    }

    private SessionHolder connectSsh(ServerInfo serverInfo, int connectTimeoutMs) throws Exception {
        Session session = createSshSession(serverInfo);
        session.connect(connectTimeoutMs);
        return new SessionHolder(session);
    }

    private int getSshPort(ServerInfo serverInfo) {
        return serverInfo.getSshPort() == null ? DEFAULT_SSH_PORT : serverInfo.getSshPort();
    }

    private int getMysqlPort(ServerInfo serverInfo) {
        return serverInfo.getMsPort() == null ? DEFAULT_MYSQL_PORT : serverInfo.getMsPort();
    }

    private int normalizeConnectionType(ServerInfo serverInfo) {
        return serverInfo.getConnectionType() == null ? 1 : serverInfo.getConnectionType();
    }

    private void addIdentity(JSch jsch, ServerInfo serverInfo) throws Exception {
        if (serverInfo.getPrivateKey() != null && !serverInfo.getPrivateKey().isBlank()) {
            Path temp = Files.createTempFile("license-server-key-", ".pem");
            Files.writeString(temp, serverInfo.getPrivateKey(), StandardCharsets.UTF_8);
            jsch.addIdentity(temp.toAbsolutePath().toString());
            return;
        }
        if (serverInfo.getPrivateKeyPath() != null && !serverInfo.getPrivateKeyPath().isBlank()) {
            Path key = Path.of(serverInfo.getPrivateKeyPath());
            if (!Files.exists(key)) {
                throw new IllegalArgumentException("私钥文件不存在: " + key);
            }
            jsch.addIdentity(key.toAbsolutePath().toString());
            return;
        }

        Path keyPath = resolveSystemPrivateKeyPath(serverInfo.getId());
        if (!Files.exists(keyPath)) {
            throw new IllegalArgumentException("未找到可用私钥，请先配置公钥");
        }
        jsch.addIdentity(keyPath.toAbsolutePath().toString());
    }

    private KeyPairHolder generateKeyPair(Long serverId) throws Exception {
        JSch jsch = new JSch();
        KeyPair keyPair = KeyPair.genKeyPair(jsch, KeyPair.ECDSA, 256);
        Path dir = resolveSystemPrivateKeyPath(serverId).getParent();
        Files.createDirectories(dir);
        Path privateKeyPath = dir.resolve("id_rsa");
        Path publicKeyPath = dir.resolve("id_rsa.pub");
        keyPair.writePrivateKey(privateKeyPath.toString());
        keyPair.writePublicKey(publicKeyPath.toString(), "license-server@" + UUID.randomUUID());
        String publicKey = Files.readString(publicKeyPath, StandardCharsets.UTF_8);
        return new KeyPairHolder(privateKeyPath, publicKeyPath, publicKey);
    }

    private Path resolveSystemPrivateKeyPath(Long serverId) {
        return Path.of("data", "ssh-keys", String.valueOf(serverId), "id_rsa");
    }

    private record KeyPairHolder(Path privateKeyPath, Path publicKeyPath, String publicKey) {
    }

    private record SessionHolder(Session session) implements AutoCloseable {
        @Override
        public void close() {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}
