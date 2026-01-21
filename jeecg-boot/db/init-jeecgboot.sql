CREATE DATABASE IF NOT EXISTS `jeecg-boot`
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `jeecg-boot`;

SOURCE /docker-entrypoint-initdb.d/jeecgboot.sql;
