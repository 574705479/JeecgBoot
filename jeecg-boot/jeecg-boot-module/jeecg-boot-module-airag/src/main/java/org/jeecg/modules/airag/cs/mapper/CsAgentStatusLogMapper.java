package org.jeecg.modules.airag.cs.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.jeecg.modules.airag.cs.entity.CsAgentStatusLog;

/**
 * 客服状态变更日志Mapper
 *
 * @author jeecg
 */
@Mapper
public interface CsAgentStatusLogMapper extends BaseMapper<CsAgentStatusLog> {

    @Update("UPDATE cs_agent_status_log SET end_time = NOW(), duration_seconds = TIMESTAMPDIFF(SECOND, start_time, NOW()) WHERE id = #{id}")
    int closeLog(@Param("id") Long id);
}
