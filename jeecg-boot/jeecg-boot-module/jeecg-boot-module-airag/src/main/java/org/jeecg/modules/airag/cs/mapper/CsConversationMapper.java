package org.jeecg.modules.airag.cs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.airag.cs.entity.CsConversation;
import org.jeecg.modules.airag.cs.vo.CsAgentWorkloadVO;

import java.util.Date;
import java.util.List;

/**
 * 客服会话Mapper (重构版)
 * 
 * @author jeecg
 * @date 2026-01-12
 */
@Mapper
public interface CsConversationMapper extends BaseMapper<CsConversation> {

    /**
     * 分页查询会话列表（包含主负责客服信息）
     * 
     * @param page           分页参数
     * @param agentId        客服ID（主负责人或协作者）
     * @param status         状态
     * @param filter         筛选类型: mine-我负责的, collab-协作中, unassigned-未分配, history-会话记录
     * @param includeDeleted 是否包含已删除的记录
     * @param filterAgentId  按指定客服筛选（用于会话记录查询）
     * @return 会话列表
     */
    IPage<CsConversation> selectConversationPage(Page<CsConversation> page, 
                                                  @Param("agentId") String agentId, 
                                                  @Param("status") Integer status,
                                                  @Param("filter") String filter,
                                                  @Param("includeDeleted") Boolean includeDeleted,
                                                  @Param("filterAgentId") String filterAgentId,
                                                  @Param("id") String id,
                                                  @Param("userId") String userId,
                                                  @Param("endType") Integer endType,
                                                  @Param("satisfaction") Integer satisfaction,
                                                  @Param("source") String source,
                                                  @Param("landingPage") String landingPage,
                                                  @Param("referrerPage") String referrerPage,
                                                  @Param("createTimeBegin") String createTimeBegin,
                                                  @Param("createTimeEnd") String createTimeEnd,
                                                  @Param("endTimeBegin") String endTimeBegin,
                                                  @Param("endTimeEnd") String endTimeEnd);

    /**
     * 获取用户进行中的会话ID列表
     */
    List<String> selectActiveConversationIdsByUser(@Param("appId") String appId,
                                                   @Param("userId") String userId);

    /**
     * 查询所有进行中的会话（监控模式），包含访客星标和客服信息
     */
    IPage<CsConversation> selectAllActiveConversations(Page<CsConversation> page);

    /**
     * 获取客服负责的会话列表
     */
    @Select("SELECT c.*, c.agent_id as owner_agent_id, " +
            "a.nickname as owner_agent_name, a.avatar as owner_agent_avatar, " +
            "v.nickname as visitor_nickname, v.star as visitor_star, v.star_time as visitor_star_time " +
            "FROM cs_conversation c " +
            "LEFT JOIN cs_agent a ON c.agent_id = a.id " +
            "LEFT JOIN cs_visitor v ON c.user_id = v.user_id AND c.app_id = v.app_id " +
            "WHERE c.agent_id = #{agentId} AND c.status != 2 " +
            "AND (c.deleted = 0 OR c.deleted IS NULL) " +
            "ORDER BY c.last_message_time DESC")
    List<CsConversation> selectByOwnerAgent(@Param("agentId") String agentId);

    /**
     * 获取未分配的会话列表
     */
    @Select("SELECT * FROM cs_conversation " +
            "WHERE (status = 0 OR agent_id IS NULL) " +
            "AND (deleted = 0 OR deleted IS NULL) " +
            "ORDER BY last_message_time DESC " +
            "LIMIT #{limit}")
    List<CsConversation> selectUnassigned(@Param("limit") int limit);

    /**
     * 客服上线 sweep 派单专用：仅普通未分配会话（humanAgentMode=0/NULL），按活跃度排序
     * 排除 humanAgentMode=1（访客需主动转人工的会话），保留智能助手转人工流程
     * COALESCE 兼容新建会话尚无消息时 last_message_time 为 NULL 的情况
     */
    @Select("SELECT * FROM cs_conversation " +
            "WHERE status = 0 " +
            "  AND (human_agent_mode IS NULL OR human_agent_mode = 0) " +
            "  AND (agent_id IS NULL OR agent_id = '') " +
            "  AND (deleted = 0 OR deleted IS NULL) " +
            "ORDER BY COALESCE(last_message_time, create_time) DESC " +
            "LIMIT #{limit}")
    List<CsConversation> selectUnassignedForSweep(@Param("limit") int limit);

    /**
     * 获取客服工作量统计
     */
    @Select("SELECT c.agent_id AS agentId, " +
            "IFNULL(a.nickname, '未知客服') AS agentName, " +
            "COUNT(c.id) AS conversationCount, " +
            "IFNULL(SUM(c.message_count), 0) AS messageCount, " +
            "ROUND(AVG(c.satisfaction), 1) AS avgSatisfaction, " +
            "ROUND(AVG(CASE WHEN c.first_response_seconds IS NULL OR c.first_response_seconds = 0 THEN NULL " +
            "ELSE c.first_response_seconds END), 0) AS avgFirstResponseSeconds " +
            "FROM cs_conversation c " +
            "LEFT JOIN cs_agent a ON c.agent_id = a.id " +
            "WHERE c.agent_id IS NOT NULL " +
            "AND (c.deleted = 0 OR c.deleted IS NULL) " +
            "AND c.create_time >= #{startTime} " +
            "AND c.create_time < #{endTime} " +
            "GROUP BY c.agent_id, a.nickname " +
            "ORDER BY conversationCount DESC, messageCount DESC " +
            "LIMIT #{limit}")
    List<CsAgentWorkloadVO> selectAgentWorkload(@Param("startTime") Date startTime,
                                                @Param("endTime") Date endTime,
                                                @Param("limit") Integer limit);

    /**
     * 查询超期已结束会话的ID（物理删除前先收集，用于级联清理）
     */
    @Select("SELECT id FROM cs_conversation WHERE status = 2 AND end_time < #{deadline} LIMIT #{limit}")
    List<String> selectExpiredClosedIds(@Param("deadline") Date deadline, @Param("limit") int limit);

    /**
     * 物理删除已结束的超期会话（绕过 @TableLogic）
     */
    @Delete("DELETE FROM cs_conversation WHERE status = 2 AND end_time < #{deadline} LIMIT #{limit}")
    int physicalDeleteExpired(@Param("deadline") Date deadline, @Param("limit") int limit);
}
