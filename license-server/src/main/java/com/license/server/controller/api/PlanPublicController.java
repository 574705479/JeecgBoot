package com.license.server.controller.api;

import com.license.server.dto.Result;
import com.license.server.entity.LicensePlan;
import com.license.server.service.LicensePlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanPublicController {

    private final LicensePlanService planService;

    @GetMapping("/public")
    public Result<List<LicensePlan>> listActivePlans(@RequestParam String appId) {
        List<LicensePlan> plans = planService.listActivePlansByAppId(appId);
        return Result.ok(plans);
    }
}
