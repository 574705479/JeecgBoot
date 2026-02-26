package org.jeecg.common.license.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.common.license.config.LicenseProperties;
import org.jeecg.common.license.spi.LicenseEventListener;
import org.jeecg.common.license.spi.QuotaChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.*;

public class LicenseClientService {

    private static final Logger log = LoggerFactory.getLogger(LicenseClientService.class);

    private volatile LicenseState state = LicenseState.EMPTY;

    private final LicenseProperties properties;
    private final RsaVerifier rsaVerifier;
    private final HmacSigner hmacSigner;
    private final LicenseCacheManager cacheManager;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final List<QuotaChecker> quotaCheckers;
    private final List<LicenseEventListener> eventListeners;

    public LicenseClientService(LicenseProperties properties,
                                 RsaVerifier rsaVerifier,
                                 HmacSigner hmacSigner,
                                 LicenseCacheManager cacheManager,
                                 ObjectMapper objectMapper,
                                 List<QuotaChecker> quotaCheckers,
                                 List<LicenseEventListener> eventListeners) {
        this.properties = properties;
        this.rsaVerifier = rsaVerifier;
        this.hmacSigner = hmacSigner;
        this.cacheManager = cacheManager;
        this.objectMapper = objectMapper;
        this.quotaCheckers = quotaCheckers != null ? quotaCheckers : Collections.emptyList();
        this.eventListeners = eventListeners != null ? eventListeners : Collections.emptyList();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeout());
        factory.setReadTimeout(properties.getReadTimeout());
        this.restTemplate = new RestTemplate(factory);

