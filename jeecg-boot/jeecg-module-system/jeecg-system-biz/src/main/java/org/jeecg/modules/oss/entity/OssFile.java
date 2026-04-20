package org.jeecg.modules.oss.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecg.common.system.base.entity.JeecgEntity;
import org.jeecgframework.poi.excel.annotation.Excel;

/**
 * @Description: oss云存储实体类（含 CSE 端到端加密元数据）
 * @author: jeecg-boot
 */
@Data
@TableName("oss_file")
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class OssFile extends JeecgEntity {

	private static final long serialVersionUID = 1L;

	@Excel(name = "文件名称")
	private String fileName;

	@Excel(name = "文件地址")
	private String url;

	// === CSE 端到端加密元数据 (V1_1_3 起) ===

	/** CSE 业务 ID（cse 协议后面那串），全局唯一 */
	private String fileId;

	/** 加密算法标识，如 AES-256-GCM；NULL 表示未加密历史数据 */
	private String algo;

	/** 12 字节 IV base64 */
	private String ivB64;

	/** KEK 包装后的 DEK base64 */
	private String dekWrappedB64;

	/** 使用的 KEK kid，如 k1/k2 */
	private String kekKid;

	/** 原始 MIME */
	private String mimeType;

	/** 原始字节数 */
	private Long originSize;

	/** 密文字节数 */
	private Long cipherSize;

	/** 1=公开/明文 0=私有/加密；历史数据默认 1 兼容 */
	private Integer publicFlag;

	/** 加密缩略图对象 key */
	private String thumbObjectKey;

	/** 租户隔离 */
	private String tenantId;

	/** 原文 sha256（秒传/校验） */
	private String sha256;

	/** 业务路径前缀，用于权限映射 */
	private String bizPath;

	/** 存储类型 local/aliyun/tencent */
	private String storageType;

	/** 存储桶（OSS/COS） */
	private String bucket;

	/** 密文对象 key（不含 bucket） */
	private String objectKey;

}
