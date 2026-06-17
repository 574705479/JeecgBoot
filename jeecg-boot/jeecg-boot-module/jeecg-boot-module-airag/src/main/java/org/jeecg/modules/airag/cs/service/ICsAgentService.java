package org.jeecg.modules.airag.cs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.airag.cs.entity.CsAgent;

import java.util.List;

/**
 * 客服管理服务接口
 * 
 * @author jeecg
 * @date 2026-01-07
 */
public interface ICsAgentService extends IService<CsAgent> {

    /**
     * 根据系统用户ID获取客服信息
     * 
     * @param userId 系统用户ID
     * @return 客服信息
     */
    CsAgent getByUserId(String userId);

    /**
     * 获取当前登录用户的客服信息
     * 
     * @return 客服信息
     */
    CsAgent getCurrentAgent();

    /**
     * 客服上线
     * 
     * @param agentId 客服ID
     */
    void goOnline(String agentId);

    /**
     * 客服下线（默认 triggerSource = manual，即隐身）
     * 
     * @param agentId 客服ID
     */
    void goOffline(String agentId);

    /**
     * 客服下线（指定触发来源）
     *
     * @param agentId       客服ID
     * @param triggerSource 触发来源: manual-隐身, websocket_disconnect-离线
     */
    void goOffline(String agentId, String triggerSource);

    /**
     * 设置客服忙碌
     * 
     * @param agentId 客服ID
     */
    void setBusy(String agentId);

    /**
     * 获取可用客服列表（在线且可接待）
     * 
     * @return 客服列表
     */
    List<CsAgent> getAvailableAgents();

    /**
     * 自动分配客服（根据全局配置的分配策略）
     * 
     * @return 分配到的客服，如果没有可用客服返回null
     */
    CsAgent assignAgent();

    /**
     * 自动分配客服（支持继承上次客服）
     * 
     * @param lastAgentId 上次服务该用户的客服ID（可为null）
     * @return 分配到的客服，如果没有可用客服返回null
     */
    CsAgent assignAgent(String lastAgentId);

    /**
     * 增加客服当前接待数
     * 
     * @param agentId 客服ID
     * @return 是否成功
     */
    boolean incrementSessions(String agentId);

    /**
     * 减少客服当前接待数
     * 
     * @param agentId 客服ID
     */
    void decrementSessions(String agentId);

    /**
     * 增加客服累计服务数（会话结束时调用）
     *
     * @param agentId 客服ID
     */
    void incrementTotalServed(String agentId);

    /**
     * 客服会话数对账（定时任务调用）。
     * <p>以 cs_conversation 进行中会话数为准，批量重算所有非离线客服的 current_sessions，
     * 自愈"加了没减"造成的计数漂移；并把因漂移误标 BUSY、重算后已有空位的客服恢复 ONLINE，
     * 最后异步补派一次未分配会话。根治"客服在线却因计数卡死分不到访客"。</p>
     */
    void reconcileActiveAgents();

    /**
     * 实时自愈触发：访客落到"未分配"但当前有客服在线时调用。
     * <p>在调用方事务提交后异步执行一次 {@link #reconcileActiveAgents()}（带 3s 去抖），
     * 让本访客在亚秒级被补派，而不必等 5 分钟定时对账。无客服在线时不触发。</p>
     */
    void triggerOnlineDriftHealAsync();

    /**
     * 查找任意一个在线且设置了AI应用的客服
     * 
     * @return 客服信息，如果没有返回null
     */
    CsAgent findOnlineAgentWithApp();

    /**
     * 获取所有在线的管理者客服
     * 管理者可以监控所有会话的消息
     * 
     * @return 在线管理者列表
     */
    List<CsAgent> getOnlineSupervisors();

    /**
     * 获取所有在线客服（不限角色，同事会话功能）
     * @return 所有在线客服列表
     */
    List<CsAgent> getOnlineAgents();

    /**
     * 获取在线客服数量。
     *
     * <p>Phase 3：访客端 bootstrap / online-status 接口高频调用，独立计数版本可走 Redis ZCARD
     * 直接拿数字，避免把整个客服列表序列化回客户端。</p>
     *
     * @return 在线客服数量（含忙碌/隐身），Redis 不可用时回落 DB 查
     */
    int countOnlineAgents();

    /**
     * 客服心跳：刷新 Redis 在线 ZSET 的 score 为当前时间戳。
     *
     * <p>由 WebSocket 心跳帧 / 客服端定时器调用。如果 agentId 不存在或已下线，自动 ZADD 进 ZSET，
     * 这样在 Redis 故障恢复后能自然回填。</p>
     *
     * @param agentId 客服ID
     */
    void markAgentHeartbeat(String agentId);

    /**
     * ws 重连时调用：从 Redis preshutdown 快照恢复客服状态。
     *
     * <p>触发时机：客服浏览器在镜像升级窗口里被强制断连，新容器启动后自动 ws 重连成功，
     * 由 {@code CsWebSocketHandler.afterConnectionEstablished} 在 {@code addSession} 之后调用。</p>
     *
     * <ul>
     *   <li>仅在 {@code cs_agent.status==OFFLINE} 时生效，避免覆盖客服已经手动切换过的状态</li>
     *   <li>快照命中后立即 DEL（一次性消费），并发 ws 重连时第二者读到 null 就跳过</li>
     *   <li>ONLINE/BUSY 都恢复为 ONLINE（current_sessions 已被 reset 为 0），并触发已有 sweep 派发累积未分配会话</li>
     *   <li>INVISIBLE 恢复为 INVISIBLE，不派单</li>
     * </ul>
     *
     * @param agentId 客服ID
     */
    void restoreFromPreshutdownSnapshot(String agentId);
}
