package com.license.server.controller.admin;

import com.jcraft.jsch.Session;
import com.license.server.dto.PageResult;
import com.license.server.dto.Result;
import com.license.server.entity.DockerComposeFile;
import com.license.server.entity.DockerServiceConfig;
import com.license.server.entity.DockerTask;
import com.license.server.entity.ServerInfo;
import com.license.server.service.DockerComposeService;
import com.license.server.service.DockerServiceConfigService;
import com.license.server.service.DockerTaskService;
import com.license.server.service.RemoteExecService;
import com.license.server.service.ServerInfoService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/admin/dockerservice/htDockerService")
@RequiredArgsConstructor
public class AdminDockerServiceController {

    private final DockerServiceConfigService dockerServiceConfigService;
    private final DockerTaskService dockerTaskService;
    private final ServerInfoService serverInfoService;
    private final RemoteExecService remoteExecService;
    private final DockerComposeService dockerComposeService;

    @GetMapping("/list")
    public PageResult<DockerServiceConfig> list(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(required = false) Long serverId) {
        Page<DockerServiceConfig> result = dockerServiceConfigService.list(page, size, serverId);
        return PageResult.of(result.getContent(), result.getTotalElements(), page, size);
    }

    @GetMapping("/listByServerId")
    public Result<List<DockerServiceConfig>> listByServerId(@RequestParam Long serverId) {
        return Result.ok(dockerServiceConfigService.listByServerId(serverId));
    }

    @GetMapping("/queryById")
    public Result<DockerServiceConfig> queryById(@RequestParam Long id) {
        return Result.ok(dockerServiceConfigService.getById(id));
    }

