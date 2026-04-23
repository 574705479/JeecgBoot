package org.jeecg.modules.airag.cs.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.jeecg.common.api.vo.Result;
import com.alibaba.fastjson.JSON;
import org.jeecg.modules.airag.cs.constant.CsRedisKeys;
import org.jeecg.modules.airag.cs.entity.CsBrandConfig;
import org.jeecg.modules.airag.cs.service.ICsBrandConfigService;
import org.jeecg.modules.airag.cs.util.CsCryptoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 客服系统品牌配置
 *
 * @author jeecg
 * @date 2026-01-20
 */
@Slf4j
@Tag(name = "品牌配置")
@RestController
@RequestMapping("/cs/brand")
public class CsBrandConfigController {

    @Autowired
    private ICsBrandConfigService brandConfigService;

    @Autowired
    private CsCryptoUtil csCryptoUtil;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 获取当前品牌配置
     *
     * <p>Phase 3 加 Redis 缓存：缓存的是「明文 JSON 字符串」（不是密文），返回时再加 transport
     * 加密。这样：
     * <ul>
     *   <li>多端共享同一份缓存（密文每次返回会因 IV/key rotate 不同，不能缓存）</li>
     *   <li>save 接口主动失效缓存，TTL 5min 兜底防漂移</li>
     *   <li>Redis 故障时回落直接查 DB（do-not-cache 路径）</li>
     * </ul>
     */
    @Operation(summary = "获取当前品牌配置")
    @org.jeecg.config.shiro.IgnoreAuth
    @GetMapping("/get")
    public Result<String> getBrandConfig() {
        return Result.OK(csCryptoUtil.encryptTransport(loadBrandConfigJson()));
    }

    /**
     * 提供给 bootstrap 合包接口直接复用：返回当前品牌配置的明文 JSON 字符串
     * （不带 transport 加密，由调用方自行处理外层加密）。
     */
    public String loadBrandConfigJson() {
        try {
            String cached = redisTemplate.opsForValue().get(CsRedisKeys.REDIS_BRAND_CONFIG);
            if (cached != null) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("[CS-Brand] Redis 读品牌配置失败，回落 DB: {}", e.getMessage());
        }
        QueryWrapper<CsBrandConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("del_flag", 0).eq("status", 1).orderByDesc("update_time");
        List<CsBrandConfig> list = brandConfigService.list(wrapper);
        CsBrandConfig config = list.isEmpty() ? null : list.get(0);
        String json = JSON.toJSONString(config);
        try {
            redisTemplate.opsForValue().set(
                    CsRedisKeys.REDIS_BRAND_CONFIG,
                    json,
                    CsRedisKeys.BRAND_CONFIG_TTL_SECONDS,
                    TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("[CS-Brand] Redis 写品牌配置失败（非致命）: {}", e.getMessage());
        }
        return json;
    }

    private void evictBrandConfigCache() {
        try {
            redisTemplate.delete(CsRedisKeys.REDIS_BRAND_CONFIG);
        } catch (Exception e) {
            log.warn("[CS-Brand] Redis 失效品牌缓存失败（非致命）: {}", e.getMessage());
        }
    }

    /**
     * 保存品牌配置（新增或更新）
     *
     * 权限：仅「管理员」(admin) 或「管理员客服」(cs_admin_agent) 可调用。
     * 子客服 (cs_sub_agent) 与普通登录用户无权修改 brand 配置。
     *
     * 安全说明：brand 字段中的 fid 会被自动加入「匿名解密白名单」（CsBrandFileController），
     * 任何能写 brand 字段的人都能间接让该 fid 被全网匿名访问。因此必须严格限制写权限。
     */
    @Operation(summary = "保存品牌配置")
    @RequiresRoles(value = {"admin", "cs_admin_agent"}, logical = Logical.OR)
    @PostMapping("/save")
    public Result<String> saveBrandConfig(@RequestBody CsBrandConfig config) {
        Date now = new Date();
        CsBrandConfig target = config;
        if (target.getId() == null || target.getId().isEmpty()) {
            QueryWrapper<CsBrandConfig> wrapper = new QueryWrapper<>();
            wrapper.eq("del_flag", 0).eq("status", 1).orderByDesc("update_time");
            List<CsBrandConfig> list = brandConfigService.list(wrapper);
            if (!list.isEmpty()) {
                target.setId(list.get(0).getId());
            } else {
                target.setId(UUID.randomUUID().toString().replace("-", ""));
                target.setCreateTime(now);
            }
        }
        target.setUpdateTime(now);
        brandConfigService.saveOrUpdate(target);
        evictBrandConfigCache();
        return Result.OK("保存成功");
    }
}
