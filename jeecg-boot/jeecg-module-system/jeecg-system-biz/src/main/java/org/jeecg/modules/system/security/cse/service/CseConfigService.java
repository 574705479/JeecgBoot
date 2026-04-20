package org.jeecg.modules.system.security.cse.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.security.cse.config.CseProperties;
import org.jeecg.modules.system.security.cse.entity.CseKekAuditLog;
import org.jeecg.modules.system.security.cse.entity.SysCseConfig;
import org.jeecg.modules.system.security.cse.mapper.SysCseConfigMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * CSE 动态配置服务（DB 单行 + 60s TTL 缓存 + invalidate + 失败回退 yml + 写审计）
 *
 * 设计参考 StorageUploadServiceImpl 的"单行配置 + TTL 缓存 + invalidate"模式，
 * 已在生产环境验证可靠。
 *
 * 调用方：
 *  - {@link CseUploader#shouldEncrypt(String)} 替换原 cseProperties.* 4 处直接读取
 *  - {@link CseConfigService#save} 由 CseConfigController 调用
 *
 * 降级策略：
 *  - DB 表未创建 / 行不存在 / 任意 RuntimeException → 回退到 cseProperties（yml）
 *    确保服务永远可读到一份"非空"的配置，不会因 DB 异常导致全站走明文。
 */
@Slf4j
@Service
public class CseConfigService {

    /** 60s TTL 缓存（与 StorageUploadServiceImpl 一致） */
    private static final long CONFIG_CACHE_TTL_MS = 60_000L;

    private volatile SysCseConfig cachedConfig;
    private volatile long cachedConfigAt = 0L;
    private final Object cacheLock = new Object();

    @Autowired
    private SysCseConfigMapper configMapper;

    @Autowired
    private CseProperties cseProperties;

    @Autowired
    private KekProvider kekProvider;

    @Autowired
    private CseKekService cseKekService;

    // ─────────────────────────────────────────────────────────────────────────
    // 读 API（被 CseUploader / Controller 调用）
    // ─────────────────────────────────────────────────────────────────────────

    public boolean isEnabled() {
        SysCseConfig c = loadCached();
        if (c != null && c.getEnabled() != null) {
            return c.getEnabled() == 1;
        }
        return cseProperties.isEnabled();
    }

    public List<String> getPublicPaths() {
        SysCseConfig c = loadCached();
        if (c != null) {
            List<String> fromDb = parseJsonArray(c.getPublicPaths());
            if (fromDb != null) {
                return fromDb;
            }
        }
        // 回退到 yml（防御性 copy 一份，避免外部修改影响全局单例）
        List<String> ymlList = cseProperties.getPublicPaths();
        return ymlList == null ? Collections.emptyList() : new ArrayList<>(ymlList);
    }

    public List<String> getEncryptedPaths() {
        SysCseConfig c = loadCached();
        if (c != null) {
            List<String> fromDb = parseJsonArray(c.getEncryptedPaths());
            if (fromDb != null) {
                return fromDb;
            }
        }
        List<String> ymlList = cseProperties.getEncryptedPaths();
        return ymlList == null ? Collections.emptyList() : new ArrayList<>(ymlList);
    }

    /**
     * 给 Controller GET /sys/cse/config 用：返回当前生效配置 + 字典 + 自定义部分。
     *
     * customEncrypted/customPublic = 当前实际生效列表 - 字典里同分类的所有 path。
     * 这样前端能区分"勾选字典项"和"自定义补充项"，分别渲染勾选表和高级 tag 区。
     */
    public JSONObject getCurrentForUi() {
        boolean enabled = isEnabled();
        List<String> encrypted = normalizeList(getEncryptedPaths());
        List<String> publicL = normalizeList(getPublicPaths());

        Set<String> dictEncrypt = new LinkedHashSet<>();
        Set<String> dictPublic = new LinkedHashSet<>();
        JSONArray dict = new JSONArray();
        for (CseBizDictionary.BizDef d : CseBizDictionary.ALL) {
            JSONObject o = new JSONObject();
            o.put("path", d.path());
            o.put("name", d.name());
            o.put("description", d.description());
            o.put("category", d.category().name());
            o.put("forceLocked", d.forceLocked());
            dict.add(o);
            if (d.category() == CseBizDictionary.Category.ENCRYPT) {
                dictEncrypt.add(d.path());
            } else {
                dictPublic.add(d.path());
            }
        }

        List<String> customEncrypted = new ArrayList<>();
        for (String p : encrypted) {
            if (!dictEncrypt.contains(p)) {
                customEncrypted.add(p);
            }
        }
        List<String> customPublic = new ArrayList<>();
        for (String p : publicL) {
            if (!dictPublic.contains(p)) {
                customPublic.add(p);
            }
        }

        JSONObject root = new JSONObject();
        root.put("enabled", enabled);
        root.put("encryptedPaths", encrypted);
        root.put("publicPaths", publicL);
        root.put("dictionary", dict);
        root.put("customEncrypted", customEncrypted);
        root.put("customPublic", customPublic);
        return root;
    }

    /**
     * dryRun：用前端待保存的 preview 配置或当前 DB 配置，模拟 shouldEncrypt 判定。
     *
     * 复用 CseUploader.shouldEncrypt 的同一套规则，避免逻辑漂移。
     *
     * @param bizPath  被测路径（可不带尾斜杠）
     * @param mode     "current" 用当前生效 / "preview" 用 previewEncrypted+previewPublic
     * @param previewEncrypted preview 模式下的白名单（可空）
     * @param previewPublic    preview 模式下的黑名单（可空）
     */
    public JSONObject dryRun(String bizPath, String mode,
                             List<String> previewEncrypted, List<String> previewPublic,
                             Boolean previewEnabled) {
        boolean enabled;
        List<String> encrypted;
        List<String> publicL;
        if ("preview".equalsIgnoreCase(mode)) {
            enabled = previewEnabled == null ? isEnabled() : previewEnabled;
            encrypted = normalizeList(previewEncrypted == null ? new ArrayList<>() : previewEncrypted);
            publicL = normalizeList(previewPublic == null ? new ArrayList<>() : previewPublic);
        } else {
            enabled = isEnabled();
            encrypted = normalizeList(getEncryptedPaths());
            publicL = normalizeList(getPublicPaths());
        }

        JSONObject r = new JSONObject();
        r.put("bizPath", bizPath);
        r.put("mode", mode);
        r.put("enabled", enabled);
        if (!enabled) {
            r.put("shouldEncrypt", false);
            r.put("reason", "总开关关闭，所有路径均不加密");
            return r;
        }
        String p = bizPath == null ? "" : bizPath.replace("\\", "/");
        if (!p.isEmpty() && !p.endsWith("/")) {
            p = p + "/";
        }
        for (String pub : publicL) {
            if (pub != null && !pub.isEmpty() && p.startsWith(pub)) {
                r.put("shouldEncrypt", false);
                r.put("matchedRule", "publicPaths[" + pub + "]");
                r.put("reason", "命中公开黑名单，明文上传");
                return r;
            }
        }
        if (!encrypted.isEmpty()) {
            for (String enc : encrypted) {
                if (enc != null && !enc.isEmpty() && p.startsWith(enc)) {
                    r.put("shouldEncrypt", true);
                    r.put("matchedRule", "encryptedPaths[" + enc + "]");
                    r.put("reason", "命中白名单，CSE 加密上传");
                    return r;
                }
            }
            r.put("shouldEncrypt", false);
            r.put("reason", "未命中任何白名单前缀，明文上传");
            return r;
        }
        // encryptedPaths 为空 → 全量加密（与 CseUploader 行为一致）
        r.put("shouldEncrypt", true);
        r.put("reason", "白名单为空，按全量加密策略处理");
        return r;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 写 API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 保存配置：双向护栏校验 → upsert → 写审计 → invalidate 缓存。
     *
     * @param req           请求体（前端拼装的最终列表，含字典勾选 + 自定义 tag）
     * @param operator      当前登录用户
     * @param operatorIp    客户端 IP
     * @param secondaryPwd  二次密码
     */
    @Transactional
    public void save(SaveRequest req, LoginUser operator, String operatorIp, String secondaryPwd) {
        // ① 二次密码（复用 KEK 同一套校验）
        cseKekService.verifyPassword(operator, secondaryPwd);

        // ② 计算最终列表 = 用户传入 ∪ forceLocked 兜底
        Set<String> finalEncrypted = new LinkedHashSet<>();
        if (req.encryptedPaths != null) {
            for (String p : req.encryptedPaths) {
                String n = normalizePath(p);
                if (n != null) finalEncrypted.add(n);
            }
        }
        finalEncrypted.addAll(CseBizDictionary.forceLockedPaths(CseBizDictionary.Category.ENCRYPT));

        Set<String> finalPublic = new LinkedHashSet<>();
        if (req.publicPaths != null) {
            for (String p : req.publicPaths) {
                String n = normalizePath(p);
                if (n != null) finalPublic.add(n);
            }
        }
        finalPublic.addAll(CseBizDictionary.forceLockedPaths(CseBizDictionary.Category.PUBLIC));

        // ③ 双向护栏：禁止把对方分类的强制锁项放进本分类
        Set<String> oppOfEncrypt = CseBizDictionary.opposingForceLockedPaths(CseBizDictionary.Category.ENCRYPT);
        for (String p : finalEncrypted) {
            if (oppOfEncrypt.contains(p)) {
                throw new JeecgBootException("不允许把公开强制锁路径放入加密白名单：" + p);
            }
        }
        Set<String> oppOfPublic = CseBizDictionary.opposingForceLockedPaths(CseBizDictionary.Category.PUBLIC);
        for (String p : finalPublic) {
            if (oppOfPublic.contains(p)) {
                throw new JeecgBootException("不允许把加密强制锁路径放入公开黑名单：" + p);
            }
        }

        // ④ 重叠校验：同一路径不能同时出现在两个列表
        Set<String> intersect = new LinkedHashSet<>(finalEncrypted);
        intersect.retainAll(finalPublic);
        if (!intersect.isEmpty()) {
            throw new JeecgBootException("路径不能同时位于加密与公开列表：" + intersect);
        }

        // ⑤ 取变更前快照（用于审计 diff）
        SysCseConfig before = safeLoadFromDb();
        String beforeJson = before == null ? "{}" : toAuditJson(before);

        // ⑥ upsert
        SysCseConfig row = before == null ? new SysCseConfig() : before;
        row.setId(SysCseConfig.ID_SINGLETON);
        row.setEnabled(req.enabled ? 1 : 0);
        row.setEncryptedPaths(JSON.toJSONString(new ArrayList<>(finalEncrypted)));
        row.setPublicPaths(JSON.toJSONString(new ArrayList<>(finalPublic)));
        row.setUpdateTime(LocalDateTime.now());
        row.setUpdateBy(operator == null ? "system" : operator.getUsername());

        if (before == null) {
            configMapper.insert(row);
        } else {
            configMapper.updateById(row);
        }

        // ⑦ 写审计
        String afterJson = toAuditJson(row);
        String detail = "before=" + beforeJson + " after=" + afterJson;
        // remark 字段最大 512，截断防越界
        if (detail.length() > 500) {
            detail = detail.substring(0, 500) + "...";
        }
        try {
            kekProvider.writeAudit(null, CseKekAuditLog.ACTION_CONFIG_UPDATE,
                    operator == null ? null : operator.getId(),
                    operator == null ? "system" : operator.getUsername(),
                    operatorIp, detail);
        } catch (Exception e) {
            log.warn("[CseConfig] write audit failed: {}", e.getMessage());
        }

        // ⑧ 失效缓存（下次读立即生效，无需重启）
        invalidateCache();
        log.info("[CseConfig] config updated by {} encrypted={} public={} enabled={}",
                row.getUpdateBy(), finalEncrypted, finalPublic, req.enabled);
    }

    public void invalidateCache() {
        synchronized (cacheLock) {
            cachedConfig = null;
            cachedConfigAt = 0L;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 内部工具
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 60s TTL 缓存读取（double-checked locking 与 StorageUploadServiceImpl 一致）。
     *
     * 任何 RuntimeException（DB 表未建 / 连接异常 / mapper 注入失败等）都返回 null
     * 让上层回退到 yml，绝不让 enabled 被误判为 false。
     */
    private SysCseConfig loadCached() {
        long now = System.currentTimeMillis();
        if (cachedConfigAt > 0 && (now - cachedConfigAt) < CONFIG_CACHE_TTL_MS) {
            return cachedConfig;
        }
        synchronized (cacheLock) {
            now = System.currentTimeMillis();
            if (cachedConfigAt > 0 && (now - cachedConfigAt) < CONFIG_CACHE_TTL_MS) {
                return cachedConfig;
            }
            cachedConfig = safeLoadFromDb();
            cachedConfigAt = now;
            return cachedConfig;
        }
    }

    private SysCseConfig safeLoadFromDb() {
        try {
            return configMapper.selectById(SysCseConfig.ID_SINGLETON);
        } catch (RuntimeException e) {
            log.warn("[CseConfig] DB 读配置失败，回退 yml 兜底：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析 JSON 数组字符串。null/空字符串/解析失败均返回 null（让上层回退 yml）。
     * 空 JSON 数组 [] 返回空 List（保留"用户显式置空"的语义，不回退）。
     */
    private static List<String> parseJsonArray(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return JSON.parseArray(json, String.class);
        } catch (Exception e) {
            log.warn("[CseConfig] parse paths json failed: {} → fallback yml", json);
            return null;
        }
    }

    /** 末尾补 / 归一化，过滤 null/空串 */
    private static String normalizePath(String p) {
        if (p == null) return null;
        String s = p.trim();
        if (s.isEmpty()) return null;
        s = s.replace("\\", "/");
        if (!s.endsWith("/")) s = s + "/";
        return s;
    }

    private static List<String> normalizeList(List<String> in) {
        if (in == null) return Collections.emptyList();
        List<String> out = new ArrayList<>(in.size());
        for (String p : in) {
            String n = normalizePath(p);
            if (n != null) out.add(n);
        }
        return out;
    }

    private static String toAuditJson(SysCseConfig c) {
        JSONObject o = new JSONObject(true);
        o.put("enabled", c.getEnabled());
        o.put("encryptedPaths", c.getEncryptedPaths());
        o.put("publicPaths", c.getPublicPaths());
        return o.toJSONString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 请求 DTO
    // ─────────────────────────────────────────────────────────────────────────

    /** 保存请求：前端拼装的最终列表（含字典勾选 + 自定义 tag） */
    public static class SaveRequest {
        public boolean enabled;
        public List<String> encryptedPaths;
        public List<String> publicPaths;
    }
}
