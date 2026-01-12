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
import org.jeecg.modules.airag.cs.entity.CsAgent;
import org.jeecg.modules.airag.cs.service.ICsAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

/**
 * 客服管理Controller
 * 
 * @author jeecg
 * @date 2026-01-07
 */
@Slf4j
@Tag(name = "客服管理")
@RestController
@RequestMapping("/cs/agent")
public class CsAgentController extends JeecgController<CsAgent, ICsAgentService> {

    @Autowired
    private ICsAgentService csAgentService;

    /**
     * 分页列表查询
     */
    @Operation(summary = "分页列表查询")
    @GetMapping("/list")
    public Result<IPage<CsAgent>> queryPageList(CsAgent csAgent,
                                                 @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
                                                 @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
                                                 HttpServletRequest req) {
        QueryWrapper<CsAgent> queryWrapper = QueryGenerator.initQueryWrapper(csAgent, req.getParameterMap());
        Page<CsAgent> page = new Page<>(pageNo, pageSize);
        IPage<CsAgent> pageList = csAgentService.page(page, queryWrapper);
        return Result.OK(pageList);
    }

    /**
     * 添加
     */
    @AutoLog(value = "客服管理-添加")
    @Operation(summary = "添加")
    @PostMapping("/add")
    public Result<String> add(@RequestBody CsAgent csAgent) {
        csAgentService.save(csAgent);
        return Result.OK("添加成功！");
    }

    /**
     * 编辑
     */
    @AutoLog(value = "客服管理-编辑")
    @Operation(summary = "编辑")
    @PutMapping("/edit")
    public Result<String> edit(@RequestBody CsAgent csAgent) {
        csAgentService.updateById(csAgent);
        return Result.OK("编辑成功!");
    }

    /**
     * 删除
     */
    @AutoLog(value = "客服管理-删除")
    @Operation(summary = "删除")
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam(name = "id") String id) {
        csAgentService.removeById(id);
        return Result.OK("删除成功!");
    }

    /**
     * 批量删除
     */
    @AutoLog(value = "客服管理-批量删除")
    @Operation(summary = "批量删除")
    @DeleteMapping("/deleteBatch")
    public Result<String> deleteBatch(@RequestParam(name = "ids") String ids) {
        csAgentService.removeByIds(Arrays.asList(ids.split(",")));
        return Result.OK("批量删除成功!");
    }

    /**
     * 客服上线
     */
    @AutoLog(value = "客服管理-上线")
    @Operation(summary = "客服上线")
    @PostMapping("/online/{id}")
    public Result<String> goOnline(@PathVariable String id) {
        csAgentService.goOnline(id);
        return Result.OK("上线成功!");
    }

    /**
     * 客服下线
     */
    @AutoLog(value = "客服管理-下线")
    @Operation(summary = "客服下线")
    @PostMapping("/offline/{id}")
    public Result<String> goOffline(@PathVariable String id) {
        csAgentService.goOffline(id);
        return Result.OK("下线成功!");
    }

    /**
     * 设置忙碌
     */
    @AutoLog(value = "客服管理-设置忙碌")
    @Operation(summary = "设置忙碌")
    @PostMapping("/busy/{id}")
    public Result<String> setBusy(@PathVariable String id) {
        csAgentService.setBusy(id);
        return Result.OK("设置成功!");
    }

    /**
     * 获取可用客服列表
     */
    @Operation(summary = "获取可用客服列表")
    @GetMapping("/available")
    public Result<List<CsAgent>> getAvailableAgents() {
        List<CsAgent> agents = csAgentService.getAvailableAgents();
        return Result.OK(agents);
    }

    /**
     * 根据用户ID获取客服信息
     */
    @Operation(summary = "根据用户ID获取客服信息")
    @GetMapping("/byUserId/{userId}")
    public Result<CsAgent> getByUserId(@PathVariable String userId) {
        CsAgent agent = csAgentService.getByUserId(userId);
        return Result.OK(agent);
    }

    /**
     * 根据当前登录用户获取客服信息
     */
    @Operation(summary = "获取当前用户的客服信息")
    @GetMapping("/current")
    public Result<CsAgent> getCurrentAgent() {
        CsAgent agent = csAgentService.getCurrentAgent();
        return Result.OK(agent);
    }
}
