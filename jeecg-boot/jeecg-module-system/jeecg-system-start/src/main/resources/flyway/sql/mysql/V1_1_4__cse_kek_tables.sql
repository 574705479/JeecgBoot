-- CSE 文件加密 KEK 主表 + 操作审计日志
-- KEK 由后端 KekProvider.@PostConstruct 检测空表自动生成 k1，本脚本不预置数据
DROP TABLE IF EXISTS `cse_kek`;
CREATE TABLE `cse_kek` (
  `kid` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '密钥 ID 如 k1/k2/k3',
  `kek_b64` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'KEK base64（32 字节 AES-256）',
  `status` varchar(16) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'ACTIVE/STAGED/DEPRECATED/ARCHIVED',
  `created_by` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `created_time` datetime NULL DEFAULT NULL,
  `activated_time` datetime NULL DEFAULT NULL,
  `deprecated_time` datetime NULL DEFAULT NULL,
  `last_used_time` datetime NULL DEFAULT NULL,
  `file_count` bigint NOT NULL DEFAULT 0 COMMENT '冗余字段定时刷新',
  `remark` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`kid`),
  KEY `idx_cse_kek_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci COMMENT='CSE 文件加密 KEK 主表';

DROP TABLE IF EXISTS `cse_kek_audit_log`;
CREATE TABLE `cse_kek_audit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `kid` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `action` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'GENERATE/ACTIVATE/DEPRECATE/EXPORT/IMPORT/INIT/ARCHIVE',
  `operator_id` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `operator_name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `operator_ip` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `operate_time` datetime NOT NULL,
  `remark` varchar(512) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_cse_kek_audit_kid` (`kid`),
  KEY `idx_cse_kek_audit_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci COMMENT='CSE KEK 操作审计日志';
