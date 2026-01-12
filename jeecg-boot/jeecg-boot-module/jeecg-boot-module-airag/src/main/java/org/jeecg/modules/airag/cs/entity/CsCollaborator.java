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

/**
 * 会话协作者表
 * 
 * 支持多客服协作同一会话:
 * - 主负责人 (owner): 首个接入的客服，拥有全部权限
 * - 协作者 (collaborator): 被邀请加入的客服，可以回复
 * - 临时介入 (temporary): 紧急情况下直接加入
 * 
 * @author jeecg
 * @date 2026-01-12
 */
@Data
@TableName("cs_collaborator")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "会话协作者表")
public class CsCollaborator implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "会话ID")
    private String conversationId;

    @Schema(description = "客服ID")
    private String agentId;

    @Schema(description = "角色: 0-主负责 1-协作者 2-临时介入")
    private Integer role;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "加入时间")
    private Date joinTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "离开时间(NULL表示仍在协作)")
    private Date leaveTime;

    @Schema(description = "邀请人ID(主动加入则为空)")
    private String inviteBy;

    // ==================== 非数据库字段 ====================

    @TableField(exist = false)
    @Schema(description = "客服昵称")
    private String agentName;

    @TableField(exist = false)
    @Schema(description = "客服头像")
    private String agentAvatar;

    @TableField(exist = false)
    @Schema(description = "客服在线状态")
    private Integer agentStatus;

    // ==================== 角色常量 ====================
    
    /** 主负责人 */
    public static final int ROLE_OWNER = 0;
    /** 协作者 */
    public static final int ROLE_COLLABORATOR = 1;
    /** 临时介入 */
    public static final int ROLE_TEMPORARY = 2;

    /**
     * 判断是否仍在协作中
     */
    public boolean isActive() {
        return leaveTime == null;
    }
}
