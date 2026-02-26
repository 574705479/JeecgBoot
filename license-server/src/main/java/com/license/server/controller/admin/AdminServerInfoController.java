package com.license.server.controller.admin;

import com.license.server.dto.PageResult;
import com.license.server.dto.Result;
import com.license.server.entity.DockerServiceConfig;
import com.license.server.entity.ServerInfo;
import com.license.server.service.*;
import com.jcraft.jsch.Session;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/admin/serverinfo/htServerInfo")
@RequiredArgsConstructor
public class AdminServerInfoController {

    private final ServerInfoService serverInfoService;
    private final ServerInfoLogService serverInfoLogService;
    private final DockerServiceConfigService dockerServiceConfigService;
    private final DockerComposeService dockerComposeService;
    private final RemoteExecService remoteExecService;

    @GetMapping("/list")
    public PageResult<ServerInfo> list(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size,
                                       @RequestParam(required = false) String keyword) {
        Page<ServerInfo> result = serverInfoService.list(page, size, keyword);
        return PageResult.of(result.getContent(), result.getTotalElements(), page, size);
    }

    @GetMapping("/queryTreeList")
    public Result<List<ServerInfo>> queryTreeList(@RequestParam(required = false) String keyword) {
        return Result.ok(serverInfoService.treeList(keyword));
    }

    @GetMapping("/queryById")
    public Result<ServerInfo> queryById(@RequestParam Long id) {
        return Result.ok(serverInfoService.getById(id));
    }

