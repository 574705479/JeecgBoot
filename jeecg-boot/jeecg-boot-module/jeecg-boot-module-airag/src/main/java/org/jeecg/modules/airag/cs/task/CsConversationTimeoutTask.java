package org.jeecg.modules.airag.cs.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.cs.entity.CsConversation;
import org.jeecg.modules.airag.cs.service.ICsConversationService;
import org.jeecg.modules.airag.cs.service.ICsMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 会话超时自动结束定时任务
 * 
 * 功能：
 * 1. 检查长时间无用户响应的会话
 * 2. 发送超时提醒消息
 * 3. 超时后自动结束会话
 * 
 * @author jeecg
 * @date 2026-01-13
 */
@Slf4j
@Component
public class CsConversationTimeoutTask {

    @Autowired
    @Lazy
    private ICsConversationService conversationService;

    @Autowired
    @Lazy
    private ICsMessageService messageService;

    /**
     * 用户无响应超时时间（分钟），默认30分钟
     */
    @Value("${cs.timeout.user-inactive:30}")
    private int userInactiveTimeout;

    /**
     * 超时提醒时间（分钟），在超时前N分钟发送提醒，默认5分钟
     */
    @Value("${cs.timeout.warning-before:5}")
    private int warningBeforeMinutes;

    /**
     * 是否启用自动超时结束功能
     */
    @Value("${cs.timeout.enabled:true}")
    private boolean timeoutEnabled;

    /**
     * 每分钟检查一次超时会话
     */
    @Scheduled(fixedRate = 60000)
    public void checkTimeoutConversations() {
        if (!timeoutEnabled) {
            return;
        }

        try {
            // 检查需要发送提醒的会话
            checkAndSendWarning();
            
            // 检查需要自动结束的会话
            checkAndCloseTimeout();
        } catch (Exception e) {
            log.error("[CS-Timeout] 检查超时会话失败", e);
        }
    }

    /**
     * 检查并发送超时提醒
     */
    private void checkAndSendWarning() {
        // 计算提醒时间点：当前时间 - (超时时间 - 提醒提前量)
        long warningThreshold = (userInactiveTimeout - warningBeforeMinutes) * 60 * 1000L;
        Date warningTime = new Date(System.currentTimeMillis() - warningThreshold);
        
        // 查询需要提醒的会话：
        // 1. 状态为已分配（有客服负责）
        // 2. 最后消息时间存在且超过阈值
        // 3. 还未发送过提醒（timeoutWarned 为 null 或 false）
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
                // 发送超时提醒消息（不持久化）
                String warningMsg = String.format("温馨提示：您已%d分钟未回复消息，会话将在%d分钟后自动结束", 
                        userInactiveTimeout - warningBeforeMinutes, warningBeforeMinutes);
                messageService.sendSystemMessage(conv.getId(), warningMsg, false);
                
                // 标记已发送提醒
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
    private void checkAndCloseTimeout() {
        // 计算超时时间点
        long timeoutThreshold = userInactiveTimeout * 60 * 1000L;
        Date timeoutTime = new Date(System.currentTimeMillis() - timeoutThreshold);
        
        // 查询需要自动结束的会话：
        // 1. 状态为已分配
        // 2. 最后消息时间存在且超过阈值
        LambdaQueryWrapper<CsConversation> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CsConversation::getStatus, CsConversation.STATUS_ASSIGNED)
                .isNotNull(CsConversation::getLastMessageTime)
                .lt(CsConversation::getLastMessageTime, timeoutTime);
        
        List<CsConversation> conversations = conversationService.list(queryWrapper);
        
        for (CsConversation conv : conversations) {
            try {
                // 自动结束会话
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
}
