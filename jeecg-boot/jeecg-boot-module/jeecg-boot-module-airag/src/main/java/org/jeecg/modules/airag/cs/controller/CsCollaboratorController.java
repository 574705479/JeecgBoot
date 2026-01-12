package org.jeecg.modules.airag.cs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.airag.cs.entity.CsCollaborator;
import org.jeecg.modules.airag.cs.service.ICsCollaboratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话协作管理
 * 
 * @author jeecg
 * @date 2026-01-12
 */
@Slf4j
@Tag(name = "会话协作管理")
@RestController
@RequestMapping("/cs/collaborator")
public class CsCollaboratorController {

    @Autowired
    private ICsCollaboratorService collaboratorService;

    /**
     * 邀请客服协作
     */
    @Operation(summary = "邀请客服协作")
    @PostMapping("/invite")
    public Result<Map<String, Object>> invite(@RequestBody Map<String, String> params) {
        String conversationId = params.get("conversationId");
        String agentId = params.get("agentId");
        String inviteBy = params.get("inviteBy");
        
        Map<String, Object> result = new HashMap<>();
        boolean success = collaboratorService.inviteCollaborator(conversationId, agentId, inviteBy);
        result.put("success", success);
        result.put("message", success ? "邀请成功" : "邀请失败");
        
        return Result.OK(result);
    }

    /**
     * 主动加入会话
     */
    @Operation(summary = "主动加入会话")
    @PostMapping("/join/{conversationId}")
    public Result<Map<String, Object>> join(
            @PathVariable String conversationId,
            @RequestParam String agentId,
            @RequestParam(defaultValue = "false") Boolean asOwner) {
        
        Map<String, Object> result = new HashMap<>();
        boolean success = collaboratorService.joinConversation(conversationId, agentId, asOwner);
        result.put("success", success);
        result.put("message", success ? "加入成功" : "加入失败");
        
        return Result.OK(result);
    }

    /**
     * 退出协作
     */
    @Operation(summary = "退出协作")
    @PostMapping("/leave/{conversationId}")
    public Result<Map<String, Object>> leave(
            @PathVariable String conversationId,
            @RequestParam String agentId) {
        
        Map<String, Object> result = new HashMap<>();
        boolean success = collaboratorService.leaveConversation(conversationId, agentId);
        result.put("success", success);
        result.put("message", success ? "退出成功" : "退出失败，主负责人需先移交会话");
        
        return Result.OK(result);
    }

    /**
     * 获取会话协作者列表
     */
    @Operation(summary = "获取会话协作者列表")
    @GetMapping("/{conversationId}")
    public Result<List<CsCollaborator>> getCollaborators(@PathVariable String conversationId) {
        List<CsCollaborator> list = collaboratorService.getCollaborators(conversationId);
        return Result.OK(list);
    }

    /**
     * 检查客服是否在会话中
     */
    @Operation(summary = "检查客服是否在会话中")
    @GetMapping("/check")
    public Result<Map<String, Object>> check(
            @RequestParam String conversationId,
            @RequestParam String agentId) {
        
        Map<String, Object> result = new HashMap<>();
        boolean inConversation = collaboratorService.isInConversation(conversationId, agentId);
        Integer role = collaboratorService.getRole(conversationId, agentId);
        
        result.put("inConversation", inConversation);
        result.put("role", role);
        result.put("roleName", role == null ? null : 
                (role == 0 ? "主负责" : (role == 1 ? "协作者" : "临时介入")));
        
        return Result.OK(result);
    }
}
