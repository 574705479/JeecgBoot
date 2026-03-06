package org.jeecg.modules.airag.cs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.cs.entity.CsAgentStatusLog;
import org.jeecg.modules.airag.cs.mapper.CsAgentStatusLogMapper;
import org.jeecg.modules.airag.cs.service.ICsAgentStatusLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Slf4j
@Service
public class CsAgentStatusLogServiceImpl extends ServiceImpl<CsAgentStatusLogMapper, CsAgentStatusLog>
        implements ICsAgentStatusLogService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void logStatusChange(String agentId, int newStatus, String triggerSource) {
        try {
            CsAgentStatusLog lastLog = getOne(new LambdaQueryWrapper<CsAgentStatusLog>()
                    .eq(CsAgentStatusLog::getAgentId, agentId)
                    .isNull(CsAgentStatusLog::getEndTime)
                    .orderByDesc(CsAgentStatusLog::getStartTime)
                    .last("LIMIT 1"));

            if (lastLog != null) {
                if (lastLog.getStatus() == newStatus) {
                    return;
                }
                baseMapper.closeLog(lastLog.getId());
            }

            CsAgentStatusLog newLog = new CsAgentStatusLog();
            newLog.setAgentId(agentId);
            newLog.setStatus(newStatus);
            newLog.setTriggerSource(triggerSource);
            newLog.setStartTime(new Date());
            newLog.setCreateTime(new Date());
            save(newLog);
        } catch (Exception e) {
            log.warn("[CS-StatusLog] 记录状态变更失败: agentId={}, status={}, error={}", agentId, newStatus, e.getMessage());
        }
    }
}
