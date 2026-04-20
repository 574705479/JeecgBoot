-- ============================================================================
-- CSE 动态配置：把 enabled / publicPaths / encryptedPaths 从 yml 搬到 DB
-- 单行表，主键 id="1"（与 sys_storage_config.ID_SINGLETON 惯例一致）
-- 种子值与新字典对齐：
--   ENCRYPT (9): avatar/cs-brand/cs-visitor/airag/comment/jeditor/markdown/import/temp
--   PUBLIC  (3): public/captcha/appVersion
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_cse_config` (
  `id`              varchar(32)  CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `enabled`         tinyint(1)   NOT NULL DEFAULT 1 COMMENT '总开关 1=启用 0=关闭',
  `public_paths`    text         CHARACTER SET utf8mb4 NULL COMMENT 'JSON 数组：黑名单（命中即明文）',
  `encrypted_paths` text         CHARACTER SET utf8mb4 NULL COMMENT 'JSON 数组：白名单（命中才加密）',
  `update_time`     datetime     NULL DEFAULT NULL,
  `update_by`       varchar(64)  CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CSE 动态配置（单行 id=1）';

-- INSERT IGNORE 防御：人工预跑 / 升级失败重跑 / 多节点并发首启
INSERT IGNORE INTO `sys_cse_config` (`id`, `enabled`, `public_paths`, `encrypted_paths`, `update_time`, `update_by`)
VALUES ('1', 1,
  '["public/","captcha/","appVersion/"]',
  '["avatar/","cs-brand/","cs-visitor/","airag/","comment/","jeditor/","markdown/","import/","temp/"]',
  NOW(), 'system');

-- ============================================================================
-- 菜单文案：原 KEK 菜单改名为「文件加密管理」（拆 Tab 后既含基础配置又含密钥管理）
-- ============================================================================
UPDATE `sys_permission` SET `name` = '文件加密管理',
       `description` = '文件加密总开关、白名单、KEK 密钥管理'
 WHERE `id` = 'cse_kek_menu';

-- ============================================================================
-- 新权限位：基础配置查看 / 编辑
-- ============================================================================
INSERT INTO `sys_permission` (`id`, `parent_id`, `name`, `menu_type`, `perms`, `perms_type`, `status`, `del_flag`, `create_by`, `create_time`) VALUES
  ('cse_config_btn_view', 'cse_kek_menu', '查看基础配置', 2, 'cse:config:view', '1', '1', 0, 'admin', NOW()),
  ('cse_config_btn_edit', 'cse_kek_menu', '编辑基础配置', 2, 'cse:config:edit', '1', '1', 0, 'admin', NOW());

-- 默认授予 admin 角色（f6817f48af4fb3af11b9e8bf182f618b）新权限
INSERT INTO `sys_role_permission` (`id`, `role_id`, `permission_id`, `data_rule_ids`, `operate_date`, `operate_ip`)
SELECT REPLACE(UUID(),'-',''), 'f6817f48af4fb3af11b9e8bf182f618b', id, NULL, NOW(), '127.0.0.1'
FROM `sys_permission` WHERE id IN ('cse_config_btn_view','cse_config_btn_edit');
