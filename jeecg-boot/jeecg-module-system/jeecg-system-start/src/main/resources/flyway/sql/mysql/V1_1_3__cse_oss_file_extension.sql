-- CSE 文件加密元数据扩展：oss_file 表新增端到端加密相关字段
-- 历史数据兼容：所有新列允许 NULL，public_flag 默认 1 让历史 URL 自动走老明文链路
ALTER TABLE `oss_file`
  ADD COLUMN `file_id` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'CSE 文件业务 ID（cse 协议后面那串）' AFTER `id`,
  ADD COLUMN `algo` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '加密算法 AES-256-GCM',
  ADD COLUMN `iv_b64` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '12 字节 IV base64',
  ADD COLUMN `dek_wrapped_b64` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'KEK 包装后的 DEK base64',
  ADD COLUMN `kek_kid` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '使用的 KEK kid',
  ADD COLUMN `mime_type` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '原始 MIME',
  ADD COLUMN `origin_size` bigint NULL DEFAULT NULL COMMENT '原始字节数',
  ADD COLUMN `cipher_size` bigint NULL DEFAULT NULL COMMENT '密文字节数',
  ADD COLUMN `public_flag` tinyint(1) NOT NULL DEFAULT 1 COMMENT '1=公开/明文 0=私有/加密；历史数据默认 1 兼容',
  ADD COLUMN `thumb_object_key` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '加密缩略图对象 key',
  ADD COLUMN `tenant_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '租户隔离',
  ADD COLUMN `sha256` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '原文 sha256（秒传/校验）',
  ADD COLUMN `biz_path` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '业务路径前缀，用于权限映射',
  ADD COLUMN `storage_type` varchar(16) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '存储类型 local/aliyun/tencent',
  ADD COLUMN `bucket` varchar(128) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '存储桶（OSS/COS）',
  ADD COLUMN `object_key` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '密文对象 key（不含 bucket）',
  ADD UNIQUE INDEX `idx_oss_file_fid` (`file_id`),
  ADD INDEX `idx_oss_file_tenant` (`tenant_id`),
  ADD INDEX `idx_oss_file_kek` (`kek_kid`),
  ADD INDEX `idx_oss_file_sha256` (`sha256`);
