package com.license.server.controller.api;

import com.license.server.config.LicenseServerProperties;
import com.license.server.dto.LicenseRequest;
import com.license.server.dto.LicenseResponse;
import com.license.server.dto.Result;
import com.license.server.entity.App;
import com.license.server.security.RateLimiter;
import com.license.server.service.AppService;
import com.license.server.service.LicenseService;
import com.license.server.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/license")
@RequiredArgsConstructor
public class LicenseApiController {

    private final LicenseService licenseService;
    private final AppService appService;
    private final RateLimiter rateLimiter;
    private final LicenseServerProperties properties;

    @PostMapping("/activate")
    public Result<LicenseResponse> activate(@RequestBody LicenseRequest request, HttpServletRequest httpRequest) {
        String clientIp = IpUtil.getClientIp(httpRequest, properties.getTrustedProxies());

        if (rateLimiter.isLockedOut("activate-key:" + request.getLicenseKey())) {
            return Result.error(40008, "请求过于频繁，请稍后重试");
        }
        if (!rateLimiter.tryAcquirePerMinute("activate-ip:" + clientIp, properties.getRateLimit().getActivatePerIp())) {
            return Result.error(40008, "请求过于频繁，请稍后重试");
        }

        Result<LicenseResponse> authResult = verifyRequest(request);
        if (authResult != null) return authResult;

        return licenseService.activate(request.getAppId(), request.getLicenseKey(), clientIp, request.getCallbackUrl());
    }

    @PostMapping("/verify")
    public Result<LicenseResponse> verify(@RequestBody LicenseRequest request, HttpServletRequest httpRequest) {
        String clientIp = IpUtil.getClientIp(httpRequest, properties.getTrustedProxies());

        if (!rateLimiter.tryAcquirePerMinute("verify-key:" + request.getLicenseKey(), properties.getRateLimit().getVerifyPerKey())) {
            return Result.error(40008, "请求过于频繁，请稍后重试");
        }

        Result<LicenseResponse> authResult = verifyRequest(request);
        if (authResult != null) return authResult;

        return licenseService.verify(request.getAppId(), request.getLicenseKey(), clientIp, request.getCallbackUrl());
    }

    @PostMapping("/deactivate")
    public Result<Void> deactivate(@RequestBody LicenseRequest request, HttpServletRequest httpRequest) {
        String clientIp = IpUtil.getClientIp(httpRequest, properties.getTrustedProxies());

        Result<LicenseResponse> authResult = verifyRequest(request);
        if (authResult != null) return Result.error(authResult.getCode(), authResult.getMessage());

        licenseService.deactivate(request.getAppId(), request.getLicenseKey(), clientIp);
        return Result.ok();
    }

    private Result<LicenseResponse> verifyRequest(LicenseRequest request) {
        if (request.getAppId() == null || request.getLicenseKey() == null
                || request.getTimestamp() == null || request.getSign() == null) {
            return Result.error(40008, "参数不完整");
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(request.getTimestamp());
        } catch (NumberFormatException e) {
            return Result.error(40009, "时间戳格式错误");
        }

        long now = System.currentTimeMillis() / 1000;
        if (Math.abs(now - timestamp) > 300) {
            return Result.error(40009, "时间戳过期");
        }

        App app = appService.getByAppId(request.getAppId());
        if (app == null) return Result.error(40001, "应用不存在");

        String signPayload = request.getAppId() + "\n" + request.getLicenseKey() + "\n" + request.getTimestamp();
        if (!appService.verifyHmac(app, signPayload, request.getSign())) {
            return Result.error(40008, "签名验证失败");
        }

        return null;
    }

    @GetMapping("/domains")
    public Result<java.util.Map<String, Object>> getDomains(
            @RequestParam String licenseKey, HttpServletRequest httpRequest) {
        String clientIp = IpUtil.getClientIp(httpRequest, properties.getTrustedProxies());
        if (!rateLimiter.tryAcquirePerMinute("domains-ip:" + clientIp, 30)) {
            return Result.error(40008, "请求过于频繁");
        }
        return licenseService.getDomainsByKey(licenseKey);
    }

    @GetMapping("/health")
    public Result<Object> health() {
        return Result.ok(java.util.Map.of("status", "UP", "version", "1.0.0"));
    }
}
