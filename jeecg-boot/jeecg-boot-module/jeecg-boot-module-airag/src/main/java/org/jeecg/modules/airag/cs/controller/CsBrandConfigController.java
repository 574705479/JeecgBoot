package org.jeecg.modules.airag.cs.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.airag.cs.entity.CsBrandConfig;
import org.jeecg.modules.airag.cs.service.ICsBrandConfigService;
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

    /**
     * 获取当前品牌配置
     */
    @Operation(summary = "获取当前品牌配置")
    @org.jeecg.config.shiro.IgnoreAuth
    @GetMapping("/get")
    public Result<CsBrandConfig> getBrandConfig() {
        QueryWrapper<CsBrandConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("del_flag", 0).eq("status", 1).orderByDesc("update_time");
        List<CsBrandConfig> list = brandConfigService.list(wrapper);
        CsBrandConfig config = list.isEmpty() ? null : list.get(0);
        return Result.OK(config);
    }

    /**
     * 保存品牌配置（新增或更新）
     */
    @Operation(summary = "保存品牌配置")
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
