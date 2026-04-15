package org.jeecg.config.init;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Shiro 缓存与单点登录映射清理
 * 在应用启动时清除所有的 Shiro 授权缓存（解决重启后未重新登录时按钮权限不生效）
 * 并清理单点登录（single_login）映射，避免并发登录受限（is-concurrent=false）时，
 * 重启后首次登录误触发互踢与 WebSocket KICK，导致页面不可用
 */
@Slf4j
@Component
public class ShiroCacheClearRunner implements ApplicationRunner {

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void run(ApplicationArguments args) {
        // 清空所有授权redis缓存
        log.info("——— Service restart, clearing all user shiro authorization cache ——— ");
        redisUtil.removeAll(CommonConstant.PREFIX_USER_SHIRO_CACHE);

        // 清理单点登录映射（PC/APP/PHONE），避免 Redis 中残留旧 username→token 映射导致重登时误互踢
        log.info("——— Service restart, clearing single sign-on token mappings ——— ");
        redisUtil.removeAll(CommonConstant.PREFIX_USER_TOKEN_PC);
        redisUtil.removeAll(CommonConstant.PREFIX_USER_TOKEN_APP);
        redisUtil.removeAll(CommonConstant.PREFIX_USER_TOKEN_PHONE);
    }
}