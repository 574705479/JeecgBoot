package org.jeecg.modules.airag.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * MongoDB 配置类
 *
 * @author jeecg
 * @date 2026-01-08
 */
@Configuration
@EnableMongoRepositories(basePackages = "org.jeecg.modules.airag.chat.repository")
@EnableMongoAuditing
public class MongoConfig {
    // MongoDB 自动配置已由 Spring Boot Data MongoDB Starter 提供
    // 此类主要用于启用 MongoDB Repository 扫描
}
