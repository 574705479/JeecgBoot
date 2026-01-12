package org.jeecg.modules.airag.cs.websocket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;

/**
 * WebSocket消息结构
 * 
 * @author jeecg
 * @date 2026-01-07
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CsWebSocketMessage {

    /**
     * 消息类型
     */
    private String type;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息类型（文本、图片等）
     */
    private Integer msgType;

    /**
     * 发送者类型
     */
    private Integer senderType;

    /**
     * 发送者ID
     */
    private String senderId;

    /**
     * 发送者名称
     */
    private String senderName;

    /**
     * 发送者头像
     */
    private String senderAvatar;

    /**
     * 时间戳
     */
    private Date timestamp;

    /**
     * 额外数据
     */
    private Map<String, Object> extra;

    /**
     * 错误信息
     */
    private String error;

    // ==================== 消息类型常量 ====================

    /** 发送消息 */
    public static final String TYPE_MESSAGE = "message";
    /** 消息已读 */
    public static final String TYPE_READ = "read";
    /** 正在输入 */
    public static final String TYPE_TYPING = "typing";
    /** 转人工 */
    public static final String TYPE_TRANSFER_AGENT = "transfer_agent";
    /** 排队更新 */
    public static final String TYPE_QUEUE_UPDATE = "queue_update";
    /** 客服接入 */
    public static final String TYPE_AGENT_CONNECTED = "agent_connected";
    /** 客服断开 */
    public static final String TYPE_AGENT_DISCONNECTED = "agent_disconnected";
    /** 会话结束 */
    public static final String TYPE_CONVERSATION_CLOSED = "conversation_closed";
    /** 新会话 */
    public static final String TYPE_NEW_CONVERSATION = "new_conversation";
    /** 心跳 */
    public static final String TYPE_PING = "ping";
    /** 心跳响应 */
    public static final String TYPE_PONG = "pong";
    /** 错误 */
    public static final String TYPE_ERROR = "error";
    /** 系统通知 */
    public static final String TYPE_SYSTEM = "system";
    /** 新排队用户（广播给所有客服） */
    public static final String TYPE_QUEUE_NEW = "queue_new";
    /** 转到AI */
    public static final String TYPE_TRANSFER_TO_AI = "transfer_to_ai";
    /** 用户离线 */
    public static final String TYPE_USER_OFFLINE = "user_offline";

    /**
     * 创建消息
     */
    public static CsWebSocketMessage message(String conversationId, String content, 
                                              Integer senderType, String senderId, String senderName) {
        return CsWebSocketMessage.builder()
                .type(TYPE_MESSAGE)
                .conversationId(conversationId)
                .content(content)
                .msgType(0)
                .senderType(senderType)
                .senderId(senderId)
                .senderName(senderName)
                .timestamp(new Date())
                .build();
    }

    /**
     * 创建排队更新消息
     */
    public static CsWebSocketMessage queueUpdate(String conversationId, int position, int waitTime) {
        return CsWebSocketMessage.builder()
                .type(TYPE_QUEUE_UPDATE)
                .conversationId(conversationId)
                .extra(Map.of("position", position, "waitTime", waitTime))
                .timestamp(new Date())
                .build();
    }

    /**
     * 创建客服接入消息
     */
    public static CsWebSocketMessage agentConnected(String conversationId, String agentId, 
                                                     String agentName, String agentAvatar) {
        return CsWebSocketMessage.builder()
                .type(TYPE_AGENT_CONNECTED)
                .conversationId(conversationId)
                .senderId(agentId)
                .senderName(agentName)
                .senderAvatar(agentAvatar)
                .timestamp(new Date())
                .build();
    }

    /**
     * 创建会话结束消息
     */
    public static CsWebSocketMessage conversationClosed(String conversationId) {
        return CsWebSocketMessage.builder()
                .type(TYPE_CONVERSATION_CLOSED)
                .conversationId(conversationId)
                .timestamp(new Date())
                .build();
    }

    /**
     * 创建错误消息
     */
    public static CsWebSocketMessage error(String error) {
        return CsWebSocketMessage.builder()
                .type(TYPE_ERROR)
                .error(error)
                .timestamp(new Date())
                .build();
    }

    /**
     * 创建系统消息
     */
    public static CsWebSocketMessage system(String conversationId, String content) {
        return CsWebSocketMessage.builder()
                .type(TYPE_SYSTEM)
                .conversationId(conversationId)
                .content(content)
                .senderType(3)
                .timestamp(new Date())
                .build();
    }
}
