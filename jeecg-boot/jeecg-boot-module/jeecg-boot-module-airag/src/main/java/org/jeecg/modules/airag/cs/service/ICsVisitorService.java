package org.jeecg.modules.airag.cs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.airag.cs.entity.CsVisitor;

/**
 * 访客信息Service接口
 * 
 * @author jeecg
 * @date 2026-01-12
 */
public interface ICsVisitorService extends IService<CsVisitor> {

    /**
     * 获取或创建访客信息
     * 
     * @param appId 应用ID
     * @param userId 访客ID
     * @param userName 访客名称(可选)
     * @param source 来源渠道(可选)
     * @return 访客信息
     */
    CsVisitor getOrCreateVisitor(String appId, String userId, String userName, String source);

    /**
     * 根据appId和userId查询访客
     */
    CsVisitor getByAppAndUser(String appId, String userId);

    /**
     * 根据userId查询访客（不指定appId，取最新创建的一条）
     * 用于新版客服系统（不再依赖appId）
     */
    CsVisitor getByUserId(String userId);

    /**
     * 更新访客访问信息
     */
    void updateVisitInfo(String visitorId);

    /**
     * 增加会话数
     */
    void incrementConversationCount(String visitorId);

    /**
     * 切换星标
     */
    boolean toggleStar(String visitorId);

    /**
     * 更新客户等级
     */
    boolean updateLevel(String visitorId, Integer level);

    /**
     * 更新标签
     */
    boolean updateTags(String visitorId, String tags);

    /**
     * 分页查询访客列表
     */
    IPage<CsVisitor> pageVisitors(Page<CsVisitor> page, String appId, String keyword, Integer level, Integer star);

    /**
     * 获取访客显示名称
     * 优先级: 备注昵称 > 真实姓名 > 原始用户名
     */
    String getDisplayName(String appId, String userId, String defaultName);
}
