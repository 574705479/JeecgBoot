package org.jeecg.modules.airag.cs.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecgframework.poi.excel.annotation.Excel;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 客服信息表 (简化版)
 * 
 * @author jeecg
 * @date 2026-01-12
 */
@Data
@TableName("cs_agent")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "客服信息表")
public class CsAgent implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键ID")
    private String id;

    @Excel(name = "关联系统用户ID", width = 15)
    @Schema(description = "关联系统用户ID")
    private String userId;

    @Excel(name = "客服昵称", width = 15)
    @Schema(description = "客服昵称")
    private String nickname;

    @Excel(name = "头像URL", width = 30)
    @Schema(description = "头像URL")
    private String avatar;

    @Excel(name = "最大同时接待数", width = 15)
    @Schema(description = "最大同时接待数")
    private Integer maxSessions;

    @Excel(name = "欢迎语", width = 30)
    @Schema(description = "欢迎语")
    private String welcomeMessage;

    @Excel(name = "状态", width = 10)
    @Schema(description = "状态: 0-离线 1-在线 2-忙碌")
    private Integer status;

    @Excel(name = "当前接待数", width = 10)
    @Schema(description = "当前接待数")
    private Integer currentSessions;

    @Excel(name = "累计服务数", width = 10)
    @Schema(description = "累计服务数")
    private Integer totalServed;

    @Excel(name = "客服AI建议应用ID", width = 20)
    @Schema(description = "客服AI建议应用ID，用于AI辅助模式下生成建议")
    private String defaultAppId;

    // 注意：访客AI应用已改为全局配置，存储在Redis中，Key: cs:global:visitor_app_id

    @Excel(name = "角色", width = 10)
    @Schema(description = "角色: 0-普通客服 1-管理者(可监控所有会话)")
    private Integer role;

    @Excel(name = "最后在线时间", width = 20, format = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后在线时间")
    private Date lastOnlineTime;

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

    // ==================== 客服状态常量 ====================
    
    /** 离线 */
    public static final int STATUS_OFFLINE = 0;
    /** 在线 */
    public static final int STATUS_ONLINE = 1;
    /** 忙碌 */
    public static final int STATUS_BUSY = 2;

    // ==================== 客服角色常量 ====================
    
    /** 普通客服 */
    public static final int ROLE_AGENT = 0;
    /** 管理者（可监控所有会话） */
    public static final int ROLE_SUPERVISOR = 1;

    /**
     * 判断客服是否可以接待新会话
     */
    public boolean canAcceptSession() {
        return status == STATUS_ONLINE && 
               (maxSessions == null || currentSessions == null || currentSessions < maxSessions);
    }

    /**
     * 判断是否为管理者
     * 注意：方法名不使用 isSupervisor，避免被 QueryGenerator 误识别为数据库字段
     */
    public boolean checkSupervisor() {
        return role != null && role == ROLE_SUPERVISOR;
    }
}
