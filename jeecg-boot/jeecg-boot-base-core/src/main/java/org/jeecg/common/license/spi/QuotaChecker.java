package org.jeecg.common.license.spi;

public interface QuotaChecker {
    String getQuotaKey();
    long getCurrentUsage();
    default String getUnit() { return ""; }
}
