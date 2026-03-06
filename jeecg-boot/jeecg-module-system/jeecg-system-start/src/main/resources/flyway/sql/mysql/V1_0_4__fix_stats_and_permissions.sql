-- =============================================
-- 修复统计指标 + 补充管理员客服角色权限
-- =============================================

-- 1. 清洗存量数据：将错误的 first_response_seconds=0 重置为 NULL
--    旧逻辑在自动分配时设为 0，新逻辑改为追踪客服首次真实回复时间
UPDATE cs_conversation SET first_response_seconds = NULL WHERE first_response_seconds = 0;

-- 2. 管理员客服角色 - 统计分析菜单权限
INSERT INTO `sys_role_permission` VALUES (REPLACE(UUID(),'-',''), 'cs_admin_agent_role_001', 'cs_statistics', NULL, NOW(), '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES (REPLACE(UUID(),'-',''), 'cs_admin_agent_role_001', 'cs_stat_agent_conv', NULL, NOW(), '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES (REPLACE(UUID(),'-',''), 'cs_admin_agent_role_001', 'cs_stat_visitor_region', NULL, NOW(), '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES (REPLACE(UUID(),'-',''), 'cs_admin_agent_role_001', 'cs_stat_attendance', NULL, NOW(), '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES (REPLACE(UUID(),'-',''), 'cs_admin_agent_role_001', 'cs_stat_agent_efficiency', NULL, NOW(), '127.0.0.1');
