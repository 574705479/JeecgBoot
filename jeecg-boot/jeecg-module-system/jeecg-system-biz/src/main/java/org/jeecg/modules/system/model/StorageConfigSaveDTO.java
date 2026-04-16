package org.jeecg.modules.system.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 存储桶配置保存
 */
@Data
public class StorageConfigSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** SYSTEM / ALIYUN / TENCENT */
    private String storageType;

    private String aliyunEndpoint;
    private String aliyunBucket;
    private String aliyunAccessKeyId;
    /** 非空则更新密文；空字符串表示保留原值 */
    private String aliyunAccessKeySecret;
    private String aliyunStaticDomain;
    /** 阿里云 OSS 传输加速 */
    private Boolean aliyunTransferAccel;
    /** 阿里云 RAM 角色 ARN（可选） */
    private String aliyunRoleArn;

    private String tencentRegion;
    private String tencentBucket;
    private String tencentSecretId;
    /** 非空则更新密文；空表示保留 */
    private String tencentSecretKey;
    private String tencentDomain;
    /** 腾讯云 COS 全球加速 */
    private Boolean tencentGlobalAccel;

    private String remark;
}
