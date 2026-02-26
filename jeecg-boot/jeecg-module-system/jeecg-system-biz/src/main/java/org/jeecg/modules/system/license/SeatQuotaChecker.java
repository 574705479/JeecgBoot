package org.jeecg.modules.system.license;

import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.license.spi.QuotaChecker;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.common.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Component
@ConditionalOnProperty(name = "license.enabled", havingValue = "true")
public class SeatQuotaChecker implements QuotaChecker {

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public String getQuotaKey() {
        return "max_seats";
    }

    @Override
    public long getCurrentUsage() {
        Collection<String> keys = redisUtil.scan(CommonConstant.PREFIX_USER_TOKEN + "*");
        Set<String> users = new HashSet<>();
        for (String key : keys) {
            String token = (String) redisUtil.get(key);
            if (token != null) {
                try {
                    String username = JwtUtil.getUsername(token);
                    if (username != null && !"_reserve_user_external".equals(username)) {
                        users.add(username);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return users.size();
    }

    @Override
    public String getUnit() {
        return "人";
    }
}
