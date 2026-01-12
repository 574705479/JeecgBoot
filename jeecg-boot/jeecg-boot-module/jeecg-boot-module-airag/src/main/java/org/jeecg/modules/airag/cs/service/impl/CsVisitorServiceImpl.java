package org.jeecg.modules.airag.cs.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.cs.entity.CsVisitor;
import org.jeecg.modules.airag.cs.mapper.CsVisitorMapper;
import org.jeecg.modules.airag.cs.service.ICsVisitorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 访客信息Service实现
 * 
 * @author jeecg
 * @date 2026-01-12
 */
@Slf4j
@Service
public class CsVisitorServiceImpl extends ServiceImpl<CsVisitorMapper, CsVisitor> implements ICsVisitorService {

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
    public CsVisitor getByAppAndUser(String appId, String userId) {
        return baseMapper.selectByAppAndUser(appId, userId);
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
    public String getDisplayName(String appId, String userId, String defaultName) {
        CsVisitor visitor = baseMapper.selectByAppAndUser(appId, userId);
        if (visitor != null) {
            // 优先返回备注昵称
            if (visitor.getNickname() != null && !visitor.getNickname().isEmpty()) {
                return visitor.getNickname();
            }
            // 其次返回真实姓名
            if (visitor.getRealName() != null && !visitor.getRealName().isEmpty()) {
                return visitor.getRealName();
            }
        }
        return defaultName;
    }
}
