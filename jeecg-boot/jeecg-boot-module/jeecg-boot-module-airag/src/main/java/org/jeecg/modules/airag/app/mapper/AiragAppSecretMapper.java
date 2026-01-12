package org.jeecg.modules.airag.app.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.airag.app.entity.AiragAppSecret;

/**
 * @Description: AI应用接入密钥
 * @Author: jeecg-boot
 * @Date: 2025-01-07
 * @Version: V1.0
 */
public interface AiragAppSecretMapper extends BaseMapper<AiragAppSecret> {

    /**
     * 根据应用ID查询启用的密钥配置(忽略租户)
     *
     * @param appId 应用ID
     * @return 密钥配置
     */
    @InterceptorIgnore(tenantLine = "true")
    AiragAppSecret getEnabledByAppId(@Param("appId") String appId);
}
