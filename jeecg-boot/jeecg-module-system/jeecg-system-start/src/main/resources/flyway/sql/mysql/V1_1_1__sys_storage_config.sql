-- 存储桶全局配置（单行）
CREATE TABLE IF NOT EXISTS `sys_storage_config` (
  `id` varchar(36) NOT NULL COMMENT '主键，固定单例',
  `storage_type` varchar(32) NOT NULL DEFAULT 'SYSTEM' COMMENT 'SYSTEM本地 ALIYUN TENCENT',
  `aliyun_endpoint` varchar(255) DEFAULT NULL,
  `aliyun_bucket` varchar(128) DEFAULT NULL,
  `aliyun_access_key_id` varchar(128) DEFAULT NULL,
  `aliyun_secret_cipher` text COMMENT 'AES密文',
  `aliyun_static_domain` varchar(512) DEFAULT NULL,
  `tencent_region` varchar(64) DEFAULT NULL,
  `tencent_bucket` varchar(128) DEFAULT NULL,
  `tencent_secret_id` varchar(128) DEFAULT NULL,
  `tencent_secret_key_cipher` text COMMENT 'AES密文',
  `tencent_domain` varchar(512) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `create_by` varchar(32) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(32) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全站存储桶配置';

-- 无初始行：未配置时沿用 yml 的 jeecg.uploadType；首次保存后写入 id=1

-- 系统管理下「存储桶配置」菜单（与同目录其它菜单一致，仅平台管理员角色）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('storage_config_menu_001', 'd7d6e2e4e2934f2c9385a623fd98c6f3', '存储桶配置', '/system/storageConfig', 'system/storageConfig/index', 1, NULL, NULL, 0, NULL, '0', 10.0, 0, 'ant-design:cloud-server-outlined', 1, 0, 0, 0, '对象存储类型与云厂商接入配置', 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);

INSERT INTO `sys_role_permission` VALUES (REPLACE(UUID(),'-',''), 'f6817f48af4fb3af11b9e8bf182f618b', 'storage_config_menu_001', NULL, NOW(), '127.0.0.1');
