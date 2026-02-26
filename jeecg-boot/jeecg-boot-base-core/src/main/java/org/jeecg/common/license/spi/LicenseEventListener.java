package org.jeecg.common.license.spi;

import org.jeecg.common.license.core.LicenseInfo;

public interface LicenseEventListener {
    default void onActivated(LicenseInfo info) {}
    default void onHeartbeatSuccess(LicenseInfo info) {}
    default void onHeartbeatFailed(int failCount) {}
    default void onGracePeriodEntered() {}
    default void onLicenseExpired() {}
    default void onLicenseInvalid() {}
}
