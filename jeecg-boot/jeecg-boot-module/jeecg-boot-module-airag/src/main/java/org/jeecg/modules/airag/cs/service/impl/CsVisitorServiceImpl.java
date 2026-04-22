package org.jeecg.modules.airag.cs.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.cs.entity.CsVisitor;
import org.jeecg.modules.airag.cs.mapper.CsVisitorMapper;
import org.jeecg.modules.airag.cs.service.ICsConversationService;
import org.jeecg.modules.airag.cs.service.ICsVisitorService;
import org.jeecg.modules.airag.cs.websocket.CsWebSocketMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 访客信息Service实现
 * 
 * @author jeecg
 * @date 2026-01-12
 */
@Slf4j
@Service
public class CsVisitorServiceImpl extends ServiceImpl<CsVisitorMapper, CsVisitor> implements ICsVisitorService {

    @Autowired
    private ICsConversationService conversationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CsVisitor getOrCreateVisitor(String appId, String userId, String userName, String source) {
        // 先查询是否存在
        CsVisitor visitor = baseMapper.selectByAppAndUser(appId, userId);
        
        if (visitor == null) {
            // 创建新访客
            visitor = new CsVisitor();
            visitor.setAppId(appId);
            visitor.setUserId(userId);
            visitor.setNickname(userName); // 初始使用用户名作为昵称
            visitor.setSource(source);
            visitor.setFirstVisitTime(new Date());
            visitor.setLastVisitTime(new Date());
            visitor.setVisitCount(1);
            visitor.setConversationCount(0);
            visitor.setLevel(CsVisitor.LEVEL_NORMAL);
            visitor.setStar(0);
            visitor.setGender(CsVisitor.GENDER_UNKNOWN);
            visitor.setCreateTime(new Date());
            
            baseMapper.insert(visitor);
            log.info("创建新访客: appId={}, userId={}", appId, userId);
        } else {
            // 更新访问信息
            baseMapper.updateVisitInfo(visitor.getId());
        }
        
        return visitor;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public CsVisitor touchVisitor(String appId, String userId, String userName,
                                  String source, boolean newConversation) {
        if (oConvertUtils.isEmpty(userId)) {
            return null;
        }

        // 1) appId 非空时优先按 appId+userId 精确匹配；
        //    appId 为空（新版客服系统）则直接退化为 userId 兜底，避免重复插入
        CsVisitor visitor = oConvertUtils.isNotEmpty(appId)
                ? baseMapper.selectByAppAndUser(appId, userId) : null;
        if (visitor == null) {
            visitor = baseMapper.selectByUserId(userId);
        }

        Date now = new Date();
        if (visitor == null) {
            visitor = new CsVisitor()
                    .setAppId(appId)
                    .setUserId(userId)
                    .setNickname(userName)
                    .setSource(source)
                    .setFirstVisitTime(now)
                    .setLastVisitTime(now)
                    .setVisitCount(1)
                    .setConversationCount(newConversation ? 1 : 0)
                    .setLevel(CsVisitor.LEVEL_NORMAL)
                    .setStar(0)
                    .setGender(CsVisitor.GENDER_UNKNOWN)
                    .setCreateTime(now);
            try {
                baseMapper.insert(visitor);
                log.info("[CS-Visitor] 触达新访客: appId={}, userId={}, newConv={}",
                        appId, userId, newConversation);
            } catch (DuplicateKeyException dup) {
                // 并发场景：唯一索引冲突，回退查询一次再走更新分支
                CsVisitor existed = baseMapper.selectByUserId(userId);
                if (existed == null) {
                    throw dup;
                }
                applyVisit(existed, newConversation, now);
                return existed;
            }
            return visitor;
        }

        applyVisit(visitor, newConversation, now);
        return visitor;
    }

    /**
     * 已存在访客：刷新访问统计 + 兜底回填 firstVisitTime
     */
    private void applyVisit(CsVisitor visitor, boolean newConversation, Date now) {
        baseMapper.updateVisitInfo(visitor.getId());
        if (newConversation) {
            baseMapper.incrementConversationCount(visitor.getId());
        }
        if (visitor.getFirstVisitTime() == null) {
            Date fallback = visitor.getCreateTime() != null ? visitor.getCreateTime() : now;
            baseMapper.fillFirstVisitTimeIfNull(visitor.getId(), fallback);
        }
    }

    @Override
    public CsVisitor getByAppAndUser(String appId, String userId) {
        return baseMapper.selectByAppAndUser(appId, userId);
    }

    @Override
    public CsVisitor getByUserId(String userId) {
        return baseMapper.selectByUserId(userId);
    }

    @Override
    public void updateVisitInfo(String visitorId) {
        baseMapper.updateVisitInfo(visitorId);
    }

    @Override
    public void incrementConversationCount(String visitorId) {
        baseMapper.incrementConversationCount(visitorId);
    }

    @Override
    public boolean toggleStar(String visitorId) {
        return baseMapper.toggleStar(visitorId) > 0;
    }

    @Override
    public boolean updateLevel(String visitorId, Integer level) {
        LambdaUpdateWrapper<CsVisitor> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CsVisitor::getId, visitorId)
               .set(CsVisitor::getLevel, level)
               .set(CsVisitor::getUpdateTime, new Date());
        return update(wrapper);
    }

    @Override
    public boolean updateTags(String visitorId, String tags) {
        LambdaUpdateWrapper<CsVisitor> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CsVisitor::getId, visitorId)
               .set(CsVisitor::getTags, tags)
               .set(CsVisitor::getUpdateTime, new Date());
        return update(wrapper);
    }

    @Override
    public IPage<CsVisitor> pageVisitors(Page<CsVisitor> page, String appId, String keyword, Integer level, Integer star) {
        return baseMapper.selectVisitorPage(page, appId, keyword, level, star);
    }

    @Override
    public void notifyVisitorUpdated(CsVisitor visitor) {
        if (visitor == null || visitor.getUserId() == null || visitor.getUserId().isEmpty()) {
            return;
        }
        List<String> conversationIds = conversationService.getActiveConversationIdsByUser(
                visitor.getAppId(), visitor.getUserId());
        if (conversationIds.isEmpty()) {
            return;
        }
        Map<String, Object> extra = new HashMap<>();
        extra.put("userId", visitor.getUserId());
        extra.put("appId", visitor.getAppId());
        extra.put("visitor", visitor);
        for (String conversationId : conversationIds) {
            CsWebSocketMessage message = CsWebSocketMessage.builder()
                    .type(CsWebSocketMessage.TYPE_VISITOR_UPDATED)
                    .conversationId(conversationId)
                    .content("访客信息更新")
                    .extra(extra)
                    .timestamp(new Date())
                    .build();
            conversationService.sendToRelatedAgents(conversationId, message);
        }
    }
}
