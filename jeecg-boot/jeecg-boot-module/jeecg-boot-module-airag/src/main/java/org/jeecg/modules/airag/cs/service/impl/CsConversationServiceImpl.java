package org.jeecg.modules.airag.cs.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.cs.constant.CsRedisKeys;
import org.jeecg.modules.airag.cs.entity.CsAgent;
import org.jeecg.modules.airag.cs.entity.CsCollaborator;
import org.jeecg.modules.airag.cs.entity.CsConversation;
import org.jeecg.modules.airag.cs.entity.CsGlobalConfig;
import org.jeecg.modules.airag.cs.entity.CsVisitor;
import org.jeecg.modules.airag.cs.mapper.CsCollaboratorMapper;
import org.jeecg.modules.airag.cs.mapper.CsConversationMapper;
import org.jeecg.modules.airag.cs.mapper.CsGlobalConfigMapper;
import org.jeecg.modules.airag.cs.mapper.CsVisitorMapper;
import org.jeecg.modules.airag.cs.service.CsIpGeoService;
import org.jeecg.modules.airag.cs.service.ICsAgentService;
import org.jeecg.modules.airag.cs.service.ICsConversationService;
import org.jeecg.modules.airag.cs.service.ICsMessageService;
import org.jeecg.modules.airag.cs.service.ICsVisitorService;
import org.jeecg.modules.airag.cs.util.CsCryptoUtil;
import org.jeecg.modules.airag.cs.util.CsUserAgentUtil;
import org.jeecg.modules.airag.cs.websocket.CsWebSocketMessage;
import org.jeecg.modules.airag.cs.websocket.CsWebSocketSessionManager;
import org.jeecg.modules.airag.cs.vo.CsAgentWorkloadVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.BeanUtils;

/**
 * 会话管理服务实现 (重构版)
 * 
 * @author jeecg
 * @date 2026-01-12
 */
