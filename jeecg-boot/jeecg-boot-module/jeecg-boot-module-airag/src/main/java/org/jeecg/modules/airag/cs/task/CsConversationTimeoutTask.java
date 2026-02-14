package org.jeecg.modules.airag.cs.task;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.cs.entity.CsConversation;
import org.jeecg.modules.airag.cs.entity.CsGlobalConfig;
import org.jeecg.modules.airag.cs.mapper.CsGlobalConfigMapper;
import org.jeecg.modules.airag.cs.service.ICsConversationService;
import org.jeecg.modules.airag.cs.service.ICsMessageService;
import org.jeecg.modules.airag.cs.websocket.CsWebSocketMessage;
import org.jeecg.modules.airag.cs.websocket.CsWebSocketSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话超时自动结束定时任务
 * 
 * 功能：
 * 1. 对话保持 - 用户无响应超时自动结束（从数据库全局配置读取）
 * 2. 客服超时未回复提示（从数据库全局配置读取）
 * 3. 超时前发送提醒消息
 * 
 * @author jeecg
 * @date 2026-01-13
 */
@Slf4j
@Component
public class CsConversationTimeoutTask {

    private static final String CONVERSATION_ASSIGN_REDIS_KEY = "cs:global:conversation_assign";
    private static final String CONVERSATION_ASSIGN_CONFIG_KEY = "conversation_assign";

    @Autowired
    @Lazy
    private ICsConversationService conversationService;

    @Autowired
    @Lazy
    private ICsMessageService messageService;

    @Autowired
    private CsGlobalConfigMapper csGlobalConfigMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private CsWebSocketSessionManager sessionManager;

    /**
     * 每分钟检查一次超时会话
     */
    @Scheduled(fixedRate = 60000)
    public void checkTimeoutConversations() {
        try {
            JSONObject config = getConversationAssignConfig();
            if (config == null) {
                return;
            }

            // 对话保持（超时自动结束）
            int holdMinutes = 10; // 默认10分钟
            JSONObject holdConfig = config.getJSONObject("conversationHold");
            if (holdConfig != null) {
                holdMinutes = holdConfig.getIntValue("minutes");
                if (holdMinutes <= 0) {
                    holdMinutes = 10;
                }
            }

            // 提醒提前量：超时时间的1/6，最少1分钟，最多5分钟
            int warningBefore = Math.max(1, Math.min(5, holdMinutes / 6));

            checkAndSendWarning(holdMinutes, warningBefore);
            checkAndCloseTimeout(holdMinutes);

            // 客服超时未回复提示
            JSONObject reminderConfig = config.getJSONObject("agentTimeoutReminder");
            if (reminderConfig != null && reminderConfig.getBooleanValue("enabled")) {
                int timeoutSeconds = reminderConfig.getIntValue("seconds");
                if (timeoutSeconds > 0) {
                    checkAgentTimeoutReminder(timeoutSeconds);
                }
            }
        } catch (Exception e) {
            log.error("[CS-Timeout] 检查超时会话失败", e);
        }
    }

    /**
     * 检查并发送超时提醒
     */
    private void checkAndSendWarning(int userInactiveTimeout, int warningBeforeMinutes) {
        long warningThreshold = (long)(userInactiveTimeout - warningBeforeMinutes) * 60 * 1000L;
        Date warningTime = new Date(System.currentTimeMillis() - warningThreshold);
        
        LambdaQueryWrapper<CsConversation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CsConversation::getStatus, CsConversation.STATUS_ASSIGNED)
                .isNotNull(CsConversation::getLastMessageTime)
                .lt(CsConversation::getLastMessageTime, warningTime)
                .and(w -> w.isNull(CsConversation::getTimeoutWarned)
                        .or()
                        .eq(CsConversation::getTimeoutWarned, false));
        
        List<CsConversation> conversations = conversationService.list(queryWrapper);
        
