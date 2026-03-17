ALTER TABLE cs_conversation ADD COLUMN human_agent_mode int DEFAULT 0 COMMENT '是否为人工客服转接模式: 0-否 1-是';
ALTER TABLE cs_conversation ADD COLUMN custom_fields text DEFAULT NULL COMMENT '访客自定义字段(JSON)';