        init();
    }

    private void init() {
        LicenseInfo cached = cacheManager.load();
        if (cached != null) {
            if (verifySignature(cached)) {
                this.state = new LicenseState(cached, System.currentTimeMillis(), 0, true);
                log.info("[License] Loaded from cache, key={}", cached.getLicenseKey());
            } else {
                log.warn("[License] Cache signature invalid, clearing");
                cacheManager.clear();
            }
        }

        String configKey = properties.getLicenseKey();
        if (configKey != null && !configKey.isBlank() && !isLicensed()) {
            try {
                activate(configKey);
                log.info("[License] Auto-activated with configured key");
            } catch (Exception e) {
                log.warn("[License] Auto-activation failed: {}", e.getMessage());
            }
        }
    }

    public boolean isLicensed() {
        LicenseState s = this.state;
        if (!s.licensed()) return false;
        if (s.heartbeatFailCount() > 0) {
            long elapsed = System.currentTimeMillis() - s.lastVerifyTime();
            return elapsed < (long) properties.getGracePeriod() * 1000;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public LicenseInfo activate(String licenseKey) {
        if (!LicenseKeyValidator.validate(licenseKey)) {
            throw new IllegalArgumentException("许可证密钥格式无效");
        }

        Map<String, Object> response = callServer("/license/activate", licenseKey);
        int code = ((Number) response.get("code")).intValue();

        if (code != 200) {
            String msg = (String) response.getOrDefault("message", "激活失败");
            throw new RuntimeException(msg);
        }

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        LicenseInfo info = mapToLicenseInfo(data);

        if (!verifySignature(info)) {
            throw new RuntimeException("服务端签名验证失败");
        }

        this.state = new LicenseState(info, System.currentTimeMillis(), 0, true);
        cacheManager.save(info);
        eventListeners.forEach(l -> l.onActivated(info));
        log.info("[License] Activated successfully, key={}", licenseKey);
        return info;
    }

    @SuppressWarnings("unchecked")
    public void heartbeat() {
        LicenseState current = this.state;
        if (current.license() == null) return;

        try {
            Map<String, Object> response = callServer("/license/verify", current.license().getLicenseKey());
            int code = ((Number) response.get("code")).intValue();

            if (code == 200) {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                LicenseInfo info = mapToLicenseInfo(data);
                if (verifySignature(info)) {
                    this.state = new LicenseState(info, System.currentTimeMillis(), 0, true);
                    cacheManager.save(info);
                    eventListeners.forEach(l -> l.onHeartbeatSuccess(info));
                    log.info("[License] Heartbeat success");
                } else {
                    log.error("[License] Heartbeat response signature invalid");
                    incrementFailCount(current);
                }
            } else {
                handleHeartbeatError(code, current);
            }
        } catch (Exception e) {
            log.warn("[License] Heartbeat failed: {}", e.getMessage());
            incrementFailCount(current);
        }
    }

    public void deactivate() {
        LicenseState current = this.state;
        if (current.license() != null) {
            try {
                callServer("/license/deactivate", current.license().getLicenseKey());
            } catch (Exception e) {
                log.warn("[License] Deactivate notify failed: {}", e.getMessage());
            }
        }
        this.state = LicenseState.EMPTY;
        cacheManager.clear();
        log.info("[License] Deactivated");
    }

    public Long getQuotaLimit(String quotaKey) {
        LicenseState s = this.state;
        if (s.license() == null || s.license().getQuotas() == null) return null;
        return s.license().getQuotas().get(quotaKey);
    }

    public boolean isFeatureEnabled(String featureCode) {
        LicenseState s = this.state;
        if (s.license() == null || s.license().getFeatures() == null) return false;
        return s.license().getFeatures().contains(featureCode);
    }

    public boolean isQuotaExceeded(String quotaKey) {
        Long limit = getQuotaLimit(quotaKey);
        if (limit == null || limit <= 0) return false;
        for (QuotaChecker checker : quotaCheckers) {
            if (checker.getQuotaKey().equals(quotaKey)) {
                return checker.getCurrentUsage() >= limit;
            }
        }
        return false;
    }

    public QuotaCheckResult checkAllQuotas() {
        LicenseState s = this.state;
        if (s.license() == null || s.license().getQuotas() == null) return null;

        for (QuotaChecker checker : quotaCheckers) {
            String key = checker.getQuotaKey();
            Long limit = s.license().getQuotas().get(key);
            if (limit != null && limit > 0) {
                long usage = checker.getCurrentUsage();
                if (usage >= limit) {
                    QuotaCheckResult result = new QuotaCheckResult();
                    result.setExceeded(true);
                    result.setQuotaKey(key);
                    result.setCurrentUsage(usage);
                    result.setLimit(limit);
                    result.setMessage(key + "已达上限(" + usage + "/" + limit + ")");
                    return result;
                }
            }
        }
        return null;
    }

    public LicenseInfo getLicenseInfo() {
        return this.state.license();
    }

    public LicenseState getState() {
        return this.state;
    }

    private void handleHeartbeatError(int code, LicenseState current) {
        switch (code) {
            case 40001 -> {
                this.state = LicenseState.EMPTY;
                cacheManager.clear();
                eventListeners.forEach(LicenseEventListener::onLicenseInvalid);
            }
            case 40002 -> {
                LicenseInfo info = current.license();
                if (info != null) info.setStatus("EXPIRED");
                this.state = new LicenseState(info, current.lastVerifyTime(), current.heartbeatFailCount(), false);
                if (info != null) cacheManager.save(info);
                eventListeners.forEach(LicenseEventListener::onLicenseExpired);
            }
            case 40003, 40004 -> {
                LicenseInfo info = current.license();
                if (info != null) info.setStatus(code == 40003 ? "REVOKED" : "SUSPENDED");
                this.state = new LicenseState(info, current.lastVerifyTime(), current.heartbeatFailCount(), false);
                if (info != null) cacheManager.save(info);
                eventListeners.forEach(LicenseEventListener::onLicenseInvalid);
            }
            default -> incrementFailCount(current);
        }
    }

    private void incrementFailCount(LicenseState current) {
        int newCount = current.heartbeatFailCount() + 1;
        this.state = new LicenseState(current.license(), current.lastVerifyTime(), newCount, current.licensed());
        eventListeners.forEach(l -> l.onHeartbeatFailed(newCount));

        long elapsed = System.currentTimeMillis() - current.lastVerifyTime();
        if (elapsed >= (long) properties.getGracePeriod() * 1000) {
            eventListeners.forEach(LicenseEventListener::onGracePeriodEntered);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callServer(String path, String licenseKey) {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String sign = hmacSigner.sign(properties.getAppId(), licenseKey, timestamp);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("appId", properties.getAppId());
        body.put("licenseKey", licenseKey);
        body.put("timestamp", timestamp);
        body.put("sign", sign);
        if (properties.getCallbackUrl() != null && !properties.getCallbackUrl().isBlank()) {
            body.put("callbackUrl", properties.getCallbackUrl());
        }

        String url = properties.getServerUrl() + "/api/" + properties.getApiVersion() + path;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
            return resp.getBody() != null ? resp.getBody() : Collections.singletonMap("code", 50000);
        } catch (Exception e) {
            log.error("[License] Server call failed: {} {}", url, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("code", 50000);
            error.put("message", "无法连接授权服务器: " + e.getMessage());
            return error;
        }
    }

    @SuppressWarnings("unchecked")
    private LicenseInfo mapToLicenseInfo(Map<String, Object> data) {
        LicenseInfo info = new LicenseInfo();
        info.setLicenseKey((String) data.get("licenseKey"));
        info.setAppId((String) data.get("appId"));
        info.setExpireDate((String) data.get("expireDate"));
        info.setStatus((String) data.get("status"));
        info.setClientIp((String) data.get("clientIp"));
        info.setSignature((String) data.get("signature"));

        Object quotasObj = data.get("quotas");
        if (quotasObj instanceof Map) {
            Map<String, Long> quotas = new LinkedHashMap<>();
            ((Map<String, Object>) quotasObj).forEach((k, v) -> {
                if (v instanceof Number) {
                    quotas.put(k, ((Number) v).longValue());
                }
            });
            info.setQuotas(quotas);
        }

        Object featuresObj = data.get("features");
        if (featuresObj instanceof List) {
            info.setFeatures((List<String>) featuresObj);
        }

        Object quotaNamesObj = data.get("quotaNames");
        if (quotaNamesObj instanceof Map) {
            Map<String, String> qn = new LinkedHashMap<>();
            ((Map<String, Object>) quotaNamesObj).forEach((k, v) -> {
                if (v != null) qn.put(k, String.valueOf(v));
            });
            info.setQuotaNames(qn);
        }
        Object featureNamesObj = data.get("featureNames");
        if (featureNamesObj instanceof Map) {
            Map<String, String> fn = new LinkedHashMap<>();
            ((Map<String, Object>) featureNamesObj).forEach((k, v) -> {
                if (v != null) fn.put(k, String.valueOf(v));
            });
            info.setFeatureNames(fn);
        }
        info.setCustomerName((String) data.get("customerName"));
        info.setPlanName((String) data.get("planName"));

        return info;
    }

    private boolean verifySignature(LicenseInfo info) {
        if (info == null || info.getSignature() == null) return false;
        try {
            Map<String, Long> quotas = info.getQuotas() != null ? new TreeMap<>(info.getQuotas()) : new TreeMap<>();
            List<String> features = info.getFeatures() != null ? new ArrayList<>(info.getFeatures()) : new ArrayList<>();
            Collections.sort(features);

            String quotasJson = objectMapper.writeValueAsString(quotas);
            String featuresJson = objectMapper.writeValueAsString(features);

            String signedPayload = String.join("|",
                    info.getLicenseKey(),
                    info.getAppId(),
                    quotasJson,
                    featuresJson,
                    info.getExpireDate() != null ? info.getExpireDate() : "",
                    info.getStatus());

            return rsaVerifier.verify(signedPayload, info.getSignature());
        } catch (Exception e) {
            log.error("[License] Signature verification error", e);
            return false;
        }
    }
}
