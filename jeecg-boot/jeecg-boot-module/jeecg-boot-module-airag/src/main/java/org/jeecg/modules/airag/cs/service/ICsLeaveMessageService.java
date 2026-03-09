package org.jeecg.modules.airag.cs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.airag.cs.entity.CsLeaveMessage;

import java.util.List;

/**
 * 客服留言服务接口
 *
 * @author jeecg
 * @date 2026-02-06
 */
public interface ICsLeaveMessageService extends IService<CsLeaveMessage> {

    /**
     * 提交留言
     */
    CsLeaveMessage submitMessage(CsLeaveMessage message);

    /**
     * 回复留言
     */
    boolean replyMessage(String id, String reply, String agentId);

    /**
     * 撤回留言回复
     */
    boolean recallReply(String id);

    /**
     * 获取用户未读的留言回复列表
     */
    List<CsLeaveMessage> getUnreadReplies(String userId);

    /**
     * 标记留言回复为已读
     */
    void markAsRead(String userId);
}
