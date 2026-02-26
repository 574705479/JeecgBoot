package org.jeecg.common.license.core;

public class QuotaCheckResult {
    private boolean exceeded;
    private String quotaKey;
    private long currentUsage;
    private long limit;
    private String message;

    public boolean isExceeded() { return exceeded; }
    public void setExceeded(boolean exceeded) { this.exceeded = exceeded; }
    public String getQuotaKey() { return quotaKey; }
    public void setQuotaKey(String quotaKey) { this.quotaKey = quotaKey; }
    public long getCurrentUsage() { return currentUsage; }
    public void setCurrentUsage(long currentUsage) { this.currentUsage = currentUsage; }
    public long getLimit() { return limit; }
    public void setLimit(long limit) { this.limit = limit; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
