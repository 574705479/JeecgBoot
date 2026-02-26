package com.license.server.dto;

import lombok.Data;

@Data
public class LicenseRequest {
    private String appId;
    private String licenseKey;
    private String timestamp;
    private String sign;
    private String callbackUrl;
}
