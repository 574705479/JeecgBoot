package org.jeecg.modules.airag.cs.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 客服消息DTO
 * 
 * 【重要】消息实际存储在MongoDB（chat_message集合），
 * 此类仅作为数据传输对象使用
 * 
 * @author jeecg
 * @date 2026-01-12
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "客服消息")
public class CsMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "消息ID")
    private String id;

    @Schema(description = "会话ID")
    private String conversationId;

    // ==================== 发送者信息 ====================

    @Schema(description = "发送者类型: 0-用户 1-AI 2-客服 3-系统")
    private Integer senderType;

    @Schema(description = "发送者ID")
    private String senderId;

    @Schema(description = "发送者名称(显示给用户)")
    private String senderName;

    @Schema(description = "发送者头像")
    private String senderAvatar;

    // ==================== 实际发送者(内部使用) ====================

    @Schema(description = "实际发送的客服ID(多客服协作时使用)")
    private String actualSenderId;

    @Schema(description = "实际发送的客服名称")
    private String actualSenderName;

    // ==================== 消息内容 ====================

    @Schema(description = "消息类型: 0-文本 1-图片 2-文件 3-语音 4-视频 5-富文本 6-卡片")
    private Integer msgType;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "扩展信息(图片URL、文件信息等)")
    private String extra;

    // ==================== AI相关 ====================

    @Schema(description = "是否AI生成")
    private Boolean isAiGenerated;

    @Schema(description = "是否经客服确认(AI辅助模式)")
    private Boolean aiConfirmed;

    @Schema(description = "AI建议ID(如果是确认AI建议发送的)")
    private String aiSuggestionId;

    // ==================== 状态信息 ====================

    @Schema(description = "状态: 0-发送中 1-已发送 2-已读 3-撤回 4-发送失败")
    private Integer status;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "发送时间")
    private Date createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "已读时间")
    private Date readTime;

    // ==================== 发送者类型常量 ====================
    
    /** 用户 */
    public static final int SENDER_USER = 0;
    /** AI */
    public static final int SENDER_AI = 1;
    /** 客服 */
    public static final int SENDER_AGENT = 2;
    /** 系统 */
    public static final int SENDER_SYSTEM = 3;

    // ==================== 消息类型常量 ====================
    
    /** 文本 */
    public static final int MSG_TYPE_TEXT = 0;
    /** 图片 */
    public static final int MSG_TYPE_IMAGE = 1;
    /** 文件 */
    public static final int MSG_TYPE_FILE = 2;
    /** 语音 */
    public static final int MSG_TYPE_VOICE = 3;
    /** 视频 */
    public static final int MSG_TYPE_VIDEO = 4;
    /** 富文本 */
    public static final int MSG_TYPE_RICH_TEXT = 5;
    /** 卡片 */
    public static final int MSG_TYPE_CARD = 6;

    // ==================== 消息状态常量 ====================
    
    /** 发送中 */
    public static final int STATUS_SENDING = 0;
    /** 已发送 */
    public static final int STATUS_SENT = 1;
    /** 已读 */
    public static final int STATUS_READ = 2;
    /** 撤回 */
    public static final int STATUS_REVOKED = 3;
    /** 发送失败 */
    public static final int STATUS_FAILED = 4;

    /**
     * 创建用户消息
     */
    public static CsMessage createUserMessage(String conversationId, String userId, String userName, String content) {
        CsMessage msg = new CsMessage();
        msg.setConversationId(conversationId);
        msg.setSenderType(SENDER_USER);
        msg.setSenderId(userId);
        msg.setSenderName(userName);
        msg.setMsgType(MSG_TYPE_TEXT);
        msg.setContent(content);
        msg.setStatus(STATUS_SENT);
        msg.setCreateTime(new Date());
        return msg;
    }

    /**
     * 创建客服消息
     */
    public static CsMessage createAgentMessage(String conversationId, String agentId, String agentName, String content) {
        CsMessage msg = new CsMessage();
        msg.setConversationId(conversationId);
        msg.setSenderType(SENDER_AGENT);
        msg.setSenderId(agentId);
        msg.setSenderName(agentName);
        msg.setActualSenderId(agentId);
        msg.setActualSenderName(agentName);
        msg.setMsgType(MSG_TYPE_TEXT);
        msg.setContent(content);
        msg.setIsAiGenerated(false);
        msg.setStatus(STATUS_SENT);
        msg.setCreateTime(new Date());
        return msg;
    }

    /**
     * 创建AI消息(显示为客服)
     */
    public static CsMessage createAiMessage(String conversationId, String displayName, String content) {
        CsMessage msg = new CsMessage();
        msg.setConversationId(conversationId);
        msg.setSenderType(SENDER_AGENT); // 用户看到的是客服
        msg.setSenderName(displayName != null ? displayName : "智能客服");
        msg.setMsgType(MSG_TYPE_TEXT);
        msg.setContent(content);
        msg.setIsAiGenerated(true);
        msg.setAiConfirmed(false); // AI自动模式直接发送
        msg.setStatus(STATUS_SENT);
        msg.setCreateTime(new Date());
        return msg;
    }

    /**
     * 创建系统消息
     */
    public static CsMessage createSystemMessage(String conversationId, String content) {
        CsMessage msg = new CsMessage();
        msg.setConversationId(conversationId);
        msg.setSenderType(SENDER_SYSTEM);
        msg.setSenderName("系统");
        msg.setMsgType(MSG_TYPE_TEXT);
        msg.setContent(content);
        msg.setStatus(STATUS_SENT);
        msg.setCreateTime(new Date());
        return msg;
    }
}
