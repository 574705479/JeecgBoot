package org.jeecg.modules.system.security.cse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * CSE KEK 主表
 */
@Data
@TableName("cse_kek")
public class CseKek implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_STAGED = "STAGED";
    public static final String STATUS_DEPRECATED = "DEPRECATED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";

    @TableId(type = IdType.INPUT)
    private String kid;

    private String kekB64;

    private String status;

    private String createdBy;

    private LocalDateTime createdTime;

    private LocalDateTime activatedTime;

    private LocalDateTime deprecatedTime;

    private LocalDateTime lastUsedTime;

    private Long fileCount;

    private String remark;
}