    @PostMapping("/add")
    public Result<ServerInfo> add(@RequestBody ServerInfo data) {
        try {
            return Result.ok(serverInfoService.create(data));
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<ServerInfo> edit(@RequestBody ServerInfo data) {
        try {
            if (data.getId() == null) {
                return Result.error(400, "ID不能为空");
            }
            return Result.ok(serverInfoService.update(data.getId(), data));
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestParam Long id) {
        serverInfoService.softDelete(id);
        return Result.ok();
    }

    @DeleteMapping("/deleteBatch")
    public Result<Void> deleteBatch(@RequestParam String ids) {
        List<Long> idList = Arrays.stream(ids.split(",")).map(String::trim).filter(s -> !s.isBlank()).map(Long::valueOf).toList();
        serverInfoService.softDeleteBatch(idList);
        return Result.ok();
    }

    @PostMapping("/generateAndConfigureSshKeyById")
    public Result<Map<String, String>> generateAndConfigureSshKeyById(@RequestBody IdRequest request) {
        try {
            ServerInfo info = serverInfoService.getById(request.getId());
            String publicKey = remoteExecService.generateAndInstallPublicKey(info);
            info.setPublicKey(1);
            info.setPublicKeyValue(publicKey);
            info.setPassword("");
            serverInfoService.update(info.getId(), info);
            return Result.ok(Map.of("publicKey", publicKey));
        } catch (Exception e) {
            return Result.error(400, "配置公钥失败: " + e.getMessage());
        }
    }

    @PostMapping("/connectToServer")
    public Result<String> connectToServer(@RequestBody IdRequest request) {
        try {
            ServerInfo info = serverInfoService.getById(request.getId());
            String message = remoteExecService.testConnection(info);
            info.setStatus(1);
            serverInfoService.update(info.getId(), info);
            return Result.ok(message);
        } catch (Exception e) {
            try {
                ServerInfo info = serverInfoService.getById(request.getId());
                info.setStatus(0);
                serverInfoService.update(info.getId(), info);
            } catch (Exception ignore) {
            }
            return Result.error(400, "连接失败: " + e.getMessage());
        }
    }

    @PostMapping("/executeSql")
    public Result<String> executeSql(@RequestBody ExecuteSqlRequest request, HttpServletRequest httpRequest) {
        if (request.getServerIds() == null || request.getServerIds().isEmpty() || request.getSqlContent() == null || request.getSqlContent().isBlank()) {
            return Result.error(400, "服务器列表或SQL内容不能为空");
        }
        Long operatorId = (Long) httpRequest.getAttribute("userId");

        List<CompletableFuture<String>> futures = request.getServerIds().stream().map(serverId ->
                CompletableFuture.supplyAsync(() -> executeSqlSingle(serverId, request.getSqlContent(), operatorId))
        ).toList();
        String output = futures.stream().map(CompletableFuture::join).collect(Collectors.joining("\n"));
        return Result.ok(output);
    }

    @PostMapping("/executeShellCommand")
    public Result<String> executeShellCommand(@RequestBody ExecuteShellRequest request, HttpServletRequest httpRequest) {
        if (request.getServerIds() == null || request.getServerIds().isEmpty() || request.getShellCommand() == null || request.getShellCommand().isBlank()) {
            return Result.error(400, "服务器列表或Shell命令不能为空");
        }
        Long operatorId = (Long) httpRequest.getAttribute("userId");
        List<CompletableFuture<String>> futures = request.getServerIds().stream().map(serverId ->
                CompletableFuture.supplyAsync(() -> executeShellSingle(serverId, request.getShellCommand(), operatorId))
        ).toList();
        String output = futures.stream().map(CompletableFuture::join).collect(Collectors.joining("\n"));
        return Result.ok(output);
    }

    @PostMapping("/uploadDockerCompose")
    public Result<Map<String, Object>> uploadDockerCompose(@RequestBody UploadComposeRequest request) {
        if (request.getServerId() == null || request.getFileContent() == null || request.getFileContent().isBlank()) {
            return Result.error(400, "服务器ID和文件内容不能为空");
        }
        try {
            serverInfoService.getById(request.getServerId());
            List<DockerServiceConfig> configs = dockerComposeService.parseToServices(request.getServerId(), request.getFileContent());
            String version = dockerComposeService.extractVersion(request.getFileContent());
            dockerServiceConfigService.saveComposeFile(request.getServerId(), request.getFileName(), request.getFileContent(), request.getFilePath(), version, 1, null);
            dockerServiceConfigService.replaceByServerId(request.getServerId(), configs);
            Map<String, Object> data = new HashMap<>();
            data.put("servicesCount", configs.size());
            data.put("services", configs);
            return Result.ok(data);
        } catch (Exception e) {
            try {
                dockerServiceConfigService.saveComposeFile(request.getServerId(), request.getFileName(), request.getFileContent(), request.getFilePath(), null, 2, e.getMessage());
            } catch (Exception ignored) {
            }
            return Result.error(400, "解析Compose失败: " + e.getMessage());
        }
    }

    @PostMapping("/syncDockerServices")
    public Result<String> syncDockerServices(@RequestBody Map<String, Object> request) {
        Long serverId = request.get("serverId") == null ? null : Long.valueOf(String.valueOf(request.get("serverId")));
        if (serverId == null) {
            return Result.error(400, "服务器ID不能为空");
        }
        try {
            ServerInfo info = serverInfoService.getById(serverId);
            Map<String, Integer> dockerStatus = remoteExecService.queryDockerStatus(info);
            int updated = 0;
            List<DockerServiceConfig> configs = dockerServiceConfigService.listByServerId(serverId);
            for (DockerServiceConfig config : configs) {
                Integer status = dockerStatus.get(config.getContainerName());
                if (status == null && config.getServiceName() != null) {
                    status = dockerStatus.get(config.getServiceName());
                }
                if (status != null && !Objects.equals(status, config.getStatus())) {
                    config.setStatus(status);
                    dockerServiceConfigService.update(config.getId(), config);
                    updated++;
                }
            }
            return Result.ok("同步成功，更新 " + updated + " 个服务状态");
        } catch (Exception e) {
            return Result.error(400, "同步失败: " + e.getMessage());
        }
    }

    private String executeSqlSingle(Long serverId, String sql, Long operatorId) {
        LocalDateTime start = LocalDateTime.now();
        ServerInfo info = serverInfoService.getById(serverId);
        try {
            String result = remoteExecService.executeSql(info, sql);
            serverInfoLogService.createLog(serverId, info.getIp(), "执行SQL", result, true, start, operatorId);
            return "[" + info.getIp() + "] SQL执行成功";
        } catch (Exception e) {
            serverInfoLogService.createLog(serverId, info.getIp(), "执行SQL", e.getMessage(), false, start, operatorId);
            return "[" + info.getIp() + "] SQL执行失败: " + e.getMessage();
        }
    }

    private String executeShellSingle(Long serverId, String shell, Long operatorId) {
        LocalDateTime start = LocalDateTime.now();
        ServerInfo info = serverInfoService.getById(serverId);
        try {
            String result = remoteExecService.executeShell(info, shell);
            serverInfoLogService.createLog(serverId, info.getIp(), "执行Shell", result, true, start, operatorId);
            return "[" + info.getIp() + "] Shell执行成功";
        } catch (Exception e) {
            serverInfoLogService.createLog(serverId, info.getIp(), "执行Shell", e.getMessage(), false, start, operatorId);
            return "[" + info.getIp() + "] Shell执行失败: " + e.getMessage();
        }
    }

    @Data
    public static class IdRequest {
        private Long id;
    }

    @Data
    public static class ExecuteSqlRequest {
        private List<Long> serverIds;
        private String sqlContent;
    }

    @Data
    public static class ExecuteShellRequest {
        private List<Long> serverIds;
        private String shellCommand;
    }

    @Data
    public static class UploadComposeRequest {
        private Long serverId;
        private String fileName;
        private String fileContent;
        private String filePath;
    }
}
