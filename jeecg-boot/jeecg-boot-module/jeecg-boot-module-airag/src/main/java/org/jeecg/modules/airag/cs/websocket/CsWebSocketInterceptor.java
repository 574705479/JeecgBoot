package org.jeecg.modules.airag.cs.websocket;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.CommonAPI;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.RedisUtil;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.cs.entity.CsAgent;
import org.jeecg.modules.airag.cs.service.ICsAgentService;
import org.jeecg.modules.airag.cs.service.ICsVisitorTokenService;
import org.jeecg.modules.airag.cs.util.CsRequestUtil;
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
    public static final String ATTR_CLIENT_IP = "clientIp";
    public static final String ATTR_USER_AGENT = "userAgent";
    public static final String ATTR_DEVICE_ID = "deviceId";
    public static final String ATTR_USER_LANG = "userLang";

    /** 用户类型：普通用户 */
    public static final String USER_TYPE_USER = "user";
    /** 用户类型：客服 */
    public static final String USER_TYPE_AGENT = "agent";

    @Autowired
    private ICsVisitorTokenService visitorTokenService;

    @Autowired
    private ICsAgentService agentService;

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private CommonAPI commonApi;

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

            // 访客上下文：UA / deviceId / 浏览器语言
            String userAgent = servletRequest.getServletRequest().getHeader("User-Agent");
            String deviceIdParam = servletRequest.getServletRequest().getParameter("deviceId");
            if (oConvertUtils.isEmpty(deviceIdParam)) {
                deviceIdParam = servletRequest.getServletRequest().getHeader("X-Device-Id");
            }
            String userLang = parsePreferredLang(servletRequest.getServletRequest().getHeader("Accept-Language"));

            // 判断用户类型
            String path = request.getURI().getPath();
            if (path.contains("/agent")) {
                userType = USER_TYPE_AGENT;
            } else {
                userType = oConvertUtils.getString(userType, USER_TYPE_USER);
            }

            // 客服连接校验：验证JWT Token
            if (USER_TYPE_AGENT.equals(userType)) {
                String token = servletRequest.getServletRequest().getParameter("token");
                if (oConvertUtils.isEmpty(token)) {
                    token = servletRequest.getServletRequest().getHeader(CommonConstant.X_ACCESS_TOKEN);
                }
                if (oConvertUtils.isEmpty(token)) {
                    log.warn("[CS-WebSocket] 客服握手失败：缺少认证Token");
                    return false;
                }
                String username = JwtUtil.getUsername(token);
                if (oConvertUtils.isEmpty(username)) {
                    log.warn("[CS-WebSocket] 客服握手失败：Token无效");
                    return false;
                }
                Object cacheToken = redisUtil.get(CommonConstant.PREFIX_USER_TOKEN + token);
                if (cacheToken == null) {
                    log.warn("[CS-WebSocket] 客服握手失败：Token已过期, username={}", username);
                    return false;
                }
                // 通过username查找系统用户，再查agent
                CsAgent agent = null;
                try {
                    LoginUser loginUser = commonApi.getUserByName(username);
                    if (loginUser != null) {
                        agent = agentService.getByUserId(loginUser.getId());
                        if (agent == null) {
                            agent = agentService.getByUserId(loginUser.getUsername());
                        }
                    }
                } catch (Exception e) {
                    log.warn("[CS-WebSocket] 查找客服用户异常: username={}", username, e);
                }
                if (agent == null) {
                    log.warn("[CS-WebSocket] 客服握手失败：非客服用户, username={}", username);
                    return false;
                }
                userId = agent.getId();
                userName = agent.getNickname();
                log.info("[CS-WebSocket] 客服身份验证通过: username={}, agentId={}", username, userId);
            }

            // 访客连接校验
            if (USER_TYPE_USER.equals(userType)) {
                // IP黑名单始终检查
                if (visitorTokenService.isIpBlacklisted(clientIp)) {
                    log.warn("[CS-WebSocket] 握手失败：IP已被拉黑");
                    return false;
                }

                if (visitorTokenService.isTokenRequired()) {
                    // === Token模式：必须有有效凭证 ===
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
                } else {
                    // === 免Token模式：设备码作为身份标识 ===
                    // 校验接入密钥
                    if (!visitorTokenService.validateAppKey(servletRequest.getServletRequest())) {
                        log.warn("[CS-WebSocket] 握手失败：接入密钥无效");
                        return false;
                    }
                    String deviceId = deviceIdParam;
                    if (oConvertUtils.isNotEmpty(deviceId)) {
                        userId = deviceId;
                    }
                    if (oConvertUtils.isEmpty(userId)) {
                        log.warn("[CS-WebSocket] 握手失败：免Token模式缺少deviceId/userId");
                        return false;
                    }
                    // 检查设备码/userId是否被拉黑
                    if (visitorTokenService.isBlacklisted(userId)) {
                        log.warn("[CS-WebSocket] 握手失败：访客已被拉黑");
                        return false;
                    }
                    // appId从全局配置获取
                    if (oConvertUtils.isEmpty(appId)) {
                        appId = visitorTokenService.getGlobalVisitorAppId();
                    }
                    // userName可选，后续由createConversation处理默认值
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
            attributes.put(ATTR_CLIENT_IP, clientIp);
            if (oConvertUtils.isNotEmpty(userAgent)) {
                attributes.put(ATTR_USER_AGENT, userAgent);
            }
            if (oConvertUtils.isNotEmpty(deviceIdParam)) {
                attributes.put(ATTR_DEVICE_ID, deviceIdParam);
            }
            if (oConvertUtils.isNotEmpty(userLang)) {
                attributes.put(ATTR_USER_LANG, userLang);
            }

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
        return CsRequestUtil.getClientIp(request);
    }

    /**
     * 解析 Accept-Language 头取首选语言
     * 例: "zh-CN,zh;q=0.9,en;q=0.8" -> "zh-CN"
     */
    private String parsePreferredLang(String acceptLanguage) {
        if (oConvertUtils.isEmpty(acceptLanguage)) {
            return null;
        }
        String first = acceptLanguage.split(",")[0].trim();
        int semi = first.indexOf(';');
        if (semi > 0) {
            first = first.substring(0, semi).trim();
        }
        return oConvertUtils.isEmpty(first) ? null : first;
    }
}
