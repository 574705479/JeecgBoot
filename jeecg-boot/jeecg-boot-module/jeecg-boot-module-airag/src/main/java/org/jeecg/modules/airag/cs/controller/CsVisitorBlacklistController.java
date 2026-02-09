package org.jeecg.modules.airag.cs.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.cs.entity.CsVisitorBlacklist;
import org.jeecg.modules.airag.cs.mapper.CsVisitorBlacklistMapper;
import org.jeecg.modules.airag.cs.service.ICsVisitorTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 访客黑名单Controller
 */
@Slf4j
@Tag(name = "访客黑名单")
@RestController
@RequestMapping("/cs/security/visitor-blacklist")
public class CsVisitorBlacklistController {

    @Autowired
    private CsVisitorBlacklistMapper visitorBlacklistMapper;

    @Autowired
    private ICsVisitorTokenService visitorTokenService;

    @Operation(summary = "分页列表")
    @GetMapping("/list")
    public Result<IPage<CsVisitorBlacklist>> list(
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "visitorName", required = false) String visitorName,
            @RequestParam(name = "visitorId", required = false) String visitorId) {
        LambdaQueryWrapper<CsVisitorBlacklist> qw = new LambdaQueryWrapper<>();
        if (oConvertUtils.isNotEmpty(visitorName)) {
            qw.like(CsVisitorBlacklist::getVisitorName, visitorName);
        }
        if (oConvertUtils.isNotEmpty(visitorId)) {
            qw.like(CsVisitorBlacklist::getVisitorId, visitorId);
        }
        qw.orderByDesc(CsVisitorBlacklist::getBanDate);
        Page<CsVisitorBlacklist> page = new Page<>(pageNo, pageSize);
        return Result.OK(visitorBlacklistMapper.selectPage(page, qw));
    }

    @Operation(summary = "解封访客")
    @DeleteMapping("/delete/{id}")
    public Result<String> delete(@PathVariable String id) {
        CsVisitorBlacklist record = visitorBlacklistMapper.selectById(id);
        if (record == null) {
            return Result.error("记录不存在");
        }
        visitorBlacklistMapper.deleteById(id);

        // 从Redis移除
        if (oConvertUtils.isNotEmpty(record.getVisitorId())) {
            visitorTokenService.unblacklist(record.getVisitorId());
        }

        log.info("[CS-Security] 访客黑名单解封: visitorId={}", record.getVisitorId());
        return Result.OK("解封成功");
    }
}
