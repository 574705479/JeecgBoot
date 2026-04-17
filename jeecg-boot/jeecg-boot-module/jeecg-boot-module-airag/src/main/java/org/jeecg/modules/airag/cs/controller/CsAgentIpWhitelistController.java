package org.jeecg.modules.airag.cs.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.cs.constant.CsRedisKeys;
import org.jeecg.modules.airag.cs.entity.CsAgentIpWhitelist;
import org.jeecg.modules.airag.cs.entity.CsGlobalConfig;
import org.jeecg.modules.airag.cs.mapper.CsAgentIpWhitelistMapper;
import org.jeecg.modules.airag.cs.mapper.CsGlobalConfigMapper;
import org.jeecg.modules.airag.cs.util.CsIpMatchUtil;
import org.jeecg.modules.airag.cs.util.CsRequestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 客服IP白名单Controller
 */
@Slf4j
@Tag(name = "客服IP白名单")
@RestController
@RequestMapping("/cs/security/agent-ip-whitelist")
public class CsAgentIpWhitelistController {

    @Autowired
    private CsAgentIpWhitelistMapper whitelistMapper;

    @Autowired
    private CsGlobalConfigMapper csGlobalConfigMapper;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private org.jeecg.modules.airag.cs.service.CsGlobalConfigCache configCache;

    @Operation(summary = "分页列表")
    @GetMapping("/list")
    public Result<IPage<CsAgentIpWhitelist>> list(
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "ip", required = false) String ip) {
        LambdaQueryWrapper<CsAgentIpWhitelist> qw = new LambdaQueryWrapper<>();
        if (oConvertUtils.isNotEmpty(ip)) {
            qw.like(CsAgentIpWhitelist::getIp, ip);
        }
        qw.orderByDesc(CsAgentIpWhitelist::getCreateTime);
        Page<CsAgentIpWhitelist> page = new Page<>(pageNo, pageSize);
        return Result.OK(whitelistMapper.selectPage(page, qw));
    }

    @Operation(summary = "添加白名单")
    @PostMapping("/add")
    public Result<String> add(@RequestBody Map<String, String> params) {
        String ipValue = params.get("ip");
        String remark = params.get("remark");
        if (oConvertUtils.isEmpty(ipValue)) {
            return Result.error("IP不能为空");
        }
        if (!CsIpMatchUtil.isValidIpOrCidr(ipValue)) {
            return Result.error("IP格式无效，支持单个IP或CIDR段（如192.168.1.0/24）");
        }
        LambdaQueryWrapper<CsAgentIpWhitelist> existQw = new LambdaQueryWrapper<>();
        existQw.eq(CsAgentIpWhitelist::getIp, ipValue.trim());
        if (whitelistMapper.selectCount(existQw) > 0) {
            return Result.error("该IP/IP段已在白名单中");
        }

        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String operator = loginUser != null ? loginUser.getUsername() : "system";

        CsAgentIpWhitelist record = new CsAgentIpWhitelist();
        record.setIp(ipValue.trim());
        record.setRemark(remark);
        record.setCreateBy(operator);
        record.setCreateTime(new Date());
        whitelistMapper.insert(record);

        log.info("[CS-Security] 客服IP白名单添加: ip={}, operator={}", ipValue, operator);
        return Result.OK("添加成功");
    }

    @Operation(summary = "编辑白名单")
    @PutMapping("/edit")
    public Result<String> edit(@RequestBody CsAgentIpWhitelist entity) {
        if (oConvertUtils.isEmpty(entity.getId())) {
            return Result.error("ID不能为空");
        }
        if (oConvertUtils.isNotEmpty(entity.getIp()) && !CsIpMatchUtil.isValidIpOrCidr(entity.getIp())) {
            return Result.error("IP格式无效");
        }
        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        entity.setUpdateBy(loginUser != null ? loginUser.getUsername() : "system");
        entity.setUpdateTime(new Date());
        whitelistMapper.updateById(entity);
        return Result.OK("编辑成功");
    }

    @Operation(summary = "删除白名单")
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable String id) {
        whitelistMapper.deleteById(id);
        return Result.OK("删除成功");
    }

    @Operation(summary = "获取白名单开关状态")
    @GetMapping("/enabled")
    public Result<Map<String, Object>> getEnabled() {
        String value = configCache.getOrCacheDefault(
                CsRedisKeys.REDIS_WHITELIST_ENABLED,
                CsRedisKeys.CONFIG_WHITELIST_ENABLED,
                "false");
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", "true".equalsIgnoreCase(value));
        return Result.OK(result);
    }

    @Operation(summary = "获取当前客服IP及白名单匹配状态")
    @GetMapping("/current-ip")
    public Result<Map<String, Object>> getCurrentIp(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        boolean inWhitelist = checkIpInWhitelist(clientIp);
        Map<String, Object> result = new HashMap<>();
        result.put("ip", clientIp);
        result.put("inWhitelist", inWhitelist);
        return Result.OK(result);
    }

    @Operation(summary = "设置白名单开关")
    @PutMapping("/enabled")
    public Result<String> setEnabled(@RequestBody Map<String, Object> params) {
        Boolean enabled = (Boolean) params.get("enabled");
        if (enabled == null) {
            enabled = false;
        }
        String value = enabled.toString();

        // 保存到数据库
        CsGlobalConfig existing = csGlobalConfigMapper.selectById(CsRedisKeys.CONFIG_WHITELIST_ENABLED);
        Date now = new Date();
        if (existing == null) {
            CsGlobalConfig config = new CsGlobalConfig();
            config.setConfigKey(CsRedisKeys.CONFIG_WHITELIST_ENABLED);
            config.setConfigValue(value);
            config.setCreateTime(now);
            config.setUpdateTime(now);
            csGlobalConfigMapper.insert(config);
        } else {
            existing.setConfigValue(value);
            existing.setUpdateTime(now);
            csGlobalConfigMapper.updateById(existing);
        }
        redisTemplate.opsForValue().set(CsRedisKeys.REDIS_WHITELIST_ENABLED, value);

        log.info("[CS-Security] 客服IP白名单开关: enabled={}", enabled);
        return Result.OK("设置成功");
    }

    private boolean checkIpInWhitelist(String clientIp) {
        if (oConvertUtils.isEmpty(clientIp)) {
            return false;
        }
        List<CsAgentIpWhitelist> records = whitelistMapper.selectList(null);
        if (records == null || records.isEmpty()) {
            return false;
        }
        List<String> ipPatterns = records.stream()
                .map(CsAgentIpWhitelist::getIp)
                .collect(Collectors.toList());
        return CsIpMatchUtil.isInAnyRange(clientIp, ipPatterns);
    }

    private String getClientIp(HttpServletRequest request) {
        return CsRequestUtil.getClientIp(request);
    }
}
