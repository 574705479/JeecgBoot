-- =============================================
-- 统计分析模块：客服状态变更日志表 + 及时回复计数 + 菜单
-- =============================================

-- 1. 客服状态变更日志表
CREATE TABLE IF NOT EXISTS cs_agent_status_log (
  id bigint NOT NULL,
  agent_id varchar(64) NOT NULL COMMENT '客服ID',
  status tinyint NOT NULL COMMENT '状态: 0-离线/隐身 1-在线 2-忙碌',
  trigger_source varchar(32) DEFAULT 'manual' COMMENT '触发来源: manual-手动, websocket_disconnect-断连, system-系统自动',
  start_time datetime NOT NULL COMMENT '状态开始时间',
  end_time datetime DEFAULT NULL COMMENT '状态结束时间(NULL表示当前状态)',
  duration_seconds int DEFAULT NULL COMMENT '持续时长(秒)',
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_agent_date (agent_id, start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服状态变更日志';

-- 2. cs_conversation 新增及时回复计数字段
ALTER TABLE cs_conversation ADD COLUMN timely_reply_count int DEFAULT 0 COMMENT '及时回复计数';

-- 3. 统计分析菜单
INSERT INTO `sys_permission` VALUES ('cs_statistics', 'cs_parent', '统计分析', '/cs/statistics', 'layouts/RouteView', 1, NULL, NULL, 0, NULL, '1', 8.00, 0, 'ant-design:bar-chart-outlined', 0, 0, 0, 0, '统计分析', 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_stat_agent_conv', 'cs_statistics', '客服对话统计', '/cs/statistics/agentConversation', 'super/airag/cs/statistics/agentConversation/index', 1, NULL, NULL, 1, NULL, '1', 1.00, 0, NULL, 1, 0, 0, 0, '客服对话统计', 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_stat_visitor_region', 'cs_statistics', '访客区域统计', '/cs/statistics/visitorRegion', 'super/airag/cs/statistics/visitorRegion/index', 1, NULL, NULL, 1, NULL, '1', 2.00, 0, NULL, 1, 0, 0, 0, '访客区域统计', 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_stat_attendance', 'cs_statistics', '出勤记录', '/cs/statistics/attendance', 'super/airag/cs/statistics/attendance/index', 1, NULL, NULL, 1, NULL, '1', 3.00, 0, NULL, 1, 0, 0, 0, '出勤记录', 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);
INSERT INTO `sys_permission` VALUES ('cs_stat_agent_efficiency', 'cs_statistics', '客服对话效率', '/cs/statistics/agentEfficiency', 'super/airag/cs/statistics/agentEfficiency/index', 1, NULL, NULL, 1, NULL, '1', 4.00, 0, NULL, 1, 0, 0, 0, '客服对话效率', 'admin', NOW(), NULL, NULL, 0, 0, '1', 0);

-- 4. 角色权限（管理员角色）
INSERT INTO `sys_role_permission` VALUES (REPLACE(UUID(),'-',''), 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_statistics', NULL, NOW(), '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES (REPLACE(UUID(),'-',''), 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_stat_agent_conv', NULL, NOW(), '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES (REPLACE(UUID(),'-',''), 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_stat_visitor_region', NULL, NOW(), '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES (REPLACE(UUID(),'-',''), 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_stat_attendance', NULL, NOW(), '127.0.0.1');
INSERT INTO `sys_role_permission` VALUES (REPLACE(UUID(),'-',''), 'f6817f48af4fb3af11b9e8bf182f618b', 'cs_stat_agent_efficiency', NULL, NOW(), '127.0.0.1');