    @PostMapping("/add")
    public Result<DockerServiceConfig> add(@RequestBody DockerServiceConfig config) {
        try {
            return Result.ok(dockerServiceConfigService.create(config));
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<DockerServiceConfig> edit(@RequestBody DockerServiceConfig config) {
        try {
            if (config.getId() == null) {
                return Result.error(400, "ID不能为空");
            }
            return Result.ok(dockerServiceConfigService.update(config.getId(), config));
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestParam Long id) {
        dockerServiceConfigService.softDelete(id);
        return Result.ok();
    }

    @DeleteMapping("/deleteBatch")
    public Result<Void> deleteBatch(@RequestParam String ids) {
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(Long::valueOf)
                .toList();
        for (Long id : idList) {
            dockerServiceConfigService.softDelete(id);
        }
        return Result.ok();
    }

    @PostMapping("/updateVersion")
    public Result<Void> updateVersion(@RequestBody Map<String, Object> payload) {
        Long id = payload.get("id") == null ? null : Long.valueOf(String.valueOf(payload.get("id")));
        String targetVersion = payload.get("targetVersion") == null ? null : String.valueOf(payload.get("targetVersion"));
        if (id == null || targetVersion == null || targetVersion.isBlank()) {
            return Result.error(400, "参数不完整");
        }
        DockerServiceConfig config = dockerServiceConfigService.getById(id);
        config.setTargetVersion(targetVersion);
        dockerServiceConfigService.update(id, config);
        return Result.ok();
    }

    @PostMapping("/executeDockerCommand")
    public Result<String> executeDockerCommand(@RequestBody ExecuteCommandRequest request) {
        try {
            DockerServiceConfig config = dockerServiceConfigService.getById(request.getServiceId());
            ServerInfo serverInfo = serverInfoService.getById(config.getServerId());
            return executeSingleDockerCommand(serverInfo, config, request.getCommandType(), request.getComposePath());
        } catch (Exception e) {
            return Result.error(400, "执行失败: " + e.getMessage());
        }
    }

    @PostMapping("/batchUpdateServices")
    public Result<String> batchUpdateServices(@RequestBody BatchRequest request) {
        if (request.getServiceIds() == null || request.getServiceIds().isEmpty()) {
            return Result.error(400, "请选择要更新的服务");
        }
        int success = 0;
        int fail = 0;
        List<String> errors = new ArrayList<>();
        for (Long serviceId : request.getServiceIds()) {
            try {
                Result<String> result = executeDockerCommand(new ExecuteCommandRequest(serviceId, "update", request.getComposePath(), null));
                if (result.getCode() == 200) {
                    success++;
                } else {
                    fail++;
                    errors.add(serviceId + ": " + result.getMessage());
                }
            } catch (Exception e) {
                fail++;
                errors.add(serviceId + ": " + e.getMessage());
            }
        }
        return Result.ok("批量更新完成，成功 " + success + " 个，失败 " + fail + " 个"
                + (errors.isEmpty() ? "" : "。失败详情：" + String.join("；", errors)));
    }

    @PostMapping("/executeDockerAsync")
    public Result<Map<String, Object>> executeDockerAsync(@RequestBody AsyncRequest request) {
        if (request.getServiceIds() == null || request.getServiceIds().isEmpty() || request.getCommandType() == null) {
            return Result.error(400, "参数不完整");
        }
        Long taskServerId = request.getServerId();
        if (taskServerId == null) {
            DockerServiceConfig firstService = dockerServiceConfigService.getById(request.getServiceIds().get(0));
            taskServerId = firstService.getServerId();
        }
        List<String> names = request.getServiceIds().stream()
                .map(dockerServiceConfigService::getById)
                .map(DockerServiceConfig::getServiceName)
                .toList();
        DockerTask task = dockerTaskService.createTask(request.getCommandType(), taskServerId, request.getServiceIds(), String.join(", ", names));

        CompletableFuture.runAsync(() -> doAsyncTask(task.getId(), request.getServiceIds(), request.getCommandType(), request.getComposePath()));

        Map<String, Object> data = new HashMap<>();
        data.put("taskId", task.getId());
        data.put("message", "任务已加入队列");
        return Result.ok(data);
    }

    @GetMapping("/queryTaskProgress")
    public Result<DockerTask> queryTaskProgress(@RequestParam Long taskId) {
        return Result.ok(dockerTaskService.getById(taskId));
    }

    @GetMapping("/getRunningTask")
    public Result<DockerTask> getRunningTask(@RequestParam Long serverId) {
        return Result.ok(dockerTaskService.getRunningTaskByServerId(serverId));
    }

    @GetMapping("/getRecentTasks")
    public Result<List<DockerTask>> getRecentTasks(@RequestParam Long serverId,
                                                   @RequestParam(defaultValue = "10") Integer limit) {
        return Result.ok(dockerTaskService.getRecentTasksByServerId(serverId, limit));
    }

    @GetMapping("/exportComposeFile")
    public void exportComposeFile(@RequestParam Long serverId, HttpServletResponse response) throws Exception {
        List<DockerServiceConfig> services = dockerServiceConfigService.listByServerId(serverId);
        DockerComposeFile latestCompose = dockerServiceConfigService.getLatestComposeByServerId(serverId);
        String version = latestCompose == null || latestCompose.getVersion() == null || latestCompose.getVersion().isBlank()
                ? "3"
                : latestCompose.getVersion();
        String content = dockerComposeService.exportComposeContent(services, version);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/x-yaml");
        response.setHeader("Content-Disposition", "attachment; filename=docker-compose-" + serverId + ".yml");
        response.getWriter().write(content);
        response.getWriter().flush();
    }

    private Result<String> executeSingleDockerCommand(ServerInfo serverInfo, DockerServiceConfig config, String commandType, String composePath) throws Exception {
        try (SshHolder holder = connectSsh(serverInfo)) {
            String dockerInstalled = remoteExecService.executeCommand(holder.session,
                    "which docker >/dev/null 2>&1 && echo 'installed' || echo 'not_installed'",
                    10_000);
            if ("not_installed".equals(dockerInstalled.trim())) {
                return Result.error(400, "目标服务器未安装Docker，请先安装后再执行");
            }
            String command = buildDockerCommand(holder.session, config, commandType, composePath);
            String result = remoteExecService.executeCommand(holder.session, command, "update".equals(commandType) ? 3_600_000 : 60_000);
            if ("logs".equals(commandType) || "ps".equals(commandType)) {
                return Result.ok(result);
            }
            if ("pull".equals(commandType)) {
                return Result.ok("拉取成功");
            }

            if ("start".equals(commandType) || "restart".equals(commandType) || "update".equals(commandType)) {
                config.setStatus(1);
                if ("update".equals(commandType) && config.getTargetVersion() != null && !config.getTargetVersion().isBlank()) {
                    config.setCurrentVersion(config.getTargetVersion());
                }
            } else if ("stop".equals(commandType)) {
                config.setStatus(0);
            }
            dockerServiceConfigService.update(config.getId(), config);
            return Result.ok("操作成功");
        }
    }

    private String buildDockerCommand(Session session, DockerServiceConfig config, String commandType, String composePath) throws Exception {
        String target = config.getContainerName() == null || config.getContainerName().isBlank()
                ? config.getServiceName() : config.getContainerName();

        // logs 和 ps 始终使用直接 docker 命令，无需 compose 文件
        if ("logs".equals(commandType)) {
            return "docker logs --tail 100 " + target;
        }
        if ("ps".equals(commandType)) {
            return "docker ps -a --filter name=^" + target + "$ --format \"{{.Names}}\\t{{.Status}}\\t{{.Image}}\"";
        }

        // 尝试使用 docker-compose 模式
        String composeDir = resolveComposeDir(session, config, composePath);

        if (composeDir != null) {
            return switch (commandType) {
                case "start" -> "cd " + composeDir + " && docker-compose up -d --remove-orphans " + config.getServiceName();
                case "stop" -> "cd " + composeDir + " && docker-compose stop " + config.getServiceName();
                case "restart" -> "cd " + composeDir + " && docker-compose stop " + config.getServiceName()
                        + " && docker-compose up -d --remove-orphans " + config.getServiceName();
                case "update" -> "cd " + composeDir
                        + " && (docker ps -aq -f name=^" + target + "$ | grep -q . && docker rm -f " + target + " || true)"
                        + " && docker-compose pull " + config.getServiceName()
                        + " && docker-compose up -d --no-deps " + config.getServiceName();
                default -> throw new IllegalArgumentException("不支持的命令类型: " + commandType);
            };
        }

        // 回退：无 compose 文件时使用直接 docker 命令
        log.warn("无法获取 compose 文件路径，回退到直接 docker 命令。serverId={}, service={}", config.getServerId(), config.getServiceName());
        String fullImage = config.getImageName() + ":" +
                (config.getTargetVersion() != null && !config.getTargetVersion().isBlank()
                        ? config.getTargetVersion() : config.getCurrentVersion());

        return switch (commandType) {
            case "start" -> "docker start " + target;
            case "stop" -> "docker stop " + target;
            case "restart" -> "docker restart " + target;
            case "update" -> "docker pull " + fullImage
                    + " && docker stop " + target
                    + " && docker rm " + target
                    + " && docker run -d --name " + target + " " + fullImage;
            default -> throw new IllegalArgumentException("不支持的命令类型: " + commandType);
        };
    }

    private String resolveComposeDir(Session session, DockerServiceConfig config, String composePath) {
        try {
            DockerComposeFile latestCompose = dockerServiceConfigService.getLatestComposeByServerId(config.getServerId());
            String version = latestCompose == null || latestCompose.getVersion() == null || latestCompose.getVersion().isBlank()
                    ? "3" : latestCompose.getVersion();
            String exportedCompose = dockerComposeService.exportComposeContent(
                    dockerServiceConfigService.listByServerId(config.getServerId()), version);
            String composeFilePath = remoteExecService.ensureComposeFile(session, config.getServerId(), exportedCompose);
            return remoteExecService.getComposeDir(
                    composePath == null || composePath.isBlank() ? composeFilePath : composePath);
        } catch (Exception e) {
            log.error("解析 compose 文件路径失败: {}", e.getMessage());
            return null;
        }
    }

    private void doAsyncTask(Long taskId, List<Long> serviceIds, String commandType, String composePath) {
        int success = 0;
        int fail = 0;
        List<String> details = new ArrayList<>();
        try {
            for (Long serviceId : serviceIds) {
                try {
                    DockerServiceConfig config = dockerServiceConfigService.getById(serviceId);
                    dockerTaskService.updateProgress(taskId, config.getServiceName(), success, fail);
                    ServerInfo serverInfo = serverInfoService.getById(config.getServerId());
                    Result<String> result = executeSingleDockerCommand(serverInfo, config, commandType, composePath);
                    if (result.getCode() == 200) {
                        success++;
                        details.add(config.getServiceName() + ": 成功");
                    } else {
                        fail++;
                        details.add(config.getServiceName() + ": " + result.getMessage());
                    }
                    dockerTaskService.updateProgress(taskId, config.getServiceName(), success, fail);
                } catch (Exception e) {
                    fail++;
                    details.add(serviceId + ": " + e.getMessage());
                    dockerTaskService.updateProgress(taskId, String.valueOf(serviceId), success, fail);
                }
            }
            dockerTaskService.completeTask(taskId, success, fail, details);
        } catch (Exception ex) {
            dockerTaskService.failTask(taskId, ex.getMessage());
        }
    }

    private SshHolder connectSsh(ServerInfo serverInfo) throws Exception {
        Session session = remoteExecService.createSshSession(serverInfo);
        session.connect(30_000);
        return new SshHolder(session);
    }

    @Data
    @AllArgsConstructor
    public static class ExecuteCommandRequest {
        private Long serviceId;
        private String commandType;
        private String composePath;
        private Long serverId;
    }

    @Data
    public static class AsyncRequest {
        private List<Long> serviceIds;
        private String commandType;
        private String composePath;
        private Long serverId;
    }

    @Data
    public static class BatchRequest {
        private List<Long> serviceIds;
        private String composePath;
    }

    private static class SshHolder implements AutoCloseable {
        private final Session session;

        private SshHolder(Session session) {
            this.session = session;
        }

        @Override
        public void close() {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}
