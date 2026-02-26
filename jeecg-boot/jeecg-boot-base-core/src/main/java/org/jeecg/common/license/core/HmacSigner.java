package org.jeecg.common.license.core;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

public class HmacSigner {

    private final String appSecret;

    public HmacSigner(String appSecret) {
        this.appSecret = appSecret;
    }

    public String sign(String appId, String licenseKey, String timestamp) {
        return signRaw(appId + "\n" + licenseKey + "\n" + timestamp);
    }

    public String signRaw(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] result = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("[License] HMAC signing failed", e);
        }
    }

    public boolean verify(String payload, String expectedSign) {
        return signRaw(payload).equalsIgnoreCase(expectedSign);
    }
}
