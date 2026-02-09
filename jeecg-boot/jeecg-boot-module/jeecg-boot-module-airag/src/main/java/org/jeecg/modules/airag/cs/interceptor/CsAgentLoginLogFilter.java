package org.jeecg.modules.airag.cs.interceptor;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.cs.entity.CsAgentIpWhitelist;
import org.jeecg.modules.airag.cs.entity.CsAgentLoginLog;
import org.jeecg.modules.airag.cs.entity.CsGlobalConfig;
import org.jeecg.modules.airag.cs.mapper.CsAgentIpWhitelistMapper;
import org.jeecg.modules.airag.cs.mapper.CsAgentLoginLogMapper;
import org.jeecg.modules.airag.cs.mapper.CsGlobalConfigMapper;
import org.jeecg.modules.airag.cs.mapper.CsSubAgentMapper;
import org.jeecg.modules.airag.cs.util.CsIpMatchUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 客服登录日志 + 白名单登录拦截 过滤器
 * <p>
 * 监听 POST /sys/login 响应:
 * 1. 如果登录成功且用户是客服角色 → 检查IP白名单 → 不在白名单则篡改响应为失败（阻止拿到token）
 * 2. 记录客服登录日志（成功/失败/IP拦截）
 */
@Slf4j
@Component
@Order(1)
public class CsAgentLoginLogFilter implements Filter {

    private static final String WHITELIST_ENABLED_CONFIG_KEY = "agent_ip_whitelist_enabled";
    private static final String WHITELIST_ENABLED_REDIS_KEY = "cs:global:agent_ip_whitelist_enabled";

    @Autowired
    private CsAgentLoginLogMapper loginLogMapper;

    @Autowired
    private CsSubAgentMapper subAgentMapper;

    @Autowired
    private CsAgentIpWhitelistMapper whitelistMapper;

    @Autowired
    private CsGlobalConfigMapper csGlobalConfigMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        // 只拦截登录请求
        if ("POST".equalsIgnoreCase(method) && path != null && path.endsWith("/sys/login")) {
            ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);
            chain.doFilter(request, responseWrapper);

            String clientIp = getClientIp(httpRequest);
            boolean rewritten = false;

            try {
                String responseBody = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
                if (oConvertUtils.isNotEmpty(responseBody)) {
                    JSONObject json = JSONObject.parseObject(responseBody);
                    if (json != null) {
                        boolean success = json.getBooleanValue("success");
                        String username = extractUsername(httpRequest, json);

                        if (oConvertUtils.isNotEmpty(username)) {
                            boolean isAgent = false;
                            try {
                                isAgent = subAgentMapper.isAgentUser(username) > 0;
                            } catch (Exception e) {
                                log.debug("[CS-Security] 检查客服角色时出错: {}", e.getMessage());
                            }

                            if (isAgent) {
                                if (success) {
                                    // ========== 登录成功 → 检查白名单 ==========
                                    if (isWhitelistBlocked(username, clientIp)) {
                                        // IP不在白名单 → 篡改响应为登录失败，阻止前端拿到token
                                        recordLog(username, CsAgentLoginLog.EVENT_IP_BLOCKED, clientIp);
                                        log.warn("[CS-Security] 客服登录被IP白名单拦截: username={}, ip={}", username, clientIp);

                                        JSONObject blocked = new JSONObject();
                                        blocked.put("success", false);
                                        blocked.put("code", 500);
                                        blocked.put("message", "您的IP(" + clientIp + ")不在客服白名单中，禁止登录");
                                        blocked.put("result", null);

                                        // 清空原始响应体，写入拦截响应
                                        responseWrapper.resetBuffer();
                                        responseWrapper.setStatus(HttpServletResponse.SC_OK);
                                        responseWrapper.setContentType("application/json;charset=UTF-8");
                                        responseWrapper.getWriter().write(blocked.toJSONString());
                                        responseWrapper.getWriter().flush();
                                        responseWrapper.copyBodyToResponse();
                                        rewritten = true;
                                    } else {
                                        // IP通过白名单 → 记录登录成功
                                        recordLog(username, CsAgentLoginLog.EVENT_LOGIN_SUCCESS, clientIp);
                                    }
                                } else {
                                    // 登录失败
                                    recordLog(username, CsAgentLoginLog.EVENT_LOGIN_FAILED, clientIp);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("[CS-Security] 解析登录响应失败: {}", e.getMessage());
            }

            if (!rewritten) {
                responseWrapper.copyBodyToResponse();
            }
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * 检查客服IP是否被白名单拦截
     * 返回 true 表示被拦截（IP不在白名单中），false 表示放行
     */
    private boolean isWhitelistBlocked(String username, String clientIp) {
        // admin 用户不受白名单限制
        if ("admin".equals(username)) {
            return false;
        }
        // 检查开关
        if (!isWhitelistEnabled()) {
            return false;
        }
        // 获取白名单列表
        List<CsAgentIpWhitelist> records = whitelistMapper.selectList(null);
        if (records == null || records.isEmpty()) {
            return false; // 白名单为空时放行
        }
        // 匹配IP
        List<String> ipPatterns = records.stream()
                .map(CsAgentIpWhitelist::getIp)
                .collect(Collectors.toList());
        return !CsIpMatchUtil.isInAnyRange(clientIp, ipPatterns);
    }

    private boolean isWhitelistEnabled() {
        String value = redisTemplate.opsForValue().get(WHITELIST_ENABLED_REDIS_KEY);
        if (value == null) {
            CsGlobalConfig config = csGlobalConfigMapper.selectById(WHITELIST_ENABLED_CONFIG_KEY);
            value = config != null ? config.getConfigValue() : "false";
            redisTemplate.opsForValue().set(WHITELIST_ENABLED_REDIS_KEY, value);
        }
        return "true".equalsIgnoreCase(value);
    }

    private String extractUsername(HttpServletRequest request, JSONObject responseJson) {
        // 先尝试从响应中获取
        if (responseJson != null) {
            JSONObject result = responseJson.getJSONObject("result");
            if (result != null) {
                JSONObject userInfo = result.getJSONObject("userInfo");
                if (userInfo != null) {
                    String name = userInfo.getString("username");
                    if (oConvertUtils.isNotEmpty(name)) {
                        return name;
                    }
                }
            }
        }
        // 尝试从请求参数中获取
        String username = request.getParameter("username");
        if (oConvertUtils.isNotEmpty(username)) {
            return username;
        }
        return null;
    }

    private void recordLog(String username, String event, String ip) {
        try {
            CsAgentLoginLog logRecord = new CsAgentLoginLog();
            logRecord.setLoginDate(new Date());
            logRecord.setUsername(username);
            logRecord.setEvent(event);
            logRecord.setIp(ip);
            logRecord.setCreateTime(new Date());
            loginLogMapper.insert(logRecord);
            log.info("[CS-Security] 客服登录日志: username={}, event={}, ip={}", username, event, ip);
        } catch (Exception e) {
            log.error("[CS-Security] 记录登录日志失败", e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (oConvertUtils.isNotEmpty(ip)) {
            int idx = ip.indexOf(',');
            return idx > -1 ? ip.substring(0, idx).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (oConvertUtils.isNotEmpty(ip)) {
            return ip.trim();
        }
        return request.getRemoteAddr();
    }
}
