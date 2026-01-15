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
     * 客服下线
     * 
     * @param agentId 客服ID
     */
    void goOffline(String agentId);

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
     * 自动分配客服（选择接待数最少的）
     * 
     * @return 分配到的客服，如果没有可用客服返回null
     */
    CsAgent assignAgent();

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
}
