package org.jeecg.common.license.core;

public record LicenseState(
    LicenseInfo license,
    long lastVerifyTime,
    int heartbeatFailCount,
    boolean licensed
) {
    public static final LicenseState EMPTY = new LicenseState(null, 0, 0, false);
}
