package org.jeecg.common.license.core;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class LicenseInfo implements Serializable {
    private static final long serialVersionUID = 2L;

    private String licenseKey;
    private String appId;
    private Map<String, Long> quotas;
    private List<String> features;
    private String expireDate;
    private String status;
    private String clientIp;
    private String signature;

    private Map<String, String> quotaNames;
    private Map<String, String> featureNames;
    private String customerName;
    private String planName;
    private Map<String, Object> domainConfig;

    public String getLicenseKey() { return licenseKey; }
    public void setLicenseKey(String licenseKey) { this.licenseKey = licenseKey; }
    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public Map<String, Long> getQuotas() { return quotas; }
    public void setQuotas(Map<String, Long> quotas) { this.quotas = quotas; }
    public List<String> getFeatures() { return features; }
    public void setFeatures(List<String> features) { this.features = features; }
    public String getExpireDate() { return expireDate; }
    public void setExpireDate(String expireDate) { this.expireDate = expireDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public Map<String, String> getQuotaNames() { return quotaNames; }
    public void setQuotaNames(Map<String, String> quotaNames) { this.quotaNames = quotaNames; }
    public Map<String, String> getFeatureNames() { return featureNames; }
    public void setFeatureNames(Map<String, String> featureNames) { this.featureNames = featureNames; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public Map<String, Object> getDomainConfig() { return domainConfig; }
    public void setDomainConfig(Map<String, Object> domainConfig) { this.domainConfig = domainConfig; }
}
