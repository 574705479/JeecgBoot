package org.jeecg.modules.airag.cs.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 统计分析Mapper
 *
 * @author jeecg
 */
@Mapper
public interface CsStatisticsMapper {

    List<Map<String, Object>> selectAgentConversationStats(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectVisitorRegionStats(@Param("startDate") String startDate, @Param("endDate") String endDate);

    List<Map<String, Object>> selectAttendanceStats(@Param("queryDate") String queryDate);

    List<Map<String, Object>> selectAttendanceDetail(@Param("agentId") String agentId, @Param("queryDate") String queryDate);

    List<Map<String, Object>> selectAgentEfficiencyStats(@Param("startDate") String startDate, @Param("endDate") String endDate);
}
