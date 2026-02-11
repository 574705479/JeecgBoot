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
import org.jeecg.modules.airag.cs.entity.CsIpBlacklist;
import org.jeecg.modules.airag.cs.mapper.CsIpBlacklistMapper;
import org.jeecg.modules.airag.cs.service.ICsVisitorTokenService;
import org.jeecg.modules.airag.cs.util.CsIpMatchUtil;
import org.jeecg.modules.airag.cs.websocket.CsWebSocketMessage;
import org.jeecg.modules.airag.cs.websocket.CsWebSocketSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 访客IP黑名单Controller
 */
@Slf4j
@Tag(name = "访客IP黑名单")
@RestController
@RequestMapping("/cs/security/ip-blacklist")
public class CsIpBlacklistController {

    @Autowired
    private CsIpBlacklistMapper ipBlacklistMapper;

    @Autowired
    private ICsVisitorTokenService visitorTokenService;

    @Autowired
    private CsWebSocketSessionManager sessionManager;

    @Operation(summary = "分页列表")
    @GetMapping("/list")
    public Result<IPage<CsIpBlacklist>> list(
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "ip", required = false) String ip) {
        LambdaQueryWrapper<CsIpBlacklist> qw = new LambdaQueryWrapper<>();
        if (oConvertUtils.isNotEmpty(ip)) {
            qw.like(CsIpBlacklist::getIp, ip);
        }
        qw.orderByDesc(CsIpBlacklist::getBanDate);
        Page<CsIpBlacklist> page = new Page<>(pageNo, pageSize);
        return Result.OK(ipBlacklistMapper.selectPage(page, qw));
    }

    @Operation(summary = "添加IP黑名单")
    @PostMapping("/add")
    public Result<String> add(@RequestBody Map<String, String> params) {
        String ipValue = params.get("ip");
        String reason = params.get("reason");
        if (oConvertUtils.isEmpty(ipValue)) {
            return Result.error("IP不能为空");
        }
        if (!CsIpMatchUtil.isValidIpOrCidr(ipValue)) {
            return Result.error("IP格式无效，支持单个IP或CIDR段（如192.168.1.0/24）");
        }
        // 检查是否已存在
        LambdaQueryWrapper<CsIpBlacklist> existQw = new LambdaQueryWrapper<>();
        existQw.eq(CsIpBlacklist::getIp, ipValue.trim());
        if (ipBlacklistMapper.selectCount(existQw) > 0) {
            return Result.error("该IP/IP段已在黑名单中");
        }

        LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        String operator = loginUser != null ? loginUser.getUsername() : "system";

        CsIpBlacklist record = new CsIpBlacklist();
        record.setIp(ipValue.trim());
        record.setReason(reason);
        record.setOperator(operator);
        record.setBanDate(new Date());
        record.setCreateBy(operator);
        record.setCreateTime(new Date());
        ipBlacklistMapper.insert(record);

        // 同步到Redis（精确IP）
        if (!CsIpMatchUtil.isCidr(ipValue.trim())) {
            visitorTokenService.blacklistIp(ipValue.trim());
        }

        log.info("[CS-Security] IP黑名单添加: ip={}, reason={}, operator={}", ipValue, reason, operator);

        // 通知所有在线客服
        notifyBlacklistChanged("ip", "block", ipValue.trim(), null);

        return Result.OK("添加成功");
    }

    @Operation(summary = "解封IP")
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable String id) {
        CsIpBlacklist record = ipBlacklistMapper.selectById(id);
        if (record == null) {
            return Result.error("记录不存在");
        }
        ipBlacklistMapper.deleteById(id);

        // 从Redis移除（精确IP）
        if (!CsIpMatchUtil.isCidr(record.getIp())) {
            visitorTokenService.unblacklistIp(record.getIp());
        }

        log.info("[CS-Security] IP黑名单解封: ip={}", record.getIp());

        // 通知所有在线客服
        notifyBlacklistChanged("ip", "unblock", record.getIp(), null);

        return Result.OK("解封成功");
    }

    private void notifyBlacklistChanged(String blacklistType, String action, String target, String visitorName) {
        try {
            Map<String, Object> extra = new HashMap<>();
            extra.put("blacklistType", blacklistType);
            extra.put("action", action);
            extra.put("target", target);
            if (visitorName != null && !visitorName.isEmpty()) {
                extra.put("visitorName", visitorName);
            }
            CsWebSocketMessage msg = CsWebSocketMessage.builder()
                    .type(CsWebSocketMessage.TYPE_BLACKLIST_CHANGED)
                    .content(action.equals("block") ? "IP已拉黑" : "IP已解封")
                    .extra(extra)
                    .timestamp(new Date())
                    .build();
            sessionManager.sendToAllAgents(msg);
        } catch (Exception e) {
            log.warn("[CS-Security] 发送黑名单变更通知失败", e);
        }
    }
}
