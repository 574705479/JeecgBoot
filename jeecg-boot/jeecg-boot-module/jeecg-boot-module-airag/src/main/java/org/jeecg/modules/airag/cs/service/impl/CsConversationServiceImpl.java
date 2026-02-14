package org.jeecg.modules.airag.cs.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.cs.entity.CsAgent;
import org.jeecg.modules.airag.cs.entity.CsCollaborator;
import org.jeecg.modules.airag.cs.entity.CsConversation;
import org.jeecg.modules.airag.cs.entity.CsGlobalConfig;
import org.jeecg.modules.airag.cs.mapper.CsCollaboratorMapper;
import org.jeecg.modules.airag.cs.mapper.CsConversationMapper;
import org.jeecg.modules.airag.cs.mapper.CsGlobalConfigMapper;
import org.jeecg.modules.airag.cs.service.CsIpGeoService;
import org.jeecg.modules.airag.cs.service.ICsAgentService;
import org.jeecg.modules.airag.cs.service.ICsConversationService;
import org.jeecg.modules.airag.cs.service.ICsMessageService;
import org.jeecg.modules.airag.cs.util.CsUserAgentUtil;
import org.jeecg.modules.airag.cs.websocket.CsWebSocketMessage;
import org.jeecg.modules.airag.cs.websocket.CsWebSocketSessionManager;
import org.jeecg.modules.airag.cs.vo.CsAgentWorkloadVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

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

    private static final String AI_ENABLED_REDIS_KEY = "cs:global:ai_enabled";
    private static final String AI_ENABLED_CONFIG_KEY = "ai_enabled";
    private static final String CONVERSATION_ASSIGN_REDIS_KEY = "cs:global:conversation_assign";
    private static final String CONVERSATION_ASSIGN_CONFIG_KEY = "conversation_assign";

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
    private CsIpGeoService ipGeoService;

    // ==================== 会话生命周期 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CsConversation createConversation(String appId, String userId, String userName, String source,
                                             String userIp, String userAgent, String deviceId, String userLang,
                                             String preferredAgentId) {
        // 读取AI开关和对话分配配置
        boolean aiEnabled = isAiEnabled();
        JSONObject assignConfig = getConversationAssignConfig();

        CsAgent assignedAgent = null;

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

        // ====== 解析设备信息和IP地理位置 ======
        Map<String, String> uaInfo = CsUserAgentUtil.parse(userAgent);
        Map<String, String> geoInfo = ipGeoService.queryGeoByIp(userIp);

        // 默认用户名逻辑：基于地理位置+IP生成
        String finalUserName = userName;
        if (oConvertUtils.isEmpty(finalUserName)) {
            finalUserName = generateDefaultUserName(geoInfo, userIp);
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

        if (assignedAgent != null) {
            // 有可用客服，直接分配
            conversation.setOwnerAgentId(assignedAgent.getId());
            conversation.setStatus(CsConversation.STATUS_ASSIGNED);
            conversation.setAssignTime(new Date());
            // AI开关决定回复模式
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
            Map<String, Object> extra = new HashMap<>();
            extra.put("replyMode", conversation.getReplyMode());
            extra.put("agentName", assignedAgent.getNickname());
            extra.put("agentId", assignedAgent.getId());
            extra.put("agentAvatar", assignedAgent.getAvatar());
            notifyUser(conversation.getId(), "agent_connected", 
                    "客服 " + assignedAgent.getNickname() + " 为您服务", extra);
        } else {
            // 无可用客服 → 标记noAgent，前端展示留言板
            conversation.setStatus(CsConversation.STATUS_UNASSIGNED);
            conversation.setReplyMode(CsConversation.REPLY_MODE_MANUAL);
            save(conversation);
            log.info("[CS-Conversation] 创建会话(无在线客服): id={}, userId={}", conversation.getId(), userId);
        }

        // 仅AI开启时发送AI开场白
        if (aiEnabled) {
            try {
                messageService.sendVisitorPrologue(conversation.getId());
            } catch (Exception e) {
                log.warn("[CS-Conversation] 发送开场白失败: {}", e.getMessage());
            }
        } else if (assignedAgent != null) {
            // AI关闭时发送自动消息（如果有配置）
            try {
                messageService.sendAutoMessages(conversation.getId(),
                        assignedAgent.getId(), assignedAgent.getNickname(), conversation.getUserLang());
            } catch (Exception e) {
                log.warn("[CS-Conversation] 发送自动消息失败: {}", e.getMessage());
            }
        }
        
        return conversation;
    }

    /**
     * 根据地理位置信息和IP生成默认用户名
     * 格式: "省份城市 (IP)"，例如: "广东深圳 (120.24.35.12)"、"北京 (10.0.0.1)"、"访客 (127.0.0.1)"
     */
    private String generateDefaultUserName(Map<String, String> geoInfo, String userIp) {
        StringBuilder sb = new StringBuilder();
        if (geoInfo != null && !geoInfo.isEmpty()) {
            String province = geoInfo.get("province");
            String city = geoInfo.get("city");
            String country = geoInfo.get("country");
            if (oConvertUtils.isNotEmpty(province) && oConvertUtils.isNotEmpty(city)) {
                sb.append(province).append(city);
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
            String value = redisTemplate.opsForValue().get(AI_ENABLED_REDIS_KEY);
            if (value == null) {
                CsGlobalConfig config = csGlobalConfigMapper.selectById(AI_ENABLED_CONFIG_KEY);
                value = config != null ? config.getConfigValue() : null;
                if (value != null) {
                    redisTemplate.opsForValue().set(AI_ENABLED_REDIS_KEY, value);
                }
            }
            return value == null || "true".equalsIgnoreCase(value);
        } catch (Exception e) {
            log.warn("[CS-Conversation] 读取AI开关失败", e);
            return true; // 默认开启
        }
    }

    /**
     * 读取对话分配配置
     */
    private JSONObject getConversationAssignConfig() {
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
            
            CsWebSocketMessage notification = CsWebSocketMessage.builder()
                    .type("new_conversation")
                    .conversationId(conversation.getId())
                    .senderId(conversation.getUserId())
                    .senderName(conversation.getUserName())
                    .content("新会话")
                    .extra(extra)
                    .build();
            sessionManager.sendToAllAgents(notification);
            log.info("[CS-Conversation] 广播新会话给所有客服: conversationId={}", conversation.getId());
        } catch (Exception e) {
            log.warn("[CS-Conversation] 广播新会话失败: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CsConversation getOrCreateConversation(String conversationId, String appId, String userId, String userName) {
        // 如果指定了conversationId，先尝试查找
        if (oConvertUtils.isNotEmpty(conversationId)) {
            CsConversation existing = getById(conversationId);
            if (existing != null) {
                return existing;
            }
            
            // 不存在则通过createConversation创建（自动分配）
            // 注意：指定ID的场景已不常见，走统一创建逻辑
            return createConversation(appId, userId, userName, null, null, null, null, null, null);
        }
        
        // 没有指定ID，查找用户的活跃会话
        CsConversation active = getActiveConversation(userId, appId);
        if (active != null) {
            return active;
        }
        
        // 创建新会话 (createConversation内部会广播)
        return createConversation(appId, userId, userName, null, null, null, null, null, null);
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
            // 会话不存在，创建新会话
            conversation = new CsConversation();
            conversation.setId(conversationId);
            conversation.setUserId(conversationId);
            conversation.setUserName("访客");
            conversation.setStatus(CsConversation.STATUS_UNASSIGNED);
            conversation.setReplyMode(CsConversation.REPLY_MODE_AI_AUTO);
            conversation.setUnreadCount(0);
            conversation.setMessageCount(0);
            conversation.setCreateTime(new Date());
            save(conversation);
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
            Map<String, Object> assignData = new HashMap<>();
            assignData.put("conversationId", conversationId);
            assignData.put("agentId", agentId);
            assignData.put("agentName", agent.getNickname());
            assignData.put("agentAvatar", agent.getAvatar());
            assignData.put("assignTime", new Date());
            broadcastToAllAgents("conversation_assigned", assignData);
            
            // 通知用户客服已接入，同时告知已切换为手动模式
            Map<String, Object> extra = new HashMap<>();
            extra.put("replyMode", CsConversation.REPLY_MODE_MANUAL);
            extra.put("agentName", agent.getNickname());
            extra.put("agentId", agent.getId());
            extra.put("agentAvatar", agent.getAvatar());
            notifyUser(conversationId, "agent_connected", "客服 " + agent.getNickname() + " 为您服务", extra);
        }
        
        log.info("[CS-Conversation] 客服接入成功: conversationId={}, agentId={}", conversationId, agentId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeConversation(String conversationId) {
        closeConversation(conversationId, "会话已结束");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeConversation(String conversationId, String reason) {
        log.info("[CS-Conversation] 结束会话: conversationId={}, reason={}", conversationId, reason);
        
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
        updateById(conversation);
        
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
            conversationData.put("lastMessage", conversation.getLastMessage());
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
        return getConversationListAdvanced(page, agentId, status, filter, false, null);
    }

    @Override
    public IPage<CsConversation> getConversationListAdvanced(Page<CsConversation> page, String agentId, 
                                                              Integer status, String filter,
                                                              Boolean includeDeleted, String filterAgentId) {
        IPage<CsConversation> result = baseMapper.selectConversationPage(page, agentId, status, filter, includeDeleted, filterAgentId);
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
        LambdaUpdateWrapper<CsConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CsConversation::getId, conversationId)
                .set(CsConversation::getLastMessage, 
                        message != null && message.length() > 100 ? message.substring(0, 100) + "..." : message)
                .set(CsConversation::getLastMessageTime, new Date())
                .setSql("message_count = IFNULL(message_count, 0) + 1");
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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetTimeoutWarning(String conversationId) {
        LambdaUpdateWrapper<CsConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CsConversation::getId, conversationId)
                .set(CsConversation::getTimeoutWarned, false);
        update(updateWrapper);
    }

    // ==================== 评价 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rateConversation(String conversationId, Integer satisfaction, String comment) {
        LambdaUpdateWrapper<CsConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CsConversation::getId, conversationId)
                .set(CsConversation::getSatisfaction, satisfaction)
                .set(CsConversation::getSatisfactionComment, comment)
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
                .content(content);
        
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
                .content(content)
                .build();
        CsConversation conversation = getById(conversationId);
        sendToRelatedAgentsInternal(conversation, message, false);
    }

    @Override
    public void notifyRelatedAgents(String conversationId, String type, String content, Map<String, Object> extra) {
        CsWebSocketMessage.CsWebSocketMessageBuilder builder = CsWebSocketMessage.builder()
                .type(type)
                .conversationId(conversationId)
                .content(content);
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
        // 查询所有进行中的会话（状态：待接入 或 服务中）
        LambdaQueryWrapper<CsConversation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(CsConversation::getStatus, 
                CsConversation.STATUS_UNASSIGNED, 
                CsConversation.STATUS_ASSIGNED)
                .orderByDesc(CsConversation::getLastMessageTime);
        
        IPage<CsConversation> result = page(page, queryWrapper);
        
        // 填充用户在线状态
        java.util.Set<String> onlineConversationIds = sessionManager.getOnlineConversationIds();
        for (CsConversation conv : result.getRecords()) {
            conv.setUserOnline(onlineConversationIds.contains(conv.getId()));
        }
        
        return result;
    }
}
