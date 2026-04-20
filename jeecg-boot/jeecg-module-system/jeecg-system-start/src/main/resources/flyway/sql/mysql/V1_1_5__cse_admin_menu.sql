-- 后台菜单：系统管理 → 文件加密密钥管理（CSE KEK 管理）
-- 父菜单 ID d7d6e2e4e2934f2c9385a623fd98c6f3 = 系统管理
-- 角色 ID f6817f48af4fb3af11b9e8bf182f618b = admin 管理员

-- 一级菜单（系统管理下）
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `url`, `component`, `is_route`, `component_name`, `redirect`, `menu_type`, `perms`, `perms_type`, `sort_no`, `always_show`, `icon`, `is_leaf`, `keep_alive`, `hidden`, `hide_tab`, `description`, `create_by`, `create_time`, `update_by`, `update_time`, `del_flag`, `rule_flag`, `status`, `internal_or_external`)
VALUES ('cse_kek_menu', 'd7d6e2e4e2934f2c9385a623fd98c6f3', '文件加密密钥', '/system/cse/kek', 'system/cse/CseKekManagement', 1, 'CseKekManagement', NULL, 1, 'cse:kek:view', '1', 99.00, 0, 'ant-design:key-outlined', 1, 0, 0, 0, '管理 KEK 与轮换', 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);

-- 子按钮权限码
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `menu_type`, `perms`, `perms_type`, `status`, `del_flag`, `create_by`, `create_time`) VALUES
  ('cse_kek_btn_view',     'cse_kek_menu', '查看密钥列表', 2, 'cse:kek:view',     '1', '1', 0, 'admin', NOW()),
  ('cse_kek_btn_generate', 'cse_kek_menu', '生成新密钥',   2, 'cse:kek:generate', '1', '1', 0, 'admin', NOW()),
  ('cse_kek_btn_activate', 'cse_kek_menu', '激活密钥',     2, 'cse:kek:activate', '1', '1', 0, 'admin', NOW()),
  ('cse_kek_btn_archive',  'cse_kek_menu', '归档密钥',     2, 'cse:kek:archive',  '1', '1', 0, 'admin', NOW()),
  ('cse_kek_btn_export',   'cse_kek_menu', '导出备份',     2, 'cse:kek:export',   '1', '1', 0, 'admin', NOW()),
  ('cse_kek_btn_import',   'cse_kek_menu', '导入恢复',     2, 'cse:kek:import',   '1', '1', 0, 'admin', NOW()),
  ('cse_kek_btn_log',      'cse_kek_menu', '查看审计日志', 2, 'cse:kek:view-log', '1', '1', 0, 'admin', NOW());

-- 默认授予 admin 角色全部 CSE KEK 权限
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `data_rule_ids`, `operate_date`, `operate_ip`)
SELECT REPLACE(UUID(),'-',''), 'f6817f48af4fb3af11b9e8bf182f618b', id, NULL, NOW(), '127.0.0.1'
FROM `sys_permission` WHERE id LIKE 'cse_kek%';
