package com.license.server.controller.admin;

import com.license.server.dto.PageResult;
import com.license.server.dto.Result;
import com.license.server.entity.App;
import com.license.server.service.AppService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/app")
@RequiredArgsConstructor
public class AdminAppController {

    private final AppService appService;

    @GetMapping("/list")
    public PageResult<App> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        Page<App> result = appService.list(page, Math.min(size, 100), keyword);
        return PageResult.of(result.getContent(), result.getTotalElements(), page, size);
    }

    @GetMapping("/{id}")
    public Result<App> getById(@PathVariable Long id) {
        return Result.ok(appService.getById(id));
    }

    @PostMapping
    public Result<App> create(@RequestBody App app) {
        try {
            return Result.ok(appService.create(app));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<App> update(@PathVariable Long id, @RequestBody App app) {
        try {
            return Result.ok(appService.update(id, app));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PostMapping("/{id}/rotate-secret")
    public Result<App> rotateSecret(@PathVariable Long id) {
        return Result.ok(appService.rotateSecret(id));
    }

    @PostMapping("/{id}/generate-keys")
    public Result<App> generateKeys(@PathVariable Long id) {
        return Result.ok(appService.generateKeys(id));
    }
}
