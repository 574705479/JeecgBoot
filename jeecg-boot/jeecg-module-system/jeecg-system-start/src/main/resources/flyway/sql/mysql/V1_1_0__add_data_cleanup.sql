-- ==========================================
-- 数据清理功能：建表 + 菜单 + 权限 + 初始配置
-- ==========================================

-- 1. 清理执行日志表
CREATE TABLE IF NOT EXISTS `cs_cleanup_log` (
  `id` varchar(32) NOT NULL,
  `trigger_type` varchar(10) DEFAULT NULL COMMENT '触发方式: auto-定时, manual-手动',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `duration_ms` int DEFAULT NULL COMMENT '耗时(ms)',
  `result_json` text COMMENT '各项删除数量JSON',
  `create_by` varchar(50) DEFAULT NULL COMMENT '触发人(手动时记录)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据清理执行日志';

-- 2. 菜单：数据清理（挂在 cs_parent 下）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`,
  `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`,
  `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`,
  `description`, `create_by`, `create_time`, `update_by`, `update_time`,
  `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('cs_data_cleanup', 'cs_parent', '数据清理', '/cs/dataCleanup',
  'super/airag/cs/dataCleanup/index', 1, NULL, NULL, 1, NULL, '1',
  12.00, 0, 'ant-design:delete-outlined', 1, 0, 0, 0,
  '数据清理设置', 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);

-- 3. 角色权限：管理员
INSERT INTO `sys_role_permission` VALUES
  (REPLACE(UUID(),'-',''), 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_data_cleanup', NULL, NOW(), '127.0.0.1');
-- 角色权限：管理员客服
INSERT INTO `sys_role_permission` VALUES
  (REPLACE(UUID(),'-',''), 'cs_admin_agent_role_001', 'cs_data_cleanup', NULL, NOW(), '127.0.0.1');

-- 4. 初始化清理配置
INSERT INTO `cs_global_config` (`config_key`, `config_value`, `create_time`, `update_time`)
VALUES ('data_cleanup', '{"enabled":true,"conversationDays":90,"logAndVisitorDays":90,"cacheDays":180}', NOW(), NOW())
ON DUPLICATE KEY UPDATE config_key = config_key;
