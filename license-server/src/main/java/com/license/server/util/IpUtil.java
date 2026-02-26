package com.license.server.util;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.net.util.SubnetUtils;

import java.util.List;

public final class IpUtil {

    private IpUtil() {}

    public static String getClientIp(HttpServletRequest request, List<String> trustedProxies) {
        String remoteAddr = request.getRemoteAddr();
        if (!isTrustedProxy(remoteAddr, trustedProxies)) {
            return normalizeIp(remoteAddr);
        }
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return normalizeIp(xff.split(",")[0].trim());
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return normalizeIp(realIp);
        }
        return normalizeIp(remoteAddr);
    }

    public static boolean isIpAllowed(String clientIp, List<String> allowedIps) {
        if (allowedIps == null || allowedIps.isEmpty()) {
            return true;
        }
        String normalized = normalizeIp(clientIp);
        for (String allowed : allowedIps) {
            if (allowed.contains("/")) {
                try {
                    SubnetUtils subnet = new SubnetUtils(allowed);
                    subnet.setInclusiveHostCount(true);
                    if (subnet.getInfo().isInRange(normalized)) {
                        return true;
                    }
                } catch (IllegalArgumentException ignored) {
                }
            } else {
                if (normalizeIp(allowed).equals(normalized)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isTrustedProxy(String ip, List<String> trustedProxies) {
        if (trustedProxies == null || trustedProxies.isEmpty()) {
            return false;
        }
        String normalized = normalizeIp(ip);
        for (String proxy : trustedProxies) {
            if (proxy.contains("/")) {
                try {
                    SubnetUtils subnet = new SubnetUtils(proxy);
                    subnet.setInclusiveHostCount(true);
                    if (subnet.getInfo().isInRange(normalized)) {
                        return true;
                    }
                } catch (IllegalArgumentException ignored) {
                }
            } else {
                if (normalizeIp(proxy).equals(normalized)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String normalizeIp(String ip) {
        if (ip == null) return "";
        // Handle IPv6-mapped IPv4: ::ffff:1.2.3.4
        if (ip.startsWith("::ffff:")) {
            return ip.substring(7);
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip.trim();
    }
}
