-- 全球加速开关、阿里云角色 ARN
ALTER TABLE `sys_storage_config`
  ADD COLUMN `aliyun_transfer_accel` tinyint(1) NOT NULL DEFAULT 0 COMMENT '阿里云OSS传输加速' AFTER `aliyun_static_domain`,
  ADD COLUMN `aliyun_role_arn` varchar(512) DEFAULT NULL COMMENT '阿里云RAM角色ARN' AFTER `aliyun_transfer_accel`,
  ADD COLUMN `tencent_global_accel` tinyint(1) NOT NULL DEFAULT 0 COMMENT '腾讯云COS全球加速' AFTER `tencent_domain`;
