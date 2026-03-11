ALTER TABLE cs_visitor ADD COLUMN star_time datetime NULL DEFAULT NULL COMMENT '星标时间' AFTER star;
UPDATE cs_visitor SET star_time = update_time WHERE star = 1;
