ALTER TABLE license ADD COLUMN domain_config JSON DEFAULT NULL
    COMMENT '域名配置(domains换行分隔, downloadLinks JSON数组)';
