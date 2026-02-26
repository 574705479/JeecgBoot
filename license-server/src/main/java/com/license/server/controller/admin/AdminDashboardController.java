package com.license.server.controller.admin;

import com.license.server.dto.Result;
import com.license.server.entity.License;
import com.license.server.service.LicenseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final LicenseService licenseService;

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@RequestParam(required = false) Long appPk) {
        return Result.ok(licenseService.getStats(appPk));
    }

    @GetMapping("/expiring")
    public Result<List<License>> expiring() {
        return Result.ok(licenseService.getExpiringLicenses());
    }

    @GetMapping("/heartbeat-lost")
    public Result<List<License>> heartbeatLost() {
        return Result.ok(licenseService.getHeartbeatLostLicenses());
    }
}
