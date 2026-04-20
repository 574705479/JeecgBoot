package org.jeecg.modules.system.security.cse.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.config.shiro.IgnoreAuth;
import org.jeecg.modules.airag.cs.mapper.CsBrandConfigMapper;
import org.jeecg.modules.airag.cs.service.CsBrandFidWhitelist;
import org.jeecg.modules.oss.entity.OssFile;
import org.jeecg.modules.system.security.cse.service.CseAwareImageReader;
import org.jeecg.modules.system.security.cse.service.OssFileMetaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 品牌文件匿名解密代理
 *
 * 用途：登录前 / 未登录场景（index.html 首屏、登录页 logo / favicon / 背景图）
 * 需要直接以普通 HTTP URL 加载品牌资源；这些资源已被 CSE 加密上传为 cse://fid 字符串，
 * 浏览器不识别 cse:// 协议，前端 cseDecrypt 又必须 token 才能调 /sys/secure/file/{fid}/key。
 *
 * 解决方案：本端点接收 fid，做严格校验后用 KEK + DEK 解密并直接返回明文图字节，
 * 浏览器 / link[rel=icon] / img.src 都能直接消费。
 *
 * 5 层校验（按顺序）：
 *  1. fid 格式校验（防 SQL 通配符注入与路径污染）
 *  2. 白名单（内存 Set） + DB 兜底校验（确保 fid 真实出现在 brand 字段中）
 *  3. bizPath 校验（防越权管理员塞他人 fid 泄漏文件）
 *  4. 内存 TTL 缓存（避免重复解密 CPU 开销）
 *  5. MIME 安全嗅探（SVG 通过 CSP 锁脚本兼容历史数据）
 *
 * 配套写权限收紧：CsBrandConfigController.save 已加 @RequiresRoles({admin, cs_admin_agent}, OR)，
 * 防止任意登录用户/子客服篡改 brand 字段塞入他人 fid。
 *
 * 模块归属：本 controller 位于 jeecg-system-biz（与 SecureFileController 同模块），
 * 因为需要直接 import system-biz 的 CseAwareImageReader / OssFileMetaService。
 * 业务依赖（CsBrandFidWhitelist / Mapper）来自 airag 模块，可通过 Spring 反向注入解析
 * （system-biz pom 已经依赖 airag）。
 */
@Slf4j
@RestController
@RequestMapping("/cs/brand")
public class CsBrandFileController {

    /** fid 格式：alnum 20-40 位（实际为 24 位 hex，但放宽适配未来变更） */
    private static final Pattern FID_PATTERN = Pattern.compile("^[a-zA-Z0-9]{20,40}$");

    /** 内存解密缓存：fid → CachedEntry。fid 不可变 → 字节不可变，TTL 过期重解 */
    private static final long CACHE_TTL_MS = 60 * 60 * 1000L; // 1h
    private final Map<String, CachedEntry> decryptCache = new ConcurrentHashMap<>();

    @Autowired private CsBrandFidWhitelist whitelist;
    @Autowired private OssFileMetaService metaService;
    @Autowired private CseAwareImageReader cseAwareImageReader;
    @Autowired private CsBrandConfigMapper brandMapper;

