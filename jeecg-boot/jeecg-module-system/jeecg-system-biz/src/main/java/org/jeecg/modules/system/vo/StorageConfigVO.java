package org.jeecg.modules.system.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 存储桶配置展示（无密钥明文）
 */
@Data
public class StorageConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** database：已落库；yml：未配置使用 jeecg.uploadType */
    private String effectiveSource;

    private String ymlUploadType;
    private String ymlUploadPath;

    private String id;
    private String storageType;

    private String aliyunEndpoint;
    private String aliyunBucket;
    private String aliyunAccessKeyId;
    private boolean aliyunSecretConfigured;
    private String aliyunStaticDomain;
    private Boolean aliyunTransferAccel;
    private String aliyunRoleArn;

    private String tencentRegion;
    private String tencentBucket;
    private String tencentSecretId;
    private boolean tencentSecretKeyConfigured;
    private String tencentDomain;
    private Boolean tencentGlobalAccel;

    private String remark;
    private String updateBy;
    private Date updateTime;
}
