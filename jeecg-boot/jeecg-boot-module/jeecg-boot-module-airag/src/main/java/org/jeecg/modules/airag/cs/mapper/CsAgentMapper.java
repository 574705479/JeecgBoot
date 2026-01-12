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
     * 查询在线且可接待的客服列表
     * 
     * @return 客服列表
     */
    @Select("SELECT * FROM cs_agent WHERE status = 1 AND current_sessions < max_sessions ORDER BY current_sessions ASC")
    List<CsAgent> selectAvailableAgents();

    /**
     * 增加客服当前接待数
     * 
     * @param agentId 客服ID
     * @return 影响行数
     */
    @Update({
        "<script>",
        "UPDATE cs_agent SET current_sessions = current_sessions + 1 WHERE id = #{agentId} AND current_sessions &lt; max_sessions",
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
}
