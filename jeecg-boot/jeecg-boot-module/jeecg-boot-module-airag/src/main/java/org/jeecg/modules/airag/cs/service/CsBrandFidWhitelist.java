package org.jeecg.modules.airag.cs.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.cs.constant.CsRedisKeys;
import org.jeecg.modules.airag.cs.entity.CsBrandConfig;
import org.jeecg.modules.airag.cs.entity.CsGlobalConfig;
import org.jeecg.modules.airag.cs.mapper.CsBrandConfigMapper;
import org.jeecg.modules.airag.cs.mapper.CsGlobalConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 品牌字段 cse:// fid 白名单（内存集合）
 *
 * 用途：CsBrandFileController 的匿名解密代理需要严格限制「哪些 fid 可被匿名解密」，
 * 仅允许出现在 cs_brand_config 表 logoUrl / faviconUrl / loginBgUrl 字段，
 * 以及 cs_global_config.chat_window_settings JSON 内的 fid。
 *
 * 设计要点：
 *  - @PostConstruct 启动时全量加载（避免冷启动首请求都走 DB）
 *  - save / update brand 后由 ICsBrandConfigService 调用 refresh() 刷新
 *  - save chat-window-settings 后由 CsAgentController 调用 refresh() 刷新
 *  - 匿名访问时若 fid 不在内存集合，CsBrandFileController 会再查 DB 兜底（
 *    覆盖集群部署时其他实例 save 后本实例缓存未刷新场景），DB 兜底命中后 addFids
 *  - 单租户假设：当前 cs_brand_config / cs_global_config 表无 tenant_id；多租户时需按 tenantId 切分
 */
@Slf4j
@Service
public class CsBrandFidWhitelist {

    /** 匹配 cse:// 前缀后的 fid（hex / alnum），首位锚定，仅供单字段 URL 抽取 */
    private static final Pattern CSE_FID = Pattern.compile("^cse://([a-zA-Z0-9]{20,40})");

    /**
     * 全局正则：用于在 JSON / 富文本等长字符串中扫描所有出现的 cse://fid。
     * 必须不带 ^ 锚定，否则只能匹配字符串开头第一个 fid，导致 JSON 内非首位 fid 全部漏掉。
     */
    private static final Pattern CSE_FID_GLOBAL = Pattern.compile("cse://([a-zA-Z0-9]{20,40})");

    /**
     * 需要扫描 cse:// fid 的 cs_global_config 表 config_key 列表。
     * 未来追加新的 JSON 配置（如其他全局品牌相关配置）只需在此数组添加。
     */
    private static final String[] BRAND_JSON_KEYS = {
            CsRedisKeys.CONFIG_CHAT_WINDOW
    };

    /** 内存白名单（线程安全 Set） */
    private final Set<String> fids = ConcurrentHashMap.newKeySet();

    @Autowired
    private CsBrandConfigMapper brandConfigMapper;

    @Autowired
    private CsGlobalConfigMapper csGlobalConfigMapper;

    @PostConstruct
    public void init() {
        try {
            refresh();
            log.info("[CsBrandFidWhitelist] 启动加载完成，共 {} 个 fid", fids.size());
        } catch (Exception e) {
            // 启动时 DB 不可用不阻塞启动，由 DB 兜底校验保底
            log.warn("[CsBrandFidWhitelist] 启动加载失败，将依赖 DB 兜底：{}", e.getMessage());
        }
    }

    /** 全量重新加载（save / update 后调用） */
    public void refresh() {
        Set<String> next = ConcurrentHashMap.newKeySet();

        // ── 1. 扫 cs_brand_config 表 logoUrl / faviconUrl / loginBgUrl
        try {
            QueryWrapper<CsBrandConfig> wrapper = new QueryWrapper<>();
            wrapper.eq("del_flag", 0).eq("status", 1);
            List<CsBrandConfig> list = brandConfigMapper.selectList(wrapper);
            for (CsBrandConfig c : list) {
                extractFid(c.getLogoUrl(), next);
                extractFid(c.getFaviconUrl(), next);
                extractFid(c.getLoginBgUrl(), next);
            }
        } catch (Exception e) {
            log.warn("[CsBrandFidWhitelist] 扫 cs_brand_config 失败：{}", e.getMessage());
        }

        // ── 2. 扫 cs_global_config 表中 BRAND_JSON_KEYS 对应的 JSON（聊天窗设置等）
        for (String key : BRAND_JSON_KEYS) {
            try {
                CsGlobalConfig cfg = csGlobalConfigMapper.selectById(key);
                if (cfg != null && cfg.getConfigValue() != null && !cfg.getConfigValue().isEmpty()) {
                    extractAllFids(cfg.getConfigValue(), next);
                }
            } catch (Exception e) {
                log.warn("[CsBrandFidWhitelist] 扫 cs_global_config[{}] 失败：{}", key, e.getMessage());
            }
        }

        // 原子替换：直接 clear + addAll
        synchronized (fids) {
            fids.clear();
            fids.addAll(next);
        }
        log.info("[CsBrandFidWhitelist] refresh 完成，{} 个 fid", fids.size());
    }

    /** 增量加入（DB 兜底命中后用，避免下次再查 DB） */
    public void addFids(Collection<String> incoming) {
        if (incoming == null || incoming.isEmpty()) return;
        fids.addAll(incoming);
    }

    /** 是否包含 fid */
    public boolean contains(String fid) {
        return fid != null && fids.contains(fid);
    }

    /** 仅供测试用 */
    public int size() {
        return fids.size();
    }

    /** 抽取单字段 URL（首位 cse://，brand 表三个字段每个 cell 就是一个完整 URL） */
    private static void extractFid(String url, Set<String> sink) {
        if (url == null || url.isEmpty()) return;
        Matcher m = CSE_FID.matcher(url);
        if (m.find()) {
            sink.add(m.group(1));
        }
    }

    /** 抽取长字符串中所有 cse://fid（JSON 全文扫描专用，不带 ^ 锚定） */
    private static void extractAllFids(String text, Set<String> sink) {
        if (text == null || text.isEmpty()) return;
        Matcher m = CSE_FID_GLOBAL.matcher(text);
        while (m.find()) {
            sink.add(m.group(1));
        }
    }
}
