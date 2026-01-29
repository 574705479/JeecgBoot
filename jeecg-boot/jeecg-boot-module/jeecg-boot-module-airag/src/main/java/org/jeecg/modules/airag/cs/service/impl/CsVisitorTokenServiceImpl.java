package org.jeecg.modules.airag.cs.service.impl;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.CommonAPI;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.cs.entity.CsGlobalConfig;
import org.jeecg.modules.airag.cs.mapper.CsGlobalConfigMapper;
import org.jeecg.modules.airag.cs.service.ICsVisitorTokenService;
import org.jeecg.modules.airag.cs.vo.CsVisitorTokenPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 访客短时token服务实现
 */
@Slf4j
@Service
public class CsVisitorTokenServiceImpl implements ICsVisitorTokenService {

    private static final String VISITOR_APP_REDIS_KEY = "cs:global:visitor_app_id";
    private static final String VISITOR_APP_CONFIG_KEY = "visitor_app_id";
    private static final String VISITOR_TOKEN_PREFIX = "cs:visitor:token:";
    private static final String VISITOR_SESSION_PREFIX = "cs:visitor:session:";
    private static final String VISITOR_BLACKLIST_KEY = "cs:visitor:blacklist";
    private static final String VISITOR_IP_BLACKLIST_KEY = "cs:visitor:ip:blacklist";
    private static final String VISITOR_TOKEN_RATE_PREFIX = "cs:visitor:token:rate:";
    private static final int DEFAULT_TOKEN_EXPIRE_MINUTES = 10;
    private static final int SESSION_TOKEN_EXPIRE_DAYS = 30;
    private static final int TOKEN_RATE_LIMIT_PER_MINUTE = 30;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private CsGlobalConfigMapper csGlobalConfigMapper;

    @Autowired
    private CommonAPI commonAPI;

    @Override
    public CsVisitorTokenPayload issueToken(String externalUserId, String userName, String source) {
        if (oConvertUtils.isEmpty(externalUserId)) {
            return null;
        }
        String sourceKey = oConvertUtils.isNotEmpty(source) ? source : "default";
        String normalizedUserId = sourceKey + ":" + externalUserId;
        if (isBlacklisted(normalizedUserId)) {
            return null;
        }
        String appId = getGlobalVisitorAppId();
        if (oConvertUtils.isEmpty(appId)) {
            return null;
        }

        int expireMinutes = DEFAULT_TOKEN_EXPIRE_MINUTES;

        String token = UUID.randomUUID().toString().replace("-", "");
        long expireAt = System.currentTimeMillis() + expireMinutes * 60L * 1000L;

        JSONObject payload = new JSONObject();
        payload.put("externalUserId", normalizedUserId);
        payload.put("userName", userName);
        payload.put("appId", appId);
        payload.put("expireAt", expireAt);
        payload.put("source", sourceKey);

        String key = VISITOR_TOKEN_PREFIX + token;
        redisTemplate.opsForValue().set(key, payload.toJSONString(), expireMinutes, TimeUnit.MINUTES);

        CsVisitorTokenPayload result = new CsVisitorTokenPayload();
        result.setToken(token);
        result.setExternalUserId(normalizedUserId);
        result.setUserName(userName);
        result.setAppId(appId);
        result.setExpireAt(expireAt);
        result.setSource(sourceKey);
        return result;
    }

    @Override
    public CsVisitorTokenPayload issueSessionToken(String externalUserId, String userName, String source) {
        if (oConvertUtils.isEmpty(externalUserId)) {
            return null;
        }
        String sourceKey = oConvertUtils.isNotEmpty(source) ? source : "default";
        String normalizedUserId = sourceKey + ":" + externalUserId;
        if (isBlacklisted(normalizedUserId)) {
            return null;
        }
        String appId = getGlobalVisitorAppId();
        if (oConvertUtils.isEmpty(appId)) {
            return null;
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        long expireAt = System.currentTimeMillis() + SESSION_TOKEN_EXPIRE_DAYS * 24L * 60L * 60L * 1000L;

        JSONObject payload = new JSONObject();
        payload.put("externalUserId", normalizedUserId);
        payload.put("userName", userName);
        payload.put("appId", appId);
        payload.put("expireAt", expireAt);
        payload.put("source", sourceKey);

        String key = VISITOR_SESSION_PREFIX + token;
        redisTemplate.opsForValue().set(key, payload.toJSONString(), SESSION_TOKEN_EXPIRE_DAYS, TimeUnit.DAYS);

        CsVisitorTokenPayload result = new CsVisitorTokenPayload();
        result.setToken(token);
        result.setExternalUserId(normalizedUserId);
        result.setUserName(userName);
        result.setAppId(appId);
        result.setExpireAt(expireAt);
        result.setSource(sourceKey);
        return result;
    }

    @Override
    public CsVisitorTokenPayload parseToken(String token) {
        if (oConvertUtils.isEmpty(token)) {
            return null;
        }
        String key = VISITOR_TOKEN_PREFIX + token;
        String json = redisTemplate.opsForValue().get(key);
        if (oConvertUtils.isEmpty(json)) {
            return null;
        }
        try {
            JSONObject obj = JSONObject.parseObject(json);
            if (obj == null) {
                return null;
            }
            Long expireAt = obj.getLong("expireAt");
            if (expireAt != null && expireAt < System.currentTimeMillis()) {
                return null;
            }
            CsVisitorTokenPayload payload = new CsVisitorTokenPayload();
            payload.setToken(token);
            payload.setExternalUserId(obj.getString("externalUserId"));
            payload.setUserName(obj.getString("userName"));
            payload.setAppId(obj.getString("appId"));
            payload.setExpireAt(expireAt);
            payload.setSource(obj.getString("source"));
            return payload;
        } catch (Exception e) {
            log.warn("[CS-VisitorToken] 解析token失败", e);
            return null;
        }
    }

    @Override
    public CsVisitorTokenPayload parseSessionToken(String token) {
        if (oConvertUtils.isEmpty(token)) {
            return null;
        }
        String key = VISITOR_SESSION_PREFIX + token;
        String json = redisTemplate.opsForValue().get(key);
        if (oConvertUtils.isEmpty(json)) {
            return null;
        }
        try {
            JSONObject obj = JSONObject.parseObject(json);
            if (obj == null) {
                return null;
            }
            Long expireAt = obj.getLong("expireAt");
            if (expireAt != null && expireAt < System.currentTimeMillis()) {
                return null;
            }
            CsVisitorTokenPayload payload = new CsVisitorTokenPayload();
            payload.setToken(token);
            payload.setExternalUserId(obj.getString("externalUserId"));
            payload.setUserName(obj.getString("userName"));
            payload.setAppId(obj.getString("appId"));
            payload.setExpireAt(expireAt);
            payload.setSource(obj.getString("source"));
            return payload;
        } catch (Exception e) {
            log.warn("[CS-VisitorToken] 解析sessionToken失败", e);
            return null;
        }
    }

    @Override
    public String extractToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String token = request.getHeader("X-Visitor-Token");
        if (oConvertUtils.isEmpty(token)) {
            token = request.getParameter("visitorToken");
        }
        if (oConvertUtils.isEmpty(token)) {
            token = request.getParameter("token");
        }
        return token;
    }

