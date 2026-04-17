package org.jeecg.modules.airag.cs.service;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.cs.entity.CsGlobalConfig;
import org.jeecg.modules.airag.cs.mapper.CsGlobalConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * CS 模块全局配置缓存：先查 Redis，未命中再查 DB 并回填 Redis。
 *
 * <p>该类统一了散布在 controller / service / interceptor / task 中
 * 的 {@code Redis -> DB -> 回写 Redis} 模式，调用方按以下两类语义选择：
 * <ul>
 *   <li>{@link #get(String, String)}：DB 也未命中时返回 {@code null}，且不回填 Redis</li>
 *   <li>{@link #getOrCacheDefault(String, String, String)}：DB 也未命中时将 fallback 值回填 Redis 并返回，
 *       适用于布尔开关等默认 false 的场景，避免每次请求都查 DB</li>
 * </ul>
 *
 * @author jeecg
 */
@Slf4j
@Service
public class CsGlobalConfigCache {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private CsGlobalConfigMapper csGlobalConfigMapper;

    /**
     * 先查 Redis；未命中再查 DB，DB 命中时回填 Redis；都未命中时返回 {@code null}。
     */
    public String get(String redisKey, String configId) {
        String value = redisTemplate.opsForValue().get(redisKey);
        if (oConvertUtils.isNotEmpty(value)) {
            return value;
        }
        CsGlobalConfig config = csGlobalConfigMapper.selectById(configId);
        String dbValue = config != null ? config.getConfigValue() : null;
        if (oConvertUtils.isNotEmpty(dbValue)) {
            redisTemplate.opsForValue().set(redisKey, dbValue);
            return dbValue;
        }
        return null;
    }

    /**
     * 同 {@link #get}，但 DB 也未命中时将 {@code fallback} 回填 Redis 并返回。
     * <p>典型场景：布尔开关的默认值 {@code "false"}。
     */
    public String getOrCacheDefault(String redisKey, String configId, String fallback) {
        String value = redisTemplate.opsForValue().get(redisKey);
        if (value != null) {
            return value;
        }
        CsGlobalConfig config = csGlobalConfigMapper.selectById(configId);
        String resolved = config != null ? config.getConfigValue() : fallback;
        if (resolved == null) {
            resolved = fallback;
        }
        redisTemplate.opsForValue().set(redisKey, resolved);
        return resolved;
    }
}
