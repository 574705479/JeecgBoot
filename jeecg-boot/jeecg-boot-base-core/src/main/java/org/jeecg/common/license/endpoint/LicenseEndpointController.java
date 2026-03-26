package org.jeecg.common.license.endpoint;

import org.jeecg.common.api.vo.Result;
import org.jeecg.common.license.core.HmacSigner;
import org.jeecg.common.license.core.LicenseClientService;
import org.jeecg.common.license.core.LicenseInfo;
import org.jeecg.common.license.core.LicenseKeyValidator;
import org.jeecg.config.shiro.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/license")
@ConditionalOnProperty(name = "license.enabled", havingValue = "true")
public class LicenseEndpointController {

    private static final Logger log = LoggerFactory.getLogger(LicenseEndpointController.class);

    private final LicenseClientService licenseClientService;
    private final HmacSigner hmacSigner;

    public LicenseEndpointController(LicenseClientService licenseClientService,
                                      HmacSigner hmacSigner) {
        this.licenseClientService = licenseClientService;
        this.hmacSigner = hmacSigner;
    }

    @IgnoreAuth
    @PostMapping("/activate")
    public Result<?> activate(@RequestBody Map<String, String> body) {
        String key = body.get("licenseKey");
        if (key == null || key.isBlank()) {
            return Result.error("请输入许可证密钥");
        }
        if (!LicenseKeyValidator.validate(key)) {
            return Result.error("许可证密钥格式无效");
        }
        try {
            LicenseInfo info = licenseClientService.activate(key);
            return Result.OK("激活成功", buildStatusMap(info));
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @IgnoreAuth
    @GetMapping("/status")
    public Result<?> status() {
        boolean licensed = licenseClientService.isLicensed();
        LicenseInfo info = licenseClientService.getLicenseInfo();
        if (!licensed) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("licensed", false);
            if (info != null) {
                data.put("licenseKey", maskKey(info.getLicenseKey()));
                data.put("status", info.getStatus());
                data.put("expireDate", info.getExpireDate());
            }
            return Result.OK(data);
        }
        Map<String, Object> data = buildStatusMap(info);
        data.put("licensed", true);
        return Result.OK(data);
    }

    @PostMapping("/refresh")
    public Result<?> refresh() {
        licenseClientService.heartbeat();
        boolean licensed = licenseClientService.isLicensed();
        LicenseInfo info = licenseClientService.getLicenseInfo();
        if (!licensed || info == null) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("licensed", false);
            return Result.OK("刷新完成", data);
        }
        Map<String, Object> data = buildStatusMap(info);
        data.put("licensed", true);
        return Result.OK("刷新完成", data);
    }

    @GetMapping("/plans")
    public Result<?> plans() {
        return Result.OK(licenseClientService.fetchPlans());
    }

    @PostMapping("/deactivate")
    public Result<?> deactivate() {
        licenseClientService.deactivate();
        return Result.OK("已注销授权");
    }

    @PostMapping("/callback")
    public Result<?> callback(@RequestBody Map<String, String> body) {
        String appId = body.get("appId");
        String licenseKey = body.get("licenseKey");
        String action = body.get("action");
        String timestamp = body.get("timestamp");
        String sign = body.get("sign");

        if (appId == null || licenseKey == null || action == null || timestamp == null || sign == null) {
            return Result.error("参数不完整");
        }

        // Verify HMAC signature
        String signPayload = appId + "\n" + licenseKey + "\n" + action + "\n" + timestamp;
        if (!hmacSigner.verify(signPayload, sign)) {
            log.warn("[License Callback] Signature verification failed");
            return Result.error("签名验证失败");
        }

        // Verify timestamp freshness (5 minutes)
        try {
            long ts = Long.parseLong(timestamp);
            long now = System.currentTimeMillis() / 1000;
            if (Math.abs(now - ts) > 300) {
                return Result.error("时间戳过期");
            }
        } catch (NumberFormatException e) {
            return Result.error("时间戳格式错误");
        }

        // Verify license key matches current license
        LicenseInfo current = licenseClientService.getLicenseInfo();
        if (current == null || !licenseKey.equals(current.getLicenseKey())) {
            return Result.error("许可证密钥不匹配");
        }

        log.info("[License Callback] Received status change: action={}, triggering immediate heartbeat", action);
        licenseClientService.heartbeat();
        return Result.OK("已处理");
    }

    private Map<String, Object> buildStatusMap(LicenseInfo info) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("licensed", true);
        data.put("licenseKey", maskKey(info.getLicenseKey()));
        data.put("appId", info.getAppId());
        data.put("quotas", info.getQuotas());
        data.put("features", info.getFeatures());
        data.put("expireDate", info.getExpireDate());
        data.put("status", info.getStatus());
        data.put("quotaNames", info.getQuotaNames());
        data.put("featureNames", info.getFeatureNames());
        data.put("customerName", info.getCustomerName());
        data.put("planName", info.getPlanName());
        return data;
    }

    private String maskKey(String key) {
        if (key == null || key.length() < 10) return "****";
        return key.substring(0, 8) + "****" + key.substring(key.length() - 4);
    }
}
