package com.license.server.schedule;

import com.license.server.config.LicenseServerProperties;
import com.license.server.entity.App;
import com.license.server.repository.AppRepository;
import com.license.server.repository.LicenseLogRepository;
import com.license.server.repository.LicenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LicenseScheduledTasks {

    private final LicenseRepository licenseRepository;
    private final LicenseLogRepository licenseLogRepository;
    private final AppRepository appRepository;
    private final LicenseServerProperties properties;

    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void expireLicenses() {
        int count = licenseRepository.expireActiveLicenses(LocalDateTime.now());
        if (count > 0) {
            log.info("[License] Auto-expired {} licenses", count);
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void checkExpiringLicenses() {
        var expiring = licenseRepository.findExpiringLicenses(LocalDateTime.now(), LocalDateTime.now().plusDays(7));
        if (!expiring.isEmpty()) {
            log.warn("[License] {} licenses expiring within 7 days", expiring.size());
        }
    }

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupLogs() {
        int days = properties.getLog().getRetentionDays();
        int count = licenseLogRepository.deleteOlderThan(LocalDateTime.now().minusDays(days));
        if (count > 0) {
            log.info("[License] Cleaned up {} log entries older than {} days", count, days);
        }
    }

    @Scheduled(cron = "0 0 * * * ?")
    @Transactional
    public void cleanupRotatedSecrets() {
        List<App> apps = appRepository.findBySecretRotateAtBeforeAndAppSecretOldIsNotNull(
                LocalDateTime.now().minusHours(24));
        for (App app : apps) {
            app.setAppSecretOld(null);
            app.setSecretRotateAt(null);
            appRepository.save(app);
            log.info("[License] Cleaned up rotated secret for app: {}", app.getAppId());
        }
    }
}
