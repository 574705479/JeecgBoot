-- =============================================
-- 客服状态新增"隐身"(status=3)，区分手动隐身与离线
-- =============================================

-- 将存量日志中手动隐身的记录 status 从 0 改为 3
UPDATE cs_agent_status_log SET status = 3 WHERE status = 0 AND trigger_source = 'manual';
