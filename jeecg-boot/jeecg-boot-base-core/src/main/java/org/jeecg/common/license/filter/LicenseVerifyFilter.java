package org.jeecg.common.license.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.common.license.config.LicenseProperties;
import org.jeecg.common.license.constant.LicenseCode;
import org.jeecg.common.license.core.LicenseClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class LicenseVerifyFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(LicenseVerifyFilter.class);

    private final LicenseClientService licenseClientService;
    private final List<String> excludePaths;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile boolean callbackUrlResolved = false;

    public LicenseVerifyFilter(LicenseClientService licenseClientService, LicenseProperties properties) {
        this.licenseClientService = licenseClientService;
        this.excludePaths = properties.getExcludePaths();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if (!callbackUrlResolved) {
            resolveCallbackUrl(req);
        }

        String path = req.getRequestURI();
        String contextPath = req.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        if (isExcluded(path)) {
            chain.doFilter(request, response);
            return;
        }

        if (licenseClientService.isLicensed()) {
            chain.doFilter(request, response);
            return;
        }

        log.debug("[License] Request blocked - system unlicensed: {}", path);
        res.setStatus(200);
        res.setContentType("application/json;charset=UTF-8");
        Map<String, Object> result = new HashMap<>();
        result.put("code", LicenseCode.UNLICENSED);
        result.put("message", "系统未授权");
        result.put("success", false);
        res.getWriter().write(objectMapper.writeValueAsString(result));
    }

    private void resolveCallbackUrl(HttpServletRequest req) {
        try {
            String host = req.getHeader("X-Forwarded-Host");
            if (host == null) host = req.getHeader("Host");
            if (host == null || !host.contains(".")) {
                return;
            }
            callbackUrlResolved = true;

            if (host.contains(",")) host = host.split(",")[0].trim();

            String scheme = req.getHeader("X-Forwarded-Proto");
            if (scheme == null || scheme.isBlank()) scheme = req.getScheme();

            String contextPath = req.getContextPath();
            String callbackUrl = scheme + "://" + host
                    + (contextPath != null ? contextPath : "")
                    + "/license/callback";

            licenseClientService.setResolvedCallbackUrl(callbackUrl);
            log.info("[License] Auto-detected callback URL: {}", callbackUrl);

            CompletableFuture.runAsync(() -> {
                try {
                    licenseClientService.heartbeat();
                } catch (Exception e) {
                    log.debug("[License] Post-detection heartbeat failed: {}", e.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("[License] Failed to auto-detect callback URL: {}", e.getMessage());
        }
    }

    private boolean isExcluded(String path) {
        if (excludePaths == null) return false;
        for (String pattern : excludePaths) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}
