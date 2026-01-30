package org.jeecg.modules.airag.cs.websocket;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.cs.service.ICsVisitorTokenService;
import org.jeecg.modules.airag.cs.vo.CsVisitorTokenPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket握手拦截器
 * 
 * @author jeecg
 * @date 2026-01-07
 */
@Slf4j
@Component
public class CsWebSocketInterceptor implements HandshakeInterceptor {

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_USER_NAME = "userName";
    public static final String ATTR_USER_TYPE = "userType";
    public static final String ATTR_APP_ID = "appId";
    public static final String ATTR_CONVERSATION_ID = "conversationId";
    public static final String ATTR_TOKEN_EXPIRE_AT = "tokenExpireAt";

    /** 用户类型：普通用户 */
    public static final String USER_TYPE_USER = "user";
    /** 用户类型：客服 */
    public static final String USER_TYPE_AGENT = "agent";

    @Autowired
    private ICsVisitorTokenService visitorTokenService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            String clientIp = getClientIp(servletRequest);
            
            // 获取请求参数
            String userId = servletRequest.getServletRequest().getParameter("userId");
            String userName = servletRequest.getServletRequest().getParameter("userName");
            String userType = servletRequest.getServletRequest().getParameter("userType");
            String appId = servletRequest.getServletRequest().getParameter("appId");
            String conversationId = servletRequest.getServletRequest().getParameter("conversationId");
            String visitorToken = servletRequest.getServletRequest().getParameter("visitorToken");
            String sessionToken = servletRequest.getServletRequest().getParameter("sessionToken");

            // 判断用户类型
            String path = request.getURI().getPath();
            if (path.contains("/agent")) {
                userType = USER_TYPE_AGENT;
            } else {
                userType = oConvertUtils.getString(userType, USER_TYPE_USER);
            }

            // 访客连接需要校验短时token
            if (USER_TYPE_USER.equals(userType)) {
                if (visitorTokenService.isIpBlacklisted(clientIp)) {
                    log.warn("[CS-WebSocket] 握手失败：IP已被拉黑");
                    return false;
                }
                CsVisitorTokenPayload payload = null;
                if (oConvertUtils.isNotEmpty(sessionToken)) {
                    payload = visitorTokenService.parseSessionToken(sessionToken);
                }
                if (payload == null) {
                    payload = visitorTokenService.parseToken(visitorToken);
                }
                if (payload == null) {
                    log.warn("[CS-WebSocket] 握手失败：访客凭证无效或已过期");
                    return false;
                }
                if (visitorTokenService.isBlacklisted(payload.getExternalUserId())) {
                    log.warn("[CS-WebSocket] 握手失败：访客已被拉黑");
                    return false;
                }
                userId = payload.getExternalUserId();
                if (oConvertUtils.isEmpty(userName)) {
                    userName = payload.getUserName();
                }
                appId = payload.getAppId();
                if (payload.getExpireAt() != null) {
                    attributes.put(ATTR_TOKEN_EXPIRE_AT, payload.getExpireAt());
                }
            }

            // 验证必要参数
            if (oConvertUtils.isEmpty(userId)) {
                log.warn("[CS-WebSocket] 握手失败：缺少userId参数");
                return false;
            }

            // 设置属性
            attributes.put(ATTR_USER_ID, userId);
            attributes.put(ATTR_USER_NAME, oConvertUtils.getString(userName, "用户" + userId.substring(0, 6)));
            attributes.put(ATTR_USER_TYPE, userType);
            attributes.put(ATTR_APP_ID, appId);
            attributes.put(ATTR_CONVERSATION_ID, conversationId);

            log.info("[CS-WebSocket] 握手成功: userId={}, userType={}, appId={}", userId, userType, appId);
            return true;
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手完成后的处理
    }

    private String getClientIp(ServletServerHttpRequest request) {
        if (request == null || request.getServletRequest() == null) {
            return null;
        }
        String ip = request.getServletRequest().getHeader("X-Forwarded-For");
        if (oConvertUtils.isNotEmpty(ip)) {
            int idx = ip.indexOf(',');
            return idx > -1 ? ip.substring(0, idx).trim() : ip.trim();
        }
        ip = request.getServletRequest().getHeader("X-Real-IP");
        if (oConvertUtils.isNotEmpty(ip)) {
            return ip.trim();
        }
        return request.getServletRequest().getRemoteAddr();
    }
}
