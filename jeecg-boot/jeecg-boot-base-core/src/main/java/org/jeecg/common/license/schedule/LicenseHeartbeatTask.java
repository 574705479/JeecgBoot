package org.jeecg.common.license.schedule;

import org.jeecg.common.license.config.LicenseProperties;
import org.jeecg.common.license.core.LicenseClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LicenseHeartbeatTask {

    private static final Logger log = LoggerFactory.getLogger(LicenseHeartbeatTask.class);

    private final LicenseClientService licenseClientService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "license-heartbeat");
        t.setDaemon(true);
        return t;
    });

    public LicenseHeartbeatTask(LicenseClientService licenseClientService, LicenseProperties properties) {
        this.licenseClientService = licenseClientService;
        long interval = properties.getHeartbeatInterval();
        scheduler.scheduleWithFixedDelay(this::doHeartbeat, interval, interval, TimeUnit.SECONDS);
        log.info("[License] Heartbeat task started, interval={}s", interval);
    }

    private void doHeartbeat() {
        try {
            licenseClientService.heartbeat();
        } catch (Exception e) {
            log.error("[License] Heartbeat error", e);
        }
    }

    public void destroy() {
        scheduler.shutdownNow();
    }
}
