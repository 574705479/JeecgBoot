package org.jeecg.modules.airag.cs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 客服会话表 (重构版)
 * 
 * 核心理念:
 * 1. 状态简化为3种: 未分配/已分配/已结束
 * 2. 新增回复模式: AI自动/手动/AI辅助
 * 3. 支持多客服协作
 * 
 * @author jeecg
 * @date 2026-01-12
 */
@Data
@TableName("cs_conversation")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "客服会话表")
public class CsConversation implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "会话ID")
    private String id;

    @Schema(description = "AI应用ID")
    private String appId;

    // ==================== 用户信息 ====================

    @Schema(description = "用户ID(可以是外部ID)")
    private String userId;

    @Schema(description = "用户名称")
    private String userName;

    @Schema(description = "用户头像")
    private String userAvatar;

    @Schema(description = "用户IP")
    private String userIp;

    @Schema(description = "用户设备信息")
    private String userDevice;

    // ==================== 客服信息 ====================

    @TableField("agent_id")
    @Schema(description = "主负责客服ID")
    private String ownerAgentId;

    // ==================== 会话状态 ====================

    @Schema(description = "状态: 0-未分配 1-已分配 2-已结束")
    private Integer status;

    @Schema(description = "回复模式: 0-AI自动 1-手动 2-AI辅助")
    private Integer replyMode;

    @Schema(description = "来源渠道")
    private String source;

    // ==================== 消息统计 ====================

    @Schema(description = "最后一条消息")
    private String lastMessage;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后消息时间")
    private Date lastMessageTime;

    @Schema(description = "客服未读消息数")
    private Integer unreadCount;

    @Schema(description = "总消息数")
    private Integer messageCount;

    // ==================== 时间信息 ====================

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "客服接入时间")
    private Date assignTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "结束时间")
    private Date endTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private Date updateTime;

    // ==================== 超时管理 ====================

    @Schema(description = "是否已发送超时提醒")
    private Boolean timeoutWarned;

    // ==================== 评价 ====================

    @Schema(description = "满意度评分: 1-5")
    private Integer satisfaction;

    @Schema(description = "评价内容")
    private String satisfactionComment;

    // ==================== 非数据库字段 ====================

    @TableField(exist = false)
    @Schema(description = "主负责客服昵称")
    private String ownerAgentName;

    @TableField(exist = false)
    @Schema(description = "主负责客服头像")
    private String ownerAgentAvatar;

    @TableField(exist = false)
    @Schema(description = "协作者列表")
    private List<CsCollaborator> collaborators;

    @TableField(exist = false)
    @Schema(description = "用户是否在线")
    private Boolean userOnline;

    // ==================== 状态常量 ====================
    
    /** 未分配 (AI自动回复中) */
    public static final int STATUS_UNASSIGNED = 0;
    /** 已分配 (有客服负责) */
    public static final int STATUS_ASSIGNED = 1;
    /** 已结束 */
    public static final int STATUS_CLOSED = 2;

    // ==================== 回复模式常量 ====================
    
    /** AI自动回复 - AI直接回复用户，客服监控 */
    public static final int REPLY_MODE_AI_AUTO = 0;
    /** 手动回复 - 客服手动回复，不调用AI */
    public static final int REPLY_MODE_MANUAL = 1;
    /** AI辅助 - AI生成建议，客服确认后发送 */
    public static final int REPLY_MODE_AI_ASSIST = 2;
}
