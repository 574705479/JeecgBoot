package org.jeecg.modules.airag.cs.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.service.TokenStream;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.app.entity.AiragApp;
import org.jeecg.modules.airag.app.mapper.AiragAppMapper;
import org.jeecg.modules.airag.chat.entity.ChatMessage;
import org.jeecg.modules.airag.chat.service.IChatMessageService;
import org.jeecg.modules.airag.common.handler.AIChatParams;
import org.jeecg.modules.airag.cs.async.CsAsyncTaskExecutor;
import org.jeecg.modules.airag.cs.entity.CsAgent;
import org.jeecg.modules.airag.cs.entity.CsCollaborator;
import org.jeecg.modules.airag.cs.entity.CsConversation;
import org.jeecg.modules.airag.cs.entity.CsMessage;
import org.jeecg.modules.airag.cs.mapper.CsGlobalConfigMapper;
import org.jeecg.modules.airag.cs.entity.CsGlobalConfig;
import org.jeecg.modules.airag.cs.service.ICsAgentService;
import org.jeecg.modules.airag.cs.service.ICsCollaboratorService;
import org.jeecg.modules.airag.cs.service.ICsConversationService;
import org.jeecg.modules.airag.cs.service.ICsMessageService;
import org.jeecg.modules.airag.cs.websocket.CsWebSocketMessage;
import org.jeecg.modules.airag.cs.websocket.CsWebSocketSessionManager;
import org.jeecg.modules.airag.llm.handler.AIChatHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 消息服务实现 (重构版)
 * 
 * 核心功能:
 * 1. 根据回复模式处理消息 (AI自动/AI辅助/手动)
 * 2. 消息存储到MongoDB
 * 3. 实时推送给相关方
 * 
 * @author jeecg
 * @date 2026-01-12
 */
@Slf4j
@Service
public class CsMessageServiceImpl implements ICsMessageService {

    /** 访客AI应用全局配置的Redis Key */
    private static final String VISITOR_APP_REDIS_KEY = "cs:global:visitor_app_id";
    private static final String VISITOR_APP_CONFIG_KEY = "visitor_app_id";

    /** 聊天窗口设置的Redis Key（含FAQ配置） */
    private static final String CHAT_WINDOW_REDIS_KEY = "cs:global:chat_window_settings";

    @Autowired
    private IChatMessageService chatMessageService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private CsGlobalConfigMapper csGlobalConfigMapper;

    @Autowired
    @Lazy
    private ICsConversationService conversationService;

    @Autowired
    private CsWebSocketSessionManager sessionManager;

    @Autowired
    private AIChatHandler aiChatHandler;

    @Autowired
    private AiragAppMapper airagAppMapper;

    @Autowired
    @Lazy
    private ICsAgentService agentService;

    @Autowired
    private ICsCollaboratorService collaboratorService;

    @Autowired
    private CsAsyncTaskExecutor asyncTaskExecutor;

    // AI建议缓存 (conversationId -> suggestion)
    private final Map<String, String> aiSuggestionCache = new ConcurrentHashMap<>();

    // AI流式回复取消标记 (conversationId -> cancelled flag)
    private final ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicBoolean> activeAiStreams = new ConcurrentHashMap<>();

    private static final long COLLABORATOR_CACHE_TTL_MS = 30000L;
    private final Map<String, CollaboratorCacheEntry> collaboratorCache = new ConcurrentHashMap<>();

    // ==================== AI流式取消 ====================

    @Override
    public void cancelAiStream(String conversationId) {
        java.util.concurrent.atomic.AtomicBoolean cancelled = activeAiStreams.get(conversationId);
        if (cancelled != null) {
            cancelled.set(true);
            log.info("[CS-Message] AI流式回复已标记取消: conversationId={}", conversationId);
        } else {
            log.debug("[CS-Message] 取消AI流式回复: 无活跃流 conversationId={}", conversationId);
        }
    }

    @Override
    public void cancelAiSuggestionStream(String conversationId) {
        String key = "suggestion:" + conversationId;
        java.util.concurrent.atomic.AtomicBoolean cancelled = activeAiStreams.get(key);
        if (cancelled != null) {
            cancelled.set(true);
            log.info("[CS-Message] AI建议流式已标记取消: conversationId={}", conversationId);
        } else {
            log.debug("[CS-Message] 取消AI建议流式: 无活跃流 conversationId={}", conversationId);
        }
    }

    // ==================== 消息发送 ====================

    @Override
    public CsMessage sendUserMessage(String conversationId, String userId, String userName, String content) {
        log.info("[CS-Message] 用户发送消息: conversationId={}, userId={}", conversationId, userId);
        
        // 确保会话存在
        CsConversation conversation = conversationService.getOrCreateConversation(
                conversationId, null, userId, userName);
        
        // ★ 诊断日志：检查会话状态
        log.info("[CS-Message] 会话状态: conversationId={}, status={}, ownerAgentId={}, replyMode={}", 
                conversationId, 
                conversation.getStatus(), 
                conversation.getOwnerAgentId(), 
                conversation.getReplyMode());
        
        // 创建用户消息
        CsMessage userMessage = CsMessage.createUserMessage(conversationId, userId, userName, content);
        
        // 保存到MongoDB
        saveToMongo(userMessage);
        
        // 更新会话最后消息
        conversationService.updateLastMessage(conversationId, content, 0);
        
        // 重置超时提醒标记（用户活跃，取消超时倒计时）
        conversationService.resetTimeoutWarning(conversationId);
        
        // 标记访客发消息时间（用于客服超时未回复精确判断）
        conversationService.updateVisitorLastMsgTime(conversationId);
        
        // 推送给所有相关客服
        pushToAgents(conversation, userMessage);
        
        // 增加客服未读数
        conversationService.incrementUnread(conversationId);
        
        // FAQ关键词匹配（最高优先级，匹配成功则跳过AI回复）
        if (tryFaqKeywordMatch(conversationId, content)) {
            return userMessage;
        }
        
        // 根据回复模式处理
        int replyMode = conversation.getReplyMode() != null ? 
                conversation.getReplyMode() : CsConversation.REPLY_MODE_AI_AUTO;
        
        switch (replyMode) {
            case CsConversation.REPLY_MODE_AI_AUTO:
                // AI自动模式：生成并发送AI回复
                generateAndSendAiReply(conversation, content);
                break;
                
            case CsConversation.REPLY_MODE_AI_ASSIST:
                // AI辅助模式：流式生成建议推送给客服（使用客服AI建议应用，支持知识库）
                generateAiSuggestionStream(conversation, content, null);
                break;
                
            case CsConversation.REPLY_MODE_MANUAL:
                // 手动模式：不做任何处理，等待客服回复
                break;
        }
        
        return userMessage;
    }

    @Override
    public CsMessage sendUserMessageRaw(String conversationId, String userId, String userName, String content) {
        log.info("[CS-Message] 用户发送消息(Raw，不触发AI): conversationId={}, userId={}", conversationId, userId);

        // 确保会话存在
        CsConversation conversation = conversationService.getOrCreateConversation(
                conversationId, null, userId, userName);

        // 创建用户消息
        CsMessage userMessage = CsMessage.createUserMessage(conversationId, userId, userName, content);

        // 保存到MongoDB
        saveToMongo(userMessage);

        // 更新会话最后消息
        conversationService.updateLastMessage(conversationId, content, 0);

        // 重置超时提醒标记
        conversationService.resetTimeoutWarning(conversationId);

        // 标记访客发消息时间（用于客服超时未回复精确判断）
        conversationService.updateVisitorLastMsgTime(conversationId);

        // 推送给访客自己（因为消息是后端代发的，前端需要通过WebSocket接收）
        pushToUser(conversationId, userMessage);

        // 推送给所有相关客服
        pushToAgents(conversation, userMessage);

        // 增加客服未读数
        conversationService.incrementUnread(conversationId);

        // 不触发AI回复，直接返回
        return userMessage;
    }

