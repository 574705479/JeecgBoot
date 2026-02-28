package com.license.server.service;

import com.license.server.dto.LoginRequest;
import com.license.server.dto.LoginResponse;
import com.license.server.entity.AdminUser;
import com.license.server.repository.AdminUserRepository;
import com.license.server.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService implements ApplicationRunner {

    private final AdminUserRepository adminUserRepository;
    private final JwtProvider jwtProvider;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(ApplicationArguments args) {
        if (adminUserRepository.count() == 0) {
            String defaultPassword = "admin123";

            AdminUser admin = new AdminUser();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode(defaultPassword));
            admin.setRealName("超级管理员");
            adminUserRepository.save(admin);

            log.warn("========================================");
            log.warn("初始管理员账号已创建");
            log.warn("用户名: admin");
            log.warn("密码: {}", defaultPassword);
            log.warn("请首次登录后立即修改密码！");
            log.warn("========================================");
        }
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String clientIp) {
        AdminUser user = adminUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("账号已锁定，请30分钟后重试");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            user.setLoginFailCount(user.getLoginFailCount() + 1);
            if (user.getLoginFailCount() >= 5) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
                user.setLoginFailCount(0);
            }
            adminUserRepository.save(user);
            throw new IllegalArgumentException("用户名或密码错误");
        }

        user.setLoginFailCount(0);
        user.setLockedUntil(null);
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(clientIp);
        adminUserRepository.save(user);

        return LoginResponse.builder()
                .accessToken(jwtProvider.createAccessToken(user.getId(), user.getUsername()))
                .refreshToken(jwtProvider.createRefreshToken(user.getId(), user.getUsername()))
                .expiresIn(jwtProvider.getAccessTokenExpire())
                .build();
    }

    public LoginResponse refresh(String refreshToken) {
        var claims = jwtProvider.parseToken(refreshToken);
        if (!"refresh".equals(claims.get("type"))) {
            throw new IllegalArgumentException("无效的refresh token");
        }
        Long userId = Long.parseLong(claims.getSubject());
        String username = (String) claims.get("username");

        jwtProvider.blacklistToken(refreshToken);

        return LoginResponse.builder()
                .accessToken(jwtProvider.createAccessToken(userId, username))
                .refreshToken(jwtProvider.createRefreshToken(userId, username))
                .expiresIn(jwtProvider.getAccessTokenExpire())
                .build();
    }

    public void logout(String refreshToken) {
        jwtProvider.blacklistToken(refreshToken);
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        AdminUser user = adminUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        adminUserRepository.save(user);
    }
}
