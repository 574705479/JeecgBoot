package org.jeecg.modules.airag.cs.license;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.license.core.LicenseInfo;
import org.jeecg.common.license.spi.LicenseEventListener;
import org.jeecg.modules.airag.cs.entity.CsDomainConfig;
import org.jeecg.modules.airag.cs.service.ICsDomainConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class CsLicenseDomainConfigSyncer implements LicenseEventListener {

    private static final String CONFIG_ID = "license_pushed";

    @Autowired
    private ICsDomainConfigService domainConfigService;

    private volatile String lastConfigHash = null;

    @Override
    public void onActivated(LicenseInfo info) {
        syncDomainConfig(info);
    }

    @Override
    public void onHeartbeatSuccess(LicenseInfo info) {
        syncDomainConfig(info);
    }

    private void syncDomainConfig(LicenseInfo info) {
        Map<String, Object> dc = info.getDomainConfig();
        if (dc == null) {
            return;
        }
        try {
            String domains = dc.get("domains") != null ? String.valueOf(dc.get("domains")) : "";
            Object linksObj = dc.get("downloadLinks");
            String downloadLinksJson = "";
            if (linksObj instanceof List) {
                List<?> linksList = (List<?>) linksObj;
                if (!linksList.isEmpty()) {
                    downloadLinksJson = JSON.toJSONString(linksList);
                }
            }

            String contentHash = Integer.toHexString(Objects.hash(domains, downloadLinksJson));
            if (contentHash.equals(lastConfigHash)) {
                return;
            }

            CsDomainConfig existing = domainConfigService.getById(CONFIG_ID);
            if (existing == null) {
                existing = new CsDomainConfig();
                existing.setId(CONFIG_ID);
                existing.setStatus(1);
                existing.setDelFlag(0);
                existing.setCreateBy("license-sync");
                existing.setCreateTime(new Date());
            }
            existing.setDomains(domains);
            existing.setDownloadLinks(downloadLinksJson);
            existing.setUpdateBy("license-sync");
            existing.setUpdateTime(new Date());

            domainConfigService.saveOrUpdate(existing);
            lastConfigHash = contentHash;
            log.info("[CS-License] 域名配置已同步");
        } catch (Exception e) {
            log.warn("[CS-License] 同步域名配置失败: {}", e.getMessage());
        }
    }
}
