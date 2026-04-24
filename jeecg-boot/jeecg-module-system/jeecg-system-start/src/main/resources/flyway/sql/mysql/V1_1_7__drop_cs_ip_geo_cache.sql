-- ----------------------------
-- 清理 ip-api.com 迁移后遗留的缓存表
-- CsIpGeoService 已切换到 ip2region 离线库（2026-04-24），不再读写该表，
-- 同时移除了 CsDataCleanupTask 里的 cleanIpGeoCache 分支。
-- 本迁移负责在生产环境下彻底删除这张空表，避免冗余 schema。
-- ----------------------------
DROP TABLE IF EXISTS `cs_ip_geo_cache`;
