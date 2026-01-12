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
 * 访客信息表
 * 用于存储访客的备注信息，跨会话共享
 * 
 * @author jeecg
 * @date 2026-01-12
 */
@Data
@TableName("cs_visitor")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "访客信息表")
public class CsVisitor implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "所属应用ID")
    private String appId;

    @Schema(description = "访客唯一标识(关联会话表的user_id)")
    private String userId;

    // ==================== 备注信息 ====================

    @Schema(description = "备注昵称")
    private String nickname;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "性别: 0-未知 1-男 2-女")
    private Integer gender;

    // ==================== 业务信息 ====================

    @Schema(description = "公司/组织")
    private String company;

    @Schema(description = "职位")
    private String position;

    @Schema(description = "地址")
    private String address;

    @Schema(description = "标签(JSON数组)")
    private String tags;

    // ==================== 来源追踪 ====================

    @Schema(description = "首次来源渠道")
    private String source;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "首次访问时间")
    private Date firstVisitTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后访问时间")
    private Date lastVisitTime;

    @Schema(description = "访问次数")
    private Integer visitCount;

    @Schema(description = "总会话数")
    private Integer conversationCount;

    // ==================== 客服备注 ====================

    @Schema(description = "详细备注")
    private String notes;

    @Schema(description = "客户等级: 1-普通 2-重要 3-VIP")
    private Integer level;

    @Schema(description = "星标: 0-否 1-是")
    private Integer star;

    // ==================== 系统字段 ====================

    @Schema(description = "创建人")
    private String createBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新人")
    private String updateBy;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private Date updateTime;

    // ==================== 非数据库字段 ====================

    @TableField(exist = false)
    @Schema(description = "标签列表(解析后)")
    private List<String> tagList;

    @TableField(exist = false)
    @Schema(description = "显示名称(优先显示备注昵称)")
    private String displayName;

    // ==================== 常量 ====================

    /** 性别-未知 */
    public static final int GENDER_UNKNOWN = 0;
    /** 性别-男 */
    public static final int GENDER_MALE = 1;
    /** 性别-女 */
    public static final int GENDER_FEMALE = 2;

    /** 等级-普通 */
    public static final int LEVEL_NORMAL = 1;
    /** 等级-重要 */
    public static final int LEVEL_IMPORTANT = 2;
    /** 等级-VIP */
    public static final int LEVEL_VIP = 3;

    /**
     * 获取显示名称
     * 优先级: 备注昵称 > 真实姓名 > userId
     */
    public String getDisplayName() {
        if (nickname != null && !nickname.isEmpty()) {
            return nickname;
        }
        if (realName != null && !realName.isEmpty()) {
            return realName;
        }
        return userId;
    }
}
