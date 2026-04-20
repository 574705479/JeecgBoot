package org.jeecg.modules.airag.cs.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.cs.entity.CsBrandConfig;
import org.jeecg.modules.airag.cs.mapper.CsBrandConfigMapper;
import org.jeecg.modules.airag.cs.service.CsBrandFidWhitelist;
import org.jeecg.modules.airag.cs.service.ICsBrandConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.Collection;

/**
 * 客服系统品牌配置 Service实现
 *
 * @author jeecg
 * @date 2026-01-20
 */
@Slf4j
@Service
public class CsBrandConfigServiceImpl extends ServiceImpl<CsBrandConfigMapper, CsBrandConfig> implements ICsBrandConfigService {

    /**
     * 注入白名单服务（required=false 防循环依赖；启动期 controller 还未就绪时不强制）
     */
    @Autowired(required = false)
    private CsBrandFidWhitelist brandFidWhitelist;

    // ---------------- 写操作钩子：触发白名单刷新 ----------------

    @Override
    public boolean save(CsBrandConfig entity) {
        boolean ok = super.save(entity);
        refreshWhitelistSafely();
        return ok;
    }

    @Override
    public boolean updateById(CsBrandConfig entity) {
        boolean ok = super.updateById(entity);
        refreshWhitelistSafely();
        return ok;
    }

    @Override
    public boolean saveOrUpdate(CsBrandConfig entity) {
        boolean ok = super.saveOrUpdate(entity);
        refreshWhitelistSafely();
        return ok;
    }

    @Override
    public boolean removeById(Serializable id) {
        boolean ok = super.removeById(id);
        refreshWhitelistSafely();
        return ok;
    }

    @Override
    public boolean removeByIds(Collection<?> idList) {
        boolean ok = super.removeByIds(idList);
        refreshWhitelistSafely();
        return ok;
    }

    private void refreshWhitelistSafely() {
        if (brandFidWhitelist == null) return;
        try {
            brandFidWhitelist.refresh();
        } catch (Exception e) {
            // 刷新失败不影响主流程；下次访问时由 CsBrandFileController 的 DB 兜底校验保底
            log.warn("[CsBrandConfigServiceImpl] 刷新白名单失败，将由 DB 兜底：{}", e.getMessage());
        }
    }
}
