ALTER TABLE `cs_quick_reply` ADD COLUMN `shortcut_key` varchar(50) NULL DEFAULT NULL COMMENT '快捷键(如 Ctrl+1)' AFTER `sort`;
