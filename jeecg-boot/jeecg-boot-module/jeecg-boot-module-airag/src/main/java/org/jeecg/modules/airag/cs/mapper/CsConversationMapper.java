package org.jeecg.modules.airag.cs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.airag.cs.entity.CsConversation;

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
                                                  @Param("filterAgentId") String filterAgentId);

    /**
     * 获取客服负责的会话列表
     */
    @Select("SELECT c.*, a.nickname as owner_agent_name, a.avatar as owner_agent_avatar " +
            "FROM cs_conversation c " +
            "LEFT JOIN cs_agent a ON c.agent_id = a.id " +
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
     * 统计客服当前负责的会话数
     */
    @Select("SELECT COUNT(*) FROM cs_conversation " +
            "WHERE agent_id = #{agentId} AND status = 1 " +
            "AND (deleted = 0 OR deleted IS NULL)")
    int countByOwnerAgent(@Param("agentId") String agentId);
}
