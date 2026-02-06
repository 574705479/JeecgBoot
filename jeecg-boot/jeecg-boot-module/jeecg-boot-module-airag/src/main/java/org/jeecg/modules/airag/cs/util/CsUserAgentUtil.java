package org.jeecg.modules.airag.cs.util;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User-Agent 解析工具类
 * 
 * 从 User-Agent 字符串中提取操作系统和浏览器信息，无需额外依赖。
 *
 * @author jeecg
 * @date 2026-02-06
 */
public class CsUserAgentUtil {

    private CsUserAgentUtil() {
    }

    /**
     * 解析 User-Agent 字符串
     *
     * @param userAgent User-Agent 字符串
     * @return Map 包含: os, osVersion, browser, browserVersion
     */
    public static Map<String, String> parse(String userAgent) {
        Map<String, String> result = new HashMap<>(4);
        result.put("os", "Unknown");
        result.put("osVersion", "");
        result.put("browser", "Unknown");
        result.put("browserVersion", "");

        if (userAgent == null || userAgent.isEmpty()) {
            return result;
        }

        // 解析操作系统
        parseOs(userAgent, result);
        // 解析浏览器
        parseBrowser(userAgent, result);

        return result;
    }

    // ==================== 操作系统解析 ====================

    private static void parseOs(String ua, Map<String, String> result) {
        // Android
        Matcher androidMatcher = Pattern.compile("Android[\\s/]([\\d.]+)").matcher(ua);
        if (androidMatcher.find()) {
            result.put("os", "Android");
            result.put("osVersion", androidMatcher.group(1));
            return;
        }

        // iOS - iPhone
        Matcher iphoneMatcher = Pattern.compile("iPhone OS ([\\d_]+)").matcher(ua);
        if (iphoneMatcher.find()) {
            result.put("os", "iOS");
            result.put("osVersion", iphoneMatcher.group(1).replace('_', '.'));
            return;
        }

        // iOS - iPad
        Matcher ipadMatcher = Pattern.compile("CPU OS ([\\d_]+)").matcher(ua);
        if (ipadMatcher.find()) {
            result.put("os", "iOS");
            result.put("osVersion", ipadMatcher.group(1).replace('_', '.'));
            return;
        }

        // Windows
        Matcher winMatcher = Pattern.compile("Windows NT ([\\d.]+)").matcher(ua);
        if (winMatcher.find()) {
            result.put("os", "Windows");
            String ntVersion = winMatcher.group(1);
            result.put("osVersion", mapWindowsVersion(ntVersion));
            return;
        }

        // macOS
        Matcher macMatcher = Pattern.compile("Mac OS X ([\\d_.]+)").matcher(ua);
        if (macMatcher.find()) {
            result.put("os", "macOS");
            result.put("osVersion", macMatcher.group(1).replace('_', '.'));
            return;
        }

        // ChromeOS
        if (ua.contains("CrOS")) {
            result.put("os", "ChromeOS");
            Matcher crosMatcher = Pattern.compile("CrOS\\s+\\S+\\s+([\\d.]+)").matcher(ua);
            if (crosMatcher.find()) {
                result.put("osVersion", crosMatcher.group(1));
            }
            return;
        }

        // Linux (放在最后，因为 Android UA 也包含 Linux)
        if (ua.contains("Linux")) {
            result.put("os", "Linux");
            return;
        }
    }

    /**
     * Windows NT 版本号映射到用户友好的版本名
     */
    private static String mapWindowsVersion(String ntVersion) {
        switch (ntVersion) {
            case "10.0":
                return "10";
            case "6.3":
                return "8.1";
            case "6.2":
                return "8";
            case "6.1":
                return "7";
            case "6.0":
                return "Vista";
            case "5.1":
            case "5.2":
                return "XP";
            default:
                return ntVersion;
        }
    }

    // ==================== 浏览器解析 ====================

    /**
     * 浏览器解析：注意匹配顺序很重要！
     * Edge UA 中包含 "Edg/"（非 "Edge/"），同时也包含 "Chrome/" 和 "Safari/"
     * Chrome UA 中也包含 "Safari/"
     * 所以优先级: Edge > Opera > Chrome > Firefox > Safari
     */
    private static void parseBrowser(String ua, Map<String, String> result) {
        // Edge (Chromium-based): "Edg/xxx"
        Matcher edgeMatcher = Pattern.compile("Edg/([\\d.]+)").matcher(ua);
        if (edgeMatcher.find()) {
            result.put("browser", "Edge");
            result.put("browserVersion", edgeMatcher.group(1));
            return;
        }

        // Edge (Legacy): "Edge/xxx"
        Matcher edgeLegacyMatcher = Pattern.compile("Edge/([\\d.]+)").matcher(ua);
        if (edgeLegacyMatcher.find()) {
            result.put("browser", "Edge");
            result.put("browserVersion", edgeLegacyMatcher.group(1));
            return;
        }

        // Opera / OPR
        Matcher oprMatcher = Pattern.compile("OPR/([\\d.]+)").matcher(ua);
        if (oprMatcher.find()) {
            result.put("browser", "Opera");
            result.put("browserVersion", oprMatcher.group(1));
            return;
        }
        Matcher operaMatcher = Pattern.compile("Opera/([\\d.]+)").matcher(ua);
        if (operaMatcher.find()) {
            result.put("browser", "Opera");
            result.put("browserVersion", operaMatcher.group(1));
            return;
        }

        // Samsung Browser
        Matcher samsungMatcher = Pattern.compile("SamsungBrowser/([\\d.]+)").matcher(ua);
        if (samsungMatcher.find()) {
            result.put("browser", "Samsung Browser");
            result.put("browserVersion", samsungMatcher.group(1));
            return;
        }

        // UC Browser
        Matcher ucMatcher = Pattern.compile("UCBrowser/([\\d.]+)").matcher(ua);
        if (ucMatcher.find()) {
            result.put("browser", "UC Browser");
            result.put("browserVersion", ucMatcher.group(1));
            return;
        }

        // QQ Browser
        Matcher qqMatcher = Pattern.compile("QQBrowser/([\\d.]+)").matcher(ua);
        if (qqMatcher.find()) {
            result.put("browser", "QQ Browser");
            result.put("browserVersion", qqMatcher.group(1));
            return;
        }

        // WeChat (微信内置浏览器)
        Matcher wechatMatcher = Pattern.compile("MicroMessenger/([\\d.]+)").matcher(ua);
        if (wechatMatcher.find()) {
            result.put("browser", "WeChat");
            result.put("browserVersion", wechatMatcher.group(1));
            return;
        }

        // Chrome (放在特殊浏览器之后)
        Matcher chromeMatcher = Pattern.compile("Chrome/([\\d.]+)").matcher(ua);
        if (chromeMatcher.find()) {
            result.put("browser", "Chrome");
            result.put("browserVersion", chromeMatcher.group(1));
            return;
        }

        // Firefox
        Matcher firefoxMatcher = Pattern.compile("Firefox/([\\d.]+)").matcher(ua);
        if (firefoxMatcher.find()) {
            result.put("browser", "Firefox");
            result.put("browserVersion", firefoxMatcher.group(1));
            return;
        }

        // Safari (放在最后，因为很多浏览器 UA 都包含 Safari)
        if (ua.contains("Safari/") && !ua.contains("Chrome/")) {
            Matcher safariMatcher = Pattern.compile("Version/([\\d.]+)").matcher(ua);
            if (safariMatcher.find()) {
                result.put("browser", "Safari");
                result.put("browserVersion", safariMatcher.group(1));
                return;
            }
            result.put("browser", "Safari");
        }
    }
}
