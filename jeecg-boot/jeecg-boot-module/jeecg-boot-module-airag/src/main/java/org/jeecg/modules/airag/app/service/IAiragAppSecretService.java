package org.jeecg.modules.airag.app.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.airag.app.entity.AiragAppSecret;

/**
 * @Description: AI应用接入密钥服务接口
 * @Author: jeecg-boot
 * @Date: 2025-01-07
 * @Version: V1.0
 */
public interface IAiragAppSecretService extends IService<AiragAppSecret> {

    /**
     * 根据应用ID获取启用的密钥配置
     *
     * @param appId 应用ID
     * @return 密钥配置
     */
    AiragAppSecret getEnabledByAppId(String appId);

    /**
     * 验证接入签名
     *
     * @param appId     应用ID
     * @param token     签名token
     * @param timestamp 时间戳
     * @param referer   请求来源域名
     * @return 验证是否通过
     */
    boolean validateToken(String appId, String token, Long timestamp, String referer);

    /**
     * 生成密钥
     *
     * @return 随机密钥
     */
    String generateSecretKey();

    /**
     * 验证域名白名单
     *
     * @param domainWhitelist 白名单配置
     * @param referer         请求来源
     * @return 是否在白名单内
     */
    boolean validateDomain(String domainWhitelist, String referer);
}
