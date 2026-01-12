package org.jeecg.modules.airag.cs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.jeecg.modules.airag.cs.entity.CsCollaborator;

import java.util.List;

/**
 * 会话协作者Mapper
 * 
 * @author jeecg
 * @date 2026-01-12
 */
@Mapper
public interface CsCollaboratorMapper extends BaseMapper<CsCollaborator> {

    /**
     * 获取会话的活跃协作者列表（包含客服信息）
     */
    @Select("SELECT c.*, a.nickname as agent_name, a.avatar as agent_avatar, a.status as agent_status " +
            "FROM cs_collaborator c " +
            "LEFT JOIN cs_agent a ON c.agent_id = a.id " +
            "WHERE c.conversation_id = #{conversationId} AND c.leave_time IS NULL " +
            "ORDER BY c.role ASC, c.join_time ASC")
    List<CsCollaborator> selectActiveCollaborators(@Param("conversationId") String conversationId);

    /**
     * 获取客服参与的会话ID列表
     */
    @Select("SELECT DISTINCT conversation_id FROM cs_collaborator " +
            "WHERE agent_id = #{agentId} AND leave_time IS NULL")
    List<String> selectConversationIdsByAgent(@Param("agentId") String agentId);

    /**
     * 检查客服是否已在协作中
     */
    @Select("SELECT COUNT(*) FROM cs_collaborator " +
            "WHERE conversation_id = #{conversationId} AND agent_id = #{agentId} AND leave_time IS NULL")
    int countActiveByConvAndAgent(@Param("conversationId") String conversationId, @Param("agentId") String agentId);
}
