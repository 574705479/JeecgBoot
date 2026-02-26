package com.license.server.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class LicenseResponse {
    private String licenseKey;
    private String appId;
    private Map<String, Object> quotas;
    private List<String> features;
    private String expireDate;
    private String status;
    private String clientIp;
    private String signature;

    private Map<String, String> quotaNames;
    private Map<String, String> featureNames;
    private String customerName;
    private String planName;
}
