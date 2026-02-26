package com.license.server.controller.admin;

import com.license.server.dto.PageResult;
import com.license.server.dto.Result;
import com.license.server.entity.License;
import com.license.server.service.LicenseService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/license")
@RequiredArgsConstructor
public class AdminLicenseController {

    private final LicenseService licenseService;

    @GetMapping("/list")
    public PageResult<License> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long appPk,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        Page<License> result = licenseService.list(page, Math.min(size, 100), appPk, customerId, status, keyword);
        return PageResult.of(result.getContent(), result.getTotalElements(), page, size);
    }

    @GetMapping("/{id}")
    public Result<License> getById(@PathVariable Long id) {
        return Result.ok(licenseService.getById(id));
    }

    @PostMapping
    public Result<License> create(@RequestBody License license) {
        try {
            return Result.ok(licenseService.create(license));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PostMapping("/{id}/suspend")
    public Result<License> suspend(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long operatorId = (Long) request.getAttribute("userId");
            return Result.ok(licenseService.updateStatus(id, "SUSPENDED", operatorId));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PostMapping("/{id}/revoke")
    public Result<License> revoke(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long operatorId = (Long) request.getAttribute("userId");
            return Result.ok(licenseService.updateStatus(id, "REVOKED", operatorId));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PostMapping("/{id}/restore")
    public Result<License> restore(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long operatorId = (Long) request.getAttribute("userId");
            return Result.ok(licenseService.updateStatus(id, "ACTIVE", operatorId));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PostMapping("/{id}/extend")
    public Result<License> extend(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            Long operatorId = (Long) request.getAttribute("userId");
            LocalDateTime expireDate = LocalDateTime.parse(body.get("expireDate"));
            return Result.ok(licenseService.extend(id, expireDate, operatorId));
        } catch (Exception e) {
            return Result.error(400, e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<License> updateContent(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                          HttpServletRequest request) {
        try {
            Long operatorId = (Long) request.getAttribute("userId");
            return Result.ok(licenseService.updateContent(id, body, operatorId));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    @PutMapping("/{id}/ips")
    public Result<License> updateIps(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                      HttpServletRequest request) {
        try {
            Long operatorId = (Long) request.getAttribute("userId");
            List<String> ips = (List<String>) body.get("allowedIps");
            return Result.ok(licenseService.updateIps(id, ips, operatorId));
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long operatorId = (Long) request.getAttribute("userId");
            licenseService.softDelete(id, operatorId);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }
}
