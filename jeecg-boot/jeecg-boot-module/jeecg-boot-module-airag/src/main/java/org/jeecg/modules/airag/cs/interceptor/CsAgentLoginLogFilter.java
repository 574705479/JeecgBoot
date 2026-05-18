package org.jeecg.modules.airag.cs.interceptor;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.config.JeecgBaseConfig;
import org.jeecg.config.vo.Firewall;
import org.jeecg.modules.airag.cs.constant.CsRedisKeys;
import org.jeecg.modules.airag.cs.entity.CsAgent;
import org.jeecg.modules.airag.cs.entity.CsAgentIpWhitelist;
import org.jeecg.modules.airag.cs.entity.CsAgentLoginLog;
import org.jeecg.modules.airag.cs.entity.CsAgentStatusLog;
import org.jeecg.modules.airag.cs.mapper.CsAgentIpWhitelistMapper;
import org.jeecg.modules.airag.cs.mapper.CsAgentLoginLogMapper;
import org.jeecg.modules.airag.cs.mapper.CsSubAgentMapper;
import org.jeecg.modules.airag.cs.service.ICsAgentService;
import org.jeecg.modules.airag.cs.util.CsIpMatchUtil;
import org.jeecg.modules.airag.cs.util.CsRequestUtil;
import org.jeecg.modules.airag.cs.websocket.CsWebSocketInterceptor;
import org.jeecg.modules.airag.cs.websocket.CsWebSocketSessionManager;
import org.jeecg.common.license.core.LicenseClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.util.ContentCachingResponseWrapper;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 客服登录日志 + 白名单登录拦截 过滤器
 * <p>
 * 监听 POST /sys/login:
 * 1. <b>请求前</b>：客服角色 + IP 不在白名单 → 直接 rewriteResponse 返回失败，不进入 LoginController，
 *    避免触发框架 SSO 误踢已在线的旧账号。
 * 2. <b>响应后</b>：
 *    - 客服登录成功 → 自动上线（坐席并发竞争乐观回滚）；
 *      若 isConcurrent=false，写入 cs:agent:recent_login Redis 标记（让旧 ws 关闭走 goOfflineIfNotKicked
 *      跳过下线）+ 主动以 4004 close code 关闭旧 /ws/cs/agent 兜底；
 *    - 记录登录日志（成功/失败/IP拦截）。
 */
@Slf4j
@Component
@Order(1)
public class CsAgentLoginLogFilter implements Filter {

    /** 4004：被新登录踢出，CsWebSocketHandler.handleAgentDisconnect 检测到此 code 直接 return 不做下线 */
    private static final int CLOSE_CODE_KICKED_BY_NEW_LOGIN = 4004;

    @Autowired
    private CsAgentLoginLogMapper loginLogMapper;

    @Autowired
    private CsSubAgentMapper subAgentMapper;

    @Autowired
    private CsAgentIpWhitelistMapper whitelistMapper;

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

    @Autowired
    private CsWebSocketSessionManager sessionManager;

