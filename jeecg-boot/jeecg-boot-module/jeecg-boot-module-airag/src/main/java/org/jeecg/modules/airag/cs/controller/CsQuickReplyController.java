package org.jeecg.modules.airag.cs.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.aspect.annotation.AutoLog;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.system.query.QueryGenerator;
import org.jeecg.modules.airag.cs.entity.CsQuickReply;
import org.jeecg.modules.airag.cs.service.ICsQuickReplyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

/**
 * 快捷回复Controller
 * 
 * @author jeecg
 * @date 2026-01-07
 */
@Slf4j
@Tag(name = "快捷回复")
@RestController
@RequestMapping("/cs/quickReply")
public class CsQuickReplyController extends JeecgController<CsQuickReply, ICsQuickReplyService> {

    @Autowired
    private ICsQuickReplyService quickReplyService;

    /**
     * 分页列表查询
     */
    @Operation(summary = "分页列表查询")
    @GetMapping("/list")
    public Result<IPage<CsQuickReply>> queryPageList(CsQuickReply csQuickReply,
                                                      @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                      @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                      HttpServletRequest req) {
        QueryWrapper<CsQuickReply> queryWrapper = QueryGenerator.initQueryWrapper(csQuickReply, req.getParameterMap());
        Page<CsQuickReply> page = new Page<>(pageNo, pageSize);
        IPage<CsQuickReply> pageList = quickReplyService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 获取客服的快捷回复列表
     */
    @Operation(summary = "获取客服的快捷回复列表")
    @GetMapping("/agent/{agentId}")
    public Result<List<CsQuickReply>> getAgentQuickReplies(@PathVariable String agentId) {
        List<CsQuickReply> list = quickReplyService.getAgentQuickReplies(agentId);
        return Result.OK(list);
    }

    /**
     * 获取公共快捷回复列表
     */
    @Operation(summary = "获取公共快捷回复列表")
    @GetMapping("/public")
    public Result<List<CsQuickReply>> getPublicQuickReplies() {
        List<CsQuickReply> list = quickReplyService.getPublicQuickReplies();
        return Result.OK(list);
    }

    /**
     * 添加
     */
    @AutoLog(value = "快捷回复-添加")
    @Operation(summary = "添加")
    @PostMapping("/add")
    public Result<String> add(@RequestBody CsQuickReply csQuickReply) {
        quickReplyService.save(csQuickReply);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     */
    @AutoLog(value = "快捷回复-编辑")
    @Operation(summary = "编辑")
    @PutMapping("/edit")
    public Result<String> edit(@RequestBody CsQuickReply csQuickReply) {
        quickReplyService.updateById(csQuickReply);
        return Result.OK("编辑成功!");
    }

    /**
     * 删除
     */
    @AutoLog(value = "快捷回复-删除")
    @Operation(summary = "删除")
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam(name = "id") String id) {
        quickReplyService.removeById(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     */
    @AutoLog(value = "快捷回复-批量删除")
    @Operation(summary = "批量删除")
    @DeleteMapping("/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids") String ids) {
        quickReplyService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功!");
    }
}
