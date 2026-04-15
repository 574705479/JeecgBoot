package org.jeecg.modules.airag.cs.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jeecg.cs.crypto")
public class CsCryptoConfig {

    private String storageKey;
    private String storageIv;
    private String transportKey;
    private String transportIv;
}
