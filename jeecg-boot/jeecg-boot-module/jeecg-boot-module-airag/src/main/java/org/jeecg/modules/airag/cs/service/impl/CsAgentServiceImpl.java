package org.jeecg.modules.airag.cs.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.cs.constant.CsRedisKeys;
import org.jeecg.modules.airag.cs.entity.CsAgent;
import org.jeecg.modules.airag.cs.entity.CsGlobalConfig;
import org.jeecg.modules.airag.cs.mapper.CsAgentMapper;
import org.jeecg.modules.airag.cs.mapper.CsGlobalConfigMapper;
import org.jeecg.modules.airag.cs.entity.CsAgentStatusLog;
import org.jeecg.modules.airag.cs.service.ICsAgentService;
import org.jeecg.modules.airag.cs.service.ICsAgentStatusLogService;
import org.jeecg.modules.airag.cs.websocket.CsWebSocketMessage;
import org.jeecg.modules.airag.cs.websocket.CsWebSocketSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * 客服管理服务实现
 * 
 * @author jeecg
 * @date 2026-01-07
 */
@Slf4j
@Service
public class CsAgentServiceImpl extends ServiceImpl<CsAgentMapper, CsAgent> implements ICsAgentService {


    @Autowired
    private CsWebSocketSessionManager sessionManager;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private CsGlobalConfigMapper csGlobalConfigMapper;

    @Autowired
    private org.jeecg.modules.airag.cs.service.CsGlobalConfigCache configCache;

    @Autowired
    private ICsAgentStatusLogService agentStatusLogService;

    @Autowired
    private org.jeecg.modules.airag.cs.async.CsAsyncTaskExecutor asyncTaskExecutor;

    /**
     * 客服会话服务（用于上线 sweep 派单）。
     * <p>{@link org.jeecg.modules.airag.cs.service.ICsConversationService} 自身也注入了
     * {@link ICsAgentService}，构成循环依赖；@Lazy 让 Spring 延迟解析这条边。</p>
     */
    @Autowired
    @Lazy
    private org.jeecg.modules.airag.cs.service.ICsConversationService conversationService;

    /**
     * sweep 防抖：同一 agentId 在 SWEEP_DEDUP_MS 内连续 goOnline 只触发一次 sweep。
     * 典型重复触发：「显式接入会话」内部调 self-online、或「decrementSessions → online」良性循环
     * 与「主动上线」短时叠加。
     * <p>注意：该 map 仅在单 JVM 内有效；多实例部署时由 sweep 内的 CAS UPDATE 兜底，不会重复分配。</p>
     */
    private final ConcurrentMap<String, Long> lastSweepTrigger = new ConcurrentHashMap<>();
    private static final long SWEEP_DEDUP_MS = 5000L;

    @PostConstruct
    public void resetAllAgentsOnStartup() {
        // 镜像升级窗口：先把当前非 OFFLINE 客服快照到 Redis（30 分钟 TTL），
        // 让 ws 重连时能恢复升级前状态，避免访客新会话堆积在「未分配」。
        snapshotPreshutdownState();

        long count = count(new LambdaQueryWrapper<CsAgent>()
                .ne(CsAgent::getStatus, CsAgent.STATUS_OFFLINE));
        if (count > 0) {
            update(new LambdaUpdateWrapper<CsAgent>()
                    .ne(CsAgent::getStatus, CsAgent.STATUS_OFFLINE)
                    .set(CsAgent::getStatus, CsAgent.STATUS_OFFLINE)
                    .set(CsAgent::getCurrentSessions, 0));
            log.info("[CS-Agent] 服务启动，批量重置{}个客服为离线状态", count);
        }
        // 同步清空 Redis 在线 ZSET（ZSET 是运行期临时态，启动时全量重置）
        try {
            redisTemplate.delete(CsRedisKeys.REDIS_AGENT_ONLINE_ZSET);
        } catch (Exception e) {
            log.warn("[CS-Agent] 启动时清空在线 ZSET 失败（非致命）: {}", e.getMessage());
        }
    }

