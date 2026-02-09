package org.jeecg.modules.airag.cs.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.cs.entity.CsAgentLoginLog;
import org.jeecg.modules.airag.cs.mapper.CsAgentLoginLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

/**
 * 客服登录日志Controller（只读）
 */
@Slf4j
@Tag(name = "客服登录日志")
@RestController
@RequestMapping("/cs/security/login-log")
public class CsAgentLoginLogController {

    @Autowired
    private CsAgentLoginLogMapper loginLogMapper;

    @Operation(summary = "分页列表")
    @GetMapping("/list")
    public Result<IPage<CsAgentLoginLog>> list(
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "event", required = false) String event,
            @RequestParam(name = "startDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(name = "endDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        LambdaQueryWrapper<CsAgentLoginLog> qw = new LambdaQueryWrapper<>();
        if (oConvertUtils.isNotEmpty(username)) {
            qw.like(CsAgentLoginLog::getUsername, username);
        }
        if (oConvertUtils.isNotEmpty(event)) {
            qw.eq(CsAgentLoginLog::getEvent, event);
        }
        if (startDate != null) {
            qw.ge(CsAgentLoginLog::getLoginDate, startDate);
        }
        if (endDate != null) {
            // 包含当天，查到 endDate 23:59:59
            qw.lt(CsAgentLoginLog::getLoginDate, new Date(endDate.getTime() + 86400000L));
        }
        qw.orderByDesc(CsAgentLoginLog::getLoginDate);
        Page<CsAgentLoginLog> page = new Page<>(pageNo, pageSize);
        return Result.OK(loginLogMapper.selectPage(page, qw));
    }
}
