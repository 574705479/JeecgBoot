package org.jeecg.modules.airag.cs.service.impl;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.jeecg.common.api.CommonAPI;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.cs.entity.CsGlobalConfig;
import org.jeecg.modules.airag.cs.entity.CsIpBlacklist;
import org.jeecg.modules.airag.cs.entity.CsVisitorBlacklist;
import org.jeecg.modules.airag.cs.mapper.CsGlobalConfigMapper;
import org.jeecg.modules.airag.cs.mapper.CsIpBlacklistMapper;
import org.jeecg.modules.airag.cs.mapper.CsVisitorBlacklistMapper;
import org.jeecg.modules.airag.cs.service.ICsVisitorTokenService;
import org.jeecg.modules.airag.cs.util.CsIpMatchUtil;
import org.jeecg.modules.airag.cs.vo.CsVisitorTokenPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;
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
    private CsIpBlacklistMapper ipBlacklistMapper;

    @Autowired
    private CsVisitorBlacklistMapper visitorBlacklistMapper;

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
        // 先查Redis缓存
        Boolean member = redisTemplate.opsForSet().isMember(VISITOR_BLACKLIST_KEY, externalUserId);
        if (Boolean.TRUE.equals(member)) {
            return true;
        }
        // 再查数据库
        LambdaQueryWrapper<CsVisitorBlacklist> qw = new LambdaQueryWrapper<>();
        qw.eq(CsVisitorBlacklist::getVisitorId, externalUserId);
        Long count = visitorBlacklistMapper.selectCount(qw);
        if (count != null && count > 0) {
            // 回填Redis缓存
            redisTemplate.opsForSet().add(VISITOR_BLACKLIST_KEY, externalUserId);
            return true;
        }
        return false;
    }

    @Override
    public void blacklist(String externalUserId) {
        if (oConvertUtils.isEmpty(externalUserId)) {
            return;
        }
        redisTemplate.opsForSet().add(VISITOR_BLACKLIST_KEY, externalUserId);
    }

    @Override
    public void blacklistWithReason(String externalUserId, String visitorName, String reason, String operator) {
        if (oConvertUtils.isEmpty(externalUserId)) {
            return;
        }
        // 写入Redis
        redisTemplate.opsForSet().add(VISITOR_BLACKLIST_KEY, externalUserId);
        // 写入数据库（先查重）
        LambdaQueryWrapper<CsVisitorBlacklist> qw = new LambdaQueryWrapper<>();
        qw.eq(CsVisitorBlacklist::getVisitorId, externalUserId);
        if (visitorBlacklistMapper.selectCount(qw) == 0) {
            CsVisitorBlacklist record = new CsVisitorBlacklist();
            record.setVisitorId(externalUserId);
            record.setVisitorName(visitorName);
            record.setReason(reason);
            record.setOperator(operator);
            record.setBanDate(new Date());
            record.setCreateBy(operator);
            record.setCreateTime(new Date());
            visitorBlacklistMapper.insert(record);
        }
    }

    @Override
    public void unblacklist(String externalUserId) {
        if (oConvertUtils.isEmpty(externalUserId)) {
            return;
        }
        redisTemplate.opsForSet().remove(VISITOR_BLACKLIST_KEY, externalUserId);
        // 同步删除数据库记录
        LambdaQueryWrapper<CsVisitorBlacklist> qw = new LambdaQueryWrapper<>();
        qw.eq(CsVisitorBlacklist::getVisitorId, externalUserId);
        visitorBlacklistMapper.delete(qw);
    }

    @Override
    public boolean isIpBlacklisted(String clientIp) {
        if (oConvertUtils.isEmpty(clientIp)) {
            return false;
        }
        // 先查Redis精确匹配
        Boolean member = redisTemplate.opsForSet().isMember(VISITOR_IP_BLACKLIST_KEY, clientIp);
        if (Boolean.TRUE.equals(member)) {
            return true;
        }
        // 再查数据库（包括CIDR段匹配）
        List<CsIpBlacklist> allRecords = ipBlacklistMapper.selectList(null);
        if (allRecords != null) {
            for (CsIpBlacklist record : allRecords) {
                if (CsIpMatchUtil.matches(clientIp, record.getIp())) {
                    // 精确IP回填Redis缓存
                    if (!CsIpMatchUtil.isCidr(record.getIp())) {
                        redisTemplate.opsForSet().add(VISITOR_IP_BLACKLIST_KEY, clientIp);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void blacklistIp(String clientIp) {
        if (oConvertUtils.isEmpty(clientIp)) {
            return;
        }
        redisTemplate.opsForSet().add(VISITOR_IP_BLACKLIST_KEY, clientIp);
    }

    @Override
    public void blacklistIpWithReason(String ip, String reason, String operator) {
        if (oConvertUtils.isEmpty(ip)) {
            return;
        }
        // 精确IP加入Redis
        if (!CsIpMatchUtil.isCidr(ip)) {
            redisTemplate.opsForSet().add(VISITOR_IP_BLACKLIST_KEY, ip);
        }
        // 写入数据库（先查重）
        LambdaQueryWrapper<CsIpBlacklist> qw = new LambdaQueryWrapper<>();
        qw.eq(CsIpBlacklist::getIp, ip.trim());
        if (ipBlacklistMapper.selectCount(qw) == 0) {
            CsIpBlacklist record = new CsIpBlacklist();
            record.setIp(ip.trim());
            record.setReason(reason);
            record.setOperator(operator);
            record.setBanDate(new Date());
            record.setCreateBy(operator);
            record.setCreateTime(new Date());
            ipBlacklistMapper.insert(record);
        }
    }

    @Override
    public void unblacklistIp(String clientIp) {
        if (oConvertUtils.isEmpty(clientIp)) {
            return;
        }
        redisTemplate.opsForSet().remove(VISITOR_IP_BLACKLIST_KEY, clientIp);
        // 同步删除数据库记录
        LambdaQueryWrapper<CsIpBlacklist> qw = new LambdaQueryWrapper<>();
        qw.eq(CsIpBlacklist::getIp, clientIp);
        ipBlacklistMapper.delete(qw);
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

    @Override
    public boolean isTokenRequired() {
        // 先查Redis缓存
        String json = redisTemplate.opsForValue().get(VISITOR_ACCESS_REDIS_KEY);
        if (oConvertUtils.isEmpty(json)) {
            // 再查数据库
            CsGlobalConfig config = csGlobalConfigMapper.selectById(VISITOR_ACCESS_CONFIG_KEY);
            json = config != null ? config.getConfigValue() : null;
            if (oConvertUtils.isNotEmpty(json)) {
                redisTemplate.opsForValue().set(VISITOR_ACCESS_REDIS_KEY, json);
            }
        }
        if (oConvertUtils.isNotEmpty(json)) {
            try {
                JSONObject obj = JSONObject.parseObject(json);
                // 默认 true（兼容现有行为）
                return obj.getBooleanValue("tokenRequired") || !obj.containsKey("tokenRequired");
            } catch (Exception e) {
                log.warn("[CS-Token] 解析visitor_access配置失败", e);
            }
        }
        // 默认需要Token
        return true;
    }

    @Override
    public String extractDeviceId(HttpServletRequest request) {
        String deviceId = request.getHeader("X-Device-Id");
        if (oConvertUtils.isEmpty(deviceId)) {
            deviceId = request.getParameter("deviceId");
        }
        return deviceId;
    }

    @Override
    public boolean validateAppKey(HttpServletRequest request) {
        // Token模式下不校验（密钥在获取Token时已校验）
        if (isTokenRequired()) {
            return true;
        }
        // 读取全局配置中的secretKey
        String json = redisTemplate.opsForValue().get(VISITOR_ACCESS_REDIS_KEY);
        if (oConvertUtils.isEmpty(json)) {
            CsGlobalConfig config = csGlobalConfigMapper.selectById(VISITOR_ACCESS_CONFIG_KEY);
            json = config != null ? config.getConfigValue() : null;
            if (oConvertUtils.isNotEmpty(json)) {
                redisTemplate.opsForValue().set(VISITOR_ACCESS_REDIS_KEY, json);
            }
        }
        if (oConvertUtils.isEmpty(json)) {
            return true; // 无配置，不校验
        }
        try {
            JSONObject obj = JSONObject.parseObject(json);
            String configuredKey = obj != null ? obj.getString("secretKey") : null;
            if (oConvertUtils.isEmpty(configuredKey)) {
                return true; // 未配置密钥，不校验
            }
            // 从请求中提取key
            String appKey = request.getHeader("X-App-Secret");
            if (oConvertUtils.isEmpty(appKey)) {
                appKey = request.getParameter("key");
            }
            if (oConvertUtils.isEmpty(appKey)) {
                log.warn("[CS-Token] 免Token模式缺少接入密钥");
                return false;
            }
            return configuredKey.equals(appKey);
        } catch (Exception e) {
            log.warn("[CS-Token] 校验接入密钥时解析配置失败", e);
            return false;
        }
    }

    private static final String VISITOR_ACCESS_REDIS_KEY = "cs:global:visitor_access";
    private static final String VISITOR_ACCESS_CONFIG_KEY = "visitor_access";
}
