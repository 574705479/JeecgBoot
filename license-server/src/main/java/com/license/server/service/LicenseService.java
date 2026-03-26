package com.license.server.service;

import com.license.server.config.LicenseServerProperties;
import com.license.server.dto.LicenseResponse;
import com.license.server.entity.App;
import com.license.server.entity.Customer;
import com.license.server.entity.License;
import com.license.server.entity.LicenseLog;
import com.license.server.entity.LicensePlan;
import com.license.server.repository.CustomerRepository;
import com.license.server.repository.LicenseLogRepository;
import com.license.server.repository.LicensePlanRepository;
import com.license.server.repository.LicenseRepository;
import com.license.server.util.CryptoUtil;
import com.license.server.util.IpUtil;
import com.license.server.util.LicenseKeyGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseService {

    private final LicenseRepository licenseRepository;
    private final LicenseLogRepository licenseLogRepository;
    private final AppService appService;
    private final CustomerRepository customerRepository;
    private final LicensePlanRepository licensePlanRepository;
    private final LicenseCallbackService callbackService;
    private final LicenseServerProperties properties;
    private final ObjectMapper objectMapper;

    public Page<License> list(int page, int size, Long appPk, Long customerId, String status, String keyword) {
        return licenseRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("delFlag"), 0));
            if (appPk != null) predicates.add(cb.equal(root.get("appPk"), appPk));
            if (customerId != null) predicates.add(cb.equal(root.get("customerId"), customerId));
            if (status != null && !status.isBlank()) predicates.add(cb.equal(root.get("status"), status));
            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(root.get("licenseKey"), "%" + keyword.trim() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createTime")));
    }

    public License getById(Long id) {
        return licenseRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("许可证不存在"));
    }

    @Transactional
    public License create(License license) {
        App app = appService.getById(license.getAppPk());
        String shortCode = app.getAppId().length() >= 4
                ? app.getAppId().substring(0, 4).toUpperCase()
                : app.getAppId().toUpperCase();
        license.setLicenseKey(LicenseKeyGenerator.generate(shortCode));
        license.setIssueDate(LocalDateTime.now());
        if (license.getAllowedIps() == null) {
            license.setAllowedIps(Collections.emptyList());
        }
        filterByDefinitions(license, app);
        License saved = licenseRepository.save(license);
        logAction(saved, "CREATE", null, null, "SUCCESS", "许可证创建");
        return saved;
    }

    @Transactional
    public License updateStatus(Long id, String status, Long operatorId) {
        License license = getById(id);
        String oldStatus = license.getStatus();

        switch (status) {
            case "SUSPENDED" -> {
                if (!"ACTIVE".equals(oldStatus)) throw new IllegalArgumentException("只能暂停激活状态的许可证");
            }
            case "REVOKED" -> {
                if (!"ACTIVE".equals(oldStatus) && !"SUSPENDED".equals(oldStatus))
                    throw new IllegalArgumentException("只能吊销激活或暂停状态的许可证");
            }
            case "ACTIVE" -> {
                if (!"SUSPENDED".equals(oldStatus) && !"EXPIRED".equals(oldStatus))
                    throw new IllegalArgumentException("只能恢复暂停或过期状态的许可证");
            }
            default -> throw new IllegalArgumentException("不支持的状态变更: " + status);
        }

        license.setStatus(status);
        licenseRepository.save(license);
        String actionName = switch (status) {
            case "ACTIVE" -> "RESTORE";
            case "SUSPENDED" -> "SUSPEND";
            case "REVOKED" -> "REVOKE";
            default -> status;
        };
        logAction(license, actionName, null, operatorId, "SUCCESS", oldStatus + " -> " + status);
        App app = appService.getById(license.getAppPk());
        callbackService.notifyStatusChange(license, app, status);
        return license;
    }

    @Transactional
    public License extend(Long id, LocalDateTime newExpireDate, Long operatorId) {
        License license = getById(id);
        if ("REVOKED".equals(license.getStatus())) {
            throw new IllegalArgumentException("已吊销的许可证不允许延期");
        }
        if ("INACTIVE".equals(license.getStatus())) {
            throw new IllegalArgumentException("未激活的许可证不允许延期");
        }
        String oldStatus = license.getStatus();
        license.setExpireDate(newExpireDate);
        if ("EXPIRED".equals(oldStatus)) {
            license.setStatus("ACTIVE");
        }
        licenseRepository.save(license);
        logAction(license, "EXTEND", null, operatorId, "SUCCESS", "延期至 " + newExpireDate);
        if ("EXPIRED".equals(oldStatus)) {
            App app = appService.getById(license.getAppPk());
            callbackService.notifyStatusChange(license, app, "ACTIVE");
        }
        return license;
    }

    @Transactional
    public void softDelete(Long id, Long operatorId) {
        License license = getById(id);
        license.setDelFlag(1);
        licenseRepository.save(license);
        logAction(license, "DELETE", null, operatorId, "SUCCESS", "软删除");
    }

    @Transactional
    public License updateIps(Long id, List<String> ips, Long operatorId) {
        License license = getById(id);
        String oldIps = license.getAllowedIps() != null ? license.getAllowedIps().toString() : "[]";
        license.setAllowedIps(ips != null ? ips : Collections.emptyList());
        licenseRepository.save(license);
        logAction(license, "UPDATE_IPS", null, operatorId, "SUCCESS", oldIps + " -> " + license.getAllowedIps());
        return license;
    }

    @SuppressWarnings("unchecked")
    @Transactional
    public License updateContent(Long id, Map<String, Object> fields, Long operatorId) {
        License license = getById(id);
        if ("REVOKED".equals(license.getStatus())) {
            throw new IllegalArgumentException("已吊销的许可证不允许编辑");
        }

        StringBuilder diff = new StringBuilder();

        if (fields.containsKey("quotas")) {
            String oldVal = license.getQuotas() != null ? license.getQuotas().toString() : "{}";
            license.setQuotas((Map<String, Object>) fields.get("quotas"));
            diff.append("quotas: ").append(oldVal).append(" -> ").append(license.getQuotas()).append("; ");
        }
        if (fields.containsKey("features")) {
            String oldVal = license.getFeatures() != null ? license.getFeatures().toString() : "[]";
            license.setFeatures((List<String>) fields.get("features"));
            diff.append("features: ").append(oldVal).append(" -> ").append(license.getFeatures()).append("; ");
        }
        if (fields.containsKey("planId")) {
            Long oldVal = license.getPlanId();
            Object planIdObj = fields.get("planId");
            Long newPlanId = planIdObj != null ? ((Number) planIdObj).longValue() : null;
            license.setPlanId(newPlanId);
            diff.append("planId: ").append(oldVal).append(" -> ").append(newPlanId).append("; ");
        }
        if (fields.containsKey("allowedIps")) {
            String oldVal = license.getAllowedIps() != null ? license.getAllowedIps().toString() : "[]";
            List<String> ips = (List<String>) fields.get("allowedIps");
            license.setAllowedIps(ips != null ? ips : Collections.emptyList());
            diff.append("allowedIps: ").append(oldVal).append(" -> ").append(license.getAllowedIps()).append("; ");
        }
        if (fields.containsKey("remark")) {
            String oldVal = license.getRemark();
            license.setRemark((String) fields.get("remark"));
            diff.append("remark: ").append(oldVal).append(" -> ").append(license.getRemark()).append("; ");
        }
        if (fields.containsKey("domainConfig")) {
            license.setDomainConfig((Map<String, Object>) fields.get("domainConfig"));
            diff.append("domainConfig: updated; ");
        }

        App app = appService.getById(license.getAppPk());
        filterByDefinitions(license, app);
        licenseRepository.save(license);
        logAction(license, "UPDATE_CONTENT", null, operatorId, "SUCCESS", diff.toString());
        callbackService.notifyStatusChange(license, app, "CONTENT_UPDATED");
        return license;
    }

    @Transactional
    public com.license.server.dto.Result<LicenseResponse> activate(String appIdStr, String licenseKey, String clientIp, String callbackUrl) {
        App app = appService.getByAppId(appIdStr);
        if (app == null) return com.license.server.dto.Result.error(40001, "应用不存在");
        if (app.getStatus() != 1) return com.license.server.dto.Result.error(40006, "应用已停用");

        License license = licenseRepository.findByLicenseKeyAndDelFlag(licenseKey, 0).orElse(null);
        if (license == null) {
            logAction(null, "ACTIVATE", clientIp, null, "FAILED", "密钥无效: " + licenseKey);
            return com.license.server.dto.Result.error(40001, "许可证密钥无效");
        }
        if (!license.getAppPk().equals(app.getId())) {
            return com.license.server.dto.Result.error(40001, "许可证密钥无效");
        }

        // Idempotent: already active
        if ("ACTIVE".equals(license.getStatus())) {
            if (!IpUtil.isIpAllowed(clientIp, license.getAllowedIps())) {
                logAction(license, "ACTIVATE", clientIp, null, "FAILED", "IP不在白名单");
                return com.license.server.dto.Result.error(40005, "IP不在白名单");
            }
            if (callbackUrl != null && !callbackUrl.isBlank()
                    && !callbackUrl.equals(license.getCallbackUrl())) {
                license.setCallbackUrl(callbackUrl);
                licenseRepository.save(license);
            }
            return buildSuccessResponse(license, app, clientIp);
        }

        if (!"INACTIVE".equals(license.getStatus())) {
            return mapStatusToError(license.getStatus());
        }

        if (!IpUtil.isIpAllowed(clientIp, license.getAllowedIps())) {
            logAction(license, "ACTIVATE", clientIp, null, "FAILED", "IP不在白名单");
            return com.license.server.dto.Result.error(40005, "IP不在白名单");
        }

        license.setStatus("ACTIVE");
        license.setActivatedAt(LocalDateTime.now());
        license.setActivatedIp(clientIp);
        license.setLastHeartbeat(LocalDateTime.now());
        if (callbackUrl != null && !callbackUrl.isBlank()) {
            license.setCallbackUrl(callbackUrl);
        }
        licenseRepository.save(license);
        logAction(license, "ACTIVATE", clientIp, null, "SUCCESS", "激活成功");
        return buildSuccessResponse(license, app, clientIp);
    }

    @Transactional
    public com.license.server.dto.Result<LicenseResponse> verify(String appIdStr, String licenseKey, String clientIp, String callbackUrl) {
        App app = appService.getByAppId(appIdStr);
        if (app == null) return com.license.server.dto.Result.error(40001, "应用不存在");
        if (app.getStatus() != 1) return com.license.server.dto.Result.error(40006, "应用已停用");

        License license = licenseRepository.findByLicenseKeyAndDelFlag(licenseKey, 0).orElse(null);
        if (license == null) return com.license.server.dto.Result.error(40001, "许可证密钥无效");
        if (!license.getAppPk().equals(app.getId())) return com.license.server.dto.Result.error(40001, "许可证密钥无效");

        if (!"ACTIVE".equals(license.getStatus())) {
            boolean shouldLog = properties.getLog().isHeartbeatSuccess() || !"ACTIVE".equals(license.getStatus());
            if (shouldLog) logAction(license, "HEARTBEAT", clientIp, null, "FAILED", "状态: " + license.getStatus());
            return mapStatusToError(license.getStatus());
        }

        if (license.getExpireDate() != null && license.getExpireDate().isBefore(LocalDateTime.now())) {
            license.setStatus("EXPIRED");
            licenseRepository.save(license);
            logAction(license, "HEARTBEAT", clientIp, null, "FAILED", "已过期");
            return com.license.server.dto.Result.error(40002, "许可证已过期");
        }

        if (!IpUtil.isIpAllowed(clientIp, license.getAllowedIps())) {
            logAction(license, "HEARTBEAT", clientIp, null, "FAILED", "IP不在白名单");
            return com.license.server.dto.Result.error(40005, "IP不在白名单");
        }

        license.setLastHeartbeat(LocalDateTime.now());
        if (callbackUrl != null && !callbackUrl.isBlank()
                && !callbackUrl.equals(license.getCallbackUrl())) {
            license.setCallbackUrl(callbackUrl);
        }
        licenseRepository.save(license);

        if (properties.getLog().isHeartbeatSuccess()) {
            logAction(license, "HEARTBEAT", clientIp, null, "SUCCESS", "心跳成功");
        }

        return buildSuccessResponse(license, app, clientIp);
    }

    @Transactional
    public void deactivate(String appIdStr, String licenseKey, String clientIp) {
        License license = licenseRepository.findByLicenseKeyAndDelFlag(licenseKey, 0).orElse(null);
        if (license != null) {
            logAction(license, "DEACTIVATE", clientIp, null, "SUCCESS", "客户端主动注销");
        }
    }

    private com.license.server.dto.Result<LicenseResponse> buildSuccessResponse(License license, App app, String clientIp) {
        try {
            String privatePem = appService.decryptPrivateKey(app);
            java.security.PrivateKey privateKey = CryptoUtil.pemToPrivateKey(privatePem);

            Map<String, String> quotaNames = new LinkedHashMap<>();
            if (app.getQuotasDef() != null) {
                for (Map<String, Object> def : app.getQuotasDef()) {
                    String code = (String) def.get("code");
                    String name = (String) def.get("name");
                    if (code != null && name != null) quotaNames.put(code, name);
                }
            }
            Map<String, String> featureNames = new LinkedHashMap<>();
            if (app.getFeaturesDef() != null) {
                for (Map<String, Object> def : app.getFeaturesDef()) {
                    String code = (String) def.get("code");
                    String name = (String) def.get("name");
                    if (code != null && name != null) featureNames.put(code, name);
                }
            }

            Set<String> validQuotaCodes = quotaNames.keySet();
            Map<String, Object> filteredQuotas = new LinkedHashMap<>();
            if (license.getQuotas() != null) {
                license.getQuotas().forEach((k, v) -> {
                    if (validQuotaCodes.contains(k)) filteredQuotas.put(k, v);
                });
            }
            Set<String> validFeatureCodes = featureNames.keySet();
            List<String> filteredFeatures = new ArrayList<>();
            if (license.getFeatures() != null) {
                for (String f : license.getFeatures()) {
                    if (validFeatureCodes.contains(f)) filteredFeatures.add(f);
                }
            }

            String quotasJson = objectMapper.writeValueAsString(new TreeMap<>(filteredQuotas));
            List<String> sortedFeatures = new ArrayList<>(filteredFeatures);
            Collections.sort(sortedFeatures);
            String featuresJson = objectMapper.writeValueAsString(sortedFeatures);
            String expireDateStr = license.getExpireDate() != null
                    ? license.getExpireDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "";

            String signedPayload = String.join("|",
                    license.getLicenseKey(),
                    app.getAppId(),
                    quotasJson,
                    featuresJson,
                    expireDateStr,
                    license.getStatus());

            String signature = CryptoUtil.rsaSign(signedPayload, privateKey);

            String custName = null;
            if (license.getCustomerId() != null) {
                custName = customerRepository.findById(license.getCustomerId())
                        .map(Customer::getCustomerName).orElse(null);
            }
            String pName = null;
            if (license.getPlanId() != null) {
                pName = licensePlanRepository.findById(license.getPlanId())
                        .map(LicensePlan::getPlanName).orElse(null);
            }

            LicenseResponse response = LicenseResponse.builder()
                    .licenseKey(license.getLicenseKey())
                    .appId(app.getAppId())
                    .quotas(filteredQuotas)
                    .features(filteredFeatures)
                    .expireDate(expireDateStr)
                    .status(license.getStatus())
                    .clientIp(clientIp)
                    .signature(signature)
                    .quotaNames(quotaNames)
                    .featureNames(featureNames)
                    .customerName(custName)
                    .planName(pName)
                    .domainConfig(license.getDomainConfig())
                    .build();

            return com.license.server.dto.Result.ok(response);
        } catch (Exception e) {
            log.error("Failed to build license response", e);
            return com.license.server.dto.Result.error(50000, "服务端内部错误");
        }
    }

    private com.license.server.dto.Result<LicenseResponse> mapStatusToError(String status) {
        return switch (status) {
            case "EXPIRED" -> com.license.server.dto.Result.error(40002, "许可证已过期");
            case "REVOKED" -> com.license.server.dto.Result.error(40003, "许可证已吊销");
            case "SUSPENDED" -> com.license.server.dto.Result.error(40004, "许可证已暂停");
            case "INACTIVE" -> com.license.server.dto.Result.error(40001, "许可证未激活");
            default -> com.license.server.dto.Result.error(40001, "许可证状态异常");
        };
    }

    @SuppressWarnings("unchecked")
    private void filterByDefinitions(License license, App app) {
        Set<String> validQuotaCodes = (app.getQuotasDef() != null)
                ? app.getQuotasDef().stream().map(d -> (String) d.get("code")).collect(Collectors.toSet())
                : Collections.emptySet();
        Set<String> validFeatureCodes = (app.getFeaturesDef() != null)
                ? app.getFeaturesDef().stream().map(d -> (String) d.get("code")).collect(Collectors.toSet())
                : Collections.emptySet();
        if (license.getQuotas() != null) {
            license.getQuotas().keySet().retainAll(validQuotaCodes);
        }
        if (license.getFeatures() != null) {
            license.getFeatures().removeIf(f -> !validFeatureCodes.contains(f));
        }
    }

    private void logAction(License license, String action, String clientIp, Long operatorId, String result, String message) {
        LicenseLog logEntry = new LicenseLog();
        if (license != null) {
            logEntry.setLicenseId(license.getId());
            logEntry.setAppPk(license.getAppPk());
        }
        logEntry.setAction(action);
        logEntry.setClientIp(clientIp);
        logEntry.setOperatorId(operatorId);
        logEntry.setResult(result);
        logEntry.setMessage(message);
        licenseLogRepository.save(logEntry);
    }

    public Map<String, Object> getStats(Long appPk) {
        Map<String, Object> stats = new HashMap<>();
        List<Object[]> grouped;
        if (appPk != null) {
            grouped = licenseRepository.countByAppPkGroupByStatus(appPk);
        } else {
            grouped = licenseRepository.countGroupByStatus();
        }
        long total = 0;
        Map<String, Long> statusCounts = new HashMap<>();
        for (Object[] row : grouped) {
            String s = (String) row[0];
            Long c = (Long) row[1];
            statusCounts.put(s, c);
            total += c;
        }
        stats.put("total", total);
        stats.put("byStatus", statusCounts);
        stats.put("expiringSoon", licenseRepository.findExpiringLicenses(LocalDateTime.now(), LocalDateTime.now().plusDays(7)).size());
        stats.put("heartbeatLost", licenseRepository.findHeartbeatLostLicenses(LocalDateTime.now().minusHours(24)).size());
        return stats;
    }

    public List<License> getExpiringLicenses() {
        return licenseRepository.findExpiringLicenses(LocalDateTime.now(), LocalDateTime.now().plusDays(7));
    }

    public List<License> getHeartbeatLostLicenses() {
        return licenseRepository.findHeartbeatLostLicenses(LocalDateTime.now().minusHours(24));
    }

    @SuppressWarnings("unchecked")
    public com.license.server.dto.Result<Map<String, Object>> getDomainsByKey(String licenseKey) {
        License license = licenseRepository.findByLicenseKeyAndDelFlag(licenseKey, 0).orElse(null);
        if (license == null) {
            return com.license.server.dto.Result.error(40001, "许可证密钥无效");
        }

        String status = license.getStatus();
        if ("REVOKED".equals(status)) {
            return com.license.server.dto.Result.error(40003, "许可证已吊销");
        }
        if ("EXPIRED".equals(status)) {
            if (license.getExpireDate() != null && license.getExpireDate().isBefore(LocalDateTime.now())) {
                return com.license.server.dto.Result.error(40002, "许可证已过期");
            }
        }
        if ("SUSPENDED".equals(status)) {
            return com.license.server.dto.Result.error(40004, "许可证已暂停");
        }

        Map<String, Object> domainConfig = license.getDomainConfig();
        String domains = "";
        if (domainConfig != null && domainConfig.get("domains") != null) {
            domains = String.valueOf(domainConfig.get("domains"));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", status);
        result.put("domains", domains);
        return com.license.server.dto.Result.ok(result);
    }
}
