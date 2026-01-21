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
 * 客服系统品牌配置
 *
 * @author jeecg
 * @date 2026-01-20
 */
@Data
@TableName("cs_brand_config")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "客服系统品牌配置")
public class CsBrandConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    @Schema(description = "主键")
    private String id;

    @Schema(description = "系统名称")
    private String appTitle;

    @Schema(description = "系统简称")
    private String appShortTitle;

    @Schema(description = "登录页副标题")
    private String appSubtitle;

    @Schema(description = "Logo地址")
    private String logoUrl;

    @Schema(description = "浏览器图标")
    private String faviconUrl;

    @Schema(description = "登录页背景图")
    private String loginBgUrl;

    @Schema(description = "加载页文案")
    private String loadingTitle;

    @Schema(description = "状态(1启用0禁用)")
    private Integer status;

    @Schema(description = "删除标记")
    private Integer delFlag;

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
}
