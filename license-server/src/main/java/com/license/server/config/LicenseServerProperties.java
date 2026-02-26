package com.license.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "license-server")
public class LicenseServerProperties {

    private String masterKey;

    private List<String> trustedProxies = new ArrayList<>();

    private JwtConfig jwt = new JwtConfig();

    private RateLimitConfig rateLimit = new RateLimitConfig();

    private LogConfig log = new LogConfig();

    @Data
    public static class JwtConfig {
        private long accessTokenExpire = 7200;
        private long refreshTokenExpire = 604800;
    }

    @Data
    public static class RateLimitConfig {
        private int activatePerIp = 5;
        private int activatePerKey = 10;
        private int verifyPerKey = 5;
    }

    @Data
    public static class LogConfig {
        private boolean heartbeatSuccess = false;
        private int retentionDays = 180;
    }
}
