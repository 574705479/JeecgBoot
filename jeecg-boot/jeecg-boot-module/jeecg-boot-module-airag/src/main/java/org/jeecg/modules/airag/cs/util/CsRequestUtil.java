package org.jeecg.modules.airag.cs.util;

import jakarta.servlet.http.HttpServletRequest;
import org.jeecg.common.util.oConvertUtils;
import org.springframework.http.server.ServletServerHttpRequest;

/**
 * CS 模块请求工具类。
 *
 * 注意：本工具刻意不复用 {@link org.jeecg.common.util.IpUtils#getIpAddr}，原因是 IpUtils 没有处理
 * Nginx 常用的 {@code X-Real-IP} 头；为保持 cs 模块原有行为（依赖 X-Real-IP），单独抽取本方法。
 *
 * @author jeecg
 */
public final class CsRequestUtil {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";

    private CsRequestUtil() {
    }

    /**
     * 解析客户端真实 IP，依次读取 {@code X-Forwarded-For} → {@code X-Real-IP} → {@code RemoteAddr}。
     * <p>
     * X-Forwarded-For 取第一个非空段（可能是 "ip1, ip2, ip3" 形式）。
     *
     * @param request 当前 HTTP 请求，可为 null
     * @return 客户端 IP，request 为 null 时返回 null
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String ip = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (oConvertUtils.isNotEmpty(ip)) {
            int idx = ip.indexOf(',');
            return idx > -1 ? ip.substring(0, idx).trim() : ip.trim();
        }
        ip = request.getHeader(HEADER_X_REAL_IP);
        if (oConvertUtils.isNotEmpty(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Spring WebSocket 握手期间使用的 {@link ServletServerHttpRequest} 重载。
     */
    public static String getClientIp(ServletServerHttpRequest request) {
        if (request == null) {
            return null;
        }
        return getClientIp(request.getServletRequest());
    }
}
