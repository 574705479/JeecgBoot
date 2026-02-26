package org.jeecg.common.license.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.common.license.annotation.RequireLicenseAspect;
import org.jeecg.common.license.core.*;
import org.jeecg.common.license.filter.LicenseVerifyFilter;
import org.jeecg.common.license.schedule.LicenseHeartbeatTask;
import org.jeecg.common.license.spi.LicenseEventListener;
import org.jeecg.common.license.spi.QuotaChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Configuration
@ConditionalOnProperty(name = "license.enabled", havingValue = "true")
@EnableConfigurationProperties(LicenseProperties.class)
public class LicenseConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LicenseConfiguration.class);

    @Bean
    public RsaVerifier rsaVerifier(LicenseProperties properties, ResourceLoader resourceLoader) {
        String publicKeyConfig = properties.getPublicKey();
        if (publicKeyConfig == null || publicKeyConfig.isBlank()) {
            throw new RuntimeException("[License] license.public-key is not configured");
        }
        try {
            String pem;
            if (publicKeyConfig.trim().startsWith("-----BEGIN")) {
                pem = publicKeyConfig;
            } else {
                Resource resource = resourceLoader.getResource(publicKeyConfig);
                try (InputStream is = resource.getInputStream()) {
                    pem = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
            return new RsaVerifier(pem);
        } catch (Exception e) {
            throw new RuntimeException("[License] Failed to load public key from: " + publicKeyConfig, e);
        }
    }

    @Bean
    public HmacSigner hmacSigner(LicenseProperties properties) {
        return new HmacSigner(properties.getAppSecret());
    }

    @Bean
    public LicenseCacheManager licenseCacheManager(LicenseProperties properties, ObjectMapper objectMapper) {
        return new LicenseCacheManager(properties.getCacheDir(), properties.getAppId(), properties.getAppSecret(), objectMapper);
    }

    @Bean
    public LicenseClientService licenseClientService(LicenseProperties properties,
                                                      RsaVerifier rsaVerifier,
                                                      HmacSigner hmacSigner,
                                                      LicenseCacheManager cacheManager,
                                                      ObjectMapper objectMapper,
                                                      ObjectProvider<List<QuotaChecker>> quotaCheckers,
                                                      ObjectProvider<List<LicenseEventListener>> eventListeners) {
        return new LicenseClientService(properties, rsaVerifier, hmacSigner, cacheManager, objectMapper,
                quotaCheckers.getIfAvailable(), eventListeners.getIfAvailable());
    }

    @Bean
    public FilterRegistrationBean<LicenseVerifyFilter> licenseVerifyFilter(
            LicenseClientService licenseClientService, LicenseProperties properties) {
        FilterRegistrationBean<LicenseVerifyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new LicenseVerifyFilter(licenseClientService, properties));
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.setName("licenseVerifyFilter");
        return registration;
    }

    @Bean(destroyMethod = "destroy")
    public LicenseHeartbeatTask licenseHeartbeatTask(LicenseClientService licenseClientService, LicenseProperties properties) {
        return new LicenseHeartbeatTask(licenseClientService, properties);
    }

    @Bean
    public RequireLicenseAspect requireLicenseAspect(LicenseClientService licenseClientService) {
        return new RequireLicenseAspect(licenseClientService);
    }

}
