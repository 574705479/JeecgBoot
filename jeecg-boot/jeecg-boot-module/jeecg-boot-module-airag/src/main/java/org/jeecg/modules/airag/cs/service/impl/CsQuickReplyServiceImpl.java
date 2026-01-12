package org.jeecg.modules.airag.cs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.cs.entity.CsQuickReply;
import org.jeecg.modules.airag.cs.mapper.CsQuickReplyMapper;
import org.jeecg.modules.airag.cs.service.ICsQuickReplyService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 快捷回复服务实现类
 * 
 * @author jeecg
 * @date 2026-01-07
 */
@Slf4j
@Service
public class CsQuickReplyServiceImpl extends ServiceImpl<CsQuickReplyMapper, CsQuickReply> implements ICsQuickReplyService {

    @Override
    public List<CsQuickReply> getAgentQuickReplies(String agentId) {
        QueryWrapper<CsQuickReply> queryWrapper = new QueryWrapper<>();
        // 获取该客服的私有回复和公共回复
        queryWrapper.and(wrapper -> 
            wrapper.eq("agent_id", agentId)
                   .or()
                   .isNull("agent_id")
        );
        queryWrapper.orderByDesc("create_time");
        return list(queryWrapper);
    }

    @Override
    public List<CsQuickReply> getPublicQuickReplies() {
        QueryWrapper<CsQuickReply> queryWrapper = new QueryWrapper<>();
        queryWrapper.isNull("agent_id");
        queryWrapper.orderByDesc("create_time");
        return list(queryWrapper);
    }
}
