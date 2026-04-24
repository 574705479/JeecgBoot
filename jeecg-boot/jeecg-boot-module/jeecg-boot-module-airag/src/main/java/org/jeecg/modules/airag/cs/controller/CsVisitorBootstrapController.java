package org.jeecg.modules.airag.cs.controller;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.cs.constant.CsRedisKeys;
import org.jeecg.modules.airag.cs.entity.CsBrandConfig;
import org.jeecg.modules.airag.cs.entity.CsConversation;
import org.jeecg.modules.airag.cs.entity.CsLeaveMessage;
import org.jeecg.modules.airag.cs.entity.CsMessage;
import org.jeecg.modules.airag.cs.service.CsGlobalConfigCache;
import org.jeecg.modules.airag.cs.service.ICsAgentService;
import org.jeecg.modules.airag.cs.service.ICsBrandConfigService;
import org.jeecg.modules.airag.cs.service.ICsConversationService;
import org.jeecg.modules.airag.cs.service.ICsLeaveMessageService;
import org.jeecg.modules.airag.cs.service.ICsMessageService;
import org.jeecg.modules.airag.cs.service.ICsVisitorTokenService;
import org.jeecg.modules.airag.cs.util.CsCryptoUtil;
import org.jeecg.modules.airag.cs.util.CsRequestUtil;
import org.jeecg.modules.airag.cs.vo.CsVisitorTokenPayload;
import org.jeecg.modules.airag.cs.vo.VisitorBootstrapVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 访客端首屏 bootstrap 合包接口。
 *
 * <p>把原本访客端首屏需要串行发起的 ~9 个 HTTP 请求合并成 1 个 POST，后端用线程池并发查询、
 * 整体外壳 transport 加密返回。冷启动可把"打开链接 → 看到可聊界面"从 ~1.5-2s 压到 < 800ms。</p>
 *
 * <h3>设计要点</h3>
 * <ol>
 *   <li>同步段：解析鉴权（IP/Token/设备码）→ 决定 visitor identity（必须串行，依赖请求上下文）</li>
 *   <li>并发段：8 个数据源用 {@link CompletableFuture} 并发查询，单段 try-catch 兜底</li>
 *   <li>整体响应只做 1 次 {@link CsCryptoUtil#encryptTransport(String)}（外壳）</li>
 *   <li>嵌套对象内部已 transport 加密的字段（如 {@code conversation.lastMessage}、留言 reply）保持原状，
 *       前端解外壳后照原有逻辑处理，零代码改动</li>
 *   <li>消息 {@code content} 字段保留 storage ENC: 状态，前端按需走 cseDecrypt</li>
 * </ol>
 *
 * <p>失败回退：bootstrap 接口失败时前端自动回退到原有 9 个接口，保证零停机切换。</p>
 *
 * @author jeecg
 * @date 2026-04-23
 */
@Slf4j
@Tag(name = "访客端首屏 bootstrap")
@RestController
@RequestMapping("/airag/cs/visitor")
public class CsVisitorBootstrapController {

    /**
     * Bootstrap 专用线程池：8 个 daemon 线程，命名为 cs-bootstrap-N，足够覆盖 8 段并发查询。
     * 用独立线程池而非 ForkJoinPool.commonPool()，避免与全局任务竞争 + 便于监控/调优。
     */
    private static final ExecutorService BOOTSTRAP_POOL = Executors.newFixedThreadPool(8, new ThreadFactory() {
        private final AtomicInteger seq = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "cs-bootstrap-" + seq.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    });

    /** 单次 bootstrap 总超时（秒）—— 任意单段查询 < 50ms 是常态，3s 兜底足够 */
    private static final long BOOTSTRAP_TIMEOUT_SECONDS = 3L;

    /** 默认首屏拉历史消息条数；上限 100 防止滥用 */
    private static final int DEFAULT_RECENT_LIMIT = 20;
    private static final int MAX_RECENT_LIMIT = 100;

    @Autowired
    private ICsVisitorTokenService visitorTokenService;

    @Autowired
    private CsGlobalConfigCache configCache;

    @Autowired
    private ICsBrandConfigService brandConfigService;

    @Autowired
    private ICsConversationService conversationService;

    @Autowired
    private ICsMessageService messageService;

    @Autowired
    private ICsLeaveMessageService leaveMessageService;

    @Autowired
    private ICsAgentService agentService;

    @Autowired
    private CsCryptoUtil csCryptoUtil;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * POST /airag/cs/visitor/bootstrap
     *
     * <p>请求体（JSON, 全部可选）：</p>
     * <pre>
     * {
     *   "conversationId": "xxx",     // 客户端缓存的会话 ID，命中则复用
     *   "skipCreate":     true,      // true 时只查 active 不主动创建
     *   "recentLimit":    20         // 首屏拉历史消息条数（1-100，默认 20）
     * }
     * </pre>
     *
     * <p>返回：transport 加密后的 {@link VisitorBootstrapVO} JSON 字符串。</p>
     */
    @Operation(summary = "访客端首屏 bootstrap 合包")
    @org.jeecg.config.shiro.IgnoreAuth
    @PostMapping("/bootstrap")
    public Result<String> bootstrap(@RequestBody(required = false) Map<String, Object> body,
                                    HttpServletRequest request) {
        VisitorBootstrapVO vo = new VisitorBootstrapVO();
        vo.setServerTime(System.currentTimeMillis());

        // ===== 同步段：必须依赖请求上下文，串行处理 =====
        String clientIp = CsRequestUtil.getClientIp(request);
        vo.setClientIp(clientIp);

        boolean ipBlocked = visitorTokenService.isIpBlacklisted(clientIp);
        vo.setIpBlocked(ipBlocked);
        if (ipBlocked) {
            vo.setAuthStatus("ip_blocked");
            return ok(vo);
        }

        boolean tokenRequired = visitorTokenService.isTokenRequired();
        vo.setTokenRequired(tokenRequired);

        // 解析访客身份：优先 sessionToken → 短时 token → 免 token 模式下用设备码兜底
        CsVisitorTokenPayload payload = resolveVisitorPayload(request);
        String visitorUserId = null;
        String visitorAppId = null;
        String visitorUserName = null;

        if (payload != null) {
            if (visitorTokenService.isBlacklisted(payload.getExternalUserId())) {
                vo.setUserBlocked(true);
                vo.setAuthStatus("user_blocked");
                return ok(vo);
            }
            visitorUserId = payload.getExternalUserId();
            visitorAppId = payload.getAppId();
            visitorUserName = payload.getUserName();
            vo.setAuthStatus("session_valid");
        } else if (!tokenRequired) {
            // 免 token 模式：先校验接入密钥
            if (!visitorTokenService.validateAppKey(request)) {
                vo.setKeyInvalid(true);
                vo.setAuthStatus("key_invalid");
                return ok(vo);
            }
            String devId = visitorTokenService.extractDeviceId(request);
            if (oConvertUtils.isNotEmpty(devId)) {
                if (visitorTokenService.isBlacklisted(devId)) {
                    vo.setUserBlocked(true);
                    vo.setAuthStatus("user_blocked");
                    return ok(vo);
                }
                visitorUserId = devId;
                // 免 Token 模式下 visitorAppId 沿用全局访客应用配置，
                // 与 CsConversationController / CsWebSocketInterceptor 行为对齐，
                // 避免 fConv 任务里 fAppId=null 导致 getOrCreateConversation 无法命中现有会话
                try {
                    visitorAppId = visitorTokenService.getGlobalVisitorAppId();
                } catch (Exception ignore) {
                    // 非致命：appId 为空时会走默认分支，不影响会话创建
                }
                vo.setAuthStatus("device_valid");
            } else {
                vo.setAuthStatus("no_device_id");
            }
        } else {
            vo.setAuthStatus("no_session");
        }

        // 是否有资格创建/查询会话
        final boolean canCreateConversation = oConvertUtils.isNotEmpty(visitorUserId);

        // 透传给并发线程的 final 引用
        final String fAppId = visitorAppId;
        final String fUserId = visitorUserId;
        final String fUserName = visitorUserName;
        final String fIp = clientIp;
        final String fUa = request.getHeader("User-Agent");
        final String fDeviceId = visitorTokenService.extractDeviceId(request);
        final String fLang = parsePreferredLang(request.getHeader("Accept-Language"));

        // 客户端 hint
        final String lastConvId = body != null ? asString(body.get("conversationId")) : null;
        final boolean skipCreate = body != null && Boolean.TRUE.equals(body.get("skipCreate"));
        final int recentLimit = clamp(parseInt(body, "recentLimit", DEFAULT_RECENT_LIMIT), 1, MAX_RECENT_LIMIT);

        // ===== 并发段：8 个数据源并发查询 =====
        CompletableFuture<Void> fChat = CompletableFuture.runAsync(() -> {
            try {
                String json = configCache.get(CsRedisKeys.REDIS_CHAT_WINDOW, CsRedisKeys.CONFIG_CHAT_WINDOW);
                vo.setChatWindowConfigJson(json != null ? json : "{}");
            } catch (Exception e) {
                log.warn("[CS-Bootstrap] chat-window 加载失败: {}", e.getMessage());
            }
        }, BOOTSTRAP_POOL);

        CompletableFuture<Void> fSensitive = CompletableFuture.runAsync(() -> {
            try {
                String json = configCache.get(CsRedisKeys.REDIS_SENSITIVE_WORDS, CsRedisKeys.CONFIG_SENSITIVE_WORDS);
                vo.setSensitiveWordsJson(json != null ? json : "{\"enabled\":false,\"words\":[]}");
            } catch (Exception e) {
                log.warn("[CS-Bootstrap] sensitive-words 加载失败: {}", e.getMessage());
            }
        }, BOOTSTRAP_POOL);

        CompletableFuture<Void> fBrand = CompletableFuture.runAsync(() -> {
            try {
                vo.setBrandConfigJson(loadBrandConfigJson());
            } catch (Exception e) {
                log.warn("[CS-Bootstrap] brand 加载失败: {}", e.getMessage());
            }
        }, BOOTSTRAP_POOL);

        CompletableFuture<Void> fAi = CompletableFuture.runAsync(() -> {
            try {
                String aiVal = configCache.get(CsRedisKeys.REDIS_AI_ENABLED, CsRedisKeys.CONFIG_AI_ENABLED);
                vo.setAiEnabled(aiVal == null || "true".equalsIgnoreCase(aiVal));
                vo.setVisitorAppId(visitorTokenService.getGlobalVisitorAppId());
            } catch (Exception e) {
                log.warn("[CS-Bootstrap] ai/visitor-app 加载失败: {}", e.getMessage());
            }
        }, BOOTSTRAP_POOL);

        CompletableFuture<Void> fBoard = CompletableFuture.runAsync(() -> {
            try {
                String json = configCache.get(CsRedisKeys.REDIS_MESSAGE_BOARD, CsRedisKeys.CONFIG_MESSAGE_BOARD);
                vo.setMessageBoardConfigJson(json);
            } catch (Exception e) {
                log.warn("[CS-Bootstrap] message-board 加载失败: {}", e.getMessage());
            }
        }, BOOTSTRAP_POOL);

        CompletableFuture<Void> fOnline = CompletableFuture.runAsync(() -> {
            try {
                int count = agentService.countOnlineAgents();
                vo.setAgentOnline(count > 0);
                vo.setAgentOnlineCount(count);
            } catch (Exception e) {
                log.warn("[CS-Bootstrap] online-status 加载失败: {}", e.getMessage());
            }
        }, BOOTSTRAP_POOL);

        CompletableFuture<Void> fConv = CompletableFuture.runAsync(() -> {
            if (!canCreateConversation) {
                return;
            }
            try {
                CsConversation conv;
                if (skipCreate) {
                    conv = conversationService.getActiveConversation(fUserId, fAppId);
                } else {
                    conv = conversationService.getOrCreateConversation(
                            lastConvId, fAppId, fUserId, fUserName,
                            fIp, fUa, fDeviceId, fLang);
                }
                if (conv != null) {
                    encryptConversationFields(conv);
                    vo.setConversation(conv);

                    // 同步拉最近 N 条消息（content 保持 storage 加密形态）
                    try {
                        List<CsMessage> messages = messageService.getMessages(conv.getId(), recentLimit);
                        vo.setRecentMessages(messages);
                        vo.setHasMoreMessages(messages != null && messages.size() >= recentLimit);
                    } catch (Exception e) {
                        log.warn("[CS-Bootstrap] message 拉取失败: convId={}, err={}", conv.getId(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("[CS-Bootstrap] conversation 加载失败: userId={}, err={}", fUserId, e.getMessage());
            }
        }, BOOTSTRAP_POOL);

        CompletableFuture<Void> fUnread = CompletableFuture.runAsync(() -> {
            if (!canCreateConversation) {
                return;
            }
            try {
                List<CsLeaveMessage> replies = leaveMessageService.getUnreadReplies(fUserId);
                if (replies != null) {
                    for (CsLeaveMessage msg : replies) {
                        msg.setPhone(null);
                        msg.setEmail(null);
                        msg.setQq(null);
                        msg.setWechat(null);
                        encryptLeaveMessageForTransport(msg);
                    }
                }
                vo.setUnreadReplies(replies);
            } catch (Exception e) {
                log.warn("[CS-Bootstrap] unread replies 加载失败: userId={}, err={}", fUserId, e.getMessage());
            }
        }, BOOTSTRAP_POOL);

        try {
            CompletableFuture.allOf(fChat, fSensitive, fBrand, fAi, fBoard, fOnline, fConv, fUnread)
                    .get(BOOTSTRAP_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[CS-Bootstrap] 部分子查询超时/失败 (整体不阻塞返回): {}", e.getMessage());
        }

        return ok(vo);
    }

    // ==================== 私有辅助方法 ====================

    /** 把 VO 序列化为 JSON 后整体 transport 加密返回 */
    private Result<String> ok(VisitorBootstrapVO vo) {
        return Result.OK(csCryptoUtil.encryptTransport(JSON.toJSONString(vo)));
    }

    /**
     * 加载品牌配置 JSON。
     *
     * <p>这里直接读 cs_brand_config 表，但 {@link CsBrandConfigController#getBrandConfig()}
     * 同步阶段会经 Redis 缓存（在 backend-cache 任务中实现）；bootstrap 也复用同一个
     * cache 入口（通过 service 内部 redis-first），无需重复一份缓存逻辑。</p>
     */
    private String loadBrandConfigJson() {
        // 先尝试 Redis（与 CsBrandConfigController.getBrandConfig 保持一致）
        try {
            String cached = redisTemplate.opsForValue().get(CsRedisKeys.REDIS_BRAND_CONFIG);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.debug("[CS-Bootstrap] brand redis 读失败回落 DB: {}", e.getMessage());
        }
        QueryWrapper<CsBrandConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("del_flag", 0).eq("status", 1).orderByDesc("update_time");
        List<CsBrandConfig> list = brandConfigService.list(wrapper);
        CsBrandConfig config = list.isEmpty() ? null : list.get(0);
        String json = JSON.toJSONString(config);
        try {
            redisTemplate.opsForValue().set(CsRedisKeys.REDIS_BRAND_CONFIG, json,
                    CsRedisKeys.BRAND_CONFIG_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.debug("[CS-Bootstrap] brand redis 写失败（非致命）: {}", e.getMessage());
        }
        return json;
    }

    /** 与 {@link CsConversationController#encryptConversationFields(CsConversation)} 行为一致 */
    private void encryptConversationFields(CsConversation c) {
        if (c == null) {
            return;
        }
        c.setLastMessage(csCryptoUtil.encryptTransport(c.getLastMessage()));
        c.setSatisfactionComment(csCryptoUtil.encryptTransport(c.getSatisfactionComment()));
    }

    /** 与 {@link CsLeaveMessageController#encryptLeaveMessageForTransport(CsLeaveMessage)} 行为一致 */
    private void encryptLeaveMessageForTransport(CsLeaveMessage msg) {
        if (msg == null) {
            return;
        }
        msg.setContent(csCryptoUtil.encryptTransport(msg.getContent()));
        msg.setReply(csCryptoUtil.encryptTransport(msg.getReply()));
    }

    /** 解析访客身份，优先 sessionToken → 短时 token */
    private CsVisitorTokenPayload resolveVisitorPayload(HttpServletRequest request) {
        String sessionToken = visitorTokenService.extractSessionToken(request);
        if (oConvertUtils.isNotEmpty(sessionToken)) {
            CsVisitorTokenPayload payload = visitorTokenService.parseSessionToken(sessionToken);
            if (payload != null) {
                return payload;
            }
        }
        String shortToken = visitorTokenService.extractToken(request);
        if (oConvertUtils.isNotEmpty(shortToken)) {
            return visitorTokenService.parseToken(shortToken);
        }
        return null;
    }

    /** 解析 Accept-Language 头取首选语言 */
    private String parsePreferredLang(String acceptLanguage) {
        if (acceptLanguage == null || acceptLanguage.isEmpty()) {
            return null;
        }
        String first = acceptLanguage.split(",")[0].trim();
        int semi = first.indexOf(';');
        if (semi > 0) {
            first = first.substring(0, semi).trim();
        }
        return first.isEmpty() ? null : first;
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static int parseInt(Map<String, Object> body, String key, int fallback) {
        if (body == null) {
            return fallback;
        }
        Object v = body.get(key);
        if (v == null) {
            return fallback;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