    @Override
    public String extractSessionToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String token = request.getHeader("X-Visitor-Session");
        if (oConvertUtils.isEmpty(token)) {
            token = request.getParameter("sessionToken");
        }
        return token;
    }

    @Override
    public boolean isBlacklisted(String externalUserId) {
        if (oConvertUtils.isEmpty(externalUserId)) {
            return false;
        }
        Boolean member = redisTemplate.opsForSet().isMember(VISITOR_BLACKLIST_KEY, externalUserId);
        return Boolean.TRUE.equals(member);
    }

    @Override
    public void blacklist(String externalUserId) {
        if (oConvertUtils.isEmpty(externalUserId)) {
            return;
        }
        redisTemplate.opsForSet().add(VISITOR_BLACKLIST_KEY, externalUserId);
    }

    @Override
    public void unblacklist(String externalUserId) {
        if (oConvertUtils.isEmpty(externalUserId)) {
            return;
        }
        redisTemplate.opsForSet().remove(VISITOR_BLACKLIST_KEY, externalUserId);
    }

    @Override
    public boolean isIpBlacklisted(String clientIp) {
        if (oConvertUtils.isEmpty(clientIp)) {
            return false;
        }
        Boolean member = redisTemplate.opsForSet().isMember(VISITOR_IP_BLACKLIST_KEY, clientIp);
        return Boolean.TRUE.equals(member);
    }

    @Override
    public void blacklistIp(String clientIp) {
        if (oConvertUtils.isEmpty(clientIp)) {
            return;
        }
        redisTemplate.opsForSet().add(VISITOR_IP_BLACKLIST_KEY, clientIp);
    }

    @Override
    public void unblacklistIp(String clientIp) {
        if (oConvertUtils.isEmpty(clientIp)) {
            return;
        }
        redisTemplate.opsForSet().remove(VISITOR_IP_BLACKLIST_KEY, clientIp);
    }

    @Override
    public boolean isAdminRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String accessToken = request.getHeader("X-Access-Token");
        if (oConvertUtils.isEmpty(accessToken)) {
            return false;
        }
        try {
            String username = JwtUtil.getUsername(accessToken);
            if (oConvertUtils.isEmpty(username)) {
                return false;
            }
            LoginUser user = commonAPI.getUserByName(username);
            if (user == null || user.getStatus() != 1) {
                return false;
            }
            return JwtUtil.verify(accessToken, username, user.getPassword());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String getGlobalVisitorAppId() {
        String appId = redisTemplate.opsForValue().get(VISITOR_APP_REDIS_KEY);
        if (oConvertUtils.isNotEmpty(appId)) {
            return appId;
        }
        CsGlobalConfig config = csGlobalConfigMapper.selectById(VISITOR_APP_CONFIG_KEY);
        appId = config != null ? config.getConfigValue() : null;
        if (oConvertUtils.isNotEmpty(appId)) {
            redisTemplate.opsForValue().set(VISITOR_APP_REDIS_KEY, appId);
        }
        return appId;
    }

    @Override
    public boolean checkRateLimit(String externalUserId, String clientIp) {
        if (oConvertUtils.isEmpty(externalUserId)) {
            return false;
        }
        String key = VISITOR_TOKEN_RATE_PREFIX + externalUserId;
        if (oConvertUtils.isNotEmpty(clientIp)) {
            key = key + ":" + clientIp;
        }
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        }
        return count != null && count <= TOKEN_RATE_LIMIT_PER_MINUTE;
    }
}
