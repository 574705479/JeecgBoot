package org.jeecg.modules.airag.cs.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.airag.cs.entity.CsDomainConfig;
import org.jeecg.modules.airag.cs.service.ICsDomainConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 域名配置
 *
 * @author jeecg
 * @date 2026-02-11
 */
@Slf4j
@Tag(name = "域名配置")
@RestController
@RequestMapping("/cs/domain")
public class CsDomainConfigController {

    @Autowired
    private ICsDomainConfigService domainConfigService;

    /**
     * 获取当前域名配置
     */
    @Operation(summary = "获取当前域名配置")
    @GetMapping("/get")
    public Result<CsDomainConfig> getDomainConfig() {
        QueryWrapper<CsDomainConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("del_flag", 0).eq("status", 1).orderByDesc("update_time");
        List<CsDomainConfig> list = domainConfigService.list(wrapper);
        CsDomainConfig config = list.isEmpty() ? null : list.get(0);
        return Result.OK(config);
    }

    /**
     * 保存域名配置（新增或更新）
     */
    @Operation(summary = "保存域名配置")
    @PostMapping("/save")
    public Result<String> saveDomainConfig(@RequestBody CsDomainConfig config) {
        Date now = new Date();
        CsDomainConfig target = config;
        if (target.getId() == null || target.getId().isEmpty()) {
            QueryWrapper<CsDomainConfig> wrapper = new QueryWrapper<>();
            wrapper.eq("del_flag", 0).eq("status", 1).orderByDesc("update_time");
            List<CsDomainConfig> list = domainConfigService.list(wrapper);
            if (!list.isEmpty()) {
                target.setId(list.get(0).getId());
            } else {
                target.setId(UUID.randomUUID().toString().replace("-", ""));
                target.setCreateTime(now);
            }
        }
        target.setUpdateTime(now);
        domainConfigService.saveOrUpdate(target);
        log.info("[CS-Domain] 域名配置已更新");
        return Result.OK("保存成功");
    }
}
