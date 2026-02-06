package org.jeecg.modules.airag.cs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.cs.entity.CsLeaveMessage;
import org.jeecg.modules.airag.cs.mapper.CsLeaveMessageMapper;
import org.jeecg.modules.airag.cs.service.ICsLeaveMessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 客服留言服务实现
 *
 * @author jeecg
 * @date 2026-02-06
 */
@Slf4j
@Service
public class CsLeaveMessageServiceImpl extends ServiceImpl<CsLeaveMessageMapper, CsLeaveMessage>
        implements ICsLeaveMessageService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CsLeaveMessage submitMessage(CsLeaveMessage message) {
        message.setStatus(CsLeaveMessage.STATUS_PENDING);
        message.setUserRead(false);
        message.setCreateTime(new Date());
        message.setUpdateTime(new Date());
        save(message);
        log.info("[CS-LeaveMessage] 用户提交留言: userId={}, id={}", message.getUserId(), message.getId());
        return message;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean replyMessage(String id, String reply, String agentId) {
        CsLeaveMessage message = getById(id);
        if (message == null) {
            log.warn("[CS-LeaveMessage] 留言不存在: id={}", id);
            return false;
        }
        message.setReply(reply);
        message.setReplyAgentId(agentId);
        message.setReplyTime(new Date());
        message.setStatus(CsLeaveMessage.STATUS_REPLIED);
        message.setUserRead(false);
        message.setUpdateTime(new Date());
        updateById(message);
        log.info("[CS-LeaveMessage] 客服回复留言: id={}, agentId={}", id, agentId);
        return true;
    }

    @Override
    public List<CsLeaveMessage> getUnreadReplies(String userId) {
        LambdaQueryWrapper<CsLeaveMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CsLeaveMessage::getUserId, userId)
                .eq(CsLeaveMessage::getStatus, CsLeaveMessage.STATUS_REPLIED)
                .eq(CsLeaveMessage::getUserRead, false)
                .orderByDesc(CsLeaveMessage::getReplyTime);
        return list(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(String userId) {
        update(new LambdaUpdateWrapper<CsLeaveMessage>()
                .eq(CsLeaveMessage::getUserId, userId)
                .eq(CsLeaveMessage::getStatus, CsLeaveMessage.STATUS_REPLIED)
                .eq(CsLeaveMessage::getUserRead, false)
                .set(CsLeaveMessage::getUserRead, true)
                .set(CsLeaveMessage::getUpdateTime, new Date()));
        log.info("[CS-LeaveMessage] 标记留言回复为已读: userId={}", userId);
    }
}
