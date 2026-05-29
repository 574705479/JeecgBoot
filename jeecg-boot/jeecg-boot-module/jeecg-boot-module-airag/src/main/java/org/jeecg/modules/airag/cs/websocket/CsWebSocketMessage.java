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
     * 是否AI生成
     */
    private Boolean isAiGenerated;

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

    /**
     * 客户端消息ID（访客端乐观消息对账用，原样回传）
     */
    private String clientMsgId;

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
    /** 黑名单变更（拉黑/解封） */
    public static final String TYPE_BLACKLIST_CHANGED = "blacklist_changed";
    /** 访客被拉黑（通知访客端） */
    public static final String TYPE_VISITOR_BLOCKED = "visitor_blocked";
    /** 消息撤回 */
    public static final String TYPE_MESSAGE_RECALL = "message_recall";
    /** 协作邀请 */
    public static final String TYPE_INVITE_COLLAB = "invite_collab";
    /** 客服状态变更 */
    public static final String TYPE_AGENT_STATUS_CHANGED = "agent_status_changed";
    /** 访客资料更新 */
    public static final String TYPE_VISITOR_UPDATED = "visitor_updated";
    /** 用户上线 */
    public static final String TYPE_USER_ONLINE = "user_online";
    /** 客服 WebSocket 连接成功 */
    public static final String TYPE_CONNECTED = "connected";
    /** 配额超限 */
    public static final String TYPE_QUOTA_EXCEEDED = "quota_exceeded";
    /** 敏感词拦截 */
    public static final String TYPE_SENSITIVE_WORD_BLOCKED = "sensitive_word_blocked";
    /** 客服超时未回复提醒 */
    public static final String TYPE_AGENT_TIMEOUT_REMINDER = "agent_timeout_reminder";
    /** 消息投递失败 */
    public static final String TYPE_DELIVERY_FAILED = "delivery_failed";
    /** 访客消息送达确认（回传 clientMsgId + 服务端 messageId，供乐观消息对账） */
    public static final String TYPE_MESSAGE_ACK = "message_ack";

    // ---------- AI 相关 ----------
    /** AI 流式 */
    public static final String TYPE_AI_STREAM = "ai_stream";
    /** AI 流式完成 */
    public static final String TYPE_AI_STREAM_COMPLETE = "ai_stream_complete";
    /** AI 正在输入 */
    public static final String TYPE_AI_TYPING = "ai_typing";
    /** AI 建议（一次性） */
    public static final String TYPE_AI_SUGGESTION = "ai_suggestion";
    /** AI 建议流式 */
    public static final String TYPE_AI_SUGGESTION_STREAM = "ai_suggestion_stream";
    /** AI 建议流式完成 */
    public static final String TYPE_AI_SUGGESTION_COMPLETE = "ai_suggestion_complete";
    /** AI 建议错误 */
    public static final String TYPE_AI_SUGGESTION_ERROR = "ai_suggestion_error";

    // ---------- 客户端 → 服务端 控制指令 ----------
    /** 客户端切换会话模式 */
    public static final String TYPE_MODE_CHANGE = "mode_change";
    /** 客户端确认采用 AI 回复 */
    public static final String TYPE_CONFIRM_AI = "confirm_ai";
    /** 客户端中止 AI 回复 */
    public static final String TYPE_STOP_AI = "stop_ai";
    /** 客户端中止 AI 建议 */
    public static final String TYPE_STOP_AI_SUGGESTION = "stop_ai_suggestion";
}
