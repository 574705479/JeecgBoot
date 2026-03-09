-- 在系统管理下新增"产品和价格"菜单
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('license_plan_menu_001', 'd7d6e2e4e2934f2c9385a623fd98c6f3', '产品和价格', '/system/licensePlan', 'system/licensePlan/index', 1, NULL, NULL, 0, NULL, '0', 9.0, 0, 'ant-design:appstore-outlined', 1, 0, 0, 0, '查看授权套餐和定价信息', 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);

-- 系统管理员角色
INSERT INTO `sys_role_permission` VALUES (REPLACE(UUID(),'-',''), 'f6817f48af4fb3af11b9e8bf182f618b', 'license_plan_menu_001', NULL, NOW(), '127.0.0.1');
-- 管理员客服角色
INSERT INTO `sys_role_permission` VALUES (REPLACE(UUID(),'-',''), 'cs_admin_agent_role_001', 'license_plan_menu_001', NULL, NOW(), '127.0.0.1');
