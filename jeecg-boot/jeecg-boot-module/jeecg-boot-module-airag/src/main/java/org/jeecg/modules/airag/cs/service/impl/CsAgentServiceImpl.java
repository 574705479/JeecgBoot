package org.jeecg.modules.airag.cs.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.oConvertUtils;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;

import java.util.Date;
import java.util.List;

/**
 * 客服管理服务实现
 * 
 * @author jeecg
 * @date 2026-01-07
 */
@Slf4j
@Service
public class CsAgentServiceImpl extends ServiceImpl<CsAgentMapper, CsAgent> implements ICsAgentService {

    private static final String CONVERSATION_ASSIGN_REDIS_KEY = "cs:global:conversation_assign";
    private static final String CONVERSATION_ASSIGN_CONFIG_KEY = "conversation_assign";
    private static final String ROUND_ROBIN_INDEX_KEY = "cs:global:round_robin_index";

    @Autowired
    private CsWebSocketSessionManager sessionManager;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private CsGlobalConfigMapper csGlobalConfigMapper;

    @Autowired
    private ICsAgentStatusLogService agentStatusLogService;

    @PostConstruct
    public void resetAllAgentsOnStartup() {
        long count = count(new LambdaQueryWrapper<CsAgent>()
                .ne(CsAgent::getStatus, CsAgent.STATUS_OFFLINE));
        if (count > 0) {
            update(new LambdaUpdateWrapper<CsAgent>()
                    .ne(CsAgent::getStatus, CsAgent.STATUS_OFFLINE)
                    .set(CsAgent::getStatus, CsAgent.STATUS_OFFLINE)
                    .set(CsAgent::getCurrentSessions, 0));
            log.info("[CS-Agent] 服务启动，批量重置{}个客服为离线状态", count);
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
        
        // ★ 广播客服状态变化
        broadcastAgentStatusChanged(agentId, CsAgent.STATUS_ONLINE, "在线");

        agentStatusLogService.logStatusChange(agentId, CsAgentStatusLog.STATUS_ONLINE, CsAgentStatusLog.TRIGGER_MANUAL);
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
        
        // ★ 广播客服状态变化
        broadcastAgentStatusChanged(agentId, newStatus, statusText);

        agentStatusLogService.logStatusChange(agentId, newStatus, triggerSource);
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
                    .type("agent_status_changed")
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
        String lastAgentId = redisTemplate.opsForValue().get(ROUND_ROBIN_INDEX_KEY);
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
                redisTemplate.opsForValue().set(ROUND_ROBIN_INDEX_KEY, agent.getId());
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
            String json = redisTemplate.opsForValue().get(CONVERSATION_ASSIGN_REDIS_KEY);
            if (json == null || json.isEmpty()) {
                CsGlobalConfig config = csGlobalConfigMapper.selectById(CONVERSATION_ASSIGN_CONFIG_KEY);
                json = config != null ? config.getConfigValue() : null;
                if (json != null && !json.isEmpty()) {
                    redisTemplate.opsForValue().set(CONVERSATION_ASSIGN_REDIS_KEY, json);
                }
            }
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
        // 查询所有非离线客服（在线/忙碌/隐身都算占坐席）
        LambdaQueryWrapper<CsAgent> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(CsAgent::getStatus, CsAgent.STATUS_OFFLINE);
        return list(queryWrapper);
    }

}
