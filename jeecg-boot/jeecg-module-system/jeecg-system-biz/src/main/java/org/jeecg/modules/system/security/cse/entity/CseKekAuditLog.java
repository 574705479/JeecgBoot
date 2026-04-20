package org.jeecg.modules.system.security.cse.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * CSE KEK 操作审计日志
 */
@Data
@TableName("cse_kek_audit_log")
public class CseKekAuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ACTION_INIT = "INIT";
    public static final String ACTION_GENERATE = "GENERATE";
    public static final String ACTION_ACTIVATE = "ACTIVATE";
    public static final String ACTION_DEPRECATE = "DEPRECATE";
    public static final String ACTION_ARCHIVE = "ARCHIVE";
    public static final String ACTION_EXPORT = "EXPORT";
    public static final String ACTION_IMPORT = "IMPORT";
    /** 动态配置变更（基础配置 Tab 保存）：kid 字段为空，remark 存变更前后 JSON diff */
    public static final String ACTION_CONFIG_UPDATE = "CONFIG_UPDATE";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String kid;

    private String action;

    private String operatorId;

    private String operatorName;

    private String operatorIp;

    private LocalDateTime operateTime;

    private String remark;
}
