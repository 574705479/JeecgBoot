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
 * 域名配置
 *
 * @author jeecg
 * @date 2026-02-11
 */
@Data
@TableName("cs_domain_config")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "域名配置")
public class CsDomainConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    @Schema(description = "主键")
    private String id;

    @Schema(description = "域名列表(换行分隔)")
    private String domains;

    @Schema(description = "桌面端下载链接(旧，兼容)")
    private String downloadUrl;

    @Schema(description = "下载链接列表(JSON数组)")
    private String downloadLinks;

    @Schema(description = "备注")
    private String remark;

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
