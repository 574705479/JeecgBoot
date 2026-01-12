package org.jeecg.modules.airag.cs.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.modules.airag.cs.entity.CsConversation;
import org.jeecg.modules.airag.cs.service.ICsConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客服会话管理 (重构版)
 * 
 * @author jeecg
 * @date 2026-01-12
 */
@Slf4j
@Tag(name = "客服会话管理")
@RestController
@RequestMapping("/cs/conversation")
public class CsConversationController extends JeecgController<CsConversation, ICsConversationService> {

    @Autowired
    private ICsConversationService conversationService;

    // ==================== 会话生命周期 ====================

    /**
     * 创建会话 (用户端调用)
     */
    @Operation(summary = "创建会话")
    @PostMapping("/create")
    public Result<CsConversation> create(@RequestBody Map<String, String> params) {
        String appId = params.get("appId");
        String userId = params.get("userId");
        String userName = params.get("userName");
        String source = params.get("source");
        
        CsConversation conversation = conversationService.createConversation(appId, userId, userName, source);
        return Result.OK(conversation);
    }

    /**
     * 获取或创建会话
     */
    @Operation(summary = "获取或创建会话")
    @PostMapping("/get-or-create")
    public Result<CsConversation> getOrCreate(@RequestBody Map<String, String> params) {
        String conversationId = params.get("conversationId");
        String appId = params.get("appId");
        String userId = params.get("userId");
        String userName = params.get("userName");
        
        CsConversation conversation = conversationService.getOrCreateConversation(
                conversationId, appId, userId, userName);
        return Result.OK(conversation);
    }

    /**
     * 获取会话详情
     */
    @Operation(summary = "获取会话详情")
    @GetMapping("/{id}")
    public Result<CsConversation> get(@PathVariable String id) {
        CsConversation conversation = conversationService.getConversation(id);
        if (conversation == null) {
            return Result.error("会话不存在");
        }
        return Result.OK(conversation);
    }

    /**
     * 客服接入会话
     */
    @Operation(summary = "客服接入会话")
    @PostMapping("/{id}/assign")
    public Result<Map<String, Object>> assign(@PathVariable String id, @RequestParam String agentId) {
        Map<String, Object> result = new HashMap<>();
        boolean success = conversationService.assignToAgent(id, agentId);
        result.put("success", success);
        result.put("message", success ? "接入成功" : "接入失败");
        
        if (success) {
            CsConversation conversation = conversationService.getConversation(id);
            result.put("conversation", conversation);
        }
        
        return Result.OK(result);
    }

    /**
     * 结束会话
     */
    @Operation(summary = "结束会话")
    @PostMapping("/{id}/close")
    public Result<String> close(@PathVariable String id) {
        conversationService.closeConversation(id);
        return Result.OK("会话已结束");
    }

    /**
     * 删除会话
     */
    @Operation(summary = "删除会话")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable String id) {
        // 先结束会话
        CsConversation conversation = conversationService.getById(id);
        if (conversation != null && conversation.getStatus() != CsConversation.STATUS_CLOSED) {
            conversationService.closeConversation(id);
        }
        
        // 删除记录
        boolean success = conversationService.removeById(id);
        return success ? Result.OK("删除成功") : Result.error("删除失败");
    }

    // ==================== 回复模式 ====================

    /**
     * 切换回复模式
     */
    @Operation(summary = "切换回复模式")
    @PutMapping("/{id}/mode")
    public Result<String> changeMode(@PathVariable String id, @RequestParam Integer mode) {
        boolean success = conversationService.changeReplyMode(id, mode);
        if (!success) {
            return Result.error("切换失败");
        }
        
        String modeName = mode == 0 ? "AI自动" : (mode == 1 ? "手动" : "AI辅助");
        return Result.OK("已切换为" + modeName + "模式");
    }

    /**
     * 获取当前回复模式
     */
    @Operation(summary = "获取当前回复模式")
    @GetMapping("/{id}/mode")
    public Result<Map<String, Object>> getMode(@PathVariable String id) {
        int mode = conversationService.getReplyMode(id);
        Map<String, Object> result = new HashMap<>();
        result.put("mode", mode);
        result.put("modeName", mode == 0 ? "AI自动" : (mode == 1 ? "手动" : "AI辅助"));
        return Result.OK(result);
    }

    // ==================== 会话移交 ====================

    /**
     * 移交会话
     */
    @Operation(summary = "移交会话")
    @PostMapping("/{id}/transfer")
    public Result<String> transfer(@PathVariable String id,
                                   @RequestParam String toAgentId,
                                   @RequestParam(required = false) String fromAgentId) {
        boolean success = conversationService.transferTo(id, toAgentId, fromAgentId);
        return success ? Result.OK("移交成功") : Result.error("移交失败");
    }

    // ==================== 查询 ====================

    /**
     * 分页查询会话列表
     */
    @Operation(summary = "分页查询会话列表")
    @GetMapping("/list")
    public Result<IPage<CsConversation>> list(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "all") String filter) {
        
        Page<CsConversation> page = new Page<>(pageNo, pageSize);
        IPage<CsConversation> result = conversationService.getConversationList(page, agentId, status, filter);
        return Result.OK(result);
    }

    /**
     * 获取我负责的会话
     */
    @Operation(summary = "获取我负责的会话")
    @GetMapping("/mine")
    public Result<List<CsConversation>> getMine(@RequestParam String agentId) {
        List<CsConversation> list = conversationService.getMyConversations(agentId);
        return Result.OK(list);
    }

    /**
     * 获取未分配的会话
     */
    @Operation(summary = "获取未分配的会话")
    @GetMapping("/unassigned")
    public Result<List<CsConversation>> getUnassigned(
            @RequestParam(defaultValue = "50") Integer limit) {
        List<CsConversation> list = conversationService.getUnassignedConversations(limit);
        return Result.OK(list);
    }

    /**
     * 获取会话状态
     */
    @Operation(summary = "获取会话状态")
    @GetMapping("/{id}/status")
    public Result<Map<String, Object>> getStatus(@PathVariable String id) {
        CsConversation conversation = conversationService.getById(id);
        Map<String, Object> result = new HashMap<>();
        
        if (conversation != null) {
            result.put("status", conversation.getStatus());
            result.put("replyMode", conversation.getReplyMode());
            result.put("ownerAgentId", conversation.getOwnerAgentId());
        } else {
            result.put("status", CsConversation.STATUS_UNASSIGNED);
            result.put("replyMode", CsConversation.REPLY_MODE_AI_AUTO);
        }
        
        return Result.OK(result);
    }

    // ==================== 未读管理 ====================

    /**
     * 清除未读消息
     */
    @Operation(summary = "清除未读消息")
    @PostMapping("/{id}/clear-unread")
    public Result<String> clearUnread(@PathVariable String id) {
        conversationService.clearUnread(id);
        return Result.OK("已清除未读");
    }

    // ==================== 评价 ====================

    /**
     * 评价会话
     */
    @Operation(summary = "评价会话")
    @PostMapping("/{id}/rate")
    public Result<String> rate(@PathVariable String id, @RequestBody Map<String, Object> params) {
        Integer satisfaction = (Integer) params.get("satisfaction");
        String comment = (String) params.get("comment");
        
        conversationService.rateConversation(id, satisfaction, comment);
        return Result.OK("评价成功");
    }
}
