package org.jeecg.modules.system.license;

import org.jeecg.common.license.spi.QuotaChecker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "license.enabled", havingValue = "true")
public class UserQuotaChecker implements QuotaChecker {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public String getQuotaKey() {
        return "max_users";
    }

    @Override
    public long getCurrentUsage() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE del_flag = 0 AND status != 0", Long.class);
        return count != null ? count : 0;
    }

    @Override
    public String getUnit() {
        return "人";
    }
}
