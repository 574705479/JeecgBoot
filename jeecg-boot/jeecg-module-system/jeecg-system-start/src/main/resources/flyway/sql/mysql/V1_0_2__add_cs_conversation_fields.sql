ALTER TABLE cs_conversation ADD COLUMN agent_message_count int DEFAULT 0 COMMENT '客服消息数';
ALTER TABLE cs_conversation ADD COLUMN visitor_message_count int DEFAULT 0 COMMENT '访客消息数';
ALTER TABLE cs_conversation ADD COLUMN end_type tinyint DEFAULT NULL COMMENT '结束方式: 0-客服主动 1-超时自动 2-访客结束 3-系统清理';
ALTER TABLE cs_conversation ADD COLUMN first_response_seconds int DEFAULT NULL COMMENT '首次响应时长(秒)';
ALTER TABLE cs_conversation ADD COLUMN landing_page varchar(1000) DEFAULT NULL COMMENT '着陆页URL';
ALTER TABLE cs_conversation ADD COLUMN referrer_page varchar(1000) DEFAULT NULL COMMENT '来源页URL';
ALTER TABLE cs_conversation ADD COLUMN agent_timeout_notified tinyint(1) DEFAULT 0 COMMENT '客服超时未回复是否已通知访客';
