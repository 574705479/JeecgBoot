package org.jeecg.modules.airag.cs.task;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.cs.service.ICsAgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 客服会话数对账定时任务。
 *
 * <p>背景：{@code cs_agent.current_sessions} 是手工 +1/-1 维护的计数，分配链路完全以
 * {@code status = 1 AND current_sessions < max_sessions} 把关。任何一次"加了没减"都会让该计数
 * 永久向上漂移，漂到 &gt;= max_sessions 后，该客服即便在线、即便会话列表为空，也会被分配 SQL
 * 静默排除，访客因此一直堆在"未分配"。</p>
 *
 * <p>本任务作为兜底：周期性以 {@code cs_conversation} 的真实进行中会话数对账 current_sessions，
 * 让计数自愈，从根本上避免"在线客服分不到访客"。上线时刻另有 {@code goOnline} 内的即时重算。</p>
 *
 * @author jeecg
 * @date 2026-06-17
 */
@Slf4j
@Component
public class CsAgentSessionReconcileTask {

    @Autowired
    @Lazy
    private ICsAgentService agentService;

    /**
     * 每 5 分钟对账一次。延迟 2 分钟启动，避开应用启动期的状态重置/重连抖动。
     */
    @Scheduled(fixedRate = 300_000L, initialDelay = 120_000L)
    public void reconcile() {
        try {
            agentService.reconcileActiveAgents();
        } catch (Exception e) {
            log.error("[CS-Reconcile] 客服会话数对账任务执行失败", e);
        }
    }
}
