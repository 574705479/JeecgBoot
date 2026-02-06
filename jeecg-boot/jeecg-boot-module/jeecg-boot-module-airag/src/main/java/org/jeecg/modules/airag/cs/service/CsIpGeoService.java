package org.jeecg.modules.airag.cs.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.cs.entity.CsIpGeoCache;
import org.jeecg.modules.airag.cs.mapper.CsIpGeoCacheMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * IP 地理位置服务
 * 
 * 查询优先级: 本地缓存表 → ip-api.com 免费接口 → 结果回写缓存
 * 
 * @author jeecg
 * @date 2026-02-06
 */
@Slf4j
@Service
public class CsIpGeoService {

    /** 内网IP正则 */
    private static final Pattern PRIVATE_IP_PATTERN = Pattern.compile(
            "^(127\\.|10\\.|172\\.(1[6-9]|2[0-9]|3[01])\\.|192\\.168\\.|0:0:0:0|::1|localhost)"
    );

    /** ip-api.com 免费接口（支持中文） */
    private static final String IP_API_URL = "http://ip-api.com/json/{ip}?lang=zh-CN&fields=status,country,regionName,city";

    @Autowired
    private CsIpGeoCacheMapper ipGeoCacheMapper;

    /**
     * 根据 IP 查询地理位置
     *
     * @param ip IP 地址
     * @return Map 包含: country, province, city；查询失败返回空 Map
     */
    public Map<String, String> queryGeoByIp(String ip) {
        Map<String, String> result = new HashMap<>(3);

        if (ip == null || ip.isEmpty() || isPrivateIp(ip)) {
            return result;
        }

        try {
            // 1. 先查本地缓存
            CsIpGeoCache cached = queryFromCache(ip);
            if (cached != null) {
                result.put("country", cached.getCountry());
                result.put("province", cached.getProvince());
                result.put("city", cached.getCity());
                log.debug("[IP-Geo] 命中缓存: ip={}, country={}, province={}, city={}",
                        ip, cached.getCountry(), cached.getProvince(), cached.getCity());
                return result;
            }

            // 2. 调用外部 API
            result = queryFromApi(ip);

            // 3. 写入缓存
            if (!result.isEmpty()) {
                saveToCache(ip, result);
            }
        } catch (Exception e) {
            log.warn("[IP-Geo] 查询IP地理位置失败: ip={}, error={}", ip, e.getMessage());
        }

        return result;
    }

    /**
     * 从本地缓存查询
     */
    private CsIpGeoCache queryFromCache(String ip) {
        try {
            LambdaQueryWrapper<CsIpGeoCache> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(CsIpGeoCache::getIp, ip).last("LIMIT 1");
            return ipGeoCacheMapper.selectOne(wrapper);
        } catch (Exception e) {
            log.warn("[IP-Geo] 查询缓存失败: ip={}", ip, e);
            return null;
        }
    }

    /**
     * 调用 ip-api.com 接口
     */
    private Map<String, String> queryFromApi(String ip) {
        Map<String, String> result = new HashMap<>(3);
        try {
            RestTemplate restTemplate = new RestTemplate();
            // 设置超时（使用默认配置，一般3-5秒）
            String url = IP_API_URL.replace("{ip}", ip);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject json = JSONObject.parseObject(response.getBody());
                if ("success".equals(json.getString("status"))) {
                    String country = json.getString("country");
                    String regionName = json.getString("regionName");
                    String city = json.getString("city");

                    if (country != null) {
                        result.put("country", country);
                    }
                    if (regionName != null) {
                        result.put("province", regionName);
                    }
                    if (city != null) {
                        result.put("city", city);
                    }

                    // 存储原始响应供调试
                    result.put("_raw", response.getBody());

                    log.info("[IP-Geo] API查询成功: ip={}, country={}, province={}, city={}",
                            ip, country, regionName, city);
                } else {
                    log.warn("[IP-Geo] API返回失败: ip={}, response={}", ip, response.getBody());
                }
            }
        } catch (Exception e) {
            log.warn("[IP-Geo] 调用ip-api.com失败: ip={}, error={}", ip, e.getMessage());
        }
        return result;
    }

    /**
     * 写入本地缓存
     */
    private void saveToCache(String ip, Map<String, String> geoData) {
        try {
            CsIpGeoCache cache = new CsIpGeoCache();
            cache.setIp(ip);
            cache.setCountry(geoData.get("country"));
            cache.setProvince(geoData.get("province"));
            cache.setCity(geoData.get("city"));
            cache.setRawResponse(geoData.get("_raw"));
            cache.setCreateTime(new Date());
            ipGeoCacheMapper.insert(cache);
            log.debug("[IP-Geo] 缓存已写入: ip={}", ip);
        } catch (Exception e) {
            // 可能是唯一索引冲突（并发写入同一IP），忽略
            log.warn("[IP-Geo] 写入缓存失败(可能已存在): ip={}, error={}", ip, e.getMessage());
        }
    }

    /**
     * 判断是否为内网/本地IP
     */
    private boolean isPrivateIp(String ip) {
        return PRIVATE_IP_PATTERN.matcher(ip).find();
    }
}