    @Autowired
    private JeecgBaseConfig jeecgBaseConfig;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        // 只拦截登录请求
        if ("POST".equalsIgnoreCase(method) && path != null && path.endsWith("/sys/login")) {
            String clientIp = getClientIp(httpRequest);

            // ========== 请求前：白名单前置拦截，避免触发框架 SSO 误踢旧账号 ==========
            HttpServletRequest effectiveRequest = httpRequest;
            String contentType = httpRequest.getContentType();
            boolean isJson = contentType != null && contentType.toLowerCase().contains("application/json");
            if (isJson) {
                try {
                    MultiReadHttpServletRequest wrapped = new MultiReadHttpServletRequest(httpRequest);
                    effectiveRequest = wrapped;
                    String preUsername = parseUsernameFromBody(wrapped);
                    if (oConvertUtils.isNotEmpty(preUsername) && isAgentUser(preUsername)
                            && isWhitelistBlocked(preUsername, clientIp)) {
                        recordLog(preUsername, CsAgentLoginLog.EVENT_IP_BLOCKED, clientIp);
                        log.warn("[CS-Security] 客服登录前置白名单拦截: username={}, ip={}", preUsername, clientIp);
                        writePlainFailure(httpResponse, "您的IP(" + clientIp + ")不在客服白名单中，禁止登录");
                        return;
                    }
                } catch (Exception e) {
                    log.warn("[CS-Security] 前置白名单解析异常，按原流程继续: {}", e.getMessage());
                    effectiveRequest = httpRequest;
                }
            }

            ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(httpResponse);
            chain.doFilter(effectiveRequest, responseWrapper);

            boolean rewritten = false;

            try {
                String responseBody = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
                if (oConvertUtils.isNotEmpty(responseBody)) {
                    JSONObject json = JSONObject.parseObject(responseBody);
                    if (json != null) {
                        boolean success = json.getBooleanValue("success");
                        String username = extractUsername(httpRequest, json);

                        if (oConvertUtils.isNotEmpty(username)) {
                            boolean isAgent = isAgentUser(username);

                            if (isAgent) {
                                if (success) {
                                    recordLog(username, CsAgentLoginLog.EVENT_LOGIN_SUCCESS, clientIp);
                                    CsAgent agent = tryAutoAgentOnline(json);
                                    if (agent == null) {
                                        invalidateLoginToken(json, username);
                                        Long limit = licenseClientService != null ? licenseClientService.getQuotaLimit("max_cs_agents") : null;
                                        rewriteResponse(responseWrapper, "客服坐席已满，在线坐席数已达授权上限(" + (limit != null ? limit : "?") + ")");
                                        rewritten = true;
                                    } else if (isSingleSignOnEnabled()) {
                                        // SSO 挤下线场景：写 recent_login 标记 + 主动兜底关闭旧 /ws/cs/agent
                                        markRecentLogin(agent.getId());
                                        WebSocketSession oldSession = sessionManager.getAgentSession(agent.getId());
                                        if (oldSession != null && oldSession.isOpen()) {
                                            // A5：清根，先删旧端 redis token，避免旧端 ws 用未失效的 token 重连握手成功后抢回 session
                                            try {
                                                Object oldTokenObj = oldSession.getAttributes()
                                                        .get(CsWebSocketInterceptor.ATTR_AGENT_TOKEN);
                                                if (oldTokenObj instanceof String && oConvertUtils.isNotEmpty((String) oldTokenObj)) {
                                                    String oldToken = (String) oldTokenObj;
                                                    redisTemplate.delete(CommonConstant.PREFIX_USER_TOKEN + oldToken);
                                                    log.info("[CS-Security] SSO 互踢同步删除旧 token: agentId={}", agent.getId());
                                                }
                                            } catch (Exception ex) {
                                                log.warn("[CS-Security] SSO 互踢删除旧 token 失败: {}", ex.getMessage());
                                            }
                                            sessionManager.closeAgentSession(
                                                    agent.getId(), oldSession.getId(),
                                                    CLOSE_CODE_KICKED_BY_NEW_LOGIN, "kicked_by_new_login");
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
     * 检查 username 是否为客服角色（封装 try/catch，统一返回 boolean）
     */
    private boolean isAgentUser(String username) {
        try {
            return subAgentMapper.isAgentUser(username) > 0;
        } catch (Exception e) {
            log.debug("[CS-Security] 检查客服角色时出错: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从已缓存的 request body 中解析 username（仅 application/json 请求）
     */
    private String parseUsernameFromBody(MultiReadHttpServletRequest wrapped) {
        try {
            String body = wrapped.getCachedBodyAsString();
            if (oConvertUtils.isEmpty(body)) {
                return null;
            }
            JSONObject json = JSONObject.parseObject(body);
            if (json == null) {
                return null;
            }
            String name = json.getString("username");
            return oConvertUtils.isNotEmpty(name) ? name.trim() : null;
        } catch (Exception e) {
            log.debug("[CS-Security] 解析请求 body username 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 直接写入失败响应（请求前置拦截场景，response 还没被任何 wrapper 包装）
     */
    private void writePlainFailure(HttpServletResponse response, String message) throws IOException {
        JSONObject blocked = new JSONObject();
        blocked.put("success", false);
        blocked.put("code", 500);
        blocked.put("message", message);
        blocked.put("result", null);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(blocked.toJSONString());
        response.getWriter().flush();
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

    /**
     * 是否启用单点登录挤下线（与 LoginController.handleSingleSignOn 行为完全一致）
     */
    private boolean isSingleSignOnEnabled() {
        Firewall firewall = jeecgBaseConfig.getFirewall();
        if (firewall == null || firewall.getIsConcurrent() == null) {
            return false;
        }
        return Boolean.FALSE.equals(firewall.getIsConcurrent());
    }

    /**
     * 写入 cs:agent:recent_login:{agentId} 标记（TTL 30s），
     * 让 CsWebSocketHandler.handleAgentDisconnect 在宽限期到期检查此标记 → 跳过 goOffline。
     * 一次性消费：handleAgentDisconnect 命中后立即 del。
     */
    private void markRecentLogin(String agentId) {
        if (oConvertUtils.isEmpty(agentId)) {
            return;
        }
        try {
            String key = CsRedisKeys.REDIS_AGENT_RECENT_LOGIN_PREFIX + agentId;
            redisTemplate.opsForValue().set(key, "1", CsRedisKeys.RECENT_LOGIN_TTL_SECONDS, TimeUnit.SECONDS);
            log.info("[CS-Security] 写入 recent_login 标记: agentId={}, ttl={}s", agentId, CsRedisKeys.RECENT_LOGIN_TTL_SECONDS);
        } catch (Exception e) {
            log.warn("[CS-Security] 写入 recent_login 标记失败: agentId={}, error={}", agentId, e.getMessage());
        }
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
     *
     * @return 上线成功的 CsAgent；若非客服 / 已在线 / license 未启用 → 返回 agent（不为 null 即可调用方继续）；
     *         若坐席超限需阻止登录 → 返回 null
     */
    private CsAgent tryAutoAgentOnline(JSONObject responseJson) {
        try {
            JSONObject result = responseJson.getJSONObject("result");
            if (result == null) return null;
            JSONObject userInfo = result.getJSONObject("userInfo");
            if (userInfo == null) return null;
            String userId = userInfo.getString("id");
            if (oConvertUtils.isEmpty(userId)) return null;

            Boolean csOnlineLogin = result.getBoolean("csOnlineLogin");

            CsAgent agent = csAgentService.getByUserId(userId);
            if (agent == null) return null;

            if (agent.getStatus() != null && agent.getStatus() != CsAgent.STATUS_OFFLINE) {
                log.info("[CS-Security] 客服已在线(status={}), 跳过自动上线: userId={}", agent.getStatus(), userId);
                return agent;
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
                return null;
            }

            return agent;
        } catch (Exception e) {
            log.error("[CS-Security] 自动上线异常: {}", e.getMessage(), e);
            return null;
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
     * 篡改响应为登录失败（chain.doFilter 之后使用，response 已被 ContentCachingResponseWrapper 包装）
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
