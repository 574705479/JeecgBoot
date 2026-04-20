package org.jeecg.modules.airag.cs.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.jeecg.common.api.vo.Result;
import com.alibaba.fastjson.JSON;
import org.jeecg.modules.airag.cs.entity.CsBrandConfig;
import org.jeecg.modules.airag.cs.service.ICsBrandConfigService;
import org.jeecg.modules.airag.cs.util.CsCryptoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.UUID;

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

    /**
     * 获取当前品牌配置
     */
    @Operation(summary = "获取当前品牌配置")
    @org.jeecg.config.shiro.IgnoreAuth
    @GetMapping("/get")
    public Result<String> getBrandConfig() {
        QueryWrapper<CsBrandConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("del_flag", 0).eq("status", 1).orderByDesc("update_time");
        List<CsBrandConfig> list = brandConfigService.list(wrapper);
        CsBrandConfig config = list.isEmpty() ? null : list.get(0);
        return Result.OK(csCryptoUtil.encryptTransport(JSON.toJSONString(config)));
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
        return Result.OK("保存成功");
    }
}
