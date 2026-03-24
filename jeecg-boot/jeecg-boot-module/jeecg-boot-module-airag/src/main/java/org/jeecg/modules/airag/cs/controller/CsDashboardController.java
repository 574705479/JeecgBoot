package org.jeecg.modules.airag.cs.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.airag.cs.entity.CsAgent;
import org.jeecg.modules.airag.cs.entity.CsConversation;
import org.jeecg.modules.airag.cs.entity.CsLeaveMessage;
import org.jeecg.modules.airag.cs.entity.CsVisitor;
import org.jeecg.modules.airag.cs.service.ICsAgentService;
import org.jeecg.modules.airag.cs.service.ICsConversationService;
import org.jeecg.modules.airag.cs.service.ICsLeaveMessageService;
import org.jeecg.modules.airag.cs.service.ICsVisitorService;
import org.jeecg.modules.airag.cs.websocket.CsWebSocketSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * 首页仪表盘
 *
 * @author jeecg
 * @date 2026-02-11
 */
@Slf4j
@Tag(name = "首页仪表盘")
@RestController
@RequestMapping("/cs/dashboard")
public class CsDashboardController {

    @Autowired
    private ICsConversationService conversationService;

    @Autowired
    private ICsAgentService agentService;

    @Autowired
    private ICsVisitorService visitorService;

    @Autowired
    private ICsLeaveMessageService leaveMessageService;

    @Autowired
    private CsWebSocketSessionManager sessionManager;

    @Autowired
    private org.jeecg.modules.airag.cs.mapper.CsSubAgentMapper csSubAgentMapper;

    /**
     * 首页综合统计数据
     */
    @Operation(summary = "首页综合统计数据")
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        Date todayStart = getTodayStart();
        Date todayEnd = getTodayEnd();

        // 1. 实时对话量（进行中的对话）
        long activeConversations = conversationService.count(
                new LambdaQueryWrapper<CsConversation>()
                        .ne(CsConversation::getStatus, CsConversation.STATUS_CLOSED));
        stats.put("activeConversations", activeConversations);

        // 2. 平均响应时长（秒）：今日客服首次回复的平均时长
        List<CsConversation> todayResponded = conversationService.list(
                new LambdaQueryWrapper<CsConversation>()
                        .isNotNull(CsConversation::getFirstResponseSeconds)
                        .gt(CsConversation::getFirstResponseSeconds, 0)
                        .ge(CsConversation::getCreateTime, todayStart)
                        .lt(CsConversation::getCreateTime, todayEnd));
        double avgResponseTime = 0;
        if (!todayResponded.isEmpty()) {
            long total = todayResponded.stream()
                    .mapToLong(c -> c.getFirstResponseSeconds())
                    .sum();
            avgResponseTime = (double) total / todayResponded.size();
        }
        stats.put("avgResponseTime", Math.round(avgResponseTime * 10) / 10.0);

        // 3. 今日有效对话量（双方都有消息交互）
        long todayEffective = conversationService.count(
                new LambdaQueryWrapper<CsConversation>()
                        .ge(CsConversation::getCreateTime, todayStart)
                        .lt(CsConversation::getCreateTime, todayEnd)
                        .gt(CsConversation::getAgentMessageCount, 0)
                        .gt(CsConversation::getVisitorMessageCount, 0));
        stats.put("todayEffectiveConversations", todayEffective);

        // 4. 今日访客量
        long todayVisitors = visitorService.count(
                new LambdaQueryWrapper<CsVisitor>()
                        .ge(CsVisitor::getCreateTime, todayStart)
                        .lt(CsVisitor::getCreateTime, todayEnd));
        stats.put("todayVisitors", todayVisitors);

        // 5. 今日对话量
        long todayConversations = conversationService.count(
                new LambdaQueryWrapper<CsConversation>()
                        .ge(CsConversation::getCreateTime, todayStart)
                        .lt(CsConversation::getCreateTime, todayEnd));
        stats.put("todayConversations", todayConversations);

        // 6. 今日留言量
        long todayLeaveMessages = leaveMessageService.count(
                new LambdaQueryWrapper<CsLeaveMessage>()
                        .ge(CsLeaveMessage::getCreateTime, todayStart)
                        .lt(CsLeaveMessage::getCreateTime, todayEnd));
        stats.put("todayLeaveMessages", todayLeaveMessages);

        // 7. 在线客服数 / 总客服数（含子客服）
        int onlineAgents = sessionManager.getOnlineAgentCount();
        long totalAgents = agentService.count();
        stats.put("onlineAgents", onlineAgents);
        stats.put("totalAgents", totalAgents);

        return Result.OK(stats);
    }

    /**
     * 坐席实时状态列表
     */
    @Operation(summary = "坐席实时状态列表")
    @GetMapping("/agent-status")
    public Result<List<Map<String, Object>>> getAgentStatus() {
        List<CsAgent> agents = agentService.list();

        Date todayStart = getTodayStart();
        Date todayEnd = getTodayEnd();

        List<Map<String, Object>> result = new ArrayList<>();
        for (CsAgent agent : agents) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("agentId", agent.getId());
            row.put("userId", agent.getUserId());
            row.put("nickname", agent.getNickname());
            // 补充登录账号
            String username = "";
            if (org.jeecg.common.util.oConvertUtils.isNotEmpty(agent.getUserId())) {
                username = csSubAgentMapper.getUsernameByUserId(agent.getUserId());
            }
            row.put("username", username != null ? username : "");
            row.put("isSubAgent", org.jeecg.common.util.oConvertUtils.isNotEmpty(agent.getParentAgentId()));
            row.put("currentSessions", agent.getCurrentSessions() != null ? agent.getCurrentSessions() : 0);

            // 查询该客服今日好评量和好评率
            List<CsConversation> agentConversations = conversationService.list(
                    new LambdaQueryWrapper<CsConversation>()
                            .eq(CsConversation::getOwnerAgentId, agent.getId())
                            .ge(CsConversation::getCreateTime, todayStart)
                            .lt(CsConversation::getCreateTime, todayEnd)
                            .isNotNull(CsConversation::getSatisfaction));

            long goodCount = agentConversations.stream()
                    .filter(c -> c.getSatisfaction() != null && c.getSatisfaction() >= 4)
                    .count();
            long ratedCount = agentConversations.size();
            double goodRate = ratedCount > 0 ? (double) goodCount / ratedCount * 100 : 0;

            row.put("goodCount", goodCount);
            row.put("goodRate", Math.round(goodRate * 10) / 10.0);

            // 在线时长：在线客服 = now - lastOnlineTime，离线 = "-"
            int status = agent.getStatus() != null ? agent.getStatus() : 0;
            row.put("status", status);
            if (status == CsAgent.STATUS_ONLINE || status == CsAgent.STATUS_BUSY) {
                if (agent.getLastOnlineTime() != null) {
                    long onlineSeconds = (System.currentTimeMillis() - agent.getLastOnlineTime().getTime()) / 1000;
                    long hours = onlineSeconds / 3600;
                    long minutes = (onlineSeconds % 3600) / 60;
                    row.put("onlineDuration", hours + "h" + String.format("%02d", minutes) + "m");
                } else {
                    row.put("onlineDuration", "-");
                }
            } else {
                row.put("onlineDuration", "-");
            }

            result.add(row);
        }

        return Result.OK(result);
    }

    private Date getTodayStart() {
        return Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private Date getTodayEnd() {
        return Date.from(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }
}
