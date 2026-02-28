package com.license.server.service;

import com.license.server.config.LicenseServerProperties;
import com.license.server.entity.App;
import com.license.server.repository.AppRepository;
import com.license.server.util.CryptoUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyPair;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppService {

    private final AppRepository appRepository;
    private final LicenseServerProperties properties;

    public Page<App> list(int page, int size, String keyword) {
        return appRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("appName"), pattern),
                        cb.like(root.get("appId"), pattern)
                ));
            }
            return predicates.isEmpty() ? null : cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime")));
    }

    public App getById(Long id) {
        return appRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("应用不存在"));
    }

    public App getByAppId(String appId) {
        return appRepository.findByAppId(appId).orElse(null);
    }

    @Transactional
    public App create(App app) {
        if (appRepository.existsByAppId(app.getAppId())) {
            throw new IllegalArgumentException("应用标识已存在");
        }
        try {
            KeyPair keyPair = CryptoUtil.generateRsaKeyPair();
            app.setPublicKey(CryptoUtil.publicKeyToPem(keyPair.getPublic()));

            String masterKey = properties.getMasterKey();
            if (masterKey != null && !masterKey.isBlank()) {
                String privatePem = CryptoUtil.privateKeyToPem(keyPair.getPrivate());
                app.setPrivateKey(CryptoUtil.encryptAesGcm(privatePem, masterKey));
            } else {
                app.setPrivateKey(CryptoUtil.privateKeyToPem(keyPair.getPrivate()));
            }

            app.setAppSecret(CryptoUtil.generateAppSecret());
            return appRepository.save(app);
        } catch (Exception e) {
            throw new RuntimeException("创建应用失败: " + e.getMessage(), e);
        }
    }

    @Transactional
    public App update(Long id, App updated) {
        App app = getById(id);
        app.setAppName(updated.getAppName());
        app.setQuotasDef(updated.getQuotasDef());
        app.setFeaturesDef(updated.getFeaturesDef());
        app.setStatus(updated.getStatus());
        app.setRemark(updated.getRemark());
        return appRepository.save(app);
    }

    @Transactional
    public App rotateSecret(Long id) {
        App app = getById(id);
        app.setAppSecretOld(app.getAppSecret());
        app.setSecretRotateAt(LocalDateTime.now());
        app.setAppSecret(CryptoUtil.generateAppSecret());
        return appRepository.save(app);
    }

    @Transactional
    public App generateKeys(Long id) {
        App app = getById(id);
        try {
            KeyPair keyPair = CryptoUtil.generateRsaKeyPair();
            app.setPublicKey(CryptoUtil.publicKeyToPem(keyPair.getPublic()));

            String masterKey = properties.getMasterKey();
            if (masterKey != null && !masterKey.isBlank()) {
                String privatePem = CryptoUtil.privateKeyToPem(keyPair.getPrivate());
                app.setPrivateKey(CryptoUtil.encryptAesGcm(privatePem, masterKey));
            } else {
                app.setPrivateKey(CryptoUtil.privateKeyToPem(keyPair.getPrivate()));
            }

            if ("PLACEHOLDER_SECRET_ROTATE_AFTER_FIRST_LOGIN".equals(app.getAppSecret())) {
                app.setAppSecret(CryptoUtil.generateAppSecret());
            }

            return appRepository.save(app);
        } catch (Exception e) {
            throw new RuntimeException("生成密钥对失败: " + e.getMessage(), e);
        }
    }

    public String decryptPrivateKey(App app) {
        String masterKey = properties.getMasterKey();
        String pk = app.getPrivateKey();
        if (masterKey == null || masterKey.isBlank()) {
            return pk;
        }
        if (pk != null && pk.startsWith("-----BEGIN")) {
            try {
                app.setPrivateKey(CryptoUtil.encryptAesGcm(pk, masterKey));
                appRepository.save(app);
                log.info("已自动加密应用[{}]的私钥", app.getAppName());
            } catch (Exception e) {
                log.warn("自动加密私钥失败: {}", e.getMessage());
            }
            return pk;
        }
        try {
            return CryptoUtil.decryptAesGcm(pk, masterKey);
        } catch (Exception e) {
            throw new RuntimeException("解密私钥失败", e);
        }
    }

    public boolean verifyHmac(App app, String signPayload, String sign) {
        try {
            String expected = CryptoUtil.hmacSha256(signPayload, app.getAppSecret());
            if (expected.equalsIgnoreCase(sign)) {
                return true;
            }
            if (app.getAppSecretOld() != null && app.getSecretRotateAt() != null
                    && app.getSecretRotateAt().plusHours(24).isAfter(LocalDateTime.now())) {
                String fallback = CryptoUtil.hmacSha256(signPayload, app.getAppSecretOld());
                return fallback.equalsIgnoreCase(sign);
            }
            return false;
        } catch (Exception e) {
            log.error("HMAC verification failed", e);
            return false;
        }
    }
}
