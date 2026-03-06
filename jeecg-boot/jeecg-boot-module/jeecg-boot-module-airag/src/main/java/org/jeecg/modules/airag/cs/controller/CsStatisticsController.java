package org.jeecg.modules.airag.cs.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.airag.cs.service.ICsStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 统计分析Controller
 *
 * @author jeecg
 */
@Slf4j
@Tag(name = "统计分析")
@RestController
@RequestMapping("/cs/statistics")
public class CsStatisticsController {

    @Autowired
    private ICsStatisticsService statisticsService;

    @Operation(summary = "客服对话统计")
    @GetMapping("/agent-conversation")
    public Result<List<Map<String, Object>>> agentConversation(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return Result.OK(statisticsService.getAgentConversationStats(startDate, endDate));
    }

    @Operation(summary = "访客区域统计")
    @GetMapping("/visitor-region")
    public Result<List<Map<String, Object>>> visitorRegion(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return Result.OK(statisticsService.getVisitorRegionStats(startDate, endDate));
    }

    @Operation(summary = "出勤记录统计")
    @GetMapping("/attendance")
    public Result<List<Map<String, Object>>> attendance(
            @RequestParam String queryDate) {
        return Result.OK(statisticsService.getAttendanceStats(queryDate));
    }

    @Operation(summary = "出勤详情")
    @GetMapping("/attendance/detail")
    public Result<List<Map<String, Object>>> attendanceDetail(
            @RequestParam String agentId,
            @RequestParam String queryDate) {
        return Result.OK(statisticsService.getAttendanceDetail(agentId, queryDate));
    }

    @Operation(summary = "客服对话效率统计")
    @GetMapping("/agent-efficiency")
    public Result<List<Map<String, Object>>> agentEfficiency(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return Result.OK(statisticsService.getAgentEfficiencyStats(startDate, endDate));
    }
}
