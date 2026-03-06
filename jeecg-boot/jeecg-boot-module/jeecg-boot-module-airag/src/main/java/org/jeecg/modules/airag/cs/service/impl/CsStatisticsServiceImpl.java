package org.jeecg.modules.airag.cs.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.cs.mapper.CsStatisticsMapper;
import org.jeecg.modules.airag.cs.service.ICsStatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class CsStatisticsServiceImpl implements ICsStatisticsService {

    @Autowired
    private CsStatisticsMapper statisticsMapper;

    @Override
    public List<Map<String, Object>> getAgentConversationStats(String startDate, String endDate) {
        return statisticsMapper.selectAgentConversationStats(startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> getVisitorRegionStats(String startDate, String endDate) {
        return statisticsMapper.selectVisitorRegionStats(startDate, endDate);
    }

    @Override
    public List<Map<String, Object>> getAttendanceStats(String queryDate) {
        return statisticsMapper.selectAttendanceStats(queryDate);
    }

    @Override
    public List<Map<String, Object>> getAttendanceDetail(String agentId, String queryDate) {
        return statisticsMapper.selectAttendanceDetail(agentId, queryDate);
    }

    @Override
    public List<Map<String, Object>> getAgentEfficiencyStats(String startDate, String endDate) {
        return statisticsMapper.selectAgentEfficiencyStats(startDate, endDate);
    }
}