    @Override
    public CsMessage sendUserMessage(String conversationId, String userId, String userName, String content,
                                     Integer msgType, String extra) {
        log.info("[CS-Message] 用户发送消息(含附件): conversationId={}, userId={}, msgType={}", conversationId, userId, msgType);

        // 确保会话存在
        CsConversation conversation = conversationService.getOrCreateConversation(
                conversationId, null, userId, userName);

        // 创建用户消息，在保存之前就设置好 msgType 和 extra，避免二次保存导致重复
        CsMessage userMessage = CsMessage.createUserMessage(conversationId, userId, userName, content);
        if (msgType != null && msgType != CsMessage.MSG_TYPE_TEXT) {
            userMessage.setMsgType(msgType);
        }
        if (oConvertUtils.isNotEmpty(extra)) {
            userMessage.setExtra(extra);
        }

        // 保存到MongoDB（一次性，包含附件信息）
        saveToMongo(userMessage);

        // 更新会话最后消息
        conversationService.updateLastMessage(conversationId, content, 0);

        // 重置超时提醒标记
        conversationService.resetTimeoutWarning(conversationId);

        // 标记访客发消息时间（用于客服超时未回复精确判断）
        conversationService.updateVisitorLastMsgTime(conversationId);

        // 推送给所有相关客服
        pushToAgents(conversation, userMessage);

        // 增加客服未读数
        conversationService.incrementUnread(conversationId);

        // FAQ关键词匹配（最高优先级，匹配成功则跳过AI回复）
        if (tryFaqKeywordMatch(conversationId, content)) {
            return userMessage;
        }

        // 根据回复模式处理
        int replyMode = conversation.getReplyMode() != null ?
                conversation.getReplyMode() : CsConversation.REPLY_MODE_AI_AUTO;

        switch (replyMode) {
            case CsConversation.REPLY_MODE_AI_AUTO:
                generateAndSendAiReply(conversation, content);
                break;
            case CsConversation.REPLY_MODE_AI_ASSIST:
                generateAiSuggestionStream(conversation, content, null);
                break;
            case CsConversation.REPLY_MODE_MANUAL:
                break;
        }

        return userMessage;
    }

