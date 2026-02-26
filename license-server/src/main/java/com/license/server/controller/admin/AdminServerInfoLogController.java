package com.license.server.controller.admin;

import com.license.server.dto.PageResult;
import com.license.server.dto.Result;
import com.license.server.entity.ServerInfoLog;
import com.license.server.service.ServerInfoLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/serverinfolog/htServerInfoLog")
@RequiredArgsConstructor
public class AdminServerInfoLogController {

    private final ServerInfoLogService serverInfoLogService;

    @GetMapping("/list")
    public PageResult<ServerInfoLog> list(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Integer status) {
        Page<ServerInfoLog> result = serverInfoLogService.list(page, size, keyword, status);
        return PageResult.of(result.getContent(), result.getTotalElements(), page, size);
    }

    @GetMapping("/queryById")
    public Result<ServerInfoLog> queryById(@RequestParam Long id) {
        return Result.ok(serverInfoLogService.getById(id));
    }

    @PostMapping("/add")
    public Result<ServerInfoLog> add(@RequestBody ServerInfoLog data) {
        return Result.ok(serverInfoLogService.create(data));
    }

    @RequestMapping(value = "/edit", method = {RequestMethod.PUT, RequestMethod.POST})
    public Result<ServerInfoLog> edit(@RequestBody ServerInfoLog data) {
        if (data.getId() == null) {
            return Result.error(400, "ID不能为空");
        }
        ServerInfoLog old = serverInfoLogService.getById(data.getId());
        old.setServer(data.getServer());
        old.setTask(data.getTask());
        old.setLog(data.getLog());
        old.setStatus(data.getStatus());
        old.setStartTime(data.getStartTime());
        old.setEndTime(data.getEndTime());
        old.setDuration(data.getDuration());
        return Result.ok(serverInfoLogService.create(old));
    }

    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestParam Long id) {
        serverInfoLogService.deleteById(id);
        return Result.ok();
    }

    @DeleteMapping("/deleteBatch")
    public Result<Void> deleteBatch(@RequestParam String ids) {
        for (Long id : java.util.Arrays.stream(ids.split(",")).map(String::trim).filter(s -> !s.isBlank()).map(Long::valueOf).toList()) {
            serverInfoLogService.deleteById(id);
        }
        return Result.ok();
    }
}
