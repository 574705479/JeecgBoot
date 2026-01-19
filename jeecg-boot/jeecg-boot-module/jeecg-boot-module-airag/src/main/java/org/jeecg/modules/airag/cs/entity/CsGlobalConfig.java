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
 * 客服全局配置表
 *
 * @author jeecg
 * @date 2026-01-19
 */
@Data
@TableName("cs_global_config")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Schema(description = "客服全局配置")
public class CsGlobalConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "config_key", type = IdType.INPUT)
    @Schema(description = "配置Key")
    private String configKey;

    @Schema(description = "配置Value")
    private String configValue;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private Date createTime;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private Date updateTime;
}
