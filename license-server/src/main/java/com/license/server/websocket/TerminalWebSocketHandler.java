package com.license.server.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.Session;
import com.license.server.entity.ServerInfo;
import com.license.server.service.RemoteExecService;
import com.license.server.service.ServerInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TerminalWebSocketHandler extends TextWebSocketHandler {

    private final ServerInfoService serverInfoService;
    private final RemoteExecService remoteExecService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, ShellContext> contextMap = new ConcurrentHashMap<>();
    private final ExecutorService streamPump = Executors.newCachedThreadPool();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long serverId = resolveServerId(session);
        if (serverId == null) {
            sendError(session, "SERVER_NOT_FOUND", "无效的服务器ID");
            session.close(CloseStatus.BAD_DATA.withReason("invalid serverId"));
            return;
        }
        try {
            ServerInfo serverInfo = serverInfoService.getById(serverId);
            Session sshSession = remoteExecService.createSshSession(serverInfo);
            sshSession.connect(10_000);

            ChannelShell shell = (ChannelShell) sshSession.openChannel("shell");
            shell.setPtyType("xterm-256color");
            shell.setPtySize(120, 36, 0, 0);
            InputStream stdout = shell.getInputStream();
            OutputStream stdin = shell.getOutputStream();
            shell.connect(10_000);

            ShellContext context = new ShellContext(sshSession, shell, stdin);
            contextMap.put(session.getId(), context);
            startReadPump(session, context, stdout);

            sendMessage(session, "connected", Map.of(
                    "message", "终端连接成功",
                    "serverId", serverId
            ));
        } catch (Exception e) {
            sendError(session, "SSH_CONNECT_FAILED", normalizeErrorMessage(e));
            session.close(CloseStatus.SERVER_ERROR.withReason("ssh connect failed"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ShellContext context = contextMap.get(session.getId());
        if (context == null) {
            sendError(session, "SESSION_NOT_READY", "终端会话未初始化");
            return;
        }
        String payload = message.getPayload();
        if (payload == null || payload.isBlank()) {
            return;
        }
        JsonNode root = tryParseJson(payload);
        if (root == null) {
            // backward compatibility: treat raw text as command input
            writeInput(context, payload);
            return;
        }
        String type = root.path("type").asText("");
        switch (type) {
            case "init" -> handleInit(root, context);
            case "input" -> writeInput(context, root.path("content").asText(""));
            case "resize" -> handleResize(root, context);
            case "ping" -> sendMessage(session, "pong", Map.of("ts", System.currentTimeMillis()));
            case "close" -> session.close(CloseStatus.NORMAL.withReason("client close"));
            default -> {
                String command = root.path("command").asText("");
                if (!command.isBlank()) {
                    writeInput(context, command + "\n");
                }
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("terminal transport error: sessionId={}, msg={}", session.getId(), exception.getMessage());
        closeContext(session.getId());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR.withReason("transport error"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        closeContext(session.getId());
        log.info("terminal closed: sessionId={}, status={}", session.getId(), status);
    }

    private void handleInit(JsonNode root, ShellContext context) {
        int cols = root.path("cols").asInt(120);
        int rows = root.path("rows").asInt(36);
        context.shell().setPtySize(Math.max(cols, 20), Math.max(rows, 10), 0, 0);
    }

    private void handleResize(JsonNode root, ShellContext context) {
        int cols = root.path("cols").asInt(120);
        int rows = root.path("rows").asInt(36);
        context.shell().setPtySize(Math.max(cols, 20), Math.max(rows, 10), 0, 0);
    }

    private void writeInput(ShellContext context, String content) throws Exception {
        if (content == null || content.isEmpty()) {
            return;
        }
        synchronized (context.stdin()) {
            context.stdin().write(content.getBytes(StandardCharsets.UTF_8));
            context.stdin().flush();
        }
    }

    private void startReadPump(WebSocketSession session, ShellContext context, InputStream stdout) {
        streamPump.submit(() -> {
            byte[] buffer = new byte[4096];
            try {
                while (session.isOpen() && context.shell().isConnected()) {
                    int read = stdout.read(buffer);
                    if (read < 0) {
                        break;
                    }
                    if (read == 0) {
                        continue;
                    }
                    String text = new String(buffer, 0, read, StandardCharsets.UTF_8);
                    sendMessage(session, "output", Map.of("content", text));
                }
            } catch (Exception e) {
                log.debug("terminal read pump stopped: {}", e.getMessage());
            }
        });
    }

    private void closeContext(String sessionId) {
        ShellContext context = contextMap.remove(sessionId);
        if (context == null) {
            return;
        }
        try {
            if (context.shell() != null && context.shell().isConnected()) {
                context.shell().disconnect();
            }
        } catch (Exception e) {
            log.debug("close shell error: {}", e.getMessage());
        }
        try {
            if (context.session() != null && context.session().isConnected()) {
                context.session().disconnect();
            }
        } catch (Exception e) {
            log.debug("close session error: {}", e.getMessage());
        }
    }

    private Long resolveServerId(WebSocketSession session) {
        Object value = session.getAttributes().get("serverId");
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private JsonNode tryParseJson(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception e) {
            return null;
        }
    }

    private void sendError(WebSocketSession session, String code, String message) throws Exception {
        sendMessage(session, "error", Map.of(
                "code", code,
                "message", Objects.requireNonNullElse(message, "终端异常")
        ));
    }

    private String normalizeErrorMessage(Exception e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        if (msg.contains("Auth fail")) {
            return "SSH认证失败，请检查用户名、公钥或私钥配置";
        }
        if (msg.contains("Connection refused") || msg.contains("timeout")) {
            return "服务器连接失败，请检查网络和端口";
        }
        return msg.isBlank() ? "终端连接异常" : msg;
    }

    private void sendMessage(WebSocketSession session, String type, Map<String, Object> data) {
        if (!session.isOpen()) {
            return;
        }
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                    "type", type,
                    "data", data
            ));
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload));
                }
            }
        } catch (Exception e) {
            log.debug("send ws message failed: {}", e.getMessage());
        }
    }

    private record ShellContext(Session session, ChannelShell shell, OutputStream stdin) {
    }
}
