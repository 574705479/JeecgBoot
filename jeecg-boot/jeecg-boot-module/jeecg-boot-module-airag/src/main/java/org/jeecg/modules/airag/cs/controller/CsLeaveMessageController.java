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
import org.jeecg.modules.airag.cs.entity.CsAgent;
import org.jeecg.modules.airag.cs.entity.CsLeaveMessage;
import org.jeecg.modules.airag.cs.service.ICsAgentService;
import org.jeecg.modules.airag.cs.service.ICsLeaveMessageService;
import org.jeecg.modules.airag.cs.service.ICsVisitorTokenService;
import org.jeecg.modules.airag.cs.util.CsCryptoUtil;
import org.jeecg.modules.airag.cs.vo.CsVisitorTokenPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 客服留言Controller
 *
 * @author jeecg
 * @date 2026-02-06
 */
@Slf4j
@Tag(name = "客服留言")
@RestController
@RequestMapping("/cs/leaveMessage")
public class CsLeaveMessageController {

    @Autowired
    private ICsLeaveMessageService leaveMessageService;

    @Autowired
    private ICsAgentService agentService;

    @Autowired
    private ICsVisitorTokenService visitorTokenService;

    @Autowired
    private CsCryptoUtil csCryptoUtil;

    /**
     * 访客提交留言（无需登录）
     */
    @Operation(summary = "访客提交留言")
    @org.jeecg.config.shiro.IgnoreAuth
    @PostMapping("/submit")
    public Result<CsLeaveMessage> submit(@RequestBody CsLeaveMessage message) {
        if (message.getUserId() == null || message.getUserId().isEmpty()) {
            return Result.error("用户ID不能为空");
        }
        message.setContent(csCryptoUtil.decryptTransport(message.getContent()));
        if ((message.getContent() == null || message.getContent().isEmpty())
                && (message.getName() == null || message.getName().isEmpty())) {
            return Result.error("留言内容或姓名不能为空");
        }
        CsLeaveMessage saved = leaveMessageService.submitMessage(message);
        encryptLeaveMessageForTransport(saved);
        return Result.OK(saved);
    }

