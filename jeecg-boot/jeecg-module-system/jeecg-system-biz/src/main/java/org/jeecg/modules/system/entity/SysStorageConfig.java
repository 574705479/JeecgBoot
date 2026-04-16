package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 全站存储桶配置（单行 id=1）
 */
@Data
@TableName("sys_storage_config")
public class SysStorageConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String ID_SINGLETON = "1";
    public static final String TYPE_SYSTEM = "SYSTEM";
    public static final String TYPE_ALIYUN = "ALIYUN";
    public static final String TYPE_TENCENT = "TENCENT";

    /**
     * 与库中 storage_type 比较前统一格式，避免大小写/首尾空格导致误判为 yml 回退。
     *
     * @return 规范代码；仅空白则 null，表示未配置类型
     */
    public static String normalizeStorageType(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        return t.isEmpty() ? null : t.toUpperCase();
    }

    @TableId(type = IdType.INPUT)
    private String id;

    /** 切换类型时需将非当前类型列更新为 NULL，须显式策略否则 MyBatis-Plus 默认忽略 null */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String storageType;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String aliyunEndpoint;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String aliyunBucket;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String aliyunAccessKeyId;
    /** AES 密文 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String aliyunSecretCipher;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String aliyunStaticDomain;
    /** 传输加速：SDK 使用 oss-accelerate.aliyuncs.com */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Boolean aliyunTransferAccel;
    /** 可选，RAM 角色 ARN（业务扩展/审计） */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String aliyunRoleArn;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String tencentRegion;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String tencentBucket;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String tencentSecretId;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String tencentSecretKeyCipher;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String tencentDomain;
    /** COS 全球加速：Client 使用 cos.accelerate.myqcloud.com */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Boolean tencentGlobalAccel;

    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String remark;

    private String createBy;
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    private String updateBy;
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
