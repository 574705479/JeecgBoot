package org.jeecg.modules.airag.cs.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.modules.airag.cs.entity.CsVisitor;
import org.jeecg.modules.airag.cs.service.ICsVisitorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

/**
 * 访客信息Controller
 * 
 * @author jeecg
 * @date 2026-01-12
 */
@Slf4j
@Tag(name = "客服-访客管理")
@RestController
@RequestMapping("/airag/cs/visitor")
public class CsVisitorController extends JeecgController<CsVisitor, ICsVisitorService> {

    @Autowired
    private ICsVisitorService visitorService;

    /**
     * 分页查询访客列表
     */
    @Operation(summary = "分页查询访客列表")
    @GetMapping("/list")
    public Result<IPage<CsVisitor>> list(
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) Integer star,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        Page<CsVisitor> page = new Page<>(pageNo, pageSize);
        IPage<CsVisitor> result = visitorService.pageVisitors(page, appId, keyword, level, star);
        return Result.OK(result);
    }

    /**
     * 根据ID查询访客详情
     */
    @Operation(summary = "查询访客详情")
    @GetMapping("/detail")
    public Result<CsVisitor> detail(@RequestParam String id) {
        CsVisitor visitor = visitorService.getById(id);
        if (visitor == null) {
            return Result.error("访客不存在");
        }
        return Result.OK(visitor);
    }

    /**
     * 根据appId和userId查询访客
     * 如果访客不存在，返回空对象（不报错，因为新访客可能还没有创建记录）
     * 注：新版客服系统不再强制要求appId，可以只通过userId查询
     */
    @Operation(summary = "根据appId和userId查询访客")
    @GetMapping("/getByUser")
    public Result<CsVisitor> getByUser(
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String userId) {
        if (userId == null || userId.isEmpty()) {
            // userId是必须的
            return Result.OK(null);
        }
        
        CsVisitor visitor;
        if (appId != null && !appId.isEmpty()) {
            // 有appId时，按appId+userId精确查询
            visitor = visitorService.getByAppAndUser(appId, userId);
        } else {
            // 无appId时，只按userId查询（取最新的一条）
            visitor = visitorService.getByUserId(userId);
        }
        // 不管是否存在都返回OK，前端根据是否有数据判断
        return Result.OK(visitor);
    }

    /**
     * 更新访客信息（如果不存在则自动创建）
     * 注：新版客服系统不再强制要求appId，可以只通过userId创建/更新访客
     */
    @Operation(summary = "更新访客信息")
    @PostMapping("/update")
    public Result<CsVisitor> update(@RequestBody CsVisitor visitor) {
        // 如果有ID，直接更新
        if (visitor.getId() != null && !visitor.getId().isEmpty()) {
            visitor.setUpdateTime(new Date());
            boolean success = visitorService.updateById(visitor);
            if (success) {
                return Result.OK(visitorService.getById(visitor.getId()));
            }
            return Result.error("更新失败");
        }
        
        // 没有ID时，通过userId查找或创建（appId可选）
        if (visitor.getUserId() == null || visitor.getUserId().isEmpty()) {
            return Result.error("userId不能为空");
        }
        
        // 先查找是否已存在
        CsVisitor existing;
        if (visitor.getAppId() != null && !visitor.getAppId().isEmpty()) {
            // 有appId时，精确匹配
            existing = visitorService.getByAppAndUser(visitor.getAppId(), visitor.getUserId());
        } else {
            // 无appId时，只按userId查询
            existing = visitorService.getByUserId(visitor.getUserId());
        }
        
        if (existing != null) {
            // 存在则更新
            visitor.setId(existing.getId());
            visitor.setUpdateTime(new Date());
            visitorService.updateById(visitor);
            return Result.OK(visitorService.getById(existing.getId()));
        } else {
            // 不存在则创建
            visitor.setCreateTime(new Date());
            visitor.setUpdateTime(new Date());
            visitor.setVisitCount(1);
            visitor.setConversationCount(1);
            visitor.setFirstVisitTime(new Date());
            visitor.setLastVisitTime(new Date());
            visitorService.save(visitor);
            return Result.OK(visitor);
        }
    }

    /**
     * 切换星标
     */
    @Operation(summary = "切换星标")
    @PostMapping("/toggleStar")
    public Result<String> toggleStar(@RequestParam(required = false) String id,
                                     @RequestBody(required = false) Map<String, String> body) {
        String visitorId = id != null ? id : (body != null ? body.get("id") : null);
        if (visitorId == null || visitorId.isEmpty()) {
            return Result.error("id不能为空");
        }
        boolean success = visitorService.toggleStar(visitorId);
        return success ? Result.OK("操作成功") : Result.error("操作失败");
    }

    /**
     * 更新客户等级
     */
    @Operation(summary = "更新客户等级")
    @PostMapping("/updateLevel")
    public Result<String> updateLevel(@RequestBody Map<String, Object> params) {
        String id = (String) params.get("id");
        Integer level = params.get("level") instanceof Number ? ((Number) params.get("level")).intValue() : null;
        
        if (id == null || id.isEmpty()) {
            return Result.error("id不能为空");
        }
        if (level == null || level < 1 || level > 3) {
            return Result.error("等级值无效");
        }
        boolean success = visitorService.updateLevel(id, level);
        return success ? Result.OK("更新成功") : Result.error("更新失败");
    }

    /**
     * 更新标签
     */
    @Operation(summary = "更新标签")
    @PostMapping("/updateTags")
    public Result<String> updateTags(@RequestBody Map<String, String> params) {
        String id = params.get("id");
        String tags = params.get("tags");
        
        if (id == null || id.isEmpty()) {
            return Result.error("id不能为空");
        }
        boolean success = visitorService.updateTags(id, tags);
        return success ? Result.OK("更新成功") : Result.error("更新失败");
    }

    /**
     * 快速备注(只更新备注昵称)
     */
    @Operation(summary = "快速备注")
    @PostMapping("/quickRemark")
    public Result<String> quickRemark(@RequestBody Map<String, String> params) {
        String id = params.get("id");
        String nickname = params.get("nickname");
        
        if (id == null || id.isEmpty()) {
            return Result.error("id不能为空");
        }
        CsVisitor visitor = new CsVisitor();
        visitor.setId(id);
        visitor.setNickname(nickname);
        visitor.setUpdateTime(new Date());
        boolean success = visitorService.updateById(visitor);
        return success ? Result.OK("备注成功") : Result.error("备注失败");
    }

    /**
     * 删除访客
     */
    @Operation(summary = "删除访客")
    @DeleteMapping("/delete")
    public Result<String> delete(@RequestParam String id) {
        boolean success = visitorService.removeById(id);
        return success ? Result.OK("删除成功") : Result.error("删除失败");
    }

    /**
     * 批量删除访客
     */
    @Operation(summary = "批量删除访客")
    @DeleteMapping("/deleteBatch")
    public Result<String> deleteBatch(@RequestParam String ids) {
        String[] idArray = ids.split(",");
        for (String id : idArray) {
            visitorService.removeById(id.trim());
        }
        return Result.OK("删除成功");
    }
}