    /**
     * 分页查询留言列表（需要登录）
     */
    @Operation(summary = "分页查询留言列表")
    @GetMapping("/list")
    public Result<IPage<CsLeaveMessage>> list(
            @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(name = "status", required = false) Integer status,
            @RequestParam(name = "userId", required = false) String userId) {
        LambdaQueryWrapper<CsLeaveMessage> queryWrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            queryWrapper.eq(CsLeaveMessage::getStatus, status);
        }
        if (oConvertUtils.isNotEmpty(userId)) {
            queryWrapper.like(CsLeaveMessage::getUserId, userId);
        }
        queryWrapper.orderByDesc(CsLeaveMessage::getCreateTime);
        Page<CsLeaveMessage> page = new Page<>(pageNo, pageSize);
        IPage<CsLeaveMessage> pageList = leaveMessageService.page(page, queryWrapper);
        if (pageList.getRecords() != null) {
            for (CsLeaveMessage msg : pageList.getRecords()) {
                encryptLeaveMessageForTransport(msg);
            }
        }
        return Result.OK(pageList);
    }

    /**
     * 获取留言详情
     */
    @Operation(summary = "获取留言详情")
    @GetMapping("/{id}")
    public Result<CsLeaveMessage> getById(@PathVariable String id) {
        CsLeaveMessage message = leaveMessageService.getById(id);
        if (message == null) {
            return Result.error("留言不存在");
        }
        encryptLeaveMessageForTransport(message);
        return Result.OK(message);
    }

    /**
     * 回复留言
     */
    @Operation(summary = "回复留言")
    @PutMapping("/{id}/reply")
    public Result<String> reply(@PathVariable String id, @RequestBody Map<String, String> params) {
        String reply = csCryptoUtil.decryptTransport(params.get("reply"));
        if (reply == null || reply.isEmpty()) {
            return Result.error("回复内容不能为空");
        }
        // 获取当前客服ID
        String agentId = null;
        try {
            LoginUser loginUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            if (loginUser != null) {
                CsAgent agent = agentService.getByUserId(loginUser.getId());
                if (agent == null) {
                    agent = agentService.getByUserId(loginUser.getUsername());
                }
                if (agent != null) {
                    agentId = agent.getId();
                }
            }
        } catch (Exception e) {
            log.warn("[CS-LeaveMessage] 获取当前客服信息失败", e);
        }

        boolean success = leaveMessageService.replyMessage(id, reply, agentId);
        if (success) {
            return Result.OK("回复成功");
        }
        return Result.error("回复失败，留言不存在");
    }

    /**
     * 撤回留言回复
     */
    @Operation(summary = "撤回留言回复")
    @PutMapping("/{id}/recallReply")
    public Result<String> recallReply(@PathVariable String id) {
        boolean success = leaveMessageService.recallReply(id);
        if (success) {
            return Result.OK("撤回成功");
        }
        return Result.error("撤回失败");
    }

    /**
     * 查询用户未读的留言回复（无需登录，访客端使用）
     */
    @Operation(summary = "查询用户未读的留言回复")
    @org.jeecg.config.shiro.IgnoreAuth
    @GetMapping("/byUser")
    public Result<List<CsLeaveMessage>> getByUser(@RequestParam String userId, HttpServletRequest request) {
        if (userId == null || userId.isEmpty()) {
            return Result.error("用户ID不能为空");
        }
        String verifiedUserId = resolveVisitorUserId(request);
        if (verifiedUserId == null || !verifiedUserId.equals(userId)) {
            return Result.error("无权查看他人留言");
        }
        List<CsLeaveMessage> replies = leaveMessageService.getUnreadReplies(userId);
        for (CsLeaveMessage msg : replies) {
            msg.setPhone(null);
            msg.setEmail(null);
            msg.setQq(null);
            msg.setWechat(null);
            encryptLeaveMessageForTransport(msg);
        }
        return Result.OK(replies);
    }

    /**
     * 标记留言回复为已读（无需登录，访客端使用）
     */
    @Operation(summary = "标记留言回复为已读")
    @org.jeecg.config.shiro.IgnoreAuth
    @PutMapping("/markRead")
    public Result<String> markRead(@RequestBody Map<String, String> params, HttpServletRequest request) {
        String userId = params.get("userId");
        if (userId == null || userId.isEmpty()) {
            return Result.error("用户ID不能为空");
        }
        String verifiedUserId = resolveVisitorUserId(request);
        if (verifiedUserId == null || !verifiedUserId.equals(userId)) {
            return Result.error("无权操作他人留言");
        }
        leaveMessageService.markAsRead(userId);
        return Result.OK("标记成功");
    }

    private void encryptLeaveMessageForTransport(CsLeaveMessage msg) {
        if (msg == null) return;
        msg.setContent(csCryptoUtil.encryptTransport(msg.getContent()));
        msg.setReply(csCryptoUtil.encryptTransport(msg.getReply()));
    }

    private String resolveVisitorUserId(HttpServletRequest request) {
        String sessionToken = visitorTokenService.extractSessionToken(request);
        if (oConvertUtils.isNotEmpty(sessionToken)) {
            CsVisitorTokenPayload payload = visitorTokenService.parseSessionToken(sessionToken);
            if (payload != null) {
                return payload.getExternalUserId();
            }
        }
        String shortToken = visitorTokenService.extractToken(request);
        if (oConvertUtils.isNotEmpty(shortToken)) {
            CsVisitorTokenPayload payload = visitorTokenService.parseToken(shortToken);
            if (payload != null) {
                return payload.getExternalUserId();
            }
        }
        if (!visitorTokenService.isTokenRequired()) {
            return visitorTokenService.extractDeviceId(request);
        }
        return null;
    }
}
