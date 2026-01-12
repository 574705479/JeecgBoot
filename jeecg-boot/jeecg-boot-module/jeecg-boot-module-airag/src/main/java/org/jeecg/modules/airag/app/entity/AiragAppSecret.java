package org.jeecg.modules.airag.app.entity;

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

/**
 * @Description: AI应用接入密钥
 * @Author: jeecg-boot
 * @Date: 2025-01-07
 * @Version: V1.0
 */
@Data
@TableName("airag_app_secret")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@Schema(description = "AI应用接入密钥")
public class AiragAppSecret implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "主键")
    private String id;

    /**
     * 应用ID
     */
    @Excel(name = "应用ID", width = 15)
    @Schema(description = "应用ID")
    private String appId;

    /**
     * 密钥
     */
    @Excel(name = "密钥", width = 15)
    @Schema(description = "密钥")
    private String secretKey;

    /**
     * 域名白名单（多个用逗号分隔）
     */
    @Excel(name = "域名白名单", width = 30)
    @Schema(description = "域名白名单")
    private String domainWhitelist;

    /**
     * 是否启用 (1=启用, 0=禁用)
     */
    @Excel(name = "是否启用", width = 15)
    @Schema(description = "是否启用")
    private Integer enabled;

    /**
     * 签名有效期（分钟）
     */
    @Excel(name = "签名有效期", width = 15)
    @Schema(description = "签名有效期(分钟)")
    private Integer tokenExpireMinutes;

    /**
     * 备注
     */
    @Excel(name = "备注", width = 30)
    @Schema(description = "备注")
    private String remark;

    /**
     * 创建人
     */
    @Schema(description = "创建人")
    private String createBy;

    /**
     * 创建日期
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建日期")
    private java.util.Date createTime;

    /**
     * 更新人
     */
    @Schema(description = "更新人")
    private String updateBy;

    /**
     * 更新日期
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新日期")
    private java.util.Date updateTime;

    /**
     * 租户id
     */
    @Excel(name = "租户id", width = 15)
    @Schema(description = "租户id")
    private String tenantId;
}
