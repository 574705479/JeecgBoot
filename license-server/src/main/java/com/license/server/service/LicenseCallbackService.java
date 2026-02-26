package com.license.server.service;

import com.license.server.entity.App;
import com.license.server.entity.License;
import com.license.server.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseCallbackService {

    private final RestTemplate callbackRestTemplate = createRestTemplate();

    public void notifyStatusChange(License license, App app, String action) {
        String url = license.getCallbackUrl();
        if (url == null || url.isBlank()) return;

        CompletableFuture.runAsync(() -> {
            try {
                String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
                String signPayload = app.getAppId() + "\n" + license.getLicenseKey()
                        + "\n" + action + "\n" + timestamp;
                String sign = CryptoUtil.hmacSha256(signPayload, app.getAppSecret());

                Map<String, String> body = new LinkedHashMap<>();
                body.put("appId", app.getAppId());
                body.put("licenseKey", license.getLicenseKey());
                body.put("action", action);
                body.put("timestamp", timestamp);
                body.put("sign", sign);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

                ResponseEntity<String> resp = callbackRestTemplate.exchange(
                        url, HttpMethod.POST, entity, String.class);
                log.info("[Callback] Notified {} action={} status={}", url, action, resp.getStatusCode());
            } catch (Exception e) {
                log.warn("[Callback] Failed to notify {}: {}", url, e.getMessage());
            }
        });
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }
}
