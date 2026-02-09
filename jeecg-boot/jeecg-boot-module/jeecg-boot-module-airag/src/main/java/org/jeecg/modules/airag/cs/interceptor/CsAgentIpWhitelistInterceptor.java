package org.jeecg.modules.airag.cs.interceptor;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.system.util.JwtUtil;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 客服IP白名单拦截器
 * 拦截客服角色的请求，如果白名单开启且白名单不为空，检查请求IP是否在白名单中
 */
@Slf4j
@Component
public class CsAgentIpWhitelistInterceptor implements HandlerInterceptor {

    private static final String WHITELIST_ENABLED_CONFIG_KEY = "agent_ip_whitelist_enabled";
    private static final String WHITELIST_ENABLED_REDIS_KEY = "cs:global:agent_ip_whitelist_enabled";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private CsGlobalConfigMapper csGlobalConfigMapper;

    @Autowired
    private CsAgentIpWhitelistMapper whitelistMapper;

    @Autowired
    private CsAgentLoginLogMapper loginLogMapper;

    @Autowired
    private CsSubAgentMapper subAgentMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 检查白名单是否开启
        if (!isWhitelistEnabled()) {
            return true;
        }

        // 获取当前登录用户
        String accessToken = request.getHeader("X-Access-Token");
        if (oConvertUtils.isEmpty(accessToken)) {
            return true; // 未登录的请求放行（其他安全机制会处理）
        }

        String username;
        try {
            username = JwtUtil.getUsername(accessToken);
        } catch (Exception e) {
            return true; // token解析失败放行
        }
        if (oConvertUtils.isEmpty(username)) {
            return true;
        }

        // admin 用户不受白名单限制
        if ("admin".equals(username)) {
            return true;
        }

        // 检查是否为客服角色
        if (!isAgentRole(username)) {
            return true; // 非客服角色放行
        }

        // 获取白名单列表
        List<CsAgentIpWhitelist> whitelistRecords = whitelistMapper.selectList(null);
        if (whitelistRecords == null || whitelistRecords.isEmpty()) {
            return true; // 白名单为空时放行
        }

        // 检查IP
        String clientIp = getClientIp(request);
        List<String> ipPatterns = whitelistRecords.stream()
                .map(CsAgentIpWhitelist::getIp)
                .collect(Collectors.toList());

        if (CsIpMatchUtil.isInAnyRange(clientIp, ipPatterns)) {
            return true; // IP在白名单中
        }

        // IP不在白名单中，拦截并记录日志
        recordLoginLog(username, CsAgentLoginLog.EVENT_IP_BLOCKED, clientIp);
        log.warn("[CS-Security] 客服IP白名单拦截: username={}, ip={}", username, clientIp);

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        JSONObject result = new JSONObject();
        result.put("success", false);
        result.put("code", 403);
        result.put("message", "您的IP不在客服白名单中，禁止访问");
        response.getWriter().write(result.toJSONString());
        return false;
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

    private boolean isAgentRole(String username) {
        try {
            return subAgentMapper.isAgentUser(username) > 0;
        } catch (Exception e) {
            log.warn("[CS-Security] 检查客服角色失败: {}", e.getMessage());
            return false;
        }
    }

    private void recordLoginLog(String username, String event, String ip) {
        try {
            CsAgentLoginLog logRecord = new CsAgentLoginLog();
            logRecord.setLoginDate(new Date());
            logRecord.setUsername(username);
            logRecord.setEvent(event);
            logRecord.setIp(ip);
            logRecord.setCreateTime(new Date());
            loginLogMapper.insert(logRecord);
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