    /**
     * 启动 reset 之前把 status!=OFFLINE 的客服快照到 Redis。
     * <p>关键不变量：仅快照非 OFFLINE 的客服。多次连续重启时（升级失败后又重启），
     * 第二次启动 DB 已经全 OFFLINE，notOffline 为空 → 不写新快照 → 旧快照保留 →
     * 客服 ws 重连仍能恢复升级前的真实状态。</p>
     * <p>整个方法被 try-catch 兜底，Redis 不可用时降级为现状（不影响 reset 主流程）。</p>
     */
    private void snapshotPreshutdownState() {
        try {
            List<CsAgent> notOffline = list(new LambdaQueryWrapper<CsAgent>()
                    .select(CsAgent::getId, CsAgent::getStatus)
                    .ne(CsAgent::getStatus, CsAgent.STATUS_OFFLINE));
            if (notOffline.isEmpty()) {
                return;
            }
            for (CsAgent a : notOffline) {
                String key = CsRedisKeys.REDIS_AGENT_PRESHUTDOWN_PREFIX + a.getId();
                redisTemplate.opsForValue().set(key, String.valueOf(a.getStatus()),
                        CsRedisKeys.PRESHUTDOWN_TTL_MINUTES, TimeUnit.MINUTES);
            }
            log.info("[CS-Agent] preshutdown 快照写入: count={}, ttl={}min",
                    notOffline.size(), CsRedisKeys.PRESHUTDOWN_TTL_MINUTES);
        } catch (Exception e) {
            log.warn("[CS-Agent] preshutdown 快照失败（非致命）: {}", e.getMessage());
        }
    }

    @Override
    public CsAgent getByUserId(String userId) {
        if (oConvertUtils.isEmpty(userId)) {
            return null;
        }
        // 先按用户ID查询
        CsAgent agent = getOne(new LambdaQueryWrapper<CsAgent>().eq(CsAgent::getUserId, userId));
        if (agent != null) {
            return agent;
        }
        // 如果没找到，尝试按用户名查询（兼容JSelectUser组件存储用户名的情况）
        // 需要先通过用户服务查找用户名
        return null;
    }

