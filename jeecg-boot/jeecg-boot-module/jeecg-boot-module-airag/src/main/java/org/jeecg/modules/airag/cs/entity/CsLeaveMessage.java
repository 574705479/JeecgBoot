package org.jeecg.modules.airag.cs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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

/**
 * 客服留言表
 *
 * @author jeecg
 * @date 2026-02-06
 */
@Data
@TableName("cs_leave_message")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "客服留言表")
public class CsLeaveMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "访客用户ID")
    private String userId;

    @Schema(description = "留言内容")
    private String content;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "手机")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "QQ")
    private String qq;

    @Schema(description = "微信")
    private String wechat;

    @Schema(description = "图片URL")
    private String imageUrl;

    @Schema(description = "客服回复")
    private String reply;

    @Schema(description = "回复客服ID")
    private String replyAgentId;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "回复时间")
    private Date replyTime;

    @Schema(description = "状态: 0-待回复 1-已回复")
    private Integer status;

    @Schema(description = "用户是否已读回复")
    private Boolean userRead;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private Date updateTime;

    /** 待回复 */
    public static final int STATUS_PENDING = 0;
    /** 已回复 */
    public static final int STATUS_REPLIED = 1;
}
