package org.jeecg.modules.system.license;

import org.jeecg.common.license.spi.QuotaChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "license.enabled", havingValue = "true")
public class TenantQuotaChecker implements QuotaChecker {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public String getQuotaKey() {
        return "max_tenants";
    }

    @Override
    public long getCurrentUsage() {
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_tenant WHERE status != 0", Long.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
