package com.license.server.controller.admin;

import com.license.server.config.LicenseServerProperties;
import com.license.server.dto.LoginRequest;
import com.license.server.dto.LoginResponse;
import com.license.server.dto.Result;
import com.license.server.service.AdminAuthService;
import com.license.server.util.IpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AdminAuthService authService;
    private final LicenseServerProperties properties;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        try {
            String clientIp = IpUtil.getClientIp(httpRequest, properties.getTrustedProxies());
            LoginResponse response = authService.login(request, clientIp);
            return Result.ok(response);
        } catch (IllegalArgumentException e) {
            return Result.error(401, e.getMessage());
        }
    }

    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestBody Map<String, String> body) {
        try {
            String refreshToken = body.get("refreshToken");
            LoginResponse response = authService.refresh(refreshToken);
            return Result.ok(response);
        } catch (Exception e) {
            return Result.error(401, "refresh token无效或已过期");
        }
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        return Result.ok();
    }

    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody Map<String, String> body, HttpServletRequest request) {
        try {
            Long userId = (Long) request.getAttribute("userId");
            authService.changePassword(userId, body.get("oldPassword"), body.get("newPassword"));
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        }
    }
}
