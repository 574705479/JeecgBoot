package org.jeecg.modules.airag.cs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.airag.cs.entity.CsAgent;

import java.util.List;

/**
 * 客服信息Mapper
 * 
 * @author jeecg
 * @date 2026-01-07
 */
@Mapper
public interface CsAgentMapper extends BaseMapper<CsAgent> {

    /**
     * 查询在线且可接待的客服列表（按饱和度排序，饱和度相同则随机）
     * 
     * @return 客服列表
     */
    @Select("SELECT * FROM cs_agent WHERE status = 1 AND current_sessions < COALESCE(NULLIF(max_sessions, 0), 10) ORDER BY current_sessions ASC, RAND()")
    List<CsAgent> selectAvailableAgents();

    /**
     * 查询在线且可接待的客服列表（按创建时间排序，用于轮流分配）
     * 
     * @return 客服列表
     */
    @Select("SELECT * FROM cs_agent WHERE status = 1 AND current_sessions < COALESCE(NULLIF(max_sessions, 0), 10) ORDER BY create_time ASC")
    List<CsAgent> selectAvailableAgentsForRoundRobin();

    /**
     * 增加客服当前接待数
     * 
     * @param agentId 客服ID
     * @return 影响行数
     */
    @Update({
        "<script>",
        "UPDATE cs_agent SET current_sessions = current_sessions + 1 WHERE id = #{agentId} AND current_sessions &lt; COALESCE(NULLIF(max_sessions, 0), 10)",
        "</script>"
    })
    int incrementCurrentSessions(@Param("agentId") String agentId);

    /**
     * 减少客服当前接待数
     * 
     * @param agentId 客服ID
     * @return 影响行数
     */
    @Update("UPDATE cs_agent SET current_sessions = GREATEST(current_sessions - 1, 0) WHERE id = #{agentId}")
    int decrementCurrentSessions(@Param("agentId") String agentId);

    /**
     * 增加客服累计服务数
     * 
     * @param agentId 客服ID
     * @return 影响行数
     */
    @Update("UPDATE cs_agent SET total_served = COALESCE(total_served, 0) + 1 WHERE id = #{agentId}")
    int incrementTotalServed(@Param("agentId") String agentId);

    /**
     * 以 cs_conversation 为准，将指定客服的 current_sessions 重算为其实际进行中(已分配)的会话数。
     *
     * <p>current_sessions 原本是手工 +1/-1 维护的计数，任何一次"加了没减"都会永久向上漂移，
     * 漂移到 &gt;= max_sessions 后该客服虽在线却被分配 SQL（current_sessions &lt; max_sessions）静默排除，
     * 导致访客一直堆在"未分配"。此方法用真实会话数对账，消除漂移。</p>
     *
     * @param agentId        客服ID
     * @param assignedStatus 进行中会话状态值（{@code CsConversation.STATUS_ASSIGNED}）
     * @return 影响行数
     */
    @Update("UPDATE cs_agent SET current_sessions = " +
            "(SELECT COUNT(*) FROM cs_conversation c WHERE c.agent_id = cs_agent.id AND c.status = #{assignedStatus}) " +
            "WHERE id = #{agentId}")
    int recalcCurrentSessions(@Param("agentId") String agentId, @Param("assignedStatus") int assignedStatus);

    /**
     * 批量把所有"非离线"客服的 current_sessions 重算为真实进行中会话数（对账兜底，定时任务调用）。
     *
     * @param assignedStatus 进行中会话状态值（{@code CsConversation.STATUS_ASSIGNED}）
     * @return 影响行数
     */
    @Update("UPDATE cs_agent SET current_sessions = " +
            "(SELECT COUNT(*) FROM cs_conversation c WHERE c.agent_id = cs_agent.id AND c.status = #{assignedStatus}) " +
            "WHERE status <> 0")
    int recalcAllActiveAgents(@Param("assignedStatus") int assignedStatus);
}
