package org.jeecg.modules.airag.cs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.airag.cs.entity.CsQuickReply;

import java.util.List;

/**
 * 快捷回复服务接口
 * 
 * @author jeecg
 * @date 2026-01-07
 */
public interface ICsQuickReplyService extends IService<CsQuickReply> {

    /**
     * 获取客服的快捷回复列表（包含公共回复）
     * 
     * @param agentId 客服ID
     * @return 快捷回复列表
     */
    List<CsQuickReply> getAgentQuickReplies(String agentId);

    /**
     * 获取公共快捷回复列表
     * 
     * @return 公共快捷回复列表
     */
    List<CsQuickReply> getPublicQuickReplies();
}
