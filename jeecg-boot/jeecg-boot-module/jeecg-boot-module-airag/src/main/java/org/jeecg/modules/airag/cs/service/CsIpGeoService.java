package org.jeecg.modules.airag.cs.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * IP 地理位置服务（ip2region 离线库版）
 *
 * <p>启动时把 {@code resources/ip2region/ip2region.xdb}（约 11 MB）全量读入内存，
 * 使用线程安全的 {@link Searcher#newWithBuffer(byte[])} 单例做查询，微秒级返回。</p>
 *
 * <p>替换原来的 {@code http://ip-api.com} 外网调用：原实现 {@code new RestTemplate()}
 * 没配 timeout，国内机房访问 ip-api.com 不通时会退化到 OS TCP 默认值（~130s），
 * 把 createConversation 拖到 nginx 504。</p>
 *
 * <p>兜底策略：xdb 加载失败（文件缺失 / 损坏）时 {@code searcher} 保持为 {@code null}，
 * {@link #queryGeoByIp(String)} 直接返回空 Map 不抛异常，最坏结果是失去归属地信息，
 * 不会再拖慢上游调用。</p>
 *
 * @author jeecg
 * @date 2026-02-06
 */
@Slf4j
@Service
public class CsIpGeoService {

    /** 内网/本地 IP 正则（与历史实现保持一致） */
    private static final Pattern PRIVATE_IP_PATTERN = Pattern.compile(
            "^(127\\.|10\\.|172\\.(1[6-9]|2[0-9]|3[01])\\.|192\\.168\\.|0:0:0:0|::1|localhost)"
    );

    /** xdb 相对 classpath 路径 */
    private static final String XDB_LOCATION = "ip2region/ip2region.xdb";

    /** 线程安全单例。加载失败时保持 null，查询直接返空 Map 兜底 */
    private volatile Searcher searcher;

    @PostConstruct
    public void init() {
        try (InputStream is = new ClassPathResource(XDB_LOCATION).getInputStream()) {
            byte[] buf = is.readAllBytes();
            this.searcher = Searcher.newWithBuffer(buf);
            log.info("[IP-Geo] ip2region 离线库加载成功, size={}KB", buf.length / 1024);
        } catch (Exception e) {
            log.error("[IP-Geo] ip2region 初始化失败，IP 归属地功能将返回空结果: {}",
                    e.getMessage(), e);
        }
    }

    /**
     * 根据 IP 查询地理位置。
     *
     * @param ip IP 地址；内网 IP / 空值 / 查询失败均安全返回空 Map，不抛异常
     * @return Map 包含 {@code country / province / city}；查询失败返回空 Map
     */
    public Map<String, String> queryGeoByIp(String ip) {
        Map<String, String> result = new HashMap<>(3);

        if (ip == null || ip.isEmpty() || isPrivateIp(ip) || searcher == null) {
            return result;
        }

        try {
            // ip2region 2.x 返回格式: "国家|区域|省|市|ISP"
            String region = searcher.search(ip);
            if (region == null || region.isEmpty()) {
                return result;
            }
            String[] parts = region.split("\\|", -1);
            String country = parts.length > 0 ? nullIfBlankOrZero(parts[0]) : null;
            String province = parts.length > 2 ? nullIfBlankOrZero(parts[2]) : null;
            String city = parts.length > 3 ? nullIfBlankOrZero(parts[3]) : null;
            if (country != null) {
                result.put("country", country);
            }
            if (province != null) {
                result.put("province", province);
            }
            if (city != null) {
                result.put("city", city);
            }
            log.debug("[IP-Geo] 离线查询成功: ip={}, country={}, province={}, city={}",
                    ip, country, province, city);
        } catch (Exception e) {
            log.warn("[IP-Geo] 离线查询异常: ip={}, error={}", ip, e.getMessage());
        }
        return result;
    }

    /** ip2region 对未知字段统一用 "0" 占位；null/空/0 都视为无效 */
    private static String nullIfBlankOrZero(String v) {
        if (v == null) return null;
        String t = v.trim();
        return (t.isEmpty() || "0".equals(t)) ? null : t;
    }

    private boolean isPrivateIp(String ip) {
        return PRIVATE_IP_PATTERN.matcher(ip).find();
    }
}
