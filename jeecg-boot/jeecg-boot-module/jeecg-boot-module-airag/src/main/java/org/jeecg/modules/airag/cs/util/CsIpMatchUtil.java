package org.jeecg.modules.airag.cs.util;

import java.net.InetAddress;
import java.util.List;
import java.util.regex.Pattern;

/**
 * IP匹配工具类，支持精确IP和CIDR段匹配
 *
 * @author jeecg
 * @date 2026-02-09
 */
public class CsIpMatchUtil {

    private static final Pattern IP_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");

    private static final Pattern CIDR_PATTERN = Pattern.compile(
            "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)/(\\d|[12]\\d|3[0-2])$");

    private CsIpMatchUtil() {
    }

    /**
     * 判断IP是否匹配规则（精确IP或CIDR段）
     *
     * @param ip      待检查的IP
     * @param pattern 规则（如 "192.168.1.1" 或 "192.168.1.0/24"）
     * @return 是否匹配
     */
    public static boolean matches(String ip, String pattern) {
        if (ip == null || pattern == null) {
            return false;
        }
        String trimmedIp = ip.trim();
        String trimmedPattern = pattern.trim();

        if (trimmedIp.isEmpty() || trimmedPattern.isEmpty()) {
            return false;
        }

        // 精确匹配
        if (!trimmedPattern.contains("/")) {
            return trimmedIp.equals(trimmedPattern);
        }

        // CIDR 匹配
        try {
            String[] parts = trimmedPattern.split("/");
            if (parts.length != 2) {
                return false;
            }
            String networkAddress = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);
            if (prefixLength < 0 || prefixLength > 32) {
                return false;
            }

            byte[] ipBytes = InetAddress.getByName(trimmedIp).getAddress();
            byte[] networkBytes = InetAddress.getByName(networkAddress).getAddress();

            if (ipBytes.length != 4 || networkBytes.length != 4) {
                return false; // 仅支持IPv4
            }

            int ipInt = bytesToInt(ipBytes);
            int networkInt = bytesToInt(networkBytes);
            int mask = prefixLength == 0 ? 0 : (-1 << (32 - prefixLength));

            return (ipInt & mask) == (networkInt & mask);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断IP是否在任意规则中
     */
    public static boolean isInAnyRange(String ip, List<String> patterns) {
        if (ip == null || patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String pattern : patterns) {
            if (matches(ip, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 校验输入是否为有效的IP或CIDR格式
     */
    public static boolean isValidIpOrCidr(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        String trimmed = input.trim();
        return IP_PATTERN.matcher(trimmed).matches() || CIDR_PATTERN.matcher(trimmed).matches();
    }

    /**
     * 判断是否为CIDR段（包含/）
     */
    public static boolean isCidr(String input) {
        if (input == null) {
            return false;
        }
        return CIDR_PATTERN.matcher(input.trim()).matches();
    }

    private static int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24)
                | ((bytes[1] & 0xFF) << 16)
                | ((bytes[2] & 0xFF) << 8)
                | (bytes[3] & 0xFF);
    }
}