@Slf4j
@Service
public class CsConversationServiceImpl extends ServiceImpl<CsConversationMapper, CsConversation> 
        implements ICsConversationService {

    @Autowired
    @Lazy
    private ICsAgentService agentService;

    @Autowired
    @Lazy
    private ICsMessageService messageService;

    @Autowired
    private CsCollaboratorMapper collaboratorMapper;

    @Autowired
    private CsWebSocketSessionManager sessionManager;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private CsGlobalConfigMapper csGlobalConfigMapper;

    @Autowired
    private org.jeecg.modules.airag.cs.service.CsGlobalConfigCache configCache;

    @Autowired
    private CsIpGeoService ipGeoService;

    @Autowired
    private CsVisitorMapper csVisitorMapper;

    @Autowired
    @Lazy
    private ICsVisitorService csVisitorService;

    @Autowired
    private CsCryptoUtil csCryptoUtil;

    /**
     * 自注入代理，用于热路径 {@code getOrCreateConversation} 去 {@code @Transactional} 后，
     * 仍能通过代理调用 {@link #createConversation} 让其 {@code @Transactional} 生效。
     *
     * <p>原实现 {@code getOrCreateConversation} 外层带 {@code @Transactional}，
     * 对「conversationId 已存在 → getById 直接返回」这一占 ~99% 的热路径仍会开 read-only 事务，
     * HikariCP 上浪费 2-5ms。拆分后：外层入口零事务，仅在真正需要创建时通过 {@code self} 代理
     * 触发 {@code createConversation} 的事务，延迟分布明显收窄。</p>
     */
    @Autowired
    @Lazy
    private ICsConversationService self;

    // ==================== 热路径会话缓存 ====================

    /**
     * 会话热路径缓存 TTL（2 秒）。
     *
     * <p>Fanout 压测显示：多访客并发对同一客服发消息时，主路径 {@code getOrCreateConversation}
     * 每条都会查 MySQL 一次（conversationId 已存在），10 并发 × 200 条 = 2000 次 SELECT
     * 把本地 MySQL 拉到接近饱和。加 2 秒本地缓存后，同一 conversationId 在 TTL 内仅查一次。</p>
     *
     * <p><b>一致性权衡：</b>关键字段（assignedAgentId / status / replyMode / collaborators）
     * 变更时通过 {@link #invalidateConvCache(String)} 主动失效；其余字段（unread_count、
     * last_message_time 等）最多 stale 2 秒，对业务无感知。转人工 / 关会话等低频写入点已显式
     * invalidate，保证推送路由正确性。</p>
     */
    private static final long CONV_CACHE_TTL_NS = TimeUnit.SECONDS.toNanos(2);

    /** 简单容量保护，避免缓存被海量访客穿透时无限增长。超限时批量回收已过期条目。 */
    private static final int CONV_CACHE_MAX_SIZE = 10_000;

    private static final class ConvCacheEntry {
        final CsConversation value;
        final long expireAtNanos;

        ConvCacheEntry(CsConversation value, long expireAtNanos) {
            this.value = value;
            this.expireAtNanos = expireAtNanos;
        }
    }

    private final ConcurrentHashMap<String, ConvCacheEntry> convHotCache = new ConcurrentHashMap<>();

    /**
     * 热路径 getById，带 2 秒本地缓存。
     *
     * <p>仅用于消息主路径（{@code sendUserMessage} / {@code sendAgentMessage} 经过
     * {@code getOrCreateConversation} 的查询分支）。其它业务查询仍走原 {@link #getById}
     * 以拿到最新数据。</p>
     *
     * <p><b>返回值是浅拷贝</b>：调用方可以安全地 mutate 返回对象（例如
     * {@code CsVisitorBootstrapController.encryptConversationFields}
     * 会改 {@code lastMessage}），不会污染缓存里的原始对象。拷贝走 Spring 反射 copyProperties，
     * 40 字段单次 ~15μs，相比 MySQL 2-5ms 仍有 100× 增益。</p>
     */
    private CsConversation getByIdHot(String conversationId) {
        if (oConvertUtils.isEmpty(conversationId)) {
            return null;
        }
        long now = System.nanoTime();
        ConvCacheEntry entry = convHotCache.get(conversationId);
        if (entry != null && entry.expireAtNanos > now) {
            return cloneConversation(entry.value);
        }
        CsConversation fresh = getById(conversationId);
        if (fresh != null) {
            if (convHotCache.size() > CONV_CACHE_MAX_SIZE) {
                // 超限时顺手清理过期项，避免并发写 map 时爆内存
                long nowForSweep = System.nanoTime();
                convHotCache.entrySet().removeIf(e -> e.getValue().expireAtNanos <= nowForSweep);
            }
            convHotCache.put(conversationId, new ConvCacheEntry(fresh, now + CONV_CACHE_TTL_NS));
            // fresh 是 MyBatis 返回的新对象，首次调用直接给出；但再 cache 里还是留了原引用，
            // 为避免首次调用方 mutate 污染缓存，同样返回拷贝
            return cloneConversation(fresh);
        }
        return null;
    }

    /** 浅拷贝 CsConversation，避免共享引用被调用方 mutate。 */
    private CsConversation cloneConversation(CsConversation src) {
        if (src == null) {
            return null;
        }
        CsConversation copy = new CsConversation();
        BeanUtils.copyProperties(src, copy);
        return copy;
    }

    /**
     * 写路径修改后主动失效热缓存，保证推送路由立即生效。
     *
     * <p>以下方法必须调用（变更对消息路由关键字段）：
     * <ul>
     *   <li>createConversation - 写入新值</li>
     *   <li>endConversation - 改 status</li>
     *   <li>assignOrAcceptAgent / acceptConversation - 改 ownerAgentId + status</li>
     *   <li>transferConversation - 改 ownerAgentId</li>
     *   <li>switchReplyMode - 改 replyMode</li>
     *   <li>addCollaborator / removeCollaborator - 影响推送范围</li>
     * </ul>
     * 其它字段（unread_count / last_message_time / timeoutWarned）变化不 invalidate，
     * 让自然 2 秒 TTL 过期即可。</p>
     */
    private void invalidateConvCache(String conversationId) {
        if (oConvertUtils.isNotEmpty(conversationId)) {
            convHotCache.remove(conversationId);
        }
    }

    // ==================== 会话生命周期 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CsConversation createConversation(String appId, String userId, String userName, String source,
                                             String userIp, String userAgent, String deviceId, String userLang,
                                             String preferredAgentId, String landingPage, String referrerPage) {
        // 监控：IP/UA/deviceId 全为空时打印告警，便于追溯漏网创建链路（同 userId 5 分钟限流）
        warnIfMissingDeviceContext(userId, userIp, userAgent, deviceId);

        // 读取AI开关和对话分配配置
        boolean aiEnabled = isAiEnabled();
        boolean aiPrologueEnabled = isAiPrologueEnabled();
        JSONObject assignConfig = getConversationAssignConfig();

        // 读取 humanAgentEnabled / faqEnabled 配置
        boolean humanAgentEnabled = false;
        boolean faqEnabled = false;
        try {
            String chatWindowJson = redisTemplate.opsForValue().get(CsRedisKeys.REDIS_CHAT_WINDOW);
            if (oConvertUtils.isNotEmpty(chatWindowJson)) {
                JSONObject chatWindowConfig = com.alibaba.fastjson.JSON.parseObject(chatWindowJson);
                Boolean hae = chatWindowConfig.getBoolean("humanAgentEnabled");
                if (hae != null && hae) {
                    humanAgentEnabled = true;
                }
                // visitorMessageConnect 旧值兼容（仅 humanAgentEnabled 未设置时）
                if (hae == null) {
                    Boolean vmc = chatWindowConfig.getBoolean("visitorMessageConnect");
                    if (vmc != null && vmc) {
                        humanAgentEnabled = true;
                    }
                }
                Boolean faqFlag = chatWindowConfig.getBoolean("faqEnabled");
                if (faqFlag != null && faqFlag) {
                    com.alibaba.fastjson.JSONArray faqList = chatWindowConfig.getJSONArray("faqList");
                    faqEnabled = faqList != null && !faqList.isEmpty();
                }
            }
        } catch (Exception e) {
            log.warn("[CS-Conversation] 读取chatWindowSettings失败: {}", e.getMessage());
        }

        CsAgent assignedAgent = null;

        if (humanAgentEnabled) {
            // humanAgentEnabled 模式: 不自动分配客服，等待访客手动转人工
            log.info("[CS-Conversation] humanAgentEnabled模式，跳过客服自动分配");
        } else {
            // 优先使用指定客服
            if (oConvertUtils.isNotEmpty(preferredAgentId)) {
                CsAgent preferred = agentService.getById(preferredAgentId);
                if (preferred != null && preferred.getStatus() != null && preferred.getStatus() == 1) {
                    assignedAgent = preferred;
                    log.info("[CS-Conversation] 使用指定客服: agentId={}", preferredAgentId);
                } else {
                    log.info("[CS-Conversation] 指定客服不可用(不存在或不在线), agentId={}, 回退自动分配", preferredAgentId);
                }
            }

            // 指定客服不可用时，走自动分配
            if (assignedAgent == null) {
                String lastAgentId = null;
                if (assignConfig != null) {
                    JSONObject inherit = assignConfig.getJSONObject("inheritLastAgent");
                    if (inherit != null && inherit.getBooleanValue("enabled")) {
                        int expireMinutes = inherit.getIntValue("expireMinutes");
                        lastAgentId = findLastAgentForUser(userId, expireMinutes);
                    }
                }
                assignedAgent = agentService.assignAgent(lastAgentId);
            }
        }

        // ====== 解析设备信息和IP地理位置 ======
        Map<String, String> uaInfo = CsUserAgentUtil.parse(userAgent);
        Map<String, String> geoInfo = ipGeoService.queryGeoByIp(userIp);

        // 默认用户名逻辑：基于地理位置+IP+设备码后4位生成
        // 前端默认传 "访客"，视为未设置昵称
        String finalUserName = userName;
        if (oConvertUtils.isEmpty(finalUserName) || "访客".equals(finalUserName)) {
            finalUserName = generateDefaultUserName(geoInfo, userIp, deviceId);
        }

        CsConversation conversation = new CsConversation();
        conversation.setAppId(appId);
        conversation.setUserId(userId);
        conversation.setUserName(finalUserName);
        conversation.setSource(source);
        conversation.setUnreadCount(0);
        conversation.setMessageCount(0);
        conversation.setCreateTime(new Date());
        conversation.setLastMessageTime(new Date());

        // 设置设备信息
        conversation.setUserIp(userIp);
        conversation.setUserDevice(userAgent);
        conversation.setUserOs(uaInfo.get("os"));
        conversation.setUserOsVersion(uaInfo.get("osVersion"));
        conversation.setUserBrowser(uaInfo.get("browser"));
        conversation.setUserBrowserVersion(uaInfo.get("browserVersion"));
        conversation.setUserDeviceId(deviceId);

        // 设置地理位置
        conversation.setUserCountry(geoInfo.get("country"));
        conversation.setUserProvince(geoInfo.get("province"));
        conversation.setUserCity(geoInfo.get("city"));

        // 设置浏览器语言
        conversation.setUserLang(userLang);

        // 设置着陆页和来源页
        conversation.setLandingPage(landingPage);
        conversation.setReferrerPage(referrerPage);

        if (humanAgentEnabled) {
            // humanAgentEnabled 模式: 不分配客服，等待用户手动转人工
            conversation.setHumanAgentMode(1);
            conversation.setStatus(CsConversation.STATUS_UNASSIGNED);
            conversation.setReplyMode(CsConversation.REPLY_MODE_MANUAL);
            save(conversation);
            log.info("[CS-Conversation] 创建会话(humanAgent模式): id={}, userId={}, aiEnabled={}", 
                    conversation.getId(), userId, aiEnabled);

            // 广播新会话给所有在线客服（工作台未分配分组可见）
            broadcastNewConversation(conversation);

            // humanAgent模式下的欢迎消息
            if (aiEnabled) {
                try {
                    if (aiPrologueEnabled) {
                        messageService.sendVisitorPrologue(conversation.getId());
                    } else {
                        messageService.sendAutoMessagesAsSystem(conversation.getId(), conversation.getUserLang());
                    }
                } catch (Exception e) {
                    log.warn("[CS-Conversation] humanAgent模式发送欢迎消息失败: {}", e.getMessage());
                }
            } else {
                try {
                    messageService.sendAutoMessagesAsSystem(conversation.getId(), conversation.getUserLang());
                } catch (Exception e) {
                    log.warn("[CS-Conversation] humanAgent模式发送自动消息失败: {}", e.getMessage());
                }
            }

            // FAQ启用时，发送初始FAQ列表消息
            if (faqEnabled) {
                try {
                    messageService.sendInitialFaqMessage(conversation.getId());
                } catch (Exception e) {
                    log.warn("[CS-Conversation] 发送初始FAQ消息失败: {}", e.getMessage());
                }
            }
        } else if (assignedAgent != null) {
            // 有可用客服，直接分配
            conversation.setHumanAgentMode(0);
            conversation.setOwnerAgentId(assignedAgent.getId());
            conversation.setStatus(CsConversation.STATUS_ASSIGNED);
            conversation.setAssignTime(new Date());
            conversation.setReplyMode(aiEnabled ? CsConversation.REPLY_MODE_AI_AUTO : CsConversation.REPLY_MODE_MANUAL);
            
            save(conversation);
            log.info("[CS-Conversation] 创建会话(自动分配): id={}, agentId={}, aiEnabled={}", 
                    conversation.getId(), assignedAgent.getId(), aiEnabled);

            // 创建协作者记录（主负责人）
            CsCollaborator collaborator = new CsCollaborator();
            collaborator.setConversationId(conversation.getId());
            collaborator.setAgentId(assignedAgent.getId());
            collaborator.setRole(CsCollaborator.ROLE_OWNER);
            collaborator.setJoinTime(new Date());
            collaboratorMapper.insert(collaborator);

            // 广播新会话给所有在线客服
            broadcastNewConversation(conversation);

            // 通知用户客服已接入
            notifyUser(conversation.getId(), "agent_connected",
                    "客服 " + assignedAgent.getNickname() + " 为您服务",
                    buildAgentConnectedExtra(conversation.getReplyMode(), assignedAgent));

            // AI欢迎消息分支
            if (aiEnabled) {
                try {
                    if (aiPrologueEnabled) {
                        messageService.sendVisitorPrologue(conversation.getId());
                    } else {
                        messageService.sendVisitorAutoMessagesAsAgent(conversation.getId(),
                                assignedAgent.getId(), assignedAgent.getNickname(), conversation.getUserLang());
                    }
                } catch (Exception e) {
                    log.warn("[CS-Conversation] 发送欢迎消息失败: {}", e.getMessage());
                }
            } else {
                try {
                    messageService.sendAutoMessages(conversation.getId(),
                            assignedAgent.getId(), assignedAgent.getNickname(), conversation.getUserLang());
                } catch (Exception e) {
                    log.warn("[CS-Conversation] 发送自动消息失败: {}", e.getMessage());
                }
            }

            // FAQ启用时，发送初始FAQ列表消息
            if (faqEnabled) {
                try {
                    messageService.sendInitialFaqMessage(conversation.getId());
                } catch (Exception e) {
                    log.warn("[CS-Conversation] 发送初始FAQ消息失败: {}", e.getMessage());
                }
            }
        } else {
            // 无可用客服 → 未分配会话
            // 留言板开启时：前端在 initConversation 之前已直接展示留言板并 return，不会进入此处
            // 留言板关闭时：访客直接进入聊天界面，此时若 FAQ 开启则需发送初始FAQ消息避免聊天界面空白
            conversation.setHumanAgentMode(0);
            conversation.setStatus(CsConversation.STATUS_UNASSIGNED);
            conversation.setReplyMode(CsConversation.REPLY_MODE_MANUAL);
            save(conversation);
            log.info("[CS-Conversation] 创建会话(无在线客服): id={}, userId={}", conversation.getId(), userId);

            if (faqEnabled) {
                try {
                    messageService.sendInitialFaqMessage(conversation.getId());
                } catch (Exception e) {
                    log.warn("[CS-Conversation] 发送初始FAQ消息失败(无客服场景): {}", e.getMessage());
                }
            }
        }

        // 同步访客访问统计：新会话 → visitCount+1 且 conversationCount+1
        // touchVisitor 内部为 REQUIRES_NEW 子事务，独立提交/回滚；try/catch 兜底防影响主流程
        try {
            csVisitorService.touchVisitor(appId, userId, finalUserName, source, true);
        } catch (Exception e) {
            log.warn("[CS-Conversation] 同步访客统计失败: userId={}, err={}", userId, e.getMessage());
        }

        return conversation;
    }

    /**
     * 监控：当 IP / UA / deviceId 三项全空时，打印 WARN 日志便于追溯漏网链路。
     * 同 userId（无 userId 时使用 "_anon"）5 分钟内最多打 1 次，避免日志爆炸。
     */
    private static final java.util.concurrent.ConcurrentHashMap<String, Long> MISSING_CTX_LOG_LIMITER =
            new java.util.concurrent.ConcurrentHashMap<>();
    private static final long MISSING_CTX_LOG_INTERVAL_MS = 5 * 60 * 1000L;

    private void warnIfMissingDeviceContext(String userId, String userIp, String userAgent, String deviceId) {
        if (oConvertUtils.isNotEmpty(userIp) || oConvertUtils.isNotEmpty(userAgent) || oConvertUtils.isNotEmpty(deviceId)) {
            return;
        }
        String key = oConvertUtils.isNotEmpty(userId) ? userId : "_anon";
        long now = System.currentTimeMillis();
        Long last = MISSING_CTX_LOG_LIMITER.get(key);
        if (last != null && (now - last) < MISSING_CTX_LOG_INTERVAL_MS) {
            return;
        }
        MISSING_CTX_LOG_LIMITER.put(key, now);
        // 简易容量保护
        if (MISSING_CTX_LOG_LIMITER.size() > 5000) {
            MISSING_CTX_LOG_LIMITER.entrySet().removeIf(e -> (now - e.getValue()) > MISSING_CTX_LOG_INTERVAL_MS);
        }

        StringBuilder stack = new StringBuilder();
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        int printed = 0;
        for (StackTraceElement el : trace) {
            String cls = el.getClassName();
            if (cls.startsWith("java.") || cls.startsWith("sun.") || cls.contains("CsConversationServiceImpl")) {
                continue;
            }
            stack.append(" <- ").append(cls).append('.').append(el.getMethodName()).append(':').append(el.getLineNumber());
            if (++printed >= 3) break;
        }
        log.warn("[CS-Conversation] 创建会话时设备上下文全空 (IP/UA/deviceId 均为空), userId={}, 调用栈摘要:{}", key, stack);
    }

    /**
     * 根据地理位置信息、IP和设备码生成默认用户名
     * 格式: "省份·城市 (IP) #设备码后4位"
     * 示例: "广东·深圳 (120.24.35.12) #a3f2"、"北京 (10.0.0.1) #b7e1"、"访客 (127.0.0.1) #f1a8"
     */
    private String generateDefaultUserName(Map<String, String> geoInfo, String userIp, String deviceId) {
        StringBuilder sb = new StringBuilder();
        if (geoInfo != null && !geoInfo.isEmpty()) {
            String province = geoInfo.get("province");
            String city = geoInfo.get("city");
            String country = geoInfo.get("country");
            if (oConvertUtils.isNotEmpty(province) && oConvertUtils.isNotEmpty(city)) {
                if (province.equals(city)) {
                    sb.append(city);
                } else {
                    sb.append(province).append("·").append(city);
                }
            } else if (oConvertUtils.isNotEmpty(province)) {
                sb.append(province);
            } else if (oConvertUtils.isNotEmpty(city)) {
                sb.append(city);
            } else if (oConvertUtils.isNotEmpty(country)) {
                sb.append(country);
            }
        }
        if (sb.length() == 0) {
            sb.append("访客");
        }
        if (oConvertUtils.isNotEmpty(userIp)) {
            sb.append(" (").append(userIp).append(")");
        }
        if (oConvertUtils.isNotEmpty(deviceId) && deviceId.length() >= 4) {
            sb.append(" #").append(deviceId.substring(deviceId.length() - 4));
        }
        return sb.toString();
    }

    /**
     * 查找用户上一次会话的客服ID（在有效期内）
     * 
     * 查询范围：已分配(ASSIGNED)和已结束(CLOSED)的会话，排除未分配的
     * 排序依据：优先用endTime，endTime为空时用lastMessageTime，最后用createTime
     */
    private String findLastAgentForUser(String userId, int expireMinutes) {
        try {
            if (expireMinutes <= 0) {
                // 有效期为0表示永远继承
                LambdaQueryWrapper<CsConversation> query = new LambdaQueryWrapper<>();
                query.eq(CsConversation::getUserId, userId)
                        .in(CsConversation::getStatus, CsConversation.STATUS_ASSIGNED, CsConversation.STATUS_CLOSED)
                        .isNotNull(CsConversation::getOwnerAgentId)
                        .orderByDesc(CsConversation::getLastMessageTime)
                        .last("LIMIT 1");
                CsConversation lastConv = getOne(query);
                if (lastConv != null) {
                    log.info("[CS-Conversation] 找到用户上次客服(永久继承): userId={}, lastAgentId={}, convId={}", 
                            userId, lastConv.getOwnerAgentId(), lastConv.getId());
                }
                return lastConv != null ? lastConv.getOwnerAgentId() : null;
            }
            
            Date expireTime = new Date(System.currentTimeMillis() - (long) expireMinutes * 60 * 1000);
            LambdaQueryWrapper<CsConversation> query = new LambdaQueryWrapper<>();
            query.eq(CsConversation::getUserId, userId)
                    .in(CsConversation::getStatus, CsConversation.STATUS_ASSIGNED, CsConversation.STATUS_CLOSED)
                    .isNotNull(CsConversation::getOwnerAgentId)
                    // 有效期判断：endTime或lastMessageTime在有效期内
                    .and(w -> w
                            .gt(CsConversation::getEndTime, expireTime)
                            .or()
                            .gt(CsConversation::getLastMessageTime, expireTime)
                    )
                    .orderByDesc(CsConversation::getLastMessageTime)
                    .last("LIMIT 1");
            CsConversation lastConv = getOne(query);
            if (lastConv != null) {
                log.info("[CS-Conversation] 找到用户上次客服(有效期{}分钟内): userId={}, lastAgentId={}, convId={}", 
                        expireMinutes, userId, lastConv.getOwnerAgentId(), lastConv.getId());
            } else {
                log.info("[CS-Conversation] 未找到用户上次客服(有效期{}分钟内): userId={}", expireMinutes, userId);
            }
            return lastConv != null ? lastConv.getOwnerAgentId() : null;
        } catch (Exception e) {
            log.warn("[CS-Conversation] 查找用户上次客服失败: userId={}", userId, e);
            return null;
        }
    }

    /**
     * 读取AI开关状态
     */
    private boolean isAiEnabled() {
        try {
            String value = configCache.get(CsRedisKeys.REDIS_AI_ENABLED, CsRedisKeys.CONFIG_AI_ENABLED);
            return value == null || "true".equalsIgnoreCase(value);
        } catch (Exception e) {
            log.warn("[CS-Conversation] 读取AI开关失败", e);
            return true; // 默认开启
        }
    }

    /**
     * 读取AI开场白开关状态
     */
    private boolean isAiPrologueEnabled() {
        try {
            String value = configCache.get(CsRedisKeys.REDIS_AI_PROLOGUE_ENABLED, CsRedisKeys.CONFIG_AI_PROLOGUE_ENABLED);
            return value == null || "true".equalsIgnoreCase(value);
        } catch (Exception e) {
            log.warn("[CS-Conversation] 读取AI开场白开关失败", e);
            return true;
        }
    }

    /**
     * 读取对话分配配置
     */
    private JSONObject getConversationAssignConfig() {
        try {
            String json = configCache.get(CsRedisKeys.REDIS_CONVERSATION_ASSIGN, CsRedisKeys.CONFIG_CONVERSATION_ASSIGN);
            if (json != null && !json.isEmpty()) {
                return JSONObject.parseObject(json);
            }
        } catch (Exception e) {
            log.warn("[CS-Conversation] 读取对话分配配置失败", e);
        }
        return null;
    }
    
    /**
     * 广播新会话给所有在线客服
     */
    private void broadcastNewConversation(CsConversation conversation) {
        try {
            java.util.Map<String, Object> extra = new java.util.HashMap<>();
            extra.put("appId", conversation.getAppId());
            extra.put("userName", conversation.getUserName());
            extra.put("createTime", conversation.getCreateTime());
            extra.put("status", conversation.getStatus());
            extra.put("replyMode", conversation.getReplyMode());
            extra.put("ownerAgentId", conversation.getOwnerAgentId());
            // 设备信息
            extra.put("userIp", conversation.getUserIp());
            extra.put("userOs", conversation.getUserOs());
            extra.put("userOsVersion", conversation.getUserOsVersion());
            extra.put("userBrowser", conversation.getUserBrowser());
            extra.put("userBrowserVersion", conversation.getUserBrowserVersion());
            extra.put("userDeviceId", conversation.getUserDeviceId());
            // 地理位置
            extra.put("userCountry", conversation.getUserCountry());
            extra.put("userProvince", conversation.getUserProvince());
            extra.put("userCity", conversation.getUserCity());
            // 浏览器语言
            extra.put("userLang", conversation.getUserLang());
            // 访客备注信息（老用户回来时直接带上）
            try {
                CsVisitor visitor = csVisitorMapper.selectByUserId(conversation.getUserId());
                if (visitor != null) {
                    extra.put("visitorNickname", visitor.getNickname());
                    extra.put("visitorStar", visitor.getStar());
                    extra.put("visitorStarTime", visitor.getStarTime());
                }
            } catch (Exception ex) {
                log.debug("[CS-Conversation] 查询访客信息失败: {}", ex.getMessage());
            }
            
            CsWebSocketMessage notification = CsWebSocketMessage.builder()
                    .type(CsWebSocketMessage.TYPE_NEW_CONVERSATION)
                    .conversationId(conversation.getId())
                    .senderId(conversation.getUserId())
                    .senderName(conversation.getUserName())
                    .content(csCryptoUtil.encryptTransport("新会话"))
                    .extra(extra)
                    .build();
            sessionManager.sendToAllAgents(notification);
            log.info("[CS-Conversation] 广播新会话给所有客服: conversationId={}", conversation.getId());
        } catch (Exception e) {
            log.warn("[CS-Conversation] 广播新会话失败: {}", e.getMessage());
        }
    }

    @Override
    public CsConversation getOrCreateConversation(String conversationId, String appId, String userId, String userName) {
        return getOrCreateConversation(conversationId, appId, userId, userName, null, null, null, null);
    }

    /**
     * 查询或创建会话。
     *
     * <p><b>热路径（99% 情况）：</b>传入的 {@code conversationId} 已存在，走一次 {@code getById}
     * 立即返回，无事务、无 Redis 锁，p99 主要取决于 MyBatis 查询（命中 L1/MP 缓存时 &lt;1ms）。</p>
     *
     * <p><b>冷路径：</b>需要实际创建时，通过自注入代理 {@link #self} 调用
     * {@link #createConversation} 触发 {@code @Transactional} 代理。避免了原实现中
     * 外层 {@code @Transactional} 覆盖所有查询路径导致的 HikariCP 事务开销。</p>
     *
     * <p>并发创建保护仍由 Redis 分布式锁 + 30ms×10 重试 + 双重检查完成。</p>
     */
    @Override
    public CsConversation getOrCreateConversation(String conversationId, String appId, String userId, String userName,
                                                  String userIp, String userAgent, String deviceId, String userLang) {
        if (oConvertUtils.isNotEmpty(conversationId)) {
            CsConversation existing = getByIdHot(conversationId);
            if (existing != null) {
                return existing;
            }
            return self.createConversation(appId, userId, userName, null,
                    userIp, userAgent, deviceId, userLang, null, null, null);
        }

        CsConversation active = getActiveConversation(userId, appId);
        if (active != null) {
            return active;
        }

        String lockKey = "cs:lock:create_conv:" + userId + ":" + appId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(10));
        if (locked == null || !locked) {
            active = null;
            for (int attempt = 0; attempt < 10; attempt++) {
                try {
                    Thread.sleep(30);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                }
                active = getActiveConversation(userId, appId);
                if (active != null) {
                    return active;
                }
            }
            log.warn("[CS-Conversation] 未获取到分布式锁，轻量重试后仍未找到活跃会话，强制创建: userId={}", userId);
        }
        try {
            active = getActiveConversation(userId, appId);
            if (active != null) {
                return active;
            }
            return self.createConversation(appId, userId, userName, null,
                    userIp, userAgent, deviceId, userLang, null, null, null);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    @Override
    public CsConversation getConversation(String conversationId) {
        if (oConvertUtils.isEmpty(conversationId)) {
            return null;
        }
        
        CsConversation conversation = getById(conversationId);
        if (conversation != null) {
            // 加载协作者列表
            List<CsCollaborator> collaborators = collaboratorMapper.selectActiveCollaborators(conversationId);
            conversation.setCollaborators(collaborators);
            
            // 加载主负责客服信息
            if (oConvertUtils.isNotEmpty(conversation.getOwnerAgentId())) {
                CsAgent agent = agentService.getById(conversation.getOwnerAgentId());
                if (agent != null) {
                    conversation.setOwnerAgentName(agent.getNickname());
                    conversation.setOwnerAgentAvatar(agent.getAvatar());
                }
            }
        }
        
        return conversation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignToAgent(String conversationId, String agentId) {
        log.info("[CS-Conversation] 客服接入会话: conversationId={}, agentId={}", conversationId, agentId);
        
        CsConversation conversation = getById(conversationId);
        if (conversation == null) {
            log.warn("[CS-Conversation] 会话不存在，无法接入: conversationId={}", conversationId);
            return false;
        }
        
        // 检查客服状态
        CsAgent agent = agentService.getById(agentId);
        if (agent == null) {
            log.warn("[CS-Conversation] 客服不存在: agentId={}", agentId);
            return false;
        }
        
        // 如果客服离线，自动上线
        if (agent.getStatus() != CsAgent.STATUS_ONLINE) {
            agentService.goOnline(agentId);
        }
        
        boolean alreadyAssignedToSame = conversation.getStatus() == CsConversation.STATUS_ASSIGNED
                && oConvertUtils.isNotEmpty(conversation.getOwnerAgentId())
                && conversation.getOwnerAgentId().equals(agentId);

        // 检查是否已被其他客服接入
        if (!alreadyAssignedToSame
                && conversation.getStatus() == CsConversation.STATUS_ASSIGNED 
                && oConvertUtils.isNotEmpty(conversation.getOwnerAgentId())
                && !conversation.getOwnerAgentId().equals(agentId)) {
            log.warn("[CS-Conversation] 会话已被其他客服接入: conversationId={}", conversationId);
            return false;
        }
        
        // 更新会话状态（已是本人接入则跳过重复更新）
        if (!alreadyAssignedToSame) {
            conversation.setOwnerAgentId(agentId);
            conversation.setStatus(CsConversation.STATUS_ASSIGNED);
            conversation.setAssignTime(new Date());
            conversation.setUpdateTime(new Date());
            // ★ 客服接入后切换为手动模式，终止AI自动回复
            conversation.setReplyMode(CsConversation.REPLY_MODE_MANUAL);
            updateById(conversation);
            // 关键字段变更（ownerAgentId/status/replyMode），主动失效热缓存
            invalidateConvCache(conversationId);
        }
        
        // 创建或更新协作者记录（主负责人）
        LambdaQueryWrapper<CsCollaborator> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(CsCollaborator::getConversationId, conversationId)
                .eq(CsCollaborator::getAgentId, agentId);
        CsCollaborator existingCollaborator = collaboratorMapper.selectOne(checkWrapper);
        if (existingCollaborator != null) {
            existingCollaborator.setRole(CsCollaborator.ROLE_OWNER);
            existingCollaborator.setJoinTime(new Date());
            existingCollaborator.setLeaveTime(null);
            collaboratorMapper.updateById(existingCollaborator);
        } else {
            CsCollaborator collaborator = new CsCollaborator();
            collaborator.setConversationId(conversationId);
            collaborator.setAgentId(agentId);
            collaborator.setRole(CsCollaborator.ROLE_OWNER);
            collaborator.setJoinTime(new Date());
            collaboratorMapper.insert(collaborator);
        }
        
        if (!alreadyAssignedToSame) {
            // 更新客服会话数
            agentService.incrementSessions(agentId);
            
            // 广播会话被接入事件给所有客服（实时推送）
            broadcastToAllAgents("conversation_assigned", buildConversationAssignedData(conversationId, agent));

            // 通知用户客服已接入，同时告知已切换为手动模式
            notifyUser(conversationId, "agent_connected", "客服 " + agent.getNickname() + " 为您服务",
                    buildAgentConnectedExtra(CsConversation.REPLY_MODE_MANUAL, agent));
        }
        
        log.info("[CS-Conversation] 客服接入成功: conversationId={}, agentId={}", conversationId, agentId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeConversation(String conversationId) {
        closeConversation(conversationId, "会话已结束", CsConversation.END_TYPE_AGENT_CLOSE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeConversation(String conversationId, String reason) {
        closeConversation(conversationId, reason, CsConversation.END_TYPE_AGENT_CLOSE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeConversation(String conversationId, String reason, Integer endType) {
        log.info("[CS-Conversation] 结束会话: conversationId={}, reason={}, endType={}", conversationId, reason, endType);
        
        CsConversation conversation = getById(conversationId);
        if (conversation == null) {
            return;
        }
        
        // 如果会话已经结束，直接返回
        if (conversation.getStatus() == CsConversation.STATUS_CLOSED) {
            log.info("[CS-Conversation] 会话已经结束，跳过: conversationId={}", conversationId);
            return;
        }
        
        // 更新会话状态为已结束
        conversation.setStatus(CsConversation.STATUS_CLOSED);
        conversation.setEndTime(new Date());
        conversation.setUpdateTime(new Date());
        if (endType != null) {
            conversation.setEndType(endType);
        }
        updateById(conversation);
        // 关键字段变更（status=CLOSED），主动失效热缓存避免旧值继续路由消息
        invalidateConvCache(conversationId);
        
        // 减少客服会话数 & 增加累计服务数
        if (oConvertUtils.isNotEmpty(conversation.getOwnerAgentId())) {
            agentService.decrementSessions(conversation.getOwnerAgentId());
            agentService.incrementTotalServed(conversation.getOwnerAgentId());
        }
        
        // 通知用户会话已结束（不持久化系统消息，只做WebSocket通知）
        notifyUser(conversationId, "conversation_closed", reason);
        
        // ★ 广播会话结束事件给所有客服（让其他客服也能实时更新）
        broadcastConversationClosed(conversation, reason);
    }
    
    /**
     * 广播会话结束给所有客服
     */
    private void broadcastConversationClosed(CsConversation conversation, String reason) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("conversationId", conversation.getId());
            data.put("reason", reason);
            data.put("endTime", new Date());
            data.put("ownerAgentId", conversation.getOwnerAgentId());
            
            CsAgent agent = null;
            if (oConvertUtils.isNotEmpty(conversation.getOwnerAgentId())) {
                agent = agentService.getById(conversation.getOwnerAgentId());
            }
            if (agent != null) {
                data.put("ownerAgentName", agent.getNickname());
            }
            
            broadcastToAllAgents("conversation_closed", data);
            log.info("[CS-Conversation] 广播会话结束给所有客服: conversationId={}", conversation.getId());
        } catch (Exception e) {
            log.warn("[CS-Conversation] 广播会话结束失败: {}", e.getMessage());
        }
    }

    // ==================== 回复模式管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changeReplyMode(String conversationId, int replyMode) {
        log.info("[CS-Conversation] 切换回复模式: conversationId={}, replyMode={}", conversationId, replyMode);
        
        if (replyMode < 0 || replyMode > 2) {
            return false;
        }
        
        LambdaUpdateWrapper<CsConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CsConversation::getId, conversationId)
                .set(CsConversation::getReplyMode, replyMode)
                .set(CsConversation::getUpdateTime, new Date());
        
        boolean success = update(updateWrapper);
        
        if (success) {
            // 关键字段变更（replyMode），主动失效热缓存确保下一条消息按新模式路由
            invalidateConvCache(conversationId);
            String modeName = replyMode == CsConversation.REPLY_MODE_AI_AUTO ? "AI自动回复" : 
                    (replyMode == CsConversation.REPLY_MODE_MANUAL ? "人工服务" : "AI辅助");
            
            // ★ 通知用户模式已切换（带replyMode参数）
            Map<String, Object> extra = new HashMap<>();
            extra.put("replyMode", replyMode);
            notifyUser(conversationId, "mode_changed", modeName, extra);
            
            // ★ 广播模式切换给所有客服
            CsConversation conversation = getById(conversationId);
            broadcastModeChanged(conversation, replyMode, modeName);
        }
        
        return success;
    }
    
    /**
     * 广播模式切换给所有客服
     */
    private void broadcastModeChanged(CsConversation conversation, int newMode, String modeName) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("conversationId", conversation.getId());
            data.put("newMode", newMode);
            data.put("modeName", modeName);
            data.put("ownerAgentId", conversation.getOwnerAgentId());
            
            CsAgent agent = null;
            if (oConvertUtils.isNotEmpty(conversation.getOwnerAgentId())) {
                agent = agentService.getById(conversation.getOwnerAgentId());
            }
            if (agent != null) {
                data.put("ownerAgentName", agent.getNickname());
            }
            
            broadcastToAllAgents("mode_changed", data);
            log.info("[CS-Conversation] 广播模式切换给所有客服: conversationId={}, mode={}", 
                    conversation.getId(), modeName);
        } catch (Exception e) {
            log.warn("[CS-Conversation] 广播模式切换失败: {}", e.getMessage());
        }
    }

    @Override
    public int getReplyMode(String conversationId) {
        CsConversation conversation = getById(conversationId);
        if (conversation == null) {
            return CsConversation.REPLY_MODE_AI_AUTO;
        }
        return conversation.getReplyMode() != null ? conversation.getReplyMode() : CsConversation.REPLY_MODE_AI_AUTO;
    }

    // ==================== 会话移交 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean transferTo(String conversationId, String toAgentId, String fromAgentId) {
        log.info("[CS-Conversation] 移交会话: conversationId={}, from={}, to={}", 
                conversationId, fromAgentId, toAgentId);
        
        CsConversation conversation = getById(conversationId);
        if (conversation == null) {
            return false;
        }
        
        // 校验发起转接的客服是否为会话当前负责人
        if (oConvertUtils.isNotEmpty(fromAgentId) && oConvertUtils.isNotEmpty(conversation.getOwnerAgentId())
                && !conversation.getOwnerAgentId().equals(fromAgentId)) {
            log.warn("[CS-Conversation] 非当前负责人发起转接: conversationId={}, ownerAgentId={}, fromAgentId={}",
                    conversationId, conversation.getOwnerAgentId(), fromAgentId);
            return false;
        }
        
        // 检查目标客服
        CsAgent toAgent = agentService.getById(toAgentId);
        if (toAgent == null) {
            log.warn("[CS-Conversation] 目标客服不存在: agentId={}", toAgentId);
            return false;
        }
        
        // 如果目标客服离线，自动上线
        if (toAgent.getStatus() != CsAgent.STATUS_ONLINE) {
            agentService.goOnline(toAgentId);
        }
        
        // 更新原负责人的协作记录
        if (oConvertUtils.isNotEmpty(fromAgentId)) {
            LambdaUpdateWrapper<CsCollaborator> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(CsCollaborator::getConversationId, conversationId)
                    .eq(CsCollaborator::getAgentId, fromAgentId)
                    .isNull(CsCollaborator::getLeaveTime)
                    .set(CsCollaborator::getLeaveTime, new Date())
                    .set(CsCollaborator::getRole, CsCollaborator.ROLE_COLLABORATOR);
            collaboratorMapper.update(null, updateWrapper);
            
            // 减少原客服会话数
            agentService.decrementSessions(fromAgentId);
        }
        
        boolean wasUnassigned = conversation.getStatus() == CsConversation.STATUS_UNASSIGNED
                || oConvertUtils.isEmpty(conversation.getOwnerAgentId());

        // 更新会话
        conversation.setOwnerAgentId(toAgentId);
        if (wasUnassigned) {
            conversation.setStatus(CsConversation.STATUS_ASSIGNED);
            conversation.setAssignTime(new Date());
            conversation.setReplyMode(CsConversation.REPLY_MODE_MANUAL);
        }
        conversation.setUpdateTime(new Date());
        updateById(conversation);
        // 关键字段变更（ownerAgentId/status/replyMode），主动失效热缓存
        invalidateConvCache(conversationId);
        
        // ★ 检查目标客服是否已有协作记录
        LambdaQueryWrapper<CsCollaborator> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(CsCollaborator::getConversationId, conversationId)
                .eq(CsCollaborator::getAgentId, toAgentId);
        CsCollaborator existingCollaborator = collaboratorMapper.selectOne(checkWrapper);
        
        if (existingCollaborator != null) {
            // 已存在记录（之前转接过），更新记录
            existingCollaborator.setRole(CsCollaborator.ROLE_OWNER);
            existingCollaborator.setJoinTime(new Date());
            existingCollaborator.setLeaveTime(null); // 清除离开时间，重新激活
            existingCollaborator.setInviteBy(fromAgentId);
            collaboratorMapper.updateById(existingCollaborator);
            log.info("[CS-Conversation] 重新激活协作记录: conversationId={}, agentId={}", conversationId, toAgentId);
        } else {
            // 不存在记录，新建协作记录
            CsCollaborator collaborator = new CsCollaborator();
            collaborator.setConversationId(conversationId);
            collaborator.setAgentId(toAgentId);
            collaborator.setRole(CsCollaborator.ROLE_OWNER);
            collaborator.setJoinTime(new Date());
            collaborator.setInviteBy(fromAgentId);
            collaboratorMapper.insert(collaborator);
            log.info("[CS-Conversation] 创建新协作记录: conversationId={}, agentId={}", conversationId, toAgentId);
        }
        
        // 增加新客服会话数
        agentService.incrementSessions(toAgentId);
        
        // 通知相关人员
        CsAgent fromAgent = fromAgentId != null ? agentService.getById(fromAgentId) : null;
        String fromName = fromAgent != null ? fromAgent.getNickname() : "系统";
        
        notifyRelatedAgents(conversationId, "transfer",
                "会话已从 " + fromName + " 移交给 " + toAgent.getNickname(), null);
        Map<String, Object> userExtra = new HashMap<>();
        userExtra.put("agentId", toAgent.getId());
        userExtra.put("agentName", toAgent.getNickname());
        userExtra.put("agentAvatar", toAgent.getAvatar());
        notifyUser(conversationId, "agent_changed",
                "客服 " + toAgent.getNickname() + " 继续为您服务", userExtra);
        
        // ★ 广播会话转接给所有客服（包含完整的会话信息）
        broadcastConversationTransfer(conversation, fromAgentId, fromName, toAgentId, toAgent.getNickname());
        
        return true;
    }
    
    /**
     * 广播会话转接给所有客服
     */
    private void broadcastConversationTransfer(CsConversation conversation, 
                                               String fromAgentId, String fromAgentName,
                                               String toAgentId, String toAgentName) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("conversationId", conversation.getId());
            data.put("fromAgentId", fromAgentId);
            data.put("fromAgentName", fromAgentName);
            data.put("toAgentId", toAgentId);
            data.put("toAgentName", toAgentName);
            CsAgent toAgent = agentService.getById(toAgentId);
            if (toAgent != null) {
                data.put("toAgentAvatar", toAgent.getAvatar());
            }
            data.put("transferTime", new Date());
            
            // ★ 添加完整的会话信息，供前端直接使用
            Map<String, Object> conversationData = new HashMap<>();
            conversationData.put("id", conversation.getId());
            conversationData.put("userId", conversation.getUserId());
            conversationData.put("userName", conversation.getUserName());
            conversationData.put("appId", conversation.getAppId());
            conversationData.put("source", conversation.getSource()); // 添加source字段
            conversationData.put("status", conversation.getStatus());
            conversationData.put("replyMode", conversation.getReplyMode());
            conversationData.put("ownerAgentId", toAgentId);
            conversationData.put("ownerAgentName", toAgentName);
            if (toAgent != null) {
                conversationData.put("ownerAgentAvatar", toAgent.getAvatar());
            }
            conversationData.put("lastMessage", csCryptoUtil.encryptTransport(conversation.getLastMessage()));
            conversationData.put("lastMessageTime", conversation.getLastMessageTime());
            conversationData.put("unreadCount", conversation.getUnreadCount());
            conversationData.put("messageCount", conversation.getMessageCount());
            conversationData.put("createTime", conversation.getCreateTime());
            conversationData.put("assignTime", conversation.getAssignTime());
            conversationData.put("updateTime", conversation.getUpdateTime());
            
            data.put("conversation", conversationData);
            
            broadcastToAllAgents("conversation_transferred", data);
            log.info("[CS-Conversation] 广播会话转接给所有客服: conversationId={}, from={}, to={}, conversation={}", 
                    conversation.getId(), fromAgentName, toAgentName, conversationData);
        } catch (Exception e) {
            log.error("[CS-Conversation] 广播会话转接失败: {}", e.getMessage(), e);
        }
    }

    // ==================== 查询接口 ====================

    @Override
    public IPage<CsConversation> getConversationList(Page<CsConversation> page, String agentId, 
                                                      Integer status, String filter) {
        // 调用高级版本，不包含已删除记录，不按特定客服筛选
        return getConversationListAdvanced(page, agentId, status, filter, false, null,
                null, null, null, null, null, null, null, null, null, null, null);
    }

    @Override
    public IPage<CsConversation> getConversationListAdvanced(Page<CsConversation> page, String agentId,
                                                              Integer status, String filter,
                                                              Boolean includeDeleted, String filterAgentId,
                                                              String id, String userId, Integer endType,
                                                              Integer satisfaction, String source,
                                                              String landingPage, String referrerPage,
                                                              String createTimeBegin, String createTimeEnd,
                                                              String endTimeBegin, String endTimeEnd) {
        IPage<CsConversation> result = baseMapper.selectConversationPage(page, agentId, status, filter,
                includeDeleted, filterAgentId, id, userId, endType, satisfaction, source,
                landingPage, referrerPage, createTimeBegin, createTimeEnd, endTimeBegin, endTimeEnd);
        java.util.Set<String> onlineConversationIds = sessionManager.getOnlineConversationIds();
        
        // 补充协作者信息
        for (CsConversation conv : result.getRecords()) {
            List<CsCollaborator> collaborators = collaboratorMapper.selectActiveCollaborators(conv.getId());
            conv.setCollaborators(collaborators);
            conv.setUserOnline(onlineConversationIds.contains(conv.getId()));
        }
        
        return result;
    }

    @Override
    public Map<String, Object> getConversationStats(String agentId) {
        Map<String, Object> stats = new HashMap<>();
        
        // 我负责的（进行中）
        long myCount = count(new LambdaQueryWrapper<CsConversation>()
                .eq(oConvertUtils.isNotEmpty(agentId), CsConversation::getOwnerAgentId, agentId)
                .ne(CsConversation::getStatus, CsConversation.STATUS_CLOSED));
        
        // 待接入的（只包含status=0的，排除已结束）
        long unassignedCount = count(new LambdaQueryWrapper<CsConversation>()
                .eq(CsConversation::getStatus, CsConversation.STATUS_UNASSIGNED));
        
        // ★ 已结束的：只统计当前客服自己负责的已结束会话
        long closedCount = count(new LambdaQueryWrapper<CsConversation>()
                .eq(CsConversation::getStatus, CsConversation.STATUS_CLOSED)
                .eq(oConvertUtils.isNotEmpty(agentId), CsConversation::getOwnerAgentId, agentId));
        
        // 总数
        long totalCount = count();
        
        stats.put("myCount", myCount);
        stats.put("unassignedCount", unassignedCount);
        stats.put("closedCount", closedCount);
        stats.put("totalCount", totalCount);
        
        return stats;
    }

    @Override
    public List<CsAgentWorkloadVO> getAgentWorkload(Integer days, Integer limit) {
        int safeDays = days == null || days <= 0 ? 7 : days;
        int safeLimit = limit == null || limit <= 0 ? 10 : limit;

        LocalDateTime startOfDay = LocalDate.now().minusDays(safeDays - 1L).atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().plusDays(1L).atStartOfDay();
        Date startTime = Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant());
        Date endTime = Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant());

        return baseMapper.selectAgentWorkload(startTime, endTime, safeLimit);
    }

    @Override
    public List<CsConversation> getMyConversations(String agentId) {
        return baseMapper.selectByOwnerAgent(agentId);
    }

    @Override
    public List<CsConversation> getUnassignedConversations(int limit) {
        return baseMapper.selectUnassigned(limit);
    }

    @Override
    public CsConversation getActiveConversation(String userId, String appId) {
        LambdaQueryWrapper<CsConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CsConversation::getUserId, userId)
                .ne(CsConversation::getStatus, CsConversation.STATUS_CLOSED)
                .orderByDesc(CsConversation::getCreateTime)
                .last("LIMIT 1");
        
        if (oConvertUtils.isNotEmpty(appId)) {
            wrapper.eq(CsConversation::getAppId, appId);
        }
        
        return getOne(wrapper);
    }

    // ==================== 消息相关 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLastMessage(String conversationId, String message) {
        String truncated = message != null && message.length() > 100 ? message.substring(0, 100) + "..." : message;
        LambdaUpdateWrapper<CsConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CsConversation::getId, conversationId)
                .set(CsConversation::getLastMessage, csCryptoUtil.encryptStorage(truncated))
                .set(CsConversation::getLastMessageTime, new Date())
                .setSql("message_count = IFNULL(message_count, 0) + 1");
        update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLastMessage(String conversationId, String message, int senderType) {
        String truncated = message != null && message.length() > 100 ? message.substring(0, 100) + "..." : message;
        LambdaUpdateWrapper<CsConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CsConversation::getId, conversationId)
                .set(CsConversation::getLastMessage, csCryptoUtil.encryptStorage(truncated))
                .set(CsConversation::getLastMessageTime, new Date())
                .setSql("message_count = IFNULL(message_count, 0) + 1");
        if (senderType == 0) {
            updateWrapper.setSql("visitor_message_count = IFNULL(visitor_message_count, 0) + 1");
        } else if (senderType == 1 || senderType == 2) {
            updateWrapper.setSql("agent_message_count = IFNULL(agent_message_count, 0) + 1");
        }
        if (senderType == 2) {
            updateWrapper.setSql("first_response_seconds = IF(" +
                "(first_response_seconds IS NULL OR first_response_seconds = 0) AND IFNULL(visitor_message_count, 0) > 0, " +
                "TIMESTAMPDIFF(SECOND, create_time, NOW()), first_response_seconds)");
        }
        update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementUnread(String conversationId) {
        LambdaUpdateWrapper<CsConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CsConversation::getId, conversationId)
                .setSql("unread_count = IFNULL(unread_count, 0) + 1");
        update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearUnread(String conversationId) {
        LambdaUpdateWrapper<CsConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CsConversation::getId, conversationId)
                .set(CsConversation::getUnreadCount, 0);
        update(updateWrapper);
        broadcastToAllAgents("unread_cleared", Map.of("conversationId", conversationId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetTimeoutWarning(String conversationId) {
        LambdaUpdateWrapper<CsConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CsConversation::getId, conversationId)
                .set(CsConversation::getTimeoutWarned, false);
        update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateVisitorLastMsgTime(String conversationId) {
        LambdaUpdateWrapper<CsConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CsConversation::getId, conversationId)
                .set(CsConversation::getVisitorLastMsgTime, new Date());
        update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearVisitorLastMsgTime(String conversationId) {
        boolean timelyReply = false;
        try {
            CsConversation conv = getById(conversationId);
            if (conv != null && conv.getVisitorLastMsgTime() != null) {
                long responseSeconds = (System.currentTimeMillis() - conv.getVisitorLastMsgTime().getTime()) / 1000;
                JSONObject config = getConversationAssignConfig();
                if (config != null) {
                    JSONObject notifyConfig = config.getJSONObject("agentTimeoutVisitorNotify");
                    if (notifyConfig != null) {
                        int threshold = notifyConfig.getIntValue("seconds");
                        if (threshold > 0 && responseSeconds <= threshold) {
                            timelyReply = true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[CS-Conversation] 计算及时回复失败: conversationId={}", conversationId, e);
        }

        LambdaUpdateWrapper<CsConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CsConversation::getId, conversationId)
                .set(CsConversation::getVisitorLastMsgTime, null)
                .set(CsConversation::getAgentTimeoutNotified, false);
        if (timelyReply) {
            updateWrapper.setSql("timely_reply_count = IFNULL(timely_reply_count, 0) + 1");
        }
        update(updateWrapper);
    }

    // ==================== 评价 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rateConversation(String conversationId, Integer satisfaction, String comment) {
        LambdaUpdateWrapper<CsConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CsConversation::getId, conversationId)
                .set(CsConversation::getSatisfaction, satisfaction)
                .set(CsConversation::getSatisfactionComment, csCryptoUtil.encryptStorage(comment))
                .set(CsConversation::getUpdateTime, new Date());
        update(updateWrapper);
        
        log.info("[CS-Conversation] 会话评价: conversationId={}, satisfaction={}", conversationId, satisfaction);
    }

    // ==================== 通知 ====================

    @Override
    public void notifyUser(String conversationId, String type, String content) {
        notifyUser(conversationId, type, content, null);
    }

    @Override
    public void notifyUser(String conversationId, String type, String content, Map<String, Object> extra) {
        CsConversation conversation = getById(conversationId);
        String userId = conversation != null ? conversation.getUserId() : conversationId;
        
        CsWebSocketMessage.CsWebSocketMessageBuilder builder = CsWebSocketMessage.builder()
                .type(type)
                .conversationId(conversationId)
                .content(csCryptoUtil.encryptTransport(content));
        
        if (extra != null) {
            builder.extra(extra);
        }
        
        sessionManager.sendToUserByConversation(conversationId, userId, builder.build());
    }

    @Override
    public void notifyAgents(String conversationId, String type, String content) {
        CsWebSocketMessage message = CsWebSocketMessage.builder()
                .type(type)
                .conversationId(conversationId)
                .content(csCryptoUtil.encryptTransport(content))
                .build();
        CsConversation conversation = getById(conversationId);
        sendToRelatedAgentsInternal(conversation, message, false);
    }

    @Override
    public void notifyRelatedAgents(String conversationId, String type, String content, Map<String, Object> extra) {
        CsWebSocketMessage.CsWebSocketMessageBuilder builder = CsWebSocketMessage.builder()
                .type(type)
                .conversationId(conversationId)
                .content(csCryptoUtil.encryptTransport(content));
        if (extra != null) {
            builder.extra(extra);
        }
        CsConversation conversation = getById(conversationId);
        sendToRelatedAgentsInternal(conversation, builder.build(), true);
    }

    @Override
    public void sendToRelatedAgents(String conversationId, CsWebSocketMessage message) {
        CsConversation conversation = getById(conversationId);
        sendToRelatedAgentsInternal(conversation, message, true);
    }

    @Override
    public List<String> getActiveConversationIdsByUser(String appId, String userId) {
        if (oConvertUtils.isEmpty(userId)) {
            return java.util.Collections.emptyList();
        }
        List<String> ids = baseMapper.selectActiveConversationIdsByUser(appId, userId);
        return ids != null ? ids : java.util.Collections.emptyList();
    }

    private void sendToRelatedAgentsInternal(CsConversation conversation, CsWebSocketMessage message, boolean includeAllIfUnassigned) {
        if (conversation == null || message == null) {
            return;
        }
        if (includeAllIfUnassigned && oConvertUtils.isEmpty(conversation.getOwnerAgentId())) {
            sessionManager.sendToAllAgents(message);
            return;
        }
        java.util.Set<String> agentIds = new java.util.HashSet<>();
        if (oConvertUtils.isNotEmpty(conversation.getOwnerAgentId())) {
            agentIds.add(conversation.getOwnerAgentId());
        }
        List<CsCollaborator> collaborators = collaboratorMapper.selectActiveCollaborators(conversation.getId());
        if (collaborators != null) {
            for (CsCollaborator collab : collaborators) {
                agentIds.add(collab.getAgentId());
            }
        }
        List<CsAgent> supervisors = agentService.getOnlineSupervisors();
        if (supervisors != null) {
            for (CsAgent supervisor : supervisors) {
                agentIds.add(supervisor.getId());
            }
        }
        for (String agentId : agentIds) {
            sessionManager.sendToAgent(agentId, message);
        }
    }
    
    /**
     * 广播消息给所有在线客服（带额外数据）
     */
    private void broadcastToAllAgents(String type, Map<String, Object> data) {
        CsWebSocketMessage.CsWebSocketMessageBuilder builder = CsWebSocketMessage.builder()
                .type(type);
        
        if (data != null) {
            // 设置常用字段
            if (data.containsKey("conversationId")) {
                builder.conversationId((String) data.get("conversationId"));
            }
            if (data.containsKey("content")) {
                builder.content((String) data.get("content"));
            }
            // 其他数据放到extra中
            builder.extra(data);
        }
        
        sessionManager.sendToAllAgents(builder.build());
    }

    @Override
    public IPage<CsConversation> getAllActiveConversations(Page<CsConversation> page) {
        IPage<CsConversation> result = baseMapper.selectAllActiveConversations(page);

        // 填充用户在线状态
        java.util.Set<String> onlineConversationIds = sessionManager.getOnlineConversationIds();
        for (CsConversation conv : result.getRecords()) {
            conv.setUserOnline(onlineConversationIds.contains(conv.getId()));
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryAssignAgent(String conversationId, String preferredAgentId) {
        CsConversation conversation = getById(conversationId);
        if (conversation == null || conversation.getStatus() != CsConversation.STATUS_UNASSIGNED) {
            return;
        }

        boolean aiEnabled = isAiEnabled();
        JSONObject assignConfig = getConversationAssignConfig();
        CsAgent assignedAgent = null;

        if (oConvertUtils.isNotEmpty(preferredAgentId)) {
            CsAgent preferred = agentService.getById(preferredAgentId);
            if (preferred != null && preferred.getStatus() != null && preferred.getStatus() == 1) {
                assignedAgent = preferred;
            }
        }

        if (assignedAgent == null) {
            String lastAgentId = null;
            if (assignConfig != null) {
                JSONObject inherit = assignConfig.getJSONObject("inheritLastAgent");
                if (inherit != null && inherit.getBooleanValue("enabled")) {
                    int expireMinutes = inherit.getIntValue("expireMinutes");
                    lastAgentId = findLastAgentForUser(conversation.getUserId(), expireMinutes);
                }
            }
            assignedAgent = agentService.assignAgent(lastAgentId);
        }

        if (assignedAgent != null) {
            conversation.setOwnerAgentId(assignedAgent.getId());
            conversation.setStatus(CsConversation.STATUS_ASSIGNED);
            conversation.setAssignTime(new Date());
            conversation.setReplyMode(aiEnabled ? CsConversation.REPLY_MODE_AI_AUTO : CsConversation.REPLY_MODE_MANUAL);
            conversation.setLastMessageTime(new Date());
            updateById(conversation);
            // 关键字段变更（ownerAgentId/status/replyMode），主动失效热缓存
            invalidateConvCache(conversationId);

            CsCollaborator collaborator = new CsCollaborator();
            collaborator.setConversationId(conversationId);
            collaborator.setAgentId(assignedAgent.getId());
            collaborator.setRole(CsCollaborator.ROLE_OWNER);
            collaborator.setJoinTime(new Date());
            collaboratorMapper.insert(collaborator);

            broadcastToAllAgents("conversation_assigned", buildConversationAssignedData(conversationId, assignedAgent));

            notifyUser(conversationId, "agent_connected",
                    "客服 " + assignedAgent.getNickname() + " 为您服务",
                    buildAgentConnectedExtra(conversation.getReplyMode(), assignedAgent));

            log.info("[CS-Conversation] 重新分配客服成功: conversationId={}, agentId={}", conversationId, assignedAgent.getId());
        } else {
            conversation.setLastMessageTime(new Date());
            updateById(conversation);
            log.info("[CS-Conversation] 重新分配客服失败(无在线客服): conversationId={}", conversationId);
        }
    }

    @Override
    public void refreshDefaultUserName(CsConversation conversation, String userIp, String deviceId) {
        if (conversation == null) {
            return;
        }
        String currentName = conversation.getUserName();
        if (oConvertUtils.isNotEmpty(currentName) && !"访客".equals(currentName)) {
            return;
        }
        Map<String, String> geoInfo = ipGeoService.queryGeoByIp(userIp);
        String newName = generateDefaultUserName(geoInfo, userIp, deviceId);
        if (!newName.equals(currentName)) {
            conversation.setUserName(newName);
            updateById(conversation);
            log.info("[CS-Conversation] 更新默认用户名: conversationId={}, newName={}", conversation.getId(), newName);
        }
    }

    /**
     * 构造客服已接入通知给访客的 extra 数据（agent_connected 事件）。
     * 三处使用：createConversation 自动分配、joinAgent 手动接入、reassignAgent 重新分配。
     * 业务差异仅在 replyMode 取值（自动分配/重分配=会话当前 replyMode；手动接入=固定 MANUAL）。
     */
    private Map<String, Object> buildAgentConnectedExtra(int replyMode, CsAgent agent) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("replyMode", replyMode);
        extra.put("agentName", agent.getNickname());
        extra.put("agentId", agent.getId());
        extra.put("agentAvatar", agent.getAvatar());
        return extra;
    }

    /**
     * 构造广播给所有客服的 conversation_assigned 事件数据。
     * 两处使用：joinAgent 手动接入、reassignAgent 重新分配。
     */
    private Map<String, Object> buildConversationAssignedData(String conversationId, CsAgent agent) {
        Map<String, Object> data = new HashMap<>();
        data.put("conversationId", conversationId);
        data.put("agentId", agent.getId());
        data.put("agentName", agent.getNickname());
        data.put("agentAvatar", agent.getAvatar());
        data.put("assignTime", new Date());
        return data;
    }

    @Override
    public void closeOtherActiveConversations(String userId, String appId, String keepConversationId) {
        LambdaQueryWrapper<CsConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CsConversation::getUserId, userId)
               .ne(CsConversation::getStatus, CsConversation.STATUS_CLOSED)
               .ne(CsConversation::getId, keepConversationId);
        if (oConvertUtils.isNotEmpty(appId)) {
            wrapper.eq(CsConversation::getAppId, appId);
        }
        List<CsConversation> others = list(wrapper);
        for (CsConversation conv : others) {
            closeConversation(conv.getId(), "系统清理：用户已有其他活跃会话", CsConversation.END_TYPE_SYSTEM_CLEAN);
        }
        if (!others.isEmpty()) {
            log.info("[CS-Conversation] 清理用户多余会话: userId={}, 关闭{}个", userId, others.size());
        }
    }
}
