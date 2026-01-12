package org.jeecg.modules.airag.app.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.app.entity.AiragAppSecret;
import org.jeecg.modules.airag.app.mapper.AiragAppSecretMapper;
import org.jeecg.modules.airag.app.service.IAiragAppSecretService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @Description: AI应用接入密钥服务实现
 * @Author: jeecg-boot
 * @Date: 2025-01-07
 * @Version: V1.0
 */
@Service
@Slf4j
public class AiragAppSecretServiceImpl extends ServiceImpl<AiragAppSecretMapper, AiragAppSecret> implements IAiragAppSecretService {

    /**
     * 默认签名有效期（分钟）
     */
    private static final int DEFAULT_TOKEN_EXPIRE_MINUTES = 5;

    @Override
    public AiragAppSecret getEnabledByAppId(String appId) {
        return baseMapper.getEnabledByAppId(appId);
    }

    @Override
    public boolean validateToken(String appId, String token, Long timestamp, String referer) {
        // 参数校验
        if (oConvertUtils.isEmpty(appId) || oConvertUtils.isEmpty(token) || timestamp == null) {
            log.warn("[接入验证] 参数不完整: appId={}, token={}, timestamp={}", appId, token, timestamp);
            return false;
        }

        // 获取密钥配置
        AiragAppSecret secret = getEnabledByAppId(appId);
        if (secret == null || oConvertUtils.isEmpty(secret.getSecretKey())) {
            log.warn("[接入验证] 未找到应用密钥配置或密钥为空: appId={}", appId);
            // 如果没有配置密钥或密钥为空，默认允许访问（向后兼容）
            return true;
        }

        // 验证时间戳有效期
        int expireMinutes = secret.getTokenExpireMinutes() != null ? secret.getTokenExpireMinutes() : DEFAULT_TOKEN_EXPIRE_MINUTES;
        long currentTime = System.currentTimeMillis();
        long timeDiff = Math.abs(currentTime - timestamp);
        if (timeDiff > expireMinutes * 60 * 1000L) {
            log.warn("[接入验证] 签名已过期: appId={}, timestamp={}, currentTime={}, expireMinutes={}", 
                    appId, timestamp, currentTime, expireMinutes);
            return false;
        }

        // 验证域名白名单
        if (oConvertUtils.isNotEmpty(secret.getDomainWhitelist()) && oConvertUtils.isNotEmpty(referer)) {
            if (!validateDomain(secret.getDomainWhitelist(), referer)) {
                log.warn("[接入验证] 域名不在白名单内: appId={}, referer={}, whitelist={}", 
                        appId, referer, secret.getDomainWhitelist());
                return false;
            }
        }

        // 验证签名 token = MD5(appId + secretKey + timestamp)
        String expectedToken = generateToken(appId, secret.getSecretKey(), timestamp);
        if (!expectedToken.equalsIgnoreCase(token)) {
            log.warn("[接入验证] 签名验证失败: appId={}", appId);
            return false;
        }

        log.debug("[接入验证] 验证通过: appId={}", appId);
        return true;
    }

    @Override
    public String generateSecretKey() {
        // 生成32位随机密钥
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public boolean validateDomain(String domainWhitelist, String referer) {
        if (oConvertUtils.isEmpty(domainWhitelist) || oConvertUtils.isEmpty(referer)) {
            return true;
        }

        try {
            // 解析referer获取域名
            URI uri = new URI(referer);
            String host = uri.getHost();
            if (host == null) {
                return false;
            }

            // 检查是否在白名单内
            String[] whitelist = domainWhitelist.split(",");
            for (String domain : whitelist) {
                domain = domain.trim().toLowerCase();
                if (domain.isEmpty()) {
                    continue;
                }
                // 支持通配符 *.example.com
                if (domain.startsWith("*.")) {
                    String suffix = domain.substring(1); // .example.com
                    if (host.toLowerCase().endsWith(suffix) || host.equalsIgnoreCase(domain.substring(2))) {
                        return true;
                    }
                } else if (host.equalsIgnoreCase(domain)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("[接入验证] 解析域名失败: referer={}", referer, e);
            return false;
        }
    }

    /**
     * 生成签名token
     *
     * @param appId     应用ID
     * @param secretKey 密钥
     * @param timestamp 时间戳
     * @return MD5签名
     */
    private String generateToken(String appId, String secretKey, Long timestamp) {
        String raw = appId + secretKey + timestamp;
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }
}
