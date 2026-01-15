package org.jeecg.modules.airag.cs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.cs.entity.CsAgent;
import org.jeecg.modules.airag.cs.mapper.CsAgentMapper;
import org.jeecg.modules.airag.cs.service.ICsAgentService;
import org.jeecg.modules.airag.cs.websocket.CsWebSocketMessage;
import org.jeecg.modules.airag.cs.websocket.CsWebSocketSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private CsWebSocketSessionManager sessionManager;

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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void goOffline(String agentId) {
        if (oConvertUtils.isEmpty(agentId)) {
            return;
        }
        update(new LambdaUpdateWrapper<CsAgent>()
                .eq(CsAgent::getId, agentId)
                .set(CsAgent::getStatus, CsAgent.STATUS_OFFLINE)
                .set(CsAgent::getCurrentSessions, 0));
        log.info("[CS-Agent] 客服下线: agentId={}", agentId);
        
        // ★ 广播客服状态变化
        broadcastAgentStatusChanged(agentId, CsAgent.STATUS_OFFLINE, "离线");
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
        List<CsAgent> agents = getAvailableAgents();
        if (agents == null || agents.isEmpty()) {
            log.warn("[CS-Agent] 没有可用客服");
            return null;
        }

        // 选择接待数最少的客服
        CsAgent agent = agents.get(0);
        
        // 尝试增加接待数
        if (incrementSessions(agent.getId())) {
            log.info("[CS-Agent] 分配客服: agentId={}", agent.getId());
            return agent;
        }

        // 如果第一个客服分配失败，尝试其他客服
        for (int i = 1; i < agents.size(); i++) {
            agent = agents.get(i);
            if (incrementSessions(agent.getId())) {
                log.info("[CS-Agent] 分配客服: agentId={}", agent.getId());
                return agent;
            }
        }

        log.warn("[CS-Agent] 所有客服都已满");
        return null;
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

}
