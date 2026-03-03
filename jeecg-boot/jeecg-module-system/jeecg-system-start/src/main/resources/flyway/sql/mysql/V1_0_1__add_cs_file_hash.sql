CREATE TABLE IF NOT EXISTS `cs_file_hash` (
  `id` varchar(32) NOT NULL,
  `md5_hash` varchar(32) NOT NULL COMMENT 'MD5哈希值',
  `file_path` varchar(500) NOT NULL COMMENT '文件存储路径',
  `file_size` bigint NOT NULL COMMENT '文件大小(字节)',
  `file_name` varchar(255) DEFAULT NULL COMMENT '原始文件名',
  `biz_path` varchar(50) DEFAULT NULL COMMENT '业务路径(airag/cs-visitor)',
  `create_by` varchar(32) DEFAULT NULL COMMENT '创建人',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_md5_size` (`md5_hash`, `file_size`),
  KEY `idx_md5_hash` (`md5_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件哈希秒传记录';
