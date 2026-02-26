package com.license.server.controller.admin;

import com.license.server.dto.PageResult;
import com.license.server.dto.Result;
import com.license.server.entity.LicensePlan;
import com.license.server.service.LicensePlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/plan")
@RequiredArgsConstructor
public class AdminPlanController {

    private final LicensePlanService planService;

    @GetMapping("/list")
    public PageResult<LicensePlan> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long appPk) {
        Page<LicensePlan> result = planService.list(page, Math.min(size, 100), appPk);
        return PageResult.of(result.getContent(), result.getTotalElements(), page, size);
    }

    @GetMapping("/by-app/{appPk}")
    public Result<List<LicensePlan>> listByApp(@PathVariable Long appPk) {
        return Result.ok(planService.listByApp(appPk));
    }

    @GetMapping("/{id}")
    public Result<LicensePlan> getById(@PathVariable Long id) {
        return Result.ok(planService.getById(id));
    }

    @PostMapping
    public Result<LicensePlan> create(@RequestBody LicensePlan plan) {
        try {
            return Result.ok(planService.create(plan));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<LicensePlan> update(@PathVariable Long id, @RequestBody LicensePlan plan) {
        try {
            return Result.ok(planService.update(id, plan));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            planService.softDelete(id);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }
}
