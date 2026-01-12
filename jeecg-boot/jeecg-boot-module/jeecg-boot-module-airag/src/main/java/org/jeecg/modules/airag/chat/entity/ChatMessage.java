package org.jeecg.modules.airag.chat.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 统一聊天消息实体（MongoDB）
 * 同时支持 AI 聊天和人工客服聊天
 *
 * @author jeecg
 * @date 2026-01-08
 */
@Data
@Accessors(chain = true)
@Document(collection = "chat_messages")
@CompoundIndexes({
    @CompoundIndex(name = "idx_conv_time", def = "{'conversationId': 1, 'createTime': 1}"),
    @CompoundIndex(name = "idx_app_user", def = "{'appId': 1, 'userId': 1}")
})
public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    // ==================== 发送者类型常量 ====================
    /** 用户/访客 */
    public static final int SENDER_USER = 0;
    /** AI */
    public static final int SENDER_AI = 1;
    /** 客服 */
    public static final int SENDER_AGENT = 2;
    /** 系统 */
    public static final int SENDER_SYSTEM = 3;

    // ==================== 消息类型常量 ====================
    /** 文本消息 */
    public static final int MSG_TYPE_TEXT = 0;
    /** 图片消息 */
    public static final int MSG_TYPE_IMAGE = 1;
    /** 文件消息 */
    public static final int MSG_TYPE_FILE = 2;
    /** 卡片消息 */
    public static final int MSG_TYPE_CARD = 3;
    /** 语音消息 */
    public static final int MSG_TYPE_VOICE = 4;

    // ==================== 会话类型常量 ====================
    /** AI 会话 */
    public static final int CONV_TYPE_AI = 0;
    /** 人工客服会话 */
    public static final int CONV_TYPE_AGENT = 1;

    // ==================== 基础字段 ====================
    
    @Id
    private String id;

    /** 会话ID */
    @Indexed
    private String conversationId;

    /** 应用ID */
    @Indexed
    private String appId;

    /** 会话类型：0-AI会话，1-人工客服会话 */
    private Integer conversationType;

    // ==================== 发送者信息 ====================

    /** 发送者ID（用户ID/客服ID/AI） */
    private String senderId;

    /** 发送者类型：0-用户，1-AI，2-客服，3-系统 */
    private Integer senderType;

    /** 发送者名称 */
    private String senderName;

    /** 发送者头像 */
    private String senderAvatar;

    // ==================== 用户信息（接收者/会话所属用户）====================

    /** 内部用户ID */
    private String userId;

    /** 外部用户ID（第三方系统用户ID） */
    private String externalUserId;

    /** 外部用户名 */
    private String externalUserName;

    // ==================== 消息内容 ====================

    /** 消息内容 */
    private String content;

    /** 消息类型：0-文本，1-图片，2-文件，3-卡片，4-语音 */
    private Integer msgType;

    /** 扩展信息（JSON格式，存储附件、引用等） */
    private Map<String, Object> extra;

    // ==================== AI 相关字段 ====================

    /** 主题ID（AI对话轮次） */
    private String topicId;

    /** 模型名称 */
    private String modelName;

    /** Token使用量 */
    private Integer tokenUsage;

    /** 知识库引用 */
    private String referenceKnowledge;

    // ==================== 客服相关字段 ====================

    /** 客服ID */
    private String agentId;

    /** 是否已读 */
    private Boolean isRead;

    /** 阅读时间 */
    private Date readTime;

    // ==================== 时间字段 ====================

    /** 创建时间 */
    @Indexed
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;

    // ==================== 状态字段 ====================

    /** 是否删除 */
    private Boolean deleted;

    /** 删除时间 */
    private Date deleteTime;
}