    @IgnoreAuth
    @GetMapping("/file/{fid}")
    public void download(@PathVariable("fid") String fid, HttpServletResponse resp) throws IOException {
        // ── 1. fid 格式校验
        if (fid == null || !FID_PATTERN.matcher(fid).matches()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        // ── 2. 白名单 + DB 兜底校验
        if (!whitelist.contains(fid)) {
            int cnt = 0;
            try {
                cnt = brandMapper.existsByFid(fid);
            } catch (Exception e) {
                log.warn("[CsBrandFile] DB existsByFid 异常 fid={} err={}", fid, e.getMessage());
            }
            if (cnt > 0) {
                whitelist.addFids(Collections.singleton(fid));
            } else {
                // 集群兜底：cs_brand_config 表查不到，可能是聊天窗 cs_global_config 内的 fid，
                // 强制 refresh 重扫两个数据源后再 contains。覆盖"A 实例 save、B 实例处理请求"场景。
                try {
                    whitelist.refresh();
                } catch (Exception e) {
                    log.warn("[CsBrandFile] DB miss 后 refresh 失败 fid={} err={}", fid, e.getMessage());
                }
                if (!whitelist.contains(fid)) {
                    log.debug("[CsBrandFile] fid 不在 brand 白名单或 DB（refresh 后仍 miss）: {}", fid);
                    resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
            }
        }

        // ── 3. OssFile bizPath 严格校验（F9：精确匹配防 cs-brand-malicious 旁路）
        OssFile file = metaService.getByFileId(fid);
        if (file == null) {
            log.debug("[CsBrandFile] OssFile 未找到: {}", fid);
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String bizPath = file.getBizPath();
        boolean isLegitBrandUpload = "cs-brand".equals(bizPath)
                || (bizPath != null && bizPath.startsWith("cs-brand/"));
        boolean isLegacyEmpty = bizPath == null || bizPath.isEmpty();
        if (!isLegitBrandUpload && !isLegacyEmpty) {
            log.warn("[CsBrandFile] 拒绝非 brand 上传 fid: fid={} bizPath={}", fid, bizPath);
            resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // ── 4. 内存 TTL 缓存解密结果
        byte[] bytes = getCachedDecrypt(fid);
        if (bytes == null) {
            log.debug("[CsBrandFile] 解密失败: {}", fid);
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // ── 5. MIME 安全嗅探（F10：SVG 用 CSP 锁脚本，不再粗暴拒绝）
        String mime = guessMimeSafely(file, bytes);
        if (mime == null) {
            resp.setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
            return;
        }

        resp.setContentType(mime);
        resp.setHeader("X-Content-Type-Options", "nosniff");
        resp.setHeader("Cache-Control", "public, max-age=3600"); // 1h
        resp.setHeader("X-Brand-Fid", fid);

        // SVG 特殊处理：CSP 头禁止 SVG 内 <script>/外链执行；inline 防下载
        if ("image/svg+xml".equals(mime)) {
            resp.setHeader("Content-Security-Policy",
                    "default-src 'none'; img-src data:; style-src 'unsafe-inline'");
            resp.setHeader("Content-Disposition", "inline; filename=\"brand.svg\"");
        }

        resp.getOutputStream().write(bytes);
        resp.getOutputStream().flush();
    }

    /** 简单 TTL 缓存：同 fid 并发可能多次解密但品牌图就 3 张可接受 */
    private byte[] getCachedDecrypt(String fid) {
        CachedEntry e = decryptCache.get(fid);
        long now = System.currentTimeMillis();
        if (e != null && e.expireAt > now) return e.bytes;
        byte[] bytes = cseAwareImageReader.readBytesByFid(fid);
        if (bytes != null) {
            CachedEntry ne = new CachedEntry();
            ne.bytes = bytes;
            ne.expireAt = now + CACHE_TTL_MS;
            decryptCache.put(fid, ne);
            // 顺手清理过期项（轻量保护）
            if (decryptCache.size() > 50) {
                decryptCache.entrySet().removeIf(en -> en.getValue().expireAt <= now);
            }
        }
        return bytes;
    }

    /**
     * MIME 安全推断
     *  - 字节魔数：PNG / JPG / GIF / WebP / ICO
     *  - 文件名后缀 / 文本前缀：SVG（外层走 CSP 锁脚本）
     *  - 完全无法识别：返回 null（415）
     */
    private String guessMimeSafely(OssFile file, byte[] bytes) {
        if (bytes.length >= 4) {
            if (bytes[0] == (byte) 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') return "image/png";
            if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF) return "image/jpeg";
            if (bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F') return "image/gif";
            if (bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                    && bytes.length >= 12
                    && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') return "image/webp";
            if (bytes[0] == 0x00 && bytes[1] == 0x00 && bytes[2] == 0x01 && bytes[3] == 0x00) return "image/x-icon";
        }
        // SVG：文件名或文本前缀
        String name = file.getFileName();
        if (name != null) {
            String lower = name.toLowerCase();
            if (lower.endsWith(".svg") || lower.endsWith(".svgz")) return "image/svg+xml";
        }
        if (bytes.length >= 5) {
            String head = new String(bytes, 0, Math.min(bytes.length, 256), StandardCharsets.UTF_8).trim().toLowerCase();
            if (head.startsWith("<?xml") || head.startsWith("<svg")) return "image/svg+xml";
        }
        return null;
    }

    private static class CachedEntry {
        byte[] bytes;
        long expireAt;
    }
}