    /**
     * FAQ关键词匹配（最高优先级）
     * 用户发送的消息中包含FAQ配置的任意关键词时，自动发送预设答案
     * @param conversationId 会话ID
     * @param content 用户消息内容
     * @return true=匹配成功并已发送答案, false=无匹配
     */
    private boolean tryFaqKeywordMatch(String conversationId, String content) {
        if (oConvertUtils.isEmpty(content)) {
            return false;
        }
        try {
            String settingsJson = redisTemplate.opsForValue().get(CHAT_WINDOW_REDIS_KEY);
            if (oConvertUtils.isEmpty(settingsJson)) {
                return false;
            }

            JSONObject settings = JSON.parseObject(settingsJson);
            Boolean faqEnabled = settings.getBoolean("faqEnabled");
            if (faqEnabled == null || !faqEnabled) {
                return false;
            }

            JSONArray faqList = settings.getJSONArray("faqList");
            if (faqList == null || faqList.isEmpty()) {
                return false;
            }

            String lowerContent = content.toLowerCase().trim();

            // 遍历FAQ列表，按顺序匹配（第一个命中的FAQ生效）
            for (int i = 0; i < faqList.size(); i++) {
                JSONObject faq = faqList.getJSONObject(i);
                if (faq == null) continue;

                JSONArray keywords = faq.getJSONArray("keywords");
                if (keywords == null || keywords.isEmpty()) continue;

                for (int j = 0; j < keywords.size(); j++) {
                    String keyword = keywords.getString(j);
                    if (oConvertUtils.isNotEmpty(keyword)
                            && lowerContent.contains(keyword.toLowerCase().trim())) {
                        // 匹配成功，发送FAQ预设答案
                        String answer = faq.getString("answer");
                        if (oConvertUtils.isNotEmpty(answer)) {
                            log.info("[CS-Message] FAQ关键词匹配成功: conversationId={}, keyword={}, faqIndex={}", 
                                    conversationId, keyword, i);
                            sendAgentMessage(conversationId, "faq_system", "智能助手", answer, 0, null);
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[CS-Message] FAQ关键词匹配异常", e);
        }
        return false;
    }

    @Override
    public CsMessage sendAgentMessage(String conversationId, String agentId, String agentName, String content,
                                      Integer msgType, String extra) {
        log.info("[CS-Message] 客服发送消息: conversationId={}, agentId={}", conversationId, agentId);
        
        CsConversation conversation = conversationService.getConversation(conversationId);
        
        // ★ 如果会话是待接入状态，客服发送消息时自动接入该会话（排除FAQ系统）
        if (conversation != null && conversation.getStatus() == CsConversation.STATUS_UNASSIGNED
                && !"faq_system".equals(agentId)) {
            boolean assigned = conversationService.assignToAgent(conversationId, agentId);
            if (assigned) {
                log.info("[CS-Message] 客服发送消息，自动接入会话: conversationId={}, agentId={}", conversationId, agentId);
                // 重新获取会话信息
                conversation = conversationService.getConversation(conversationId);
            }
        }
        
        // ★ 客服发送消息时，自动切换到手动模式（终止AI自动回复），排除FAQ系统消息
        if (conversation != null && conversation.getReplyMode() != CsConversation.REPLY_MODE_MANUAL
                && !"faq_system".equals(agentId)) {
            conversationService.changeReplyMode(conversationId, CsConversation.REPLY_MODE_MANUAL);
            log.info("[CS-Message] 客服发送消息，自动切换为手动模式: conversationId={}", conversationId);
        }
        
        // 创建客服消息（用户看到的显示为"客服"）
        CsMessage agentMessage = CsMessage.createAgentMessage(conversationId, agentId, agentName, content);
        agentMessage.setSenderAvatar(resolveAgentAvatar(agentId));
        if (msgType != null) {
            agentMessage.setMsgType(msgType);
        }
        if (oConvertUtils.isNotEmpty(extra)) {
            agentMessage.setExtra(extra);
        }
        agentMessage.setSenderName(agentName); // 显示实际客服名称
        
        // 保存到MongoDB（异步）
        asyncTaskExecutor.submitMongo(() -> saveToMongo(agentMessage));

        // 更新会话最后消息 + 清除未读 + 清除访客等待标记（异步）
        String lastMessage = buildMessagePreview(content, msgType, extra);
        asyncTaskExecutor.submitConversation(() -> {
            conversationService.updateLastMessage(conversationId, lastMessage, 2);
            conversationService.clearUnread(conversationId);
            // 客服回复后清除访客等待标记（FAQ自动回复也算已回复）
            conversationService.clearVisitorLastMsgTime(conversationId);
        });

        // 推送给用户 + 其他客服（异步）
        CsConversation conversationSnapshot = conversation;
        asyncTaskExecutor.submitWs(() -> {
            boolean delivered = pushToUser(conversationId, agentMessage);
            if (!delivered) {
                String userId = conversationSnapshot != null ? conversationSnapshot.getUserId() : null;
                Map<String, Object> notifyExtra = new HashMap<>();
                notifyExtra.put("reason", "USER_OFFLINE");
                notifyExtra.put("userId", userId);
                notifyExtra.put("messageId", agentMessage.getId());
                CsWebSocketMessage deliveryFailed = CsWebSocketMessage.builder()
                        .type("delivery_failed")
                        .conversationId(conversationId)
                        .content("用户不在线，消息未送达")
                        .extra(notifyExtra)
                        .timestamp(agentMessage.getCreateTime())
                        .build();
                sessionManager.sendToAgent(agentId, deliveryFailed);
            }
            pushToOtherAgents(conversationId, agentId, agentMessage);
        });
        
        // 清除AI建议缓存
        aiSuggestionCache.remove(conversationId);
        
        return agentMessage;
    }

    @Override
    public CsMessage sendSystemMessage(String conversationId, String content) {
        return sendSystemMessage(conversationId, content, true);
    }

    @Override
    public CsMessage sendSystemMessage(String conversationId, String content, boolean persist) {
        log.info("[CS-Message] 系统消息: conversationId={}, content={}, persist={}", conversationId, content, persist);
        
        CsMessage systemMessage = CsMessage.createSystemMessage(conversationId, content);
        
        // 只有persist为true时才保存到MongoDB
        if (persist) {
            saveToMongo(systemMessage);
        }
        
        // 推送给用户
        pushToUser(conversationId, systemMessage);
        
        // 推送给所有客服
        CsConversation conversation = conversationService.getConversation(conversationId);
        if (conversation != null) {
            pushToAgents(conversation, systemMessage);
        }
        
        return systemMessage;
    }

    @Override
    public CsMessage sendVisitorPrologue(String conversationId) {
        if (oConvertUtils.isEmpty(conversationId)) {
            return null;
        }

        String appId = getGlobalVisitorAppId();
        if (oConvertUtils.isEmpty(appId)) {
            return null;
        }

        AiragApp app = airagAppMapper.getByIdIgnoreTenant(appId);
        if (app == null || oConvertUtils.isEmpty(app.getPrologue())) {
            return null;
        }

        String displayName = oConvertUtils.isNotEmpty(app.getName()) ? app.getName() : "智能客服";
        CsMessage aiMessage = CsMessage.createAiMessage(conversationId, displayName, app.getPrologue());

        saveToMongo(aiMessage);
        conversationService.updateLastMessage(conversationId, app.getPrologue(), 1);

        pushToUser(conversationId, aiMessage);

        CsConversation conversation = conversationService.getConversation(conversationId);
        if (conversation != null) {
            pushToAgents(conversation, aiMessage);
        }

        return aiMessage;
    }

    @Override
    public void sendAutoMessages(String conversationId, String agentId, String agentName, String userLang) {
        sendConfiguredAutoMessages(conversationId, agentId, agentName, userLang, true);
    }

    @Override
    public void sendVisitorAutoMessagesAsAgent(String conversationId, String agentId, String agentName, String userLang) {
        sendConfiguredAutoMessages(conversationId, agentId, agentName, userLang, false);
    }

    private void sendConfiguredAutoMessages(String conversationId, String agentId, String agentName, String userLang,
                                            boolean switchToManualMode) {
        if (oConvertUtils.isEmpty(conversationId) || oConvertUtils.isEmpty(agentId)) {
            return;
        }

        List<String> contents = resolveAutoMessageContents(userLang);
        if (contents.isEmpty()) {
            return;
        }

        for (String content : contents) {
            if (switchToManualMode) {
                sendAgentMessage(conversationId, agentId, agentName, content, null, null);
            } else {
                sendAgentWelcomeMessage(conversationId, agentId, agentName, content);
            }
        }

        log.info("[CS-Message] 自动消息已发送: conversationId={}, agentId={}, lang={}, count={}, switchToManual={}",
                conversationId, agentId, mapUserLang(userLang), contents.size(), switchToManualMode);
    }

    private List<String> resolveAutoMessageContents(String userLang) {
        String autoMsgRedisKey = "cs:global:auto_messages";
        String autoMsgConfigKey = "auto_messages";
        String json = redisTemplate.opsForValue().get(autoMsgRedisKey);
        if (json == null || json.isEmpty()) {
            CsGlobalConfig config = csGlobalConfigMapper.selectById(autoMsgConfigKey);
            json = config != null ? config.getConfigValue() : null;
            if (json != null && !json.isEmpty()) {
                redisTemplate.opsForValue().set(autoMsgRedisKey, json);
            }
        }
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            JSONObject autoConfig = JSONObject.parseObject(json);
            String defaultLang = autoConfig.getString("defaultLang");
            JSONObject languages = autoConfig.getJSONObject("languages");
            if (languages == null || languages.isEmpty()) {
                return Collections.emptyList();
            }

            // 语言映射：将浏览器语言映射到配置中的语言key
            String mappedLang = mapUserLang(userLang);
            
            // 查找匹配的语言配置
            com.alibaba.fastjson.JSONArray messages = null;
            JSONObject langConfig = languages.getJSONObject(mappedLang);
            if (langConfig != null) {
                messages = langConfig.getJSONArray("messages");
            }
            
            // 如果没找到或消息为空，使用默认语言
            if ((messages == null || messages.isEmpty()) && oConvertUtils.isNotEmpty(defaultLang)) {
                langConfig = languages.getJSONObject(defaultLang);
                if (langConfig != null) {
                    messages = langConfig.getJSONArray("messages");
                }
            }

            if (messages == null || messages.isEmpty()) {
                return Collections.emptyList();
            }

            List<String> contents = new ArrayList<>();
            for (int i = 0; i < messages.size(); i++) {
                JSONObject msgObj = messages.getJSONObject(i);
                String content = msgObj.getString("content");
                if (oConvertUtils.isNotEmpty(content)) {
                    contents.add(content);
                }
            }
            return contents;
        } catch (Exception e) {
            log.warn("[CS-Message] 解析自动消息失败: userLang={}, error={}", userLang, e.getMessage());
            return Collections.emptyList();
        }
    }

    private CsMessage sendAgentWelcomeMessage(String conversationId, String agentId, String agentName, String content) {
        log.info("[CS-Message] 发送客服欢迎消息: conversationId={}, agentId={}", conversationId, agentId);

        CsConversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            return null;
        }

        CsMessage agentMessage = CsMessage.createAgentMessage(conversationId, agentId, agentName, content);
        agentMessage.setSenderAvatar(resolveAgentAvatar(agentId));
        agentMessage.setSenderName(agentName);

        asyncTaskExecutor.submitMongo(() -> saveToMongo(agentMessage));

        String lastMessage = buildMessagePreview(content, null, null);
        asyncTaskExecutor.submitConversation(() -> {
            conversationService.updateLastMessage(conversationId, lastMessage, 2);
            conversationService.clearUnread(conversationId);
        });

        CsConversation conversationSnapshot = conversation;
        asyncTaskExecutor.submitWs(() -> {
            boolean delivered = pushToUser(conversationId, agentMessage);
            if (!delivered) {
                String userId = conversationSnapshot.getUserId();
                Map<String, Object> notifyExtra = new HashMap<>();
                notifyExtra.put("reason", "USER_OFFLINE");
                notifyExtra.put("userId", userId);
                notifyExtra.put("messageId", agentMessage.getId());
                CsWebSocketMessage deliveryFailed = CsWebSocketMessage.builder()
                        .type("delivery_failed")
                        .conversationId(conversationId)
                        .content("用户不在线，消息未送达")
                        .extra(notifyExtra)
                        .timestamp(agentMessage.getCreateTime())
                        .build();
                sessionManager.sendToAgent(agentId, deliveryFailed);
            }
            pushToOtherAgents(conversationId, agentId, agentMessage);
        });

        aiSuggestionCache.remove(conversationId);
        return agentMessage;
    }

    /**
     * 映射用户浏览器语言到配置语言key
     * zh-CN -> zh-CN, zh -> zh-CN, zh-TW/zh-HK -> zh-TW, en-* -> en
     */
    private String mapUserLang(String userLang) {
        if (oConvertUtils.isEmpty(userLang)) {
            return "";
        }
        String lang = userLang.toLowerCase().trim();
        if ("zh-cn".equals(lang) || "zh".equals(lang)) {
            return "zh-CN";
        }
        if ("zh-tw".equals(lang) || "zh-hk".equals(lang)) {
            return "zh-TW";
        }
        if (lang.startsWith("en")) {
            return "en";
        }
        // 原样返回，尝试直接匹配
        return userLang;
    }

    @Override
    public CsMessage sendMessage(CsMessage message) {
        // 保存到MongoDB
        saveToMongo(message);
        
        // 更新会话
        conversationService.updateLastMessage(message.getConversationId(), message.getContent(),
                message.getSenderType() != null ? message.getSenderType() : 3);
        
        return message;
    }

    // ==================== AI相关 ====================

    @Override
    public String generateAiSuggestion(String conversationId, String userMessage) {
        return generateAiSuggestion(conversationId, userMessage, null);
    }

    @Override
    public String generateAiSuggestion(String conversationId, String userMessage, String agentId) {
        try {
            log.info("[CS-Message] 开始生成流式AI建议: conversationId={}", conversationId);
            
            // 获取会话信息
            CsConversation conversation = conversationService.getConversation(conversationId);
            if (conversation == null) {
                return null;
            }
            
            // 异步调用流式AI服务，通过WebSocket返回
            generateAiSuggestionStream(conversation, userMessage, agentId);
            
            // 返回一个标识，表示正在生成
            return "__STREAMING__";
        } catch (Exception e) {
            log.error("[CS-Message] 生成AI建议失败: conversationId={}", conversationId, e);
            return null;
        }
    }
    
    /**
     * 流式生成AI建议，通过WebSocket推送给客服
     * 支持通过 cancelAiSuggestionStream() 取消正在进行的流式建议
     */
    @Async
    public void generateAiSuggestionStream(CsConversation conversation, String userMessage, String fallbackAgentId) {
        String conversationId = conversation.getId();
        String ownerAgentId = conversation.getOwnerAgentId();
        String targetAgentId = oConvertUtils.isNotEmpty(fallbackAgentId) ? fallbackAgentId : ownerAgentId;

        if (oConvertUtils.isEmpty(targetAgentId)) {
            log.warn("[CS-Message] 会话没有分配客服且未指定请求客服，无法推送AI建议");
            return;
        }

        // 注册取消标记
        String cancelKey = "suggestion:" + conversationId;
        java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean(false);
        activeAiStreams.put(cancelKey, cancelled);
        
        try {
            StringBuilder fullSuggestion = new StringBuilder();
            
            // 调用流式AI服务 (forVisitor=false: 使用客服AI建议应用)
            callAiServiceStream(conversationId, userMessage, new AiStreamCallback() {
                @Override
                public void onToken(String token, boolean isComplete) {
                    try {
                        // 检查取消标记
                        if (cancelled.get()) {
                            log.info("[CS-Message] AI建议流式已被取消，停止处理: conversationId={}", conversationId);
                            activeAiStreams.remove(cancelKey);
                            return;
                        }

                        if (token != null && !token.isEmpty()) {
                            fullSuggestion.append(token);
                            
                            // 通过WebSocket发送流式AI建议
                            CsWebSocketMessage streamMsg = CsWebSocketMessage.builder()
                                    .type("ai_suggestion_stream")
                                    .conversationId(conversationId)
                                    .content(token)
                                    .extra(Map.of("isComplete", false))
                                    .build();
                            
                            sessionManager.sendToAgent(targetAgentId, streamMsg);
                        }
                        
                        if (isComplete) {
                            String suggestion = fullSuggestion.toString();
                            log.info("[CS-Message] AI建议流式生成完成: conversationId={}, length={}", 
                                    conversationId, suggestion.length());
                            
                            // 缓存完整建议
                            if (oConvertUtils.isNotEmpty(suggestion)) {
                                aiSuggestionCache.put(conversationId, suggestion);
                            }
                            
                            // 发送完成消息
                            CsWebSocketMessage completeMsg = CsWebSocketMessage.builder()
                                    .type("ai_suggestion_complete")
                                    .conversationId(conversationId)
                                    .content(suggestion)
                                    .build();
                            
                            sessionManager.sendToAgent(targetAgentId, completeMsg);
                            activeAiStreams.remove(cancelKey);
                        }
                    } catch (Exception e) {
                        log.error("[CS-Message] 处理AI建议流式token失败", e);
                    }
                }
            }, false);  // forVisitor=false: 使用客服AI建议应用
            
        } catch (Exception e) {
            log.error("[CS-Message] 流式AI建议生成失败: conversationId={}", conversationId, e);
            activeAiStreams.remove(cancelKey);
            
            // 发送错误消息
            CsWebSocketMessage errorMsg = CsWebSocketMessage.builder()
                    .type("ai_suggestion_error")
                    .conversationId(conversationId)
                    .error("AI建议生成失败: " + e.getMessage())
                    .build();
            
            sessionManager.sendToAgent(targetAgentId, errorMsg);
        }
    }

    @Override
    public CsMessage confirmAiSuggestion(String conversationId, String suggestionId, 
                                          String agentId, String agentName, String editedContent) {
        log.info("[CS-Message] 确认AI建议: conversationId={}, agentId={}", conversationId, agentId);
        
        // 获取原始建议或使用编辑后的内容
        String content = oConvertUtils.isNotEmpty(editedContent) ? 
                editedContent : aiSuggestionCache.get(conversationId);
        
        if (oConvertUtils.isEmpty(content)) {
            log.warn("[CS-Message] AI建议不存在或已过期: conversationId={}", conversationId);
            return null;
        }
        
        // 创建消息
        CsMessage message = CsMessage.createAgentMessage(conversationId, agentId, agentName, content);
        message.setSenderAvatar(resolveAgentAvatar(agentId));
        message.setIsAiGenerated(true);
        message.setAiConfirmed(true);
        message.setAiSuggestionId(suggestionId);
        
        // 保存到MongoDB
        saveToMongo(message);
        
        // 更新会话最后消息 + 清除未读 + 清除访客等待标记
        conversationService.updateLastMessage(conversationId, content, 2);
        conversationService.clearUnread(conversationId);
        conversationService.clearVisitorLastMsgTime(conversationId);
        
        // 推送给用户
        pushToUser(conversationId, message);
        
        // 推送给其他客服
        pushToOtherAgents(conversationId, agentId, message);
        
        // 清除缓存
        aiSuggestionCache.remove(conversationId);
        
        return message;
    }

    @Override
    public String getCurrentAiSuggestion(String conversationId) {
        return aiSuggestionCache.get(conversationId);
    }

    // ==================== 消息撤回 ====================

    @Override
    public boolean recallMessage(String messageId, String agentId) {
        log.info("[CS-Message] 撤回消息: messageId={}, agentId={}", messageId, agentId);

        ChatMessage message = chatMessageService.findById(messageId);
        if (message == null) {
            log.warn("[CS-Message] 撤回失败，消息不存在: messageId={}", messageId);
            return false;
        }

        Integer senderType = message.getSenderType();
        if (senderType == null || (senderType != ChatMessage.SENDER_AI && senderType != ChatMessage.SENDER_AGENT)) {
            log.warn("[CS-Message] 撤回失败，只能撤回客服/AI消息: messageId={}, senderType={}", messageId, senderType);
            return false;
        }

        if (senderType == ChatMessage.SENDER_AGENT) {
            String senderId = message.getSenderId();
            if (senderId != null && !agentId.equals(senderId)) {
                log.warn("[CS-Message] 撤回失败，只能撤回自己发送的消息: messageId={}, senderId={}, agentId={}", messageId, senderId, agentId);
                return false;
            }
            if (senderId == null) {
                CsConversation conv = conversationService.getById(message.getConversationId());
                if (conv != null && !agentId.equals(conv.getOwnerAgentId())) {
                    log.warn("[CS-Message] 撤回失败，senderId为空且不是会话负责客服: messageId={}, ownerAgentId={}, agentId={}", messageId, conv.getOwnerAgentId(), agentId);
                    return false;
                }
            }
        }

        boolean updated = chatMessageService.updateMessageStatus(messageId, ChatMessage.STATUS_REVOKED);
        if (!updated) {
            log.error("[CS-Message] 撤回失败，更新MongoDB状态失败: messageId={}", messageId);
            return false;
        }

        String conversationId = message.getConversationId();
        CsWebSocketMessage wsMessage = CsWebSocketMessage.builder()
                .type(CsWebSocketMessage.TYPE_MESSAGE_RECALL)
                .conversationId(conversationId)
                .messageId(messageId)
                .senderId(agentId)
                .timestamp(new java.util.Date())
                .build();

        CsConversation conversation = conversationService.getById(conversationId);
        if (conversation != null) {
            String userId = conversation.getUserId();
            sessionManager.sendToUserByConversation(conversationId, userId, wsMessage);
        }
        sessionManager.sendToAllAgents(wsMessage);

        log.info("[CS-Message] 消息撤回成功: messageId={}, conversationId={}", messageId, conversationId);
        return true;
    }

    // ==================== 消息查询 ====================

    @Override
    public List<CsMessage> getMessages(String conversationId, int limit) {
        try {
            // 从MongoDB获取消息
            List<ChatMessage> chatMessages = chatMessageService.getRecentMessages(conversationId, limit);
            if (chatMessages == null) {
                return new ArrayList<>();
            }

            return toCsMessages(chatMessages, conversationId);
        } catch (Exception e) {
            log.error("[CS-Message] 获取消息失败: conversationId={}", conversationId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<CsMessage> getMessages(String conversationId, String beforeId, int limit) {
        try {
            List<ChatMessage> chatMessages = chatMessageService.getMessagesBefore(conversationId, beforeId, limit);
            if (chatMessages == null) {
                return new ArrayList<>();
            }
            return toCsMessages(chatMessages, conversationId);
        } catch (Exception e) {
            log.error("[CS-Message] 分页获取消息失败: conversationId={}, beforeId={}", conversationId, beforeId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<CsMessage> getRecentMessages(String conversationId, int limit) {
        return getMessages(conversationId, limit);
    }

    // ==================== 已读状态 ====================

    @Override
    public void markAsRead(String conversationId, String userId) {
        // 清除未读数
        conversationService.clearUnread(conversationId);
    }

    @Override
    public int getUnreadCount(String conversationId) {
        CsConversation conversation = conversationService.getById(conversationId);
        return conversation != null && conversation.getUnreadCount() != null ? 
                conversation.getUnreadCount() : 0;
    }

    // ==================== 内部方法 ====================

    /**
     * 保存消息到MongoDB
     */
    private void saveToMongo(CsMessage message) {
        try {
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setId(message.getId());
            chatMessage.setConversationId(message.getConversationId());
            chatMessage.setContent(message.getContent());
            chatMessage.setSenderId(message.getSenderId());
            chatMessage.setSenderName(message.getSenderName());
            chatMessage.setCreateTime(message.getCreateTime() != null ? message.getCreateTime() : new Date());
            chatMessage.setSenderType(message.getSenderType());
            chatMessage.setSenderAvatar(message.getSenderAvatar());
            chatMessage.setIsAiGenerated(message.getIsAiGenerated());
            chatMessage.setAiConfirmed(message.getAiConfirmed());
            chatMessage.setAiSuggestionId(message.getAiSuggestionId());
            chatMessage.setMsgType(message.getMsgType() != null ? message.getMsgType() : ChatMessage.MSG_TYPE_TEXT);
            if (oConvertUtils.isNotEmpty(message.getExtra())) {
                try {
                    chatMessage.setExtra(JSONObject.parseObject(message.getExtra()));
                } catch (Exception e) {
                    log.warn("[CS-Message] 解析extra失败，已忽略: {}", e.getMessage());
                }
            }
            
            chatMessageService.saveMessage(chatMessage);
        } catch (Exception e) {
            log.error("[CS-Message] 保存消息到MongoDB失败", e);
        }
    }

    private List<CsCollaborator> getActiveCollaboratorsCached(String conversationId) {
        if (oConvertUtils.isEmpty(conversationId)) {
            return Collections.emptyList();
        }
        long now = System.currentTimeMillis();
        CollaboratorCacheEntry cached = collaboratorCache.get(conversationId);
        if (cached != null && cached.expiresAt > now) {
            return cached.data;
        }
        List<CsCollaborator> list = collaboratorService.getCollaborators(conversationId);
        List<CsCollaborator> safeList = list != null ? list : Collections.emptyList();
        collaboratorCache.put(conversationId, new CollaboratorCacheEntry(safeList, now + COLLABORATOR_CACHE_TTL_MS));
        return safeList;
    }

    private static class CollaboratorCacheEntry {
        private final List<CsCollaborator> data;
        private final long expiresAt;

        private CollaboratorCacheEntry(List<CsCollaborator> data, long expiresAt) {
            this.data = data;
            this.expiresAt = expiresAt;
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void cleanExpiredCollaboratorCache() {
        long now = System.currentTimeMillis();
        int removed = 0;
        var it = collaboratorCache.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().expiresAt < now) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("[CS-Message] 清理过期协作者缓存: 移除{}条", removed);
        }
    }

    private String buildMessagePreview(String content, Integer msgType, String extra) {
        if (oConvertUtils.isNotEmpty(content)) {
            // 去除HTML标签，保留纯文本摘要（避免侧边栏预览显示原始HTML标签）
            String plain = content.replaceAll("<[^>]*>", "").replaceAll("&nbsp;", " ").trim();
            if (oConvertUtils.isNotEmpty(plain)) {
                return plain.length() > 50 ? plain.substring(0, 50) + "..." : plain;
            }
            return content;
        }
        if (msgType == null) {
            return "";
        }
        if (msgType == CsMessage.MSG_TYPE_IMAGE) {
            return "[图片]";
        }
        if (msgType == CsMessage.MSG_TYPE_VIDEO) {
            return "[视频]";
        }
        if (msgType == CsMessage.MSG_TYPE_FILE) {
            return "[文件]";
        }
        if (msgType == CsMessage.MSG_TYPE_VOICE) {
            return "[语音]";
        }
        if (msgType == CsMessage.MSG_TYPE_CARD) {
            return "[卡片]";
        }
        if (msgType == CsMessage.MSG_TYPE_RICH_TEXT && oConvertUtils.isNotEmpty(extra)) {
            try {
                JSONObject obj = JSONObject.parseObject(extra);
                if (obj != null && obj.containsKey("attachments")) {
                    com.alibaba.fastjson.JSONArray list = obj.getJSONArray("attachments");
                    if (list != null && !list.isEmpty()) {
                        java.util.Set<String> labels = new java.util.LinkedHashSet<>();
                        for (int i = 0; i < list.size(); i++) {
                            JSONObject item = list.getJSONObject(i);
                            String type = item != null ? item.getString("type") : null;
                            if ("image".equals(type)) {
                                labels.add("图片");
                            } else if ("video".equals(type)) {
                                labels.add("视频");
                            } else if ("file".equals(type)) {
                                labels.add("文件");
                            }
                        }
                        if (!labels.isEmpty()) {
                            return "[" + String.join("/", labels) + "]";
                        }
                    }
                }
            } catch (Exception e) {
                log.debug("[CS-Message] 解析预览失败: {}", e.getMessage());
            }
        }
        return "[消息]";
    }

    private List<CsMessage> toCsMessages(List<ChatMessage> chatMessages, String conversationId) {
        List<CsMessage> messages = new ArrayList<>();
        for (ChatMessage msg : chatMessages) {
            CsMessage csMsg = new CsMessage();
            csMsg.setId(msg.getId());
            csMsg.setConversationId(conversationId);
            csMsg.setContent(msg.getContent());
            csMsg.setMsgType(msg.getMsgType());
            if (msg.getExtra() != null && !msg.getExtra().isEmpty()) {
                csMsg.setExtra(JSONObject.toJSONString(msg.getExtra()));
            }
            csMsg.setSenderType(msg.getSenderType() != null ? msg.getSenderType() : CsMessage.SENDER_USER);
            csMsg.setSenderId(msg.getSenderId());
            csMsg.setSenderName(msg.getSenderName());
            csMsg.setCreateTime(msg.getCreateTime());
            csMsg.setSenderAvatar(msg.getSenderAvatar());
            csMsg.setStatus(msg.getStatus() != null ? msg.getStatus() : CsMessage.STATUS_SENT);
            csMsg.setIsAiGenerated(msg.getIsAiGenerated());
            csMsg.setAiConfirmed(msg.getAiConfirmed());
            csMsg.setAiSuggestionId(msg.getAiSuggestionId());
            messages.add(csMsg);
        }
        return messages;
    }

    private Map<String, Object> parseExtraMap(String extra) {
        if (oConvertUtils.isEmpty(extra)) {
            return null;
        }
        try {
            return JSONObject.parseObject(extra);
        } catch (Exception e) {
            log.debug("[CS-Message] 解析extra失败: {}", e.getMessage());
            return null;
        }
    }

    private String resolveAgentAvatar(String agentId) {
        if (oConvertUtils.isEmpty(agentId)) {
            return null;
        }
        try {
            CsAgent agent = agentService.getById(agentId);
            return agent != null ? agent.getAvatar() : null;
        } catch (Exception e) {
            log.debug("[CS-Message] 获取客服头像失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 推送消息给用户
     */
    private boolean pushToUser(String conversationId, CsMessage message) {
        Map<String, Object> extraMap = parseExtraMap(message.getExtra());
        CsWebSocketMessage wsMessage = CsWebSocketMessage.builder()
                .type("message")
                .conversationId(conversationId)
                .messageId(message.getId())
                .content(message.getContent())
                .msgType(message.getMsgType())
                .senderId(message.getSenderId())
                .senderName(message.getSenderName())
                .senderAvatar(message.getSenderAvatar())
                .senderType(message.getSenderType())
                .isAiGenerated(message.getIsAiGenerated())
                .extra(extraMap)
                .timestamp(message.getCreateTime())
                .build();
        
        CsConversation conversation = conversationService.getById(conversationId);
        String userId = conversation != null ? conversation.getUserId() : conversationId;
        
        return sessionManager.sendToUserByConversation(conversationId, userId, wsMessage);
    }

    /**
     * 推送消息给所有相关客服（主负责人 + 协作者 + 管理者）
     * 
     * 推送策略：
     * 1. 会话未分配 -> 广播给所有在线客服
     * 2. 会话已分配 -> 推送给主负责人 + 所有活跃协作者 + 所有在线管理者（监控功能）
     */
    private void pushToAgents(CsConversation conversation, CsMessage message) {
        Map<String, Object> extraMap = parseExtraMap(message.getExtra());
        CsWebSocketMessage wsMessage = CsWebSocketMessage.builder()
                .type("message")
                .conversationId(conversation.getId())
                .messageId(message.getId())
                .content(message.getContent())
                .msgType(message.getMsgType())
                .senderId(message.getSenderId())
                .senderName(message.getSenderName())
                .senderAvatar(message.getSenderAvatar())
                .senderType(message.getSenderType())
                .isAiGenerated(message.getIsAiGenerated())
                .extra(extraMap)
                .timestamp(message.getCreateTime())
                .build();
        
        // 广播给所有在线客服（同事会话功能：所有客服都能看到所有会话）
        log.info("[CS-Message] 广播消息给所有在线客服: conversationId={}", conversation.getId());
        sessionManager.sendToAllAgents(wsMessage);
    }

    /**
     * 推送消息给其他客服（排除发送者）+ 管理者监控
     */
    private void pushToOtherAgents(String conversationId, String excludeAgentId, CsMessage message) {
        CsConversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            return;
        }
        Map<String, Object> extraMap = parseExtraMap(message.getExtra());
        CsWebSocketMessage wsMessage = CsWebSocketMessage.builder()
                .type("message")
                .conversationId(conversationId)
                .messageId(message.getId())
                .content(message.getContent())
                .msgType(message.getMsgType())
                .senderId(message.getSenderId())
                .senderName(message.getSenderName())
                .senderAvatar(message.getSenderAvatar())
                .senderType(message.getSenderType())
                .isAiGenerated(message.getIsAiGenerated())
                .extra(extraMap)
                .timestamp(message.getCreateTime())
                .build();
        
        // 收集所有在线客服ID（同事会话功能：全员推送，排除发送者）
        Set<String> agentIds = new HashSet<>();
        List<CsAgent> onlineAgents = agentService.getOnlineAgents();
        if (onlineAgents != null) {
            for (CsAgent agent : onlineAgents) {
                agentIds.add(agent.getId());
            }
        }
        
        // 排除发送者并推送
        agentIds.remove(excludeAgentId);
        agentIds.parallelStream().forEach(targetAgentId -> sessionManager.sendToAgent(targetAgentId, wsMessage));
    }

    /**
     * 生成并发送AI回复 (AI自动模式) - 流式版本
     * 通过WebSocket逐步发送AI回复，实现实时打字效果
     * 支持通过 cancelAiStream() 取消正在进行的流式回复
     */
    @Async
    public void generateAndSendAiReply(CsConversation conversation, String userMessage) {
        String conversationId = conversation.getId();
        String userId = conversation.getUserId();
        String ownerAgentId = conversation.getOwnerAgentId();
        
        // 解析 AI 显示名称（与开场白 sendVisitorPrologue 使用同一来源）
        String visitorAppId = getGlobalVisitorAppId();
        String aiDisplayName = "智能客服";
        if (oConvertUtils.isNotEmpty(visitorAppId)) {
            AiragApp visitorApp = airagAppMapper.getByIdIgnoreTenant(visitorAppId);
            if (visitorApp != null && oConvertUtils.isNotEmpty(visitorApp.getName())) {
                aiDisplayName = visitorApp.getName();
            }
        }
        
        // 注册取消标记
        java.util.concurrent.atomic.AtomicBoolean cancelled = new java.util.concurrent.atomic.AtomicBoolean(false);
        activeAiStreams.put(conversationId, cancelled);
        
        // effectively final for lambda capture
        final String displayName = aiDisplayName;
        
        try {
            log.info("[CS-Message] 开始流式AI回复: conversationId={}", conversationId);
            
            // 先发送一个"AI正在输入"的状态
            sendAiTypingStatus(conversationId, userId, true);
            
            // 生成唯一消息ID
            String messageId = java.util.UUID.randomUUID().toString().replace("-", "");
            
            // 创建一个StringBuilder来累积完整的AI回复
            StringBuilder fullResponse = new StringBuilder();
            // 标记是否已经发送过完成消息（防止重复发送）
            java.util.concurrent.atomic.AtomicBoolean completeSent = new java.util.concurrent.atomic.AtomicBoolean(false);
            
            // 调用流式AI服务 (forVisitor=true: 使用访客AI应用)
            callAiServiceStream(conversationId, userMessage, new AiStreamCallback() {
                @Override
                public void onToken(String token, boolean isComplete) {
                    try {
                        // ★ 检查取消标记：被取消后不再推送新token，直接完成
                        if (cancelled.get()) {
                            if (completeSent.compareAndSet(false, true)) {
                                finalizeAiReply(conversationId, userId, ownerAgentId, messageId, fullResponse.toString(), displayName);
                                activeAiStreams.remove(conversationId);
                            }
                            return;
                        }
                        
                        if (token != null && !token.isEmpty()) {
                            fullResponse.append(token);
                            
                            // 通过WebSocket发送增量token
                            CsWebSocketMessage streamMsg = CsWebSocketMessage.builder()
                                    .type("ai_stream")
                                    .conversationId(conversationId)
                                    .messageId(messageId)
                                    .content(token)
                                    .senderName(displayName)
                                    .extra(Map.of("isComplete", false))
                                    .build();
                            
                            // 推送给用户（精确到会话，避免多标签页串消息）
                            sessionManager.sendToUserByConversation(conversationId, userId, streamMsg);
                            
                            // 推送给客服（负责人 + 协作者 + 管理者；未分配则广播所有在线客服）
                            sendAiStreamToAgents(conversationId, ownerAgentId, streamMsg);
                        }
                        
                        if (isComplete) {
                            if (completeSent.compareAndSet(false, true)) {
                                finalizeAiReply(conversationId, userId, ownerAgentId, messageId, fullResponse.toString(), displayName);
                                activeAiStreams.remove(conversationId);
                            }
                        }
                    } catch (Exception e) {
                        log.error("[CS-Message] 处理AI流式token失败", e);
                    }
                }
            }, true);  // forVisitor=true: 使用访客AI应用
            
        } catch (Exception e) {
            log.error("[CS-Message] AI流式回复失败: conversationId={}", conversationId, e);
            activeAiStreams.remove(conversationId);
            sendAiTypingStatus(conversationId, userId, false);
            
            // 发送错误消息
            String errorMsg = "抱歉，AI服务暂时不可用，请稍后再试或联系人工客服。";
            CsMessage errorMessage = CsMessage.createAiMessage(conversationId, aiDisplayName, errorMsg);
            saveToMongo(errorMessage);
            pushToUser(conversationId, errorMessage);
        }
    }
    
    /**
     * 完成AI回复（正常完成或被取消时统一调用）
     * 保存已累积的内容到MongoDB，发送 ai_stream_complete，关闭 typing 状态
     */
    private void finalizeAiReply(String conversationId, String userId, String ownerAgentId,
                                  String messageId, String aiReply, String displayName) {
        log.info("[CS-Message] AI流式回复完成/中止: conversationId={}, length={}", 
                conversationId, aiReply != null ? aiReply.length() : 0);
        
        if (oConvertUtils.isNotEmpty(aiReply)) {
            // 创建AI消息并保存
            CsMessage aiMessage = CsMessage.createAiMessage(conversationId, displayName, aiReply);
            aiMessage.setId(messageId);
            
            // 保存到MongoDB
            saveToMongo(aiMessage);
            
            // 更新会话
            conversationService.updateLastMessage(conversationId, aiReply, 1);
            
            // 发送完成消息
            CsWebSocketMessage completeMsg = CsWebSocketMessage.builder()
                    .type("ai_stream_complete")
                    .conversationId(conversationId)
                    .messageId(messageId)
                    .content(aiReply)
                    .senderName(displayName)
                    .build();
            
            sessionManager.sendToUserByConversation(conversationId, userId, completeMsg);
            sendAiStreamToAgents(conversationId, ownerAgentId, completeMsg);
        }
        
        // 取消"AI正在输入"状态
        sendAiTypingStatus(conversationId, userId, false);
    }

    /**
     * 发送AI正在输入状态
     */
    private void sendAiTypingStatus(String conversationId, String userId, boolean isTyping) {
        CsWebSocketMessage statusMsg = CsWebSocketMessage.builder()
                .type("ai_typing")
                .conversationId(conversationId)
                .extra(Map.of("isTyping", isTyping))
                .build();
        
        sessionManager.sendToUserByConversation(conversationId, userId, statusMsg);
    }

    /**
     * 推送AI流式消息给相关客服（负责人 + 协作者 + 在线管理者）
     * 未分配会话则广播给所有在线客服
     */
    private void sendAiStreamToAgents(String conversationId, String ownerAgentId, CsWebSocketMessage message) {
        if (oConvertUtils.isEmpty(ownerAgentId)) {
            // 待接入会话，广播给所有在线客服
            sessionManager.sendToAllAgents(message);
            return;
        }
        Set<String> agentIds = new HashSet<>();
        agentIds.add(ownerAgentId);
        List<CsCollaborator> activeCollaborators = collaboratorService.getCollaborators(conversationId);
        if (activeCollaborators != null) {
            for (CsCollaborator collab : activeCollaborators) {
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
     * AI流式回调接口
     */
    interface AiStreamCallback {
        void onToken(String token, boolean isComplete);
    }
    
    /**
     * 流式调用AI服务
     * 
     * @param conversationId 会话ID
     * @param userMessage 用户消息
     * @param callback 回调
     * @param forVisitor true=访客AI应用（AI自动回复），false=客服AI建议应用（AI辅助模式）
     */
    private void callAiServiceStream(String conversationId, String userMessage, AiStreamCallback callback, boolean forVisitor) {
        try {
            // 获取会话信息
            CsConversation conversation = conversationService.getConversation(conversationId);
            if (conversation == null) {
                callback.onToken("抱歉，会话信息不存在。", true);
                return;
            }
            
            // 获取AI应用配置
            AiragApp app = null;
            String modelId = null;
            String systemPrompt = null;
            int msgNum = 10;
            String appIdToUse = null;
            
            // 根据场景选择不同的AI应用
            // forVisitor=true: 使用全局访客AI应用（存储在Redis），用于AI自动回复
            // forVisitor=false: 使用客服AI建议应用（defaultAppId），用于AI辅助模式
            
            if (forVisitor) {
                // ★ 访客AI应用：从Redis获取全局配置
                appIdToUse = getGlobalVisitorAppId();
                if (oConvertUtils.isNotEmpty(appIdToUse)) {
                    log.info("[CS-Message] 使用全局访客AI应用: appId={}", appIdToUse);
                }
            } else {
                // 客服AI建议应用：从客服配置获取
                // 1. 优先从会话负责客服获取
                if (oConvertUtils.isNotEmpty(conversation.getOwnerAgentId())) {
                    CsAgent agent = agentService.getById(conversation.getOwnerAgentId());
                    if (agent != null && oConvertUtils.isNotEmpty(agent.getDefaultAppId())) {
                        appIdToUse = agent.getDefaultAppId();
                        log.info("[CS-Message] 使用客服AI建议应用: agentId={}, defaultAppId={}", 
                                conversation.getOwnerAgentId(), appIdToUse);
                    }
                }
                
                // 2. 如果没有，查找任意一个在线客服的应用
                if (oConvertUtils.isEmpty(appIdToUse)) {
                    CsAgent onlineAgent = agentService.findOnlineAgentWithApp();
                    if (onlineAgent != null && oConvertUtils.isNotEmpty(onlineAgent.getDefaultAppId())) {
                        appIdToUse = onlineAgent.getDefaultAppId();
                        log.info("[CS-Message] 使用在线客服的AI建议应用: agentId={}, defaultAppId={}", 
                                onlineAgent.getId(), appIdToUse);
                    }
                }
            }
            
            // 如果仍然没有可用的AI应用，返回错误提示
            if (oConvertUtils.isEmpty(appIdToUse)) {
                String appType = forVisitor ? "访客AI应用" : "客服AI建议应用";
                String hint = forVisitor ? "请在设置中配置访客AI应用" : "请客服在设置中配置AI建议应用";
                log.warn("[CS-Message] 没有可用的{}配置: conversationId={}", appType, conversationId);
                callback.onToken("抱歉，当前没有配置" + appType + "，" + hint + "。", true);
                return;
            }
            
            // 获取AI应用配置
            if (oConvertUtils.isNotEmpty(appIdToUse)) {
                app = airagAppMapper.getByIdIgnoreTenant(appIdToUse);
                if (app != null) {
                    modelId = app.getModelId();
                    systemPrompt = app.getPrompt();
                    if (app.getMsgNum() != null && app.getMsgNum() > 0) {
                        msgNum = app.getMsgNum();
                    }
                    log.info("[CS-Message] 流式AI配置: appId={}, modelId={}", appIdToUse, modelId);
                }
            }
            
            // 构建消息列表
            List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
            
            // 添加系统提示词
            if (oConvertUtils.isNotEmpty(systemPrompt)) {
                messages.add(new SystemMessage(systemPrompt));
            }
            
            // 获取聊天历史
            List<ChatMessage> history = chatMessageService.getRecentMessages(conversationId, msgNum);
            if (oConvertUtils.isObjectNotEmpty(history)) {
                history = history.stream()
                    .sorted(Comparator.comparing(ChatMessage::getCreateTime))
                    .collect(Collectors.toList());
                
                for (ChatMessage msg : history) {
                    if (ChatMessage.SENDER_USER == msg.getSenderType()) {
                        messages.add(UserMessage.from(msg.getContent()));
                    } else if (ChatMessage.SENDER_AI == msg.getSenderType() 
                            || ChatMessage.SENDER_AGENT == msg.getSenderType()) {
                        messages.add(AiMessage.from(msg.getContent()));
                    }
                }
            }
            
            // 添加当前用户消息
            messages.add(UserMessage.from(userMessage));
            
            // 构建AI调用参数
            AIChatParams params = new AIChatParams();
            
            // 使用应用配置的模型参数
            if (app != null && oConvertUtils.isNotEmpty(app.getMetadata())) {
                try {
                    JSONObject metadata = JSONObject.parseObject(app.getMetadata());
                    if (metadata != null) {
                        if (metadata.containsKey("temperature")) {
                            params.setTemperature(metadata.getDouble("temperature"));
                        }
                        if (metadata.containsKey("topP")) {
                            params.setTopP(metadata.getDouble("topP"));
                        }
                    }
                } catch (Exception e) {
                    log.warn("[CS-Message] 解析metadata失败: {}", e.getMessage());
                }
            }
            
            // 使用应用配置的知识库
            if (app != null && oConvertUtils.isNotEmpty(app.getKnowIds())) {
                params.setKnowIds(app.getKnowIds());
            }
            
            // 调用流式AI服务
            TokenStream tokenStream;
            if (oConvertUtils.isNotEmpty(modelId)) {
                tokenStream = aiChatHandler.chat(modelId, messages, params);
            } else {
                tokenStream = aiChatHandler.chatByDefaultModel(messages, params);
            }
            
            // 处理流式响应
            log.info("[CS-Message] 开始流式AI调用: modelId={}, messagesCount={}", modelId, messages.size());
            
            tokenStream.onPartialResponse(token -> {
                callback.onToken(token, false);
            }).onCompleteResponse(response -> {
                log.info("[CS-Message] 流式AI调用完成");
                callback.onToken(null, true);
            }).onError(error -> {
                String errorMsg = error.getMessage();
                log.error("[CS-Message] 流式AI调用出错: {}", errorMsg, error);
                // 返回更详细的错误信息
                if (errorMsg != null && errorMsg.contains("未激活")) {
                    callback.onToken("抱歉，AI模型未激活，请在后台配置中激活模型。", true);
                } else if (errorMsg != null && errorMsg.contains("余额")) {
                    callback.onToken("抱歉，AI服务余额不足，请联系管理员充值。", true);
                } else {
                    callback.onToken("抱歉，AI服务出现错误: " + (errorMsg != null ? errorMsg : "未知错误"), true);
                }
            }).start();
            
        } catch (Exception e) {
            String errorMsg = e.getMessage();
            log.error("[CS-Message] 流式AI服务调用失败: conversationId={}, error={}", conversationId, errorMsg, e);
            // 返回更详细的错误信息
            if (errorMsg != null && errorMsg.contains("未激活")) {
                callback.onToken("抱歉，AI模型未激活，请在后台配置中激活模型。", true);
            } else if (errorMsg != null && errorMsg.contains("请选择模型")) {
                callback.onToken("抱歉，未配置AI模型，请在AI应用设置中配置模型。", true);
            } else {
                callback.onToken("抱歉，AI服务暂时不可用: " + (errorMsg != null ? errorMsg : ""), true);
            }
        }
    }

    private String getGlobalVisitorAppId() {
        String appId = redisTemplate.opsForValue().get(VISITOR_APP_REDIS_KEY);
        if (oConvertUtils.isNotEmpty(appId)) {
            return appId;
        }
        CsGlobalConfig config = csGlobalConfigMapper.selectById(VISITOR_APP_CONFIG_KEY);
        appId = config != null ? config.getConfigValue() : null;
        if (oConvertUtils.isNotEmpty(appId)) {
            redisTemplate.opsForValue().set(VISITOR_APP_REDIS_KEY, appId);
        }
        return appId;
    }

    /**
     * 异步生成AI建议 (AI辅助模式)
     */
    @Async
    public void generateAiSuggestionAsync(String conversationId, String userMessage) {
        try {
            String suggestion = callAiService(conversationId, userMessage);
            
            if (oConvertUtils.isNotEmpty(suggestion)) {
                // 缓存建议
                aiSuggestionCache.put(conversationId, suggestion);
                
                // 推送给客服
                CsConversation conversation = conversationService.getConversation(conversationId);
                if (conversation != null) {
                    CsWebSocketMessage wsMessage = CsWebSocketMessage.builder()
                            .type("ai_suggestion")
                            .conversationId(conversationId)
                            .content(suggestion)
                            .build();
                    
                    // 收集所有需要推送的客服ID
                    Set<String> agentIds = new HashSet<>();
                    if (conversation.getOwnerAgentId() != null) {
                        agentIds.add(conversation.getOwnerAgentId());
                    }
                    
                    // 从数据库查询协作者
                    List<CsCollaborator> collaborators = collaboratorService.getCollaborators(conversationId);
                    if (collaborators != null) {
                        for (CsCollaborator collab : collaborators) {
                            agentIds.add(collab.getAgentId());
                        }
                    }
                    
                    // 推送给所有相关客服
                    for (String agentId : agentIds) {
                        sessionManager.sendToAgent(agentId, wsMessage);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[CS-Message] 生成AI建议失败: conversationId={}", conversationId, e);
        }
    }

    /**
     * 调用AI服务
     * 根据会话的appId获取AI应用配置，使用真实AI模型进行回复
     * 自动使用应用配置的：提示词、模型参数(metadata)、知识库、历史消息数等
     */
    private String callAiService(String conversationId, String userMessage) {
        try {
            log.info("[CS-Message] 调用AI服务: conversationId={}, userMessage={}", conversationId, userMessage);
            
            // 获取会话信息
            CsConversation conversation = conversationService.getConversation(conversationId);
            if (conversation == null) {
                log.error("[CS-Message] 会话不存在: conversationId={}", conversationId);
                return "抱歉，会话信息不存在。";
            }
            
            // 获取AI应用配置
            AiragApp app = null;
            String modelId = null;
            String systemPrompt = null;
            int msgNum = 10; // 默认历史消息数
            
            if (oConvertUtils.isNotEmpty(conversation.getAppId())) {
                app = airagAppMapper.getByIdIgnoreTenant(conversation.getAppId());
                if (app != null) {
                    modelId = app.getModelId();
                    systemPrompt = app.getPrompt();
                    // 使用应用配置的历史消息数
                    if (app.getMsgNum() != null && app.getMsgNum() > 0) {
                        msgNum = app.getMsgNum();
                    }
                    log.info("[CS-Message] 获取到AI应用配置: appId={}, modelId={}, msgNum={}", 
                            conversation.getAppId(), modelId, msgNum);
                }
            }
            
            // 构建消息列表
            List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
            
            // 添加系统提示词（使用应用配置的提示词）
            if (oConvertUtils.isNotEmpty(systemPrompt)) {
                messages.add(new SystemMessage(systemPrompt));
            }
            // 注意：如果应用没有配置提示词，就不添加默认提示词，让AI按默认行为回复
            
            // 获取最近的聊天历史（使用应用配置的历史消息数）
            List<ChatMessage> history = chatMessageService.getRecentMessages(conversationId, msgNum);
            if (oConvertUtils.isObjectNotEmpty(history)) {
                // 按时间正序排列
                history = history.stream()
                    .sorted(Comparator.comparing(ChatMessage::getCreateTime))
                    .collect(Collectors.toList());
                
                for (ChatMessage msg : history) {
                    // 根据senderType判断消息角色
                    if (ChatMessage.SENDER_USER == msg.getSenderType()) {
                        messages.add(UserMessage.from(msg.getContent()));
                    } else if (ChatMessage.SENDER_AI == msg.getSenderType() 
                            || ChatMessage.SENDER_AGENT == msg.getSenderType()) {
                        messages.add(AiMessage.from(msg.getContent()));
                    }
                }
            }
            
            // 添加当前用户消息
            messages.add(UserMessage.from(userMessage));
            
            // 构建AI调用参数
            AIChatParams params = new AIChatParams();
            
            // 使用应用配置的模型参数（从metadata中读取）
            if (app != null && oConvertUtils.isNotEmpty(app.getMetadata())) {
                try {
                    JSONObject metadata = JSONObject.parseObject(app.getMetadata());
                    if (metadata != null) {
                        if (metadata.containsKey("temperature")) {
                            params.setTemperature(metadata.getDouble("temperature"));
                        }
                        if (metadata.containsKey("topP")) {
                            params.setTopP(metadata.getDouble("topP"));
                        }
                        if (metadata.containsKey("presencePenalty")) {
                            params.setPresencePenalty(metadata.getDouble("presencePenalty"));
                        }
                        if (metadata.containsKey("frequencyPenalty")) {
                            params.setFrequencyPenalty(metadata.getDouble("frequencyPenalty"));
                        }
                        if (metadata.containsKey("maxTokens")) {
                            params.setMaxTokens(metadata.getInteger("maxTokens"));
                        }
                    }
                } catch (Exception e) {
                    log.warn("[CS-Message] 解析metadata失败: {}", e.getMessage());
                }
            }
            
            // 使用应用配置的知识库
            if (app != null && oConvertUtils.isNotEmpty(app.getKnowIds())) {
                params.setKnowIds(app.getKnowIds());
                log.info("[CS-Message] 使用知识库: {}", app.getKnowIds());
            }
            
            // 设置历史消息数量
            params.setMaxMsgNumber(msgNum);
            
            // 调用AI服务
            String aiResponse;
            if (oConvertUtils.isNotEmpty(modelId)) {
                // 使用应用配置的模型
                aiResponse = aiChatHandler.completions(modelId, messages, params);
            } else {
                // 使用默认模型
                aiResponse = aiChatHandler.completionsByDefaultModel(messages, params);
            }
            
            log.info("[CS-Message] AI回复成功: conversationId={}, responseLength={}", 
                    conversationId, aiResponse != null ? aiResponse.length() : 0);
            
            return aiResponse;
            
        } catch (Exception e) {
            log.error("[CS-Message] 调用AI服务失败: conversationId={}", conversationId, e);
            return "抱歉，AI服务暂时不可用，请稍后再试或联系人工客服。";
        }
    }

}