    @Override
    public CsAgent getCurrentAgent() {
        try {
            LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            if (loginUser == null) {
                return null;
            }
            // 先按用户ID查询
            CsAgent agent = getOne(new LambdaQueryWrapper<CsAgent>().eq(CsAgent::getUserId, loginUser.getId()));
            if (agent != null) {
                return agent;
            }
            // 再按用户名查询（兼容JSelectUser组件存储用户名的情况）
            agent = getOne(new LambdaQueryWrapper<CsAgent>().eq(CsAgent::getUserId, loginUser.getUsername()));
            return agent;
        } catch (Exception e) {
            log.error("[CS-Agent] 获取当前用户客服信息失败", e);
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void goOnline(String agentId) {
        if (oConvertUtils.isEmpty(agentId)) {
            return;
        }
        update(new LambdaUpdateWrapper<CsAgent>()
                .eq(CsAgent::getId, agentId)
                .set(CsAgent::getStatus, CsAgent.STATUS_ONLINE)
                .set(CsAgent::getLastOnlineTime, new Date()));
        log.info("[CS-Agent] 客服上线: agentId={}", agentId);

        addToOnlineZset(agentId);

        // ★ 广播客服状态变化
        broadcastAgentStatusChanged(agentId, CsAgent.STATUS_ONLINE, "在线");

        agentStatusLogService.logStatusChange(agentId, CsAgentStatusLog.STATUS_ONLINE, CsAgentStatusLog.TRIGGER_MANUAL);

        triggerSweepUnassigned(agentId);
    }

    /**
     * 客服上线后异步 sweep 派单未分配会话。
     *
     * <p>同 agentId 5 秒内防抖；走 cs-conv 线程池避免阻塞当前事务（{@link #goOnline} 带
     * {@code @Transactional}，sweep 自身不参与该事务，避免长事务持锁）。</p>
     */
    private void triggerSweepUnassigned(String agentId) {
        long now = System.currentTimeMillis();
        Long prev = lastSweepTrigger.put(agentId, now);
        if (prev != null && now - prev < SWEEP_DEDUP_MS) {
            log.debug("[CS-Agent] sweep 防抖跳过: agentId={}, gap={}ms", agentId, now - prev);
            return;
        }
        asyncTaskExecutor.submitConversation(() -> {
            try {
                int n = conversationService.sweepUnassignedToOnlineAgents(agentId);
                if (n > 0) {
                    log.info("[CS-Agent] 上线 sweep 派单完成: triggerBy={}, assigned={}", agentId, n);
                }
            } catch (Exception e) {
                log.error("[CS-Agent] 上线 sweep 失败: triggerBy={}", agentId, e);
            }
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void goOffline(String agentId) {
        goOffline(agentId, CsAgentStatusLog.TRIGGER_MANUAL);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void goOffline(String agentId, String triggerSource) {
        if (oConvertUtils.isEmpty(agentId)) {
            return;
        }
        boolean isManual = CsAgentStatusLog.TRIGGER_MANUAL.equals(triggerSource);
        int newStatus = isManual ? CsAgent.STATUS_INVISIBLE : CsAgent.STATUS_OFFLINE;
        String statusText = isManual ? "隐身" : "离线";

        update(new LambdaUpdateWrapper<CsAgent>()
                .eq(CsAgent::getId, agentId)
                .set(CsAgent::getStatus, newStatus)
                .set(CsAgent::getCurrentSessions, 0));
        log.info("[CS-Agent] 客服{}: agentId={}, triggerSource={}", statusText, agentId, triggerSource);

        // 隐身（manual）也算占坐席（与原 getOnlineAgents 行为一致：ne OFFLINE），
        // 因此只在真正 OFFLINE 时才从 ZSET 移除；隐身仍续写心跳保持在 ZSET。
        if (newStatus == CsAgent.STATUS_OFFLINE) {
            removeFromOnlineZset(agentId);
        } else {
            addToOnlineZset(agentId);
        }

        // ★ 广播客服状态变化
        broadcastAgentStatusChanged(agentId, newStatus, statusText);

        agentStatusLogService.logStatusChange(agentId, newStatus, triggerSource);
    }

    @Override
    public void restoreFromPreshutdownSnapshot(String agentId) {
        if (oConvertUtils.isEmpty(agentId)) {
            return;
        }
        String key = CsRedisKeys.REDIS_AGENT_PRESHUTDOWN_PREFIX + agentId;
        String value;
        try {
            value = redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("[CS-Agent] 读取 preshutdown 快照失败: agentId={}, err={}", agentId, e.getMessage());
            return;
        }
        if (oConvertUtils.isEmpty(value)) {
            return;
        }
        // 一次性消费：DEL 在前，避免并发 ws 重连重复触发 goOnline / sweep
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("[CS-Agent] DEL preshutdown 快照失败: agentId={}, err={}", agentId, e.getMessage());
        }

        CsAgent agent = getById(agentId);
        if (agent == null || agent.getStatus() == null) {
            return;
        }
        // 守卫：只在 OFFLINE 时才恢复，避免覆盖客服已经手动切换过的状态
        if (agent.getStatus() != CsAgent.STATUS_OFFLINE) {
            log.info("[CS-Agent] 跳过快照恢复(已非 OFFLINE): agentId={}, current={}",
                    agentId, agent.getStatus());
            return;
        }

        int prev;
        try {
            prev = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("[CS-Agent] preshutdown 快照值非法: agentId={}, value={}", agentId, value);
            return;
        }

        if (prev == CsAgent.STATUS_INVISIBLE) {
            // INVISIBLE → goOffline(MANUAL) 把 status 置为 INVISIBLE，不派单
            goOffline(agentId);
            log.info("[CS-Agent] 快照恢复为隐身: agentId={}", agentId);
        } else if (prev == CsAgent.STATUS_ONLINE || prev == CsAgent.STATUS_BUSY) {
            // BUSY 在 reset 后 current_sessions=0，恢复为 ONLINE 即可。
            // goOnline 内部会触发 sweep 派发累积未分配会话。
            goOnline(agentId);
            log.info("[CS-Agent] 快照恢复为在线: agentId={}, prev={}", agentId, prev);
        } else {
            log.warn("[CS-Agent] preshutdown 快照值未识别: agentId={}, prev={}", agentId, prev);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setBusy(String agentId) {
        if (oConvertUtils.isEmpty(agentId)) {
            return;
        }
        update(new LambdaUpdateWrapper<CsAgent>()
                .eq(CsAgent::getId, agentId)
                .set(CsAgent::getStatus, CsAgent.STATUS_BUSY));
        log.info("[CS-Agent] 客服设置忙碌: agentId={}", agentId);

        // 忙碌仍然算占坐席，保持在线 ZSET 中
        addToOnlineZset(agentId);

        // ★ 广播客服状态变化
        broadcastAgentStatusChanged(agentId, CsAgent.STATUS_BUSY, "忙碌");

        agentStatusLogService.logStatusChange(agentId, CsAgentStatusLog.STATUS_BUSY, CsAgentStatusLog.TRIGGER_SYSTEM);
    }
    
    /**
     * 广播客服状态变化给所有客服
     */
    private void broadcastAgentStatusChanged(String agentId, int status, String statusText) {
        try {
            CsAgent agent = getById(agentId);
            if (agent == null) {
                return;
            }
            
            CsWebSocketMessage message = CsWebSocketMessage.builder()
                    .type(CsWebSocketMessage.TYPE_AGENT_STATUS_CHANGED)
                    .senderId(agentId)
                    .senderName(agent.getNickname())
                    .extra(java.util.Map.of(
                            "agentId", agentId,
                            "agentName", agent.getNickname(),
                            "status", status,
                            "statusText", statusText,
                            "avatar", agent.getAvatar() != null ? agent.getAvatar() : ""
                    ))
                    .build();
            
            sessionManager.sendToAllAgents(message);
            log.info("[CS-Agent] 广播客服状态变化: agentId={}, status={}", agentId, statusText);
        } catch (Exception e) {
            log.warn("[CS-Agent] 广播客服状态变化失败: {}", e.getMessage());
        }
    }

    @Override
    public List<CsAgent> getAvailableAgents() {
        return baseMapper.selectAvailableAgents();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CsAgent assignAgent() {
        return assignAgent(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CsAgent assignAgent(String lastAgentId) {
        // 如果指定了上次客服，优先尝试分配给上次客服
        if (oConvertUtils.isNotEmpty(lastAgentId)) {
            CsAgent lastAgent = getById(lastAgentId);
            if (lastAgent != null && lastAgent.canAcceptSession()) {
                if (incrementSessions(lastAgent.getId())) {
                    log.info("[CS-Agent] 继承上次客服分配: agentId={}", lastAgent.getId());
                    return lastAgent;
                }
            }
            log.info("[CS-Agent] 上次客服不可用({}), 按策略分配", lastAgentId);
        }

        List<CsAgent> agents = getAvailableAgents();
        if (agents == null || agents.isEmpty()) {
            log.warn("[CS-Agent] 没有可用客服");
            return null;
        }

        // 读取分配策略配置
        String assignMode = getAssignMode();

        if ("round_robin".equals(assignMode)) {
            return assignByRoundRobin(agents);
        } else {
            return assignBySaturation(agents);
        }
    }

    /**
     * 轮流分配策略
     * 使用按创建时间稳定排序的客服列表，通过Redis记录上次分配的客服ID来实现轮流
     */
    private CsAgent assignByRoundRobin(List<CsAgent> ignoredAgents) {
        // 使用按创建时间稳定排序的查询（而不是按current_sessions排序的列表）
        List<CsAgent> agents = baseMapper.selectAvailableAgentsForRoundRobin();
        if (agents == null || agents.isEmpty()) {
            log.warn("[CS-Agent] 轮流分配：没有可用客服");
            return null;
        }

        int size = agents.size();
        
        // 从Redis读取上次分配的客服ID，找到其在列表中的位置
        String lastAgentId = redisTemplate.opsForValue().get(CsRedisKeys.REDIS_ROUND_ROBIN_INDEX);
        int startIndex = 0;
        if (lastAgentId != null) {
            for (int i = 0; i < size; i++) {
                if (agents.get(i).getId().equals(lastAgentId)) {
                    startIndex = (i + 1) % size;  // 从上次分配的下一个开始
                    break;
                }
            }
        }

        // 从startIndex开始轮流尝试
        for (int i = 0; i < size; i++) {
            int idx = (startIndex + i) % size;
            CsAgent agent = agents.get(idx);
            if (incrementSessions(agent.getId())) {
                // 记录本次分配的客服ID（而不是索引）
                redisTemplate.opsForValue().set(CsRedisKeys.REDIS_ROUND_ROBIN_INDEX, agent.getId());
                log.info("[CS-Agent] 轮流分配客服: agentId={}, nickname={}", agent.getId(), agent.getNickname());
                return agent;
            }
        }

        log.warn("[CS-Agent] 轮流分配失败，所有客服都已满");
        return null;
    }

    /**
     * 饱和度分配策略（选择接待数最少的客服，相同饱和度随机分配）
     */
    private CsAgent assignBySaturation(List<CsAgent> agents) {
        // agents已按 current_sessions ASC, RAND() 排序
        // 相同饱和度的客服会被随机排列
        for (CsAgent agent : agents) {
            if (incrementSessions(agent.getId())) {
                log.info("[CS-Agent] 饱和度分配客服: agentId={}, nickname={}, currentSessions={}", 
                        agent.getId(), agent.getNickname(), agent.getCurrentSessions());
                return agent;
            }
        }
        log.warn("[CS-Agent] 饱和度分配失败，所有客服都已满");
        return null;
    }

    /**
     * 获取分配策略模式
     */
    private String getAssignMode() {
        try {
            String json = configCache.get(CsRedisKeys.REDIS_CONVERSATION_ASSIGN, CsRedisKeys.CONFIG_CONVERSATION_ASSIGN);
            if (json != null && !json.isEmpty()) {
                JSONObject obj = JSONObject.parseObject(json);
                return obj.getString("assignMode");
            }
        } catch (Exception e) {
            log.warn("[CS-Agent] 读取分配策略配置失败", e);
        }
        return "saturation"; // 默认饱和度分配
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean incrementSessions(String agentId) {
        if (oConvertUtils.isEmpty(agentId)) {
            return false;
        }
        int rows = baseMapper.incrementCurrentSessions(agentId);
        if (rows > 0) {
            // 检查是否需要设置为忙碌
            CsAgent agent = getById(agentId);
            if (agent != null && agent.getCurrentSessions() >= agent.getMaxSessions()) {
                setBusy(agentId);
            }
            return true;
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void decrementSessions(String agentId) {
        if (oConvertUtils.isEmpty(agentId)) {
            return;
        }
        baseMapper.decrementCurrentSessions(agentId);
        
        // 检查是否需要恢复在线状态
        CsAgent agent = getById(agentId);
        if (agent != null && agent.getStatus() == CsAgent.STATUS_BUSY 
                && agent.getCurrentSessions() < agent.getMaxSessions()) {
            goOnline(agentId);
        }
    }

    @Override
    public void incrementTotalServed(String agentId) {
        if (oConvertUtils.isEmpty(agentId)) {
            return;
        }
        baseMapper.incrementTotalServed(agentId);
    }

    @Override
    public CsAgent findOnlineAgentWithApp() {
        // 查找在线且设置了AI应用的客服
        LambdaQueryWrapper<CsAgent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CsAgent::getStatus, CsAgent.STATUS_ONLINE)
                .isNotNull(CsAgent::getDefaultAppId)
                .ne(CsAgent::getDefaultAppId, "")
                .last("LIMIT 1");
        return getOne(queryWrapper);
    }

    @Override
    public List<CsAgent> getOnlineSupervisors() {
        // 查询所有在线的管理者客服
        LambdaQueryWrapper<CsAgent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CsAgent::getStatus, CsAgent.STATUS_ONLINE)
                .eq(CsAgent::getRole, CsAgent.ROLE_SUPERVISOR);
        return list(queryWrapper);
    }

    @Override
    public List<CsAgent> getOnlineAgents() {
        // Phase 3: 优先走 Redis ZSET 拿在线 agentId 集合，再 IN 查询拿详情；
        // ZSET 不可用 / 为空时回落到原有"全表 ne OFFLINE"查询，保证首次部署平滑。
        try {
            List<String> ids = getOnlineAgentIdsFromZset();
            if (!ids.isEmpty()) {
                return list(new LambdaQueryWrapper<CsAgent>()
                        .in(CsAgent::getId, ids)
                        .ne(CsAgent::getStatus, CsAgent.STATUS_OFFLINE));
            }
        } catch (Exception e) {
            log.warn("[CS-Agent] Redis ZSET 在线列表读取失败，回落 DB: {}", e.getMessage());
        }
        // 兜底：DB 全表查
        return list(new LambdaQueryWrapper<CsAgent>()
                .ne(CsAgent::getStatus, CsAgent.STATUS_OFFLINE));
    }

    @Override
    public int countOnlineAgents() {
        // Phase 3: ZCARD（先按 score 过期清理一遍）→ DB 兜底
        try {
            long now = System.currentTimeMillis();
            // 清理过期心跳
            redisTemplate.opsForZSet().removeRangeByScore(
                    CsRedisKeys.REDIS_AGENT_ONLINE_ZSET, 0, now - CsRedisKeys.AGENT_ONLINE_TTL_MS);
            Long size = redisTemplate.opsForZSet().zCard(CsRedisKeys.REDIS_AGENT_ONLINE_ZSET);
            if (size != null && size > 0) {
                return size.intValue();
            }
        } catch (Exception e) {
            log.warn("[CS-Agent] Redis ZSET 计数失败，回落 DB: {}", e.getMessage());
        }
        return (int) count(new LambdaQueryWrapper<CsAgent>()
                .ne(CsAgent::getStatus, CsAgent.STATUS_OFFLINE));
    }

    @Override
    public void markAgentHeartbeat(String agentId) {
        if (oConvertUtils.isEmpty(agentId)) {
            return;
        }
        addToOnlineZset(agentId);
    }

    // ==================== Redis ZSET 辅助 ====================

    /**
     * 把 agentId 加入在线 ZSET，score 为当前时间戳（毫秒）。Redis 故障时静默吞异常，
     * 不影响主流程（getOnlineAgents 会回落 DB）。
     */
    private void addToOnlineZset(String agentId) {
        try {
            redisTemplate.opsForZSet().add(
                    CsRedisKeys.REDIS_AGENT_ONLINE_ZSET, agentId, System.currentTimeMillis());
        } catch (Exception e) {
            log.debug("[CS-Agent] Redis ZSET ZADD 失败（非致命）: agentId={}, err={}", agentId, e.getMessage());
        }
    }

    private void removeFromOnlineZset(String agentId) {
        try {
            redisTemplate.opsForZSet().remove(CsRedisKeys.REDIS_AGENT_ONLINE_ZSET, agentId);
        } catch (Exception e) {
            log.debug("[CS-Agent] Redis ZSET ZREM 失败（非致命）: agentId={}, err={}", agentId, e.getMessage());
        }
    }

    /**
     * 从 ZSET 拉取最近 30s 内有过心跳/上线的 agentId 列表。
     * 同时把超过 TTL 的成员清理掉，避免 ZSET 无限膨胀。
     */
    private List<String> getOnlineAgentIdsFromZset() {
        long now = System.currentTimeMillis();
        long minScore = now - CsRedisKeys.AGENT_ONLINE_TTL_MS;
        // 顺手清理过期 member
        redisTemplate.opsForZSet().removeRangeByScore(
                CsRedisKeys.REDIS_AGENT_ONLINE_ZSET, 0, minScore);
        Set<String> ids = redisTemplate.opsForZSet().rangeByScore(
                CsRedisKeys.REDIS_AGENT_ONLINE_ZSET, minScore, Double.POSITIVE_INFINITY);
        return ids == null ? new ArrayList<>() : new ArrayList<>(ids);
    }

}
