package org.jeecg.modules.airag.cs.service;

import java.util.List;
import java.util.Map;

/**
 * 统计分析Service
 *
 * @author jeecg
 */
public interface ICsStatisticsService {

    List<Map<String, Object>> getAgentConversationStats(String startDate, String endDate);

    List<Map<String, Object>> getVisitorRegionStats(String startDate, String endDate);

    List<Map<String, Object>> getAttendanceStats(String queryDate);

    List<Map<String, Object>> getAttendanceDetail(String agentId, String queryDate);

    List<Map<String, Object>> getAgentEfficiencyStats(String startDate, String endDate);
}
