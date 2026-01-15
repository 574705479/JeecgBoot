package org.jeecg.modules.airag.cs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.airag.cs.entity.CsMessage;
import org.jeecg.modules.airag.cs.service.ICsMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 消息管理 (重构版)
 * 
 * @author jeecg
 * @date 2026-01-12
 */
@Slf4j
@Tag(name = "客服消息管理")
@RestController
@RequestMapping("/cs/message")
public class CsMessageController {

    @Autowired
    private ICsMessageService messageService;

    /**
     * 发送消息 (通用)
     */
    @Operation(summary = "发送消息")
    @PostMapping("/send")
    public Result<CsMessage> send(@RequestBody Map<String, Object> params) {
        String conversationId = (String) params.get("conversationId");
        String content = (String) params.get("content");
        String senderId = (String) params.get("senderId");
        String senderName = (String) params.get("senderName");
        
        // 兼容处理 senderType，可能是字符串或数字
        Object senderTypeObj = params.get("senderType");
        String senderType;
        if (senderTypeObj instanceof Integer) {
            // 1=用户, 2=客服
            senderType = ((Integer) senderTypeObj) == 1 ? "user" : "agent";
        } else if (senderTypeObj instanceof String) {
            String typeStr = (String) senderTypeObj;
            // 支持 "1", "user" 等格式
            senderType = "1".equals(typeStr) || "user".equals(typeStr) ? "user" : "agent";
        } else {
            senderType = "user"; // 默认用户
        }
        
        CsMessage message;
        if ("user".equals(senderType)) {
            message = messageService.sendUserMessage(conversationId, senderId, senderName, content);
        } else {
            message = messageService.sendAgentMessage(conversationId, senderId, senderName, content);
        }
        
        return Result.OK(message);
    }

    /**
     * 用户发送消息
     */
    @Operation(summary = "用户发送消息")
    @PostMapping("/user/send")
    public Result<CsMessage> sendUserMessage(@RequestBody Map<String, String> params) {
        String conversationId = params.get("conversationId");
        String userId = params.get("userId");
        String userName = params.get("userName");
        String content = params.get("content");
        
        CsMessage message = messageService.sendUserMessage(conversationId, userId, userName, content);
        return Result.OK(message);
    }

    /**
     * 客服发送消息
     */
    @Operation(summary = "客服发送消息")
    @PostMapping("/agent/send")
    public Result<CsMessage> sendAgentMessage(@RequestBody Map<String, String> params) {
        String conversationId = params.get("conversationId");
        String agentId = params.get("agentId");
        String agentName = params.get("agentName");
        String content = params.get("content");
        
        CsMessage message = messageService.sendAgentMessage(conversationId, agentId, agentName, content);
        return Result.OK(message);
    }

    /**
     * 获取会话消息
     */
    @Operation(summary = "获取会话消息")
    @GetMapping("/{conversationId}")
    public Result<List<CsMessage>> getMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "50") Integer limit) {
        List<CsMessage> messages = messageService.getMessages(conversationId, limit);
        return Result.OK(messages);
    }

    /**
     * 获取会话消息（通过参数）
     */
    @Operation(summary = "获取会话消息列表")
    @GetMapping("/list")
    public Result<List<CsMessage>> getMessageList(
            @RequestParam String conversationId,
            @RequestParam(defaultValue = "100") Integer limit) {
        List<CsMessage> messages = messageService.getMessages(conversationId, limit);
        return Result.OK(messages);
    }

    /**
     * 获取会话消息（分页）
     */
    @Operation(summary = "获取会话消息(分页)")
    @GetMapping("/{conversationId}/page")
    public Result<List<CsMessage>> getMessagesPage(
            @PathVariable String conversationId,
            @RequestParam(required = false) String beforeId,
            @RequestParam(defaultValue = "20") Integer limit) {
        List<CsMessage> messages = messageService.getMessages(conversationId, beforeId, limit);
        return Result.OK(messages);
    }

    // ==================== AI相关 ====================

    /**
     * 获取AI建议
     */
    @Operation(summary = "获取AI建议")
    @GetMapping("/ai-suggestion/{conversationId}")
    public Result<Map<String, Object>> getAiSuggestion(@PathVariable String conversationId) {
        String suggestion = messageService.getCurrentAiSuggestion(conversationId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("suggestion", suggestion);
        result.put("hasSuggestion", suggestion != null);
        
        return Result.OK(result);
    }

    /**
     * 确认AI建议
     */
    @Operation(summary = "确认AI建议")
    @PostMapping("/ai-confirm/{conversationId}")
    public Result<CsMessage> confirmAiSuggestion(
            @PathVariable String conversationId,
            @RequestBody Map<String, String> params) {
        
        String suggestionId = params.get("suggestionId");
        String agentId = params.get("agentId");
        String agentName = params.get("agentName");
        String editedContent = params.get("editedContent");
        
        CsMessage message = messageService.confirmAiSuggestion(
                conversationId, suggestionId, agentId, agentName, editedContent);
        
        if (message == null) {
            return Result.error("AI建议已过期或不存在");
        }
        
        return Result.OK(message);
    }

    /**
     * 生成AI建议（流式）
     * 建议内容通过WebSocket推送，这里只返回状态
     */
    @Operation(summary = "生成AI建议")
    @PostMapping("/ai-generate/{conversationId}")
    public Result<Map<String, Object>> generateAiSuggestion(
            @PathVariable String conversationId,
            @RequestBody Map<String, String> params) {
        
        String userMessage = params.get("userMessage");
        String result = messageService.generateAiSuggestion(conversationId, userMessage);
        
        Map<String, Object> response = new HashMap<>();
        // 返回__STREAMING__表示正在流式生成，建议通过WebSocket推送
        if ("__STREAMING__".equals(result)) {
            response.put("streaming", true);
            response.put("success", true);
            response.put("message", "AI建议正在生成，请通过WebSocket接收");
        } else if (result != null) {
            response.put("suggestion", result);
            response.put("success", true);
        } else {
            response.put("success", false);
            response.put("message", "AI建议生成失败");
        }
        
        return Result.OK(response);
    }

    // ==================== 已读状态 ====================

    /**
     * 标记已读
     */
    @Operation(summary = "标记已读")
    @PostMapping("/{conversationId}/read")
    public Result<String> markAsRead(
            @PathVariable String conversationId,
            @RequestParam String userId) {
        messageService.markAsRead(conversationId, userId);
        return Result.OK("已标记已读");
    }

    /**
     * 获取未读数
     */
    @Operation(summary = "获取未读数")
    @GetMapping("/{conversationId}/unread")
    public Result<Map<String, Object>> getUnreadCount(@PathVariable String conversationId) {
        int count = messageService.getUnreadCount(conversationId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("unreadCount", count);
        
        return Result.OK(result);
    }
}