        for (CsConversation conv : conversations) {
            try {
                String warningMsg = String.format("温馨提示：您已%d分钟未回复消息，会话将在%d分钟后自动结束", 
                        userInactiveTimeout - warningBeforeMinutes, warningBeforeMinutes);
                messageService.sendSystemMessage(conv.getId(), warningMsg, false);
                
                conv.setTimeoutWarned(true);
                conversationService.updateById(conv);
                
                log.info("[CS-Timeout] 发送超时提醒: conversationId={}", conv.getId());
            } catch (Exception e) {
                log.error("[CS-Timeout] 发送超时提醒失败: conversationId={}", conv.getId(), e);
            }
        }
    }

    /**
     * 检查并自动结束超时会话
     */
    private void checkAndCloseTimeout(int userInactiveTimeout) {
        long timeoutThreshold = (long)userInactiveTimeout * 60 * 1000L;
        Date timeoutTime = new Date(System.currentTimeMillis() - timeoutThreshold);
        
        LambdaQueryWrapper<CsConversation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(CsConversation::getStatus, 
                        CsConversation.STATUS_UNASSIGNED,
                        CsConversation.STATUS_ASSIGNED)
                .isNotNull(CsConversation::getLastMessageTime)
                .lt(CsConversation::getLastMessageTime, timeoutTime);
        
        List<CsConversation> conversations = conversationService.list(queryWrapper);
        
        for (CsConversation conv : conversations) {
            try {
                String reason = String.format("用户%d分钟无响应，会话自动结束", userInactiveTimeout);
                conversationService.closeConversation(conv.getId(), reason);
                
                log.info("[CS-Timeout] 自动结束超时会话: conversationId={}, lastMessageTime={}", 
                        conv.getId(), conv.getLastMessageTime());
            } catch (Exception e) {
                log.error("[CS-Timeout] 自动结束会话失败: conversationId={}", conv.getId(), e);
            }
        }
        
        if (!conversations.isEmpty()) {
            log.info("[CS-Timeout] 本次自动结束{}个超时会话", conversations.size());
        }
    }

    /**
     * 检查客服超时未回复并发送提示
     */
    private void checkAgentTimeoutReminder(int timeoutSeconds) {
        Date timeoutTime = new Date(System.currentTimeMillis() - (long)timeoutSeconds * 1000L);
        
        // 查找已分配但客服超时未回复的会话
        // 条件：已分配 + 访客发消息时间非空(有未回复消息) + 访客等待时间超过阈值
        LambdaQueryWrapper<CsConversation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CsConversation::getStatus, CsConversation.STATUS_ASSIGNED)
                .isNotNull(CsConversation::getVisitorLastMsgTime)
                .lt(CsConversation::getVisitorLastMsgTime, timeoutTime);
        
        List<CsConversation> conversations = conversationService.list(queryWrapper);
        
        for (CsConversation conv : conversations) {
            try {
                // 通过WebSocket向客服发送超时提醒
                Map<String, Object> extra = new HashMap<>();
                extra.put("conversationId", conv.getId());
                extra.put("timeoutSeconds", timeoutSeconds);
                extra.put("type", "agent_timeout_reminder");
                
                CsWebSocketMessage wsMsg = CsWebSocketMessage.builder()
                        .type("agent_timeout_reminder")
                        .conversationId(conv.getId())
                        .content(String.format("客服超过%d秒未回复", timeoutSeconds))
                        .extra(extra)
                        .build();
                
                if (conv.getOwnerAgentId() != null) {
                    sessionManager.sendToAgent(conv.getOwnerAgentId(), wsMsg);
                }
            } catch (Exception e) {
                log.warn("[CS-Timeout] 发送客服超时提醒失败: conversationId={}", conv.getId(), e);
            }
        }
    }

    /**
     * 从数据库/Redis读取对话分配配置
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
            log.warn("[CS-Timeout] 读取对话分配配置失败", e);
        }
        return null;
    }
}
