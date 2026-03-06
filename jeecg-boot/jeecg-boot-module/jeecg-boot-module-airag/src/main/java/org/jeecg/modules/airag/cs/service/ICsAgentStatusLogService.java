package org.jeecg.modules.airag.cs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.airag.cs.entity.CsAgentStatusLog;

/**
 * 客服状态变更日志Service
 *
 * @author jeecg
 */
public interface ICsAgentStatusLogService extends IService<CsAgentStatusLog> {

    /**
     * 记录客服状态变更
     * 关闭上一条日志并插入新状态日志，若状态未变则跳过
     *
     * @param agentId       客服ID
     * @param newStatus     新状态 (0-离线/隐身 1-在线 2-忙碌)
     * @param triggerSource 触发来源
     */
    void logStatusChange(String agentId, int newStatus, String triggerSource);
}
