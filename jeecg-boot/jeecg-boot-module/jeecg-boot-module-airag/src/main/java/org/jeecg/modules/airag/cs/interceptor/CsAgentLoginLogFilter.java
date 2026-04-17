package org.jeecg.modules.airag.cs.interceptor;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.cs.constant.CsRedisKeys;
import org.jeecg.modules.airag.cs.entity.CsAgent;
import org.jeecg.modules.airag.cs.entity.CsAgentIpWhitelist;
import org.jeecg.modules.airag.cs.entity.CsAgentLoginLog;
import org.jeecg.modules.airag.cs.entity.CsAgentStatusLog;
import org.jeecg.modules.airag.cs.entity.CsGlobalConfig;
import org.jeecg.modules.airag.cs.mapper.CsAgentIpWhitelistMapper;
import org.jeecg.modules.airag.cs.mapper.CsAgentLoginLogMapper;
import org.jeecg.modules.airag.cs.mapper.CsGlobalConfigMapper;
import org.jeecg.modules.airag.cs.mapper.CsSubAgentMapper;
import org.jeecg.modules.airag.cs.service.ICsAgentService;
import org.jeecg.modules.airag.cs.util.CsIpMatchUtil;
import org.jeecg.modules.airag.cs.util.CsRequestUtil;
import org.jeecg.common.license.core.LicenseClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
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

    @Autowired
    private org.jeecg.modules.airag.cs.service.CsGlobalConfigCache configCache;

    @Autowired
    private ICsAgentService csAgentService;

    @Autowired(required = false)
    private LicenseClientService licenseClientService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
                                        recordLog(username, CsAgentLoginLog.EVENT_IP_BLOCKED, clientIp);
                                        log.warn("[CS-Security] 客服登录被IP白名单拦截: username={}, ip={}", username, clientIp);
                                        invalidateLoginToken(json, username);
                                        rewriteResponse(responseWrapper, "您的IP(" + clientIp + ")不在客服白名单中，禁止登录");
                                        rewritten = true;
                                    } else {
                                        recordLog(username, CsAgentLoginLog.EVENT_LOGIN_SUCCESS, clientIp);
                                        boolean onlineOk = tryAutoAgentOnline(json);
                                        if (!onlineOk) {
                                            invalidateLoginToken(json, username);
                                            Long limit = licenseClientService != null ? licenseClientService.getQuotaLimit("max_cs_agents") : null;
                                            rewriteResponse(responseWrapper, "客服坐席已满，在线坐席数已达授权上限(" + (limit != null ? limit : "?") + ")");
                                            rewritten = true;
                                        }
                                    }
                                } else {
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
        String value = configCache.getOrCacheDefault(
                CsRedisKeys.REDIS_WHITELIST_ENABLED,
                CsRedisKeys.CONFIG_WHITELIST_ENABLED,
                "false");
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

    /**
     * 客服登录成功后自动上线（乐观锁策略：先上线，再检查超限，超限则回滚）。
     * @return true=上线成功或无需处理（非客服/已在线/license未启用），false=坐席超限需阻止登录
     */
    private boolean tryAutoAgentOnline(JSONObject responseJson) {
        try {
            JSONObject result = responseJson.getJSONObject("result");
            if (result == null) return true;
            JSONObject userInfo = result.getJSONObject("userInfo");
            if (userInfo == null) return true;
            String userId = userInfo.getString("id");
            if (oConvertUtils.isEmpty(userId)) return true;

            Boolean csOnlineLogin = result.getBoolean("csOnlineLogin");

            CsAgent agent = csAgentService.getByUserId(userId);
            if (agent == null) return true;

            if (agent.getStatus() != null && agent.getStatus() != CsAgent.STATUS_OFFLINE) {
                log.info("[CS-Security] 客服已在线(status={}), 跳过自动上线: userId={}", agent.getStatus(), userId);
                return true;
            }

            if (Boolean.FALSE.equals(csOnlineLogin)) {
                csAgentService.goOffline(agent.getId());
                log.info("[CS-Security] 客服登录自动隐身: agentId={}", agent.getId());
            } else {
                csAgentService.goOnline(agent.getId());
                log.info("[CS-Security] 客服登录自动上线: agentId={}", agent.getId());
            }

            if (isOverQuota()) {
                log.warn("[CS-Security] 坐席超限（并发竞争），回滚上线: agentId={}, userId={}", agent.getId(), userId);
                csAgentService.goOffline(agent.getId(), CsAgentStatusLog.TRIGGER_SYSTEM);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("[CS-Security] 自动上线异常: {}", e.getMessage(), e);
            return true;
        }
    }

    /**
     * 检查当前在线坐席是否严格超过限额（> limit 才算超限，= limit 是合法的）
     */
    private boolean isOverQuota() {
        if (licenseClientService == null || !licenseClientService.isLicensed()) return false;
        Long limit = licenseClientService.getQuotaLimit("max_cs_agents");
        if (limit == null || limit <= 0) return false;
        try {
            Long currentUsage = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM cs_agent WHERE status != 0", Long.class);
            return currentUsage != null && currentUsage > limit;
        } catch (Exception e) {
            log.error("[CS-Security] 查询坐席数量失败", e);
            return false;
        }
    }

    /**
     * 清除已生成的登录 token；PC 端 single_login 映射仅在与本次发放的 token 一致时再删，避免误清 APP/PHONE 端映射。
     * （本过滤器仅处理 /sys/login，对应 PC 端签发。）
     */
    private void invalidateLoginToken(JSONObject responseJson, String username) {
        try {
            JSONObject result = responseJson.getJSONObject("result");
            if (result == null) return;
            String token = result.getString("token");
            if (oConvertUtils.isNotEmpty(token)) {
                redisTemplate.delete(CommonConstant.PREFIX_USER_TOKEN + token);
                log.info("[CS-Security] 已清除token: {}", token);
            }
            if (oConvertUtils.isNotEmpty(username) && oConvertUtils.isNotEmpty(token)) {
                String pcKey = CommonConstant.PREFIX_USER_TOKEN_PC + username;
                String mapped = redisTemplate.opsForValue().get(pcKey);
                if (token.equals(mapped)) {
                    redisTemplate.delete(pcKey);
                    log.info("[CS-Security] 已按条件清除 PC 单点登录映射: username={}", username);
                }
            }
        } catch (Exception e) {
            log.error("[CS-Security] 清除token失败", e);
        }
    }

    /**
     * 篡改响应为登录失败
     */
    private void rewriteResponse(ContentCachingResponseWrapper responseWrapper, String message) throws IOException {
        JSONObject blocked = new JSONObject();
        blocked.put("success", false);
        blocked.put("code", 500);
        blocked.put("message", message);
        blocked.put("result", null);

        responseWrapper.resetBuffer();
        responseWrapper.setStatus(HttpServletResponse.SC_OK);
        responseWrapper.setContentType("application/json;charset=UTF-8");
        responseWrapper.getWriter().write(blocked.toJSONString());
        responseWrapper.getWriter().flush();
        responseWrapper.copyBodyToResponse();
    }

    private String getClientIp(HttpServletRequest request) {
        return CsRequestUtil.getClientIp(request);
    }
}
