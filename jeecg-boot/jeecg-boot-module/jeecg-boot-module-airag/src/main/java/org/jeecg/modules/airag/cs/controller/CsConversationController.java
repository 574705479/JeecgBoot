package org.jeecg.modules.airag.cs.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.base.controller.JeecgController;
import org.jeecg.common.util.IpUtils;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.cs.entity.CsAgent;
import org.jeecg.modules.airag.cs.entity.CsCollaborator;
import org.jeecg.modules.airag.cs.entity.CsConversation;
import org.jeecg.modules.airag.cs.mapper.CsCollaboratorMapper;
import org.jeecg.modules.airag.cs.service.ICsAgentService;
import org.jeecg.modules.airag.cs.service.ICsConversationService;
import org.jeecg.modules.airag.cs.service.ICsMessageService;
import org.jeecg.modules.airag.cs.service.ICsVisitorTokenService;
import org.jeecg.modules.airag.cs.vo.CsAgentWorkloadVO;
import org.jeecg.modules.airag.cs.vo.CsVisitorTokenPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
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

    @Autowired
    private ICsAgentService csAgentService;

    @Autowired
    private ICsVisitorTokenService visitorTokenService;

    @Autowired
    private ICsMessageService messageService;

    @Autowired
    private CsCollaboratorMapper collaboratorMapper;

    // ==================== 会话生命周期 ====================

    /**
     * 创建会话 (用户端调用)
     */
    @Operation(summary = "创建会话")
    @org.jeecg.config.shiro.IgnoreAuth
    @PostMapping("/create")
    public Result<CsConversation> create(@RequestBody Map<String, String> params, HttpServletRequest request) {
        String appId = params.get("appId");
        String userId = params.get("userId");
        String userName = params.get("userName");
        String source = params.get("source");
        String deviceId = params.get("deviceId");

        boolean isAdmin = visitorTokenService.isAdminRequest(request);
        if (!isAdmin) {
            // 优先尝试Token方式（即使免Token模式也兼容带Token的请求）
            CsVisitorTokenPayload payload = resolveVisitorPayload(request);
            if (payload != null) {
                if (visitorTokenService.isBlacklisted(payload.getExternalUserId())) {
                    return Result.error("访客已被拉黑");
                }
                appId = payload.getAppId();
                userId = payload.getExternalUserId();
                if (oConvertUtils.isEmpty(userName)) {
                    userName = payload.getUserName();
                }
            } else if (!visitorTokenService.isTokenRequired()) {
                // 免Token模式：先校验接入密钥
                if (!visitorTokenService.validateAppKey(request)) {
                    return Result.error("接入密钥无效");
                }
                // 设备码作为访客标识
                String devId = visitorTokenService.extractDeviceId(request);
                if (oConvertUtils.isNotEmpty(devId)) {
                    userId = devId;
                }
                if (oConvertUtils.isEmpty(userId)) {
                    return Result.error("缺少设备码或用户标识");
                }
                if (visitorTokenService.isBlacklisted(userId)) {
                    return Result.error("访客已被拉黑");
                }
                // appId从全局配置获取
                if (oConvertUtils.isEmpty(appId)) {
                    appId = visitorTokenService.getGlobalVisitorAppId();
                }
                // userName可选，后端createConversation会处理默认值
            } else {
                return Result.error("访客凭证无效或已过期");
            }
        }

        // 获取用户IP和User-Agent
        String userIp = IpUtils.getIpAddr(request);
        String userAgent = request.getHeader("User-Agent");

        // 解析浏览器语言（优先前端传递的lang，其次Accept-Language头）
        String userLang = params.get("lang");
        if (userLang == null || userLang.isEmpty()) {
            String acceptLang = request.getHeader("Accept-Language");
            if (acceptLang != null && !acceptLang.isEmpty()) {
                // 取首选语言，如 "zh-CN,zh;q=0.9,en;q=0.8" -> "zh-CN"
                int commaIdx = acceptLang.indexOf(',');
                userLang = commaIdx > 0 ? acceptLang.substring(0, commaIdx).trim() : acceptLang.trim();
                // 去掉权重部分，如 "zh-CN;q=0.9" -> "zh-CN"
                int semicolonIdx = userLang.indexOf(';');
                if (semicolonIdx > 0) {
                    userLang = userLang.substring(0, semicolonIdx).trim();
                }
            }
        }

        String agentId = params.get("agentId");
        String landingPage = params.get("landingPage");
        String referrerPage = params.get("referrerPage");

        // 复用活跃会话：检查该用户是否已有未结束的会话
        CsConversation active = conversationService.getActiveConversation(userId, appId);
        if (active != null) {
            if (active.getStatus() == CsConversation.STATUS_UNASSIGNED) {
                conversationService.retryAssignAgent(active.getId(), agentId);
                active = conversationService.getById(active.getId());
            }
            // 如果 userName 是默认的"访客"，用新格式重新生成
            conversationService.refreshDefaultUserName(active, userIp, deviceId);
            conversationService.closeOtherActiveConversations(userId, appId, active.getId());
            return Result.OK(active);
        }

        CsConversation conversation = conversationService.createConversation(
                appId, userId, userName, source, userIp, userAgent, deviceId, userLang, agentId,
                landingPage, referrerPage);
        return Result.OK(conversation);
    }

    /**
     * 获取或创建会话
     */
    @Operation(summary = "获取或创建会话")
    @org.jeecg.config.shiro.IgnoreAuth
    @PostMapping("/get-or-create")
    public Result<CsConversation> getOrCreate(@RequestBody Map<String, String> params, HttpServletRequest request) {
        String conversationId = params.get("conversationId");
        String appId = params.get("appId");
        String userId = params.get("userId");
        String userName = params.get("userName");

        boolean isAdmin = visitorTokenService.isAdminRequest(request);
        if (!isAdmin) {
            CsVisitorTokenPayload payload = resolveVisitorPayload(request);
            if (payload != null) {
                if (visitorTokenService.isBlacklisted(payload.getExternalUserId())) {
                    return Result.error("访客已被拉黑");
                }
                appId = payload.getAppId();
                userId = payload.getExternalUserId();
                if (oConvertUtils.isEmpty(userName)) {
                    userName = payload.getUserName();
                }
            } else if (!visitorTokenService.isTokenRequired()) {
                if (!visitorTokenService.validateAppKey(request)) {
                    return Result.error("接入密钥无效");
                }
                String devId = visitorTokenService.extractDeviceId(request);
                if (oConvertUtils.isNotEmpty(devId)) {
                    userId = devId;
                }
                if (oConvertUtils.isEmpty(userId)) {
                    return Result.error("缺少设备码或用户标识");
                }
                if (visitorTokenService.isBlacklisted(userId)) {
                    return Result.error("访客已被拉黑");
                }
                if (oConvertUtils.isEmpty(appId)) {
                    appId = visitorTokenService.getGlobalVisitorAppId();
                }
            } else {
                return Result.error("访客凭证无效或已过期");
            }
        }
        
        CsConversation conversation = conversationService.getOrCreateConversation(
                conversationId, appId, userId, userName);
        return Result.OK(conversation);
    }

    /**
     * 获取会话详情
     */
    @Operation(summary = "获取会话详情")
    @org.jeecg.config.shiro.IgnoreAuth
    @GetMapping("/{id}")
    public Result<CsConversation> get(@PathVariable String id, HttpServletRequest request) {
        CsConversation conversation = conversationService.getConversation(id);
        if (conversation == null) {
            return Result.error("会话不存在");
        }
        if (!visitorTokenService.isAdminRequest(request)) {
            CsVisitorTokenPayload payload = resolveVisitorPayload(request);
            if (payload != null) {
                if (visitorTokenService.isBlacklisted(payload.getExternalUserId())) {
                    return Result.error("访客已被拉黑");
                }
                if (!payload.getExternalUserId().equals(conversation.getUserId())) {
                    return Result.error("无权访问该会话");
                }
            } else if (!visitorTokenService.isTokenRequired()) {
                // 免Token模式：先校验接入密钥
                if (!visitorTokenService.validateAppKey(request)) {
                    return Result.error("接入密钥无效");
                }
                // 通过设备码校验归属
                String devId = visitorTokenService.extractDeviceId(request);
                if (oConvertUtils.isEmpty(devId)) {
                    return Result.error("缺少设备码");
                }
                if (visitorTokenService.isBlacklisted(devId)) {
                    return Result.error("访客已被拉黑");
                }
                if (!devId.equals(conversation.getUserId())) {
                    return Result.error("无权访问该会话");
                }
            } else {
                return Result.error("访客凭证无效或已过期");
            }
        }
        return Result.OK(conversation);
    }

    private CsVisitorTokenPayload resolveVisitorPayload(HttpServletRequest request) {
        String sessionToken = visitorTokenService.extractSessionToken(request);
        if (oConvertUtils.isNotEmpty(sessionToken)) {
            CsVisitorTokenPayload payload = visitorTokenService.parseSessionToken(sessionToken);
            if (payload != null) {
                return payload;
            }
        }
        String shortToken = visitorTokenService.extractToken(request);
        if (oConvertUtils.isNotEmpty(shortToken)) {
            return visitorTokenService.parseToken(shortToken);
        }
        return null;
    }

    /**
     * 客服接入会话
     */
    @Operation(summary = "客服接入会话")
    @PostMapping("/{id}/assign")
    public Result<Map<String, Object>> assign(@PathVariable String id, 
                                              @RequestParam(required = false) String agentId,
                                              @RequestBody(required = false) Map<String, String> body) {
        // 优先从query参数获取，其次从body获取
        String agent = agentId;
        if (agent == null && body != null) {
            agent = body.get("agentId");
        }
        
        if (agent == null || agent.isEmpty()) {
            return Result.error("agentId不能为空");
        }
        
        Map<String, Object> result = new HashMap<>();
        boolean success = conversationService.assignToAgent(id, agent);
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
     * 删除会话（逻辑删除）
     * 使用@TableLogic注解，removeById会自动执行逻辑删除（UPDATE SET deleted=1）
     * 不会物理删除表数据
     */
    @Operation(summary = "删除会话")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable String id) {
        // 先结束会话
        CsConversation conversation = conversationService.getById(id);
        if (conversation != null && conversation.getStatus() != CsConversation.STATUS_CLOSED) {
            conversationService.closeConversation(id);
        }
        
        // ★ 逻辑删除：设置 deleted=1，不会物理删除数据
        boolean success = conversationService.removeById(id);
        return success ? Result.OK("删除成功") : Result.error("删除失败");
    }

    // ==================== 回复模式 ====================

    /**
     * 切换回复模式
     */
    @Operation(summary = "切换回复模式")
    @PutMapping("/{id}/mode")
    public Result<String> changeMode(@PathVariable String id, 
                                     @RequestParam(required = false) Integer mode,
                                     @RequestBody(required = false) Map<String, Object> body) {
        // 优先从query参数获取，其次从body获取
        Integer replyMode = mode;
        if (replyMode == null && body != null) {
            Object modeObj = body.get("mode");
            if (modeObj instanceof Integer) {
                replyMode = (Integer) modeObj;
            } else if (modeObj instanceof Number) {
                replyMode = ((Number) modeObj).intValue();
            }
        }
        
        if (replyMode == null) {
            return Result.error("mode不能为空");
        }
        
        boolean success = conversationService.changeReplyMode(id, replyMode);
        if (!success) {
            return Result.error("切换失败");
        }
        
        String modeName = replyMode == 0 ? "AI自动" : (replyMode == 1 ? "手动" : "AI辅助");
        return Result.OK("已切换为" + modeName + "模式");
    }

    /**
     * 更新会话的AI应用
     */
    @Operation(summary = "更新会话的AI应用")
    @PutMapping("/{id}/app")
    public Result<String> updateApp(@PathVariable String id, @RequestBody Map<String, String> params) {
        String appId = params.get("appId");
        if (appId == null) {
            return Result.error("appId不能为空");
        }
        
        CsConversation conversation = conversationService.getById(id);
        if (conversation == null) {
            return Result.error("会话不存在");
        }
        
        conversation.setAppId(appId);
        conversationService.updateById(conversation);
        
        return Result.OK("AI应用已更新");
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
     * 
     * @param supervisorMode  管理者监控模式，为true时返回所有进行中的会话（仅管理者可用）
     * @param includeDeleted  是否包含已删除的记录（用于会话记录查询）
     * @param filterAgentId   按指定客服筛选（用于会话记录查询）
     */
    @Operation(summary = "分页查询会话列表")
    @GetMapping("/list")
    public Result<IPage<CsConversation>> list(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(defaultValue = "false") Boolean supervisorMode,
            @RequestParam(required = false) Boolean includeDeleted,
            @RequestParam(required = false) String filterAgentId,
            @RequestParam(required = false) String id,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) Integer endType,
            @RequestParam(required = false) Integer satisfaction,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String landingPage,
            @RequestParam(required = false) String referrerPage,
            @RequestParam(required = false) String createTimeBegin,
            @RequestParam(required = false) String createTimeEnd,
            @RequestParam(required = false) String endTimeBegin,
            @RequestParam(required = false) String endTimeEnd) {
        
        Page<CsConversation> page = new Page<>(pageNo, pageSize);
        
        // 同事会话模式：返回所有进行中的会话（所有客服均可查看）
        if (Boolean.TRUE.equals(supervisorMode) || "monitor".equals(filter)) {
            CsAgent currentAgent = csAgentService.getCurrentAgent();
            if (currentAgent == null && oConvertUtils.isNotEmpty(agentId)) {
                currentAgent = csAgentService.getById(agentId);
            }
            if (currentAgent != null) {
                IPage<CsConversation> result = conversationService.getAllActiveConversations(page);
                return Result.OK(result);
            }
        }
        
        // 会话记录模式：使用高级查询（支持包含已删除记录和按客服筛选）
        if ("history".equals(filter) || Boolean.TRUE.equals(includeDeleted) || oConvertUtils.isNotEmpty(filterAgentId)) {
            IPage<CsConversation> result = conversationService.getConversationListAdvanced(
                    page, agentId, status, filter, includeDeleted, filterAgentId,
                    id, userId, endType, satisfaction, source, landingPage, referrerPage,
                    createTimeBegin, createTimeEnd, endTimeBegin, endTimeEnd);
            return Result.OK(result);
        }
        
        IPage<CsConversation> result = conversationService.getConversationList(page, agentId, status, filter);
        return Result.OK(result);
    }

    /**
     * 获取会话统计数据
     */
    @Operation(summary = "获取会话统计数据")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(@RequestParam(required = false) String agentId) {
        Map<String, Object> stats = conversationService.getConversationStats(agentId);
        return Result.OK(stats);
    }

    /**
     * 获取客服工作量统计
     */
    @Operation(summary = "获取客服工作量统计")
    @GetMapping("/workload")
    public Result<List<CsAgentWorkloadVO>> getWorkload(@RequestParam(defaultValue = "7") Integer days,
                                                       @RequestParam(defaultValue = "10") Integer limit) {
        List<CsAgentWorkloadVO> workload = conversationService.getAgentWorkload(days, limit);
        return Result.OK(workload);
    }

    /**
     * 获取我负责的会话
     */
    @Operation(summary = "获取我负责的会话")
    @GetMapping("/mine")
    public Result<Map<String, Object>> getMine(@RequestParam String agentId) {
        List<CsConversation> list = conversationService.getMyConversations(agentId);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("count", list.size());
        return Result.OK(result);
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
     * 评价会话（用户端调用，免登录）
     */
    @Operation(summary = "评价会话")
    @org.jeecg.config.shiro.IgnoreAuth
    @PostMapping("/{id}/rate")
    public Result<String> rate(@PathVariable String id, @RequestBody Map<String, Object> params, HttpServletRequest request) {
        // 校验会话存在
        CsConversation conversation = conversationService.getById(id);
        if (conversation == null) {
            return Result.error("会话不存在");
        }

        // 访客身份校验
        boolean isAdmin = visitorTokenService.isAdminRequest(request);
        if (!isAdmin) {
            CsVisitorTokenPayload payload = resolveVisitorPayload(request);
            if (payload != null) {
                if (!payload.getExternalUserId().equals(conversation.getUserId())) {
                    return Result.error("无权评价此会话");
                }
            } else if (!visitorTokenService.isTokenRequired()) {
                String devId = visitorTokenService.extractDeviceId(request);
                if (oConvertUtils.isEmpty(devId) || !devId.equals(conversation.getUserId())) {
                    return Result.error("无权评价此会话");
                }
            } else {
                return Result.error("访客凭证无效或已过期");
            }
        }

        // 类型安全转换
        Object satObj = params.get("satisfaction");
        if (satObj == null) {
            return Result.error("satisfaction不能为空");
        }
        int satisfaction;
        if (satObj instanceof Number) {
            satisfaction = ((Number) satObj).intValue();
        } else {
            return Result.error("satisfaction参数类型错误");
        }
        if (satisfaction < 1 || satisfaction > 5) {
            return Result.error("satisfaction取值范围为1-5");
        }

        // 防止重复评价
        if (conversation.getSatisfaction() != null && conversation.getSatisfaction() > 0) {
            return Result.error("该会话已评价，不能重复评价");
        }

        String comment = (String) params.get("comment");
        conversationService.rateConversation(id, satisfaction, comment);
        return Result.OK("评价成功");
    }

    /**
     * 推送满意度评价给用户
     */
    @Operation(summary = "推送满意度评价给用户")
    @PostMapping("/{id}/push-satisfaction")
    public Result<String> pushSatisfaction(@PathVariable String id) {
        CsConversation conversation = conversationService.getById(id);
        if (conversation == null) {
            return Result.error("会话不存在");
        }
        if (conversation.getStatus() == CsConversation.STATUS_CLOSED) {
            return Result.error("会话已结束，无法推送评价");
        }
        if (conversation.getSatisfaction() != null && conversation.getSatisfaction() > 0) {
            return Result.error("该会话已有评价");
        }

        // 通过 WebSocket 推送满意度评价给用户
        Map<String, Object> extra = new HashMap<>();
        extra.put("conversationId", id);
        conversationService.notifyUser(id, "satisfaction_survey", "请对本次服务进行评价", extra);

        log.info("[CS-Conversation] 推送满意度评价: conversationId={}", id);
        return Result.OK("已推送满意度评价");
    }

    /**
     * 获取访客历史会话ID列表（已结束的，按时间倒序）
     * 用于访客端滚动加载历史消息
     */
    @Operation(summary = "获取访客历史会话列表")
    @org.jeecg.config.shiro.IgnoreAuth
    @GetMapping("/visitor-history")
    public Result<List<String>> visitorHistory(
            @RequestParam String userId,
            @RequestParam(required = false) String excludeId,
            HttpServletRequest request) {
        if (oConvertUtils.isEmpty(userId)) {
            return Result.error("userId不能为空");
        }

        // 访客身份校验：确保只能查询自己的历史
        boolean isAdmin = visitorTokenService.isAdminRequest(request);
        if (!isAdmin) {
            CsVisitorTokenPayload payload = resolveVisitorPayload(request);
            if (payload != null) {
                if (!payload.getExternalUserId().equals(userId)) {
                    return Result.error("无权查看他人会话历史");
                }
            } else if (!visitorTokenService.isTokenRequired()) {
                String devId = visitorTokenService.extractDeviceId(request);
                if (oConvertUtils.isEmpty(devId) || !devId.equals(userId)) {
                    return Result.error("无权查看他人会话历史");
                }
            } else {
                return Result.error("访客凭证无效或已过期");
            }
        }

        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CsConversation> qw =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        qw.eq(CsConversation::getUserId, userId)
          .eq(CsConversation::getStatus, CsConversation.STATUS_CLOSED)
          .orderByDesc(CsConversation::getCreateTime);
        if (oConvertUtils.isNotEmpty(excludeId)) {
            qw.ne(CsConversation::getId, excludeId);
        }
        qw.select(CsConversation::getId);
        List<CsConversation> list = conversationService.list(qw);
        List<String> ids = list.stream().map(CsConversation::getId).collect(java.util.stream.Collectors.toList());
        return Result.OK(ids);
    }

    // ==================== 人工客服转接 ====================

    @Operation(summary = "访客请求转人工客服")
    @org.jeecg.config.shiro.IgnoreAuth
    @PostMapping("/{id}/request-human-agent")
    public Result<?> requestHumanAgent(@PathVariable String id,
                                       @RequestBody Map<String, Object> params,
                                       HttpServletRequest request) {
        // 访客身份校验
        boolean isAdmin = visitorTokenService.isAdminRequest(request);
        if (!isAdmin) {
            CsVisitorTokenPayload payload = resolveVisitorPayload(request);
            if (payload != null) {
                if (visitorTokenService.isBlacklisted(payload.getExternalUserId())) {
                    return Result.error("访客已被拉黑");
                }
            } else if (!visitorTokenService.isTokenRequired()) {
                if (!visitorTokenService.validateAppKey(request)) {
                    return Result.error("接入密钥无效");
                }
                String devId = visitorTokenService.extractDeviceId(request);
                if (oConvertUtils.isEmpty(devId)) {
                    return Result.error("缺少设备码");
                }
                if (visitorTokenService.isBlacklisted(devId)) {
                    return Result.error("访客已被拉黑");
                }
            } else {
                return Result.error("访客凭证无效或已过期");
            }
        }

        CsConversation conversation = conversationService.getById(id);
        if (conversation == null) {
            return Result.error("会话不存在");
        }
        if (conversation.getHumanAgentMode() == null || conversation.getHumanAgentMode() != 1) {
            return Result.error("该会话不支持人工转接");
        }
        if (conversation.getStatus() == CsConversation.STATUS_ASSIGNED) {
            return Result.error("已有客服接入");
        }

        // 保存自定义字段
        String customFieldsJson = null;
        Object customFields = params.get("customFields");
        if (customFields != null) {
            customFieldsJson = com.alibaba.fastjson.JSON.toJSONString(customFields);
        }
        conversation.setCustomFields(customFieldsJson);

        // 分配客服
        CsAgent assignedAgent = csAgentService.assignAgent();
        if (assignedAgent == null) {
            // 仅保存 customFields，不改变会话状态
            conversationService.updateById(conversation);
            return Result.error("暂无客服在线，请留言");
        }

        // 分配成功
        conversation.setOwnerAgentId(assignedAgent.getId());
        conversation.setStatus(CsConversation.STATUS_ASSIGNED);
        conversation.setReplyMode(CsConversation.REPLY_MODE_MANUAL);
        conversation.setAssignTime(new Date());
        conversationService.updateById(conversation);

        // 创建协作者记录
        CsCollaborator collaborator = new CsCollaborator();
        collaborator.setConversationId(conversation.getId());
        collaborator.setAgentId(assignedAgent.getId());
        collaborator.setRole(CsCollaborator.ROLE_OWNER);
        collaborator.setJoinTime(new Date());
        collaboratorMapper.insert(collaborator);

        // 通知访客客服已接入
        Map<String, Object> extra = new HashMap<>();
        extra.put("replyMode", CsConversation.REPLY_MODE_MANUAL);
        extra.put("agentName", assignedAgent.getNickname());
        extra.put("agentId", assignedAgent.getId());
        extra.put("agentAvatar", assignedAgent.getAvatar());
        conversationService.notifyUser(conversation.getId(), "agent_connected",
                "客服 " + assignedAgent.getNickname() + " 为您服务", extra);

        // 广播给客服工作台（含完整分配数据，前端需要agentId等更新会话列表）
        Map<String, Object> assignData = new HashMap<>();
        assignData.put("conversationId", conversation.getId());
        assignData.put("agentId", assignedAgent.getId());
        assignData.put("agentName", assignedAgent.getNickname());
        assignData.put("agentAvatar", assignedAgent.getAvatar());
        assignData.put("assignTime", new Date());
        if (customFieldsJson != null) {
            assignData.put("customFields", customFieldsJson);
        }
        conversationService.notifyRelatedAgents(conversation.getId(),
                "conversation_assigned", "访客已转人工", assignData);

        log.info("[CS-Conversation] 访客转人工成功: conversationId={}, agentId={}", id, assignedAgent.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("agentName", assignedAgent.getNickname());
        result.put("agentId", assignedAgent.getId());
        return Result.OK(result);
    }
}
