package org.jeecg.modules.system.security.cse.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.config.TenantContext;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.oss.entity.OssFile;
import org.jeecg.modules.system.entity.SysStorageConfig;
import org.jeecg.modules.system.mapper.SysStorageConfigMapper;
import org.jeecg.modules.system.security.cse.config.CseProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * CSE 加密上传外观：判定 → 加密 → 写存储 → 落库 → 返回 cse://{fid}
 */
@Slf4j
@Service
public class CseUploader {

    public static final String CSE_PREFIX = "cse://";
    private static final String CSE_SUFFIX = ".cse";

    @Autowired
    private CseProperties cseProperties;

    /** 动态配置服务：enabled / publicPaths / encryptedPaths 走 DB（带 60s TTL 缓存 + yml 回退） */
    @Autowired
    private CseConfigService cseConfigService;

    @Autowired
    private FileEncryptionService encryptionService;

    @Autowired
    private CseFileStorage cseFileStorage;

    @Autowired
    private OssFileMetaService metaService;

    @Autowired
    private SysStorageConfigMapper storageConfigMapper;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    /**
     * 判断该 bizPath 是否需要走加密链路
     */
    public boolean shouldEncrypt(String bizPath) {
        // 4 处 cseProperties 切到 cseConfigService（DB 优先 + yml 兜底，60s TTL 缓存）
        if (!cseConfigService.isEnabled()) {
            return false;
        }
        String p = bizPath == null ? "" : bizPath.replace("\\", "/");
        // 末尾补 / 归一化：让 "airag" 与配置 "airag/" 等价；
        // 同时避免 "airagXX" 误命中 "airag/"（startsWith("airagXX/", "airag/") 仍 false）。
        if (!p.isEmpty() && !p.endsWith("/")) {
            p = p + "/";
        }
        // 黑名单：公开路径不加密
        for (String pub : cseConfigService.getPublicPaths()) {
            if (pub != null && !pub.isEmpty() && p.startsWith(pub)) {
                return false;
            }
        }
        // 白名单：若配置了 encryptedPaths，只加密命中的
        java.util.List<String> encList = cseConfigService.getEncryptedPaths();
        if (encList != null && !encList.isEmpty()) {
            for (String enc : encList) {
                if (enc != null && !enc.isEmpty() && p.startsWith(enc)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * 加密上传 MultipartFile，返回 cse://{fid}
     */
    public String uploadEncrypted(MultipartFile file, String bizPath) {
        try {
            byte[] raw = file.getBytes();
            String mime = file.getContentType();
            String originalName = file.getOriginalFilename();
            // 安全策略：禁止 SVG 类（XSS 风险高，且加密后 <img> 仍可执行内嵌脚本）
            rejectIfSvg(mime, originalName);
            // 安全策略：图片去除 EXIF / GPS / 缩略图等敏感元数据。
            // R6 优化：客户端已用 Canvas 重新编码（天然剥离 EXIF/GPS），可显式声明
            // X-No-Strip-Metadata: 1 让后端跳过这一步重新编码，避免双重压缩画质雪崩。
            // 单纯减少一次 ImageIO 读写，安全语义不变（前端 canvas.toBlob 输出本身不含 EXIF）。
            byte[] sanitized = clientAlreadyStripped() ? raw : stripImageMetadata(raw, mime, originalName);
            return uploadEncryptedBytes(sanitized, originalName, mime, bizPath);
        } catch (JeecgBootException e) {
            recordEncryptFail("upload");
            throw e;
        } catch (Exception e) {
            recordEncryptFail("upload");
            throw new JeecgBootException("[CSE] 加密上传失败: " + e.getMessage());
        }
    }

    /**
     * R6: 客户端是否已声明本次上传的图片已剥离 EXIF / 已经过 Canvas 重新编码。
     * 通过 HTTP 头 X-No-Strip-Metadata: 1 表达意图。
     * 取不到 RequestContext（非 web 上下文 / 后台任务调用）时安全返回 false。
     */
    private boolean clientAlreadyStripped() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return false;
            HttpServletRequest req = attrs.getRequest();
            if (req == null) return false;
            String v = req.getHeader("X-No-Strip-Metadata");
            return "1".equals(v) || "true".equalsIgnoreCase(v);
        } catch (Exception e) {
            return false;
        }
    }

    private void rejectIfSvg(String mime, String name) {
        if (mime != null && mime.toLowerCase(Locale.ROOT).contains("svg")) {
            throw new JeecgBootException("[CSE] 出于安全考虑，禁止上传 SVG 类型图片");
        }
        if (name != null && name.toLowerCase(Locale.ROOT).endsWith(".svg")) {
            throw new JeecgBootException("[CSE] 出于安全考虑，禁止上传 SVG 类型图片");
        }
    }

    /**
     * 移除 JPEG/PNG/WebP 中的 EXIF/IPTC/XMP/GPS 等元数据。
     * 失败时回退原字节，不阻塞上传。
     */
    private byte[] stripImageMetadata(byte[] raw, String mime, String name) {
        if (raw == null || raw.length == 0 || !isImageMime(mime, name)) {
            return raw;
        }
        try (ByteArrayInputStream in = new ByteArrayInputStream(raw);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) return raw;
            String fmt = inferWriterFormat(mime, name);
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(fmt);
            if (!writers.hasNext()) return raw;
            ImageWriter writer = writers.next();
            try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
                writer.setOutput(ios);
                ImageWriteParam param = writer.getDefaultWriteParam();
                writer.write(null, new IIOImage(img, null, null), param);
            } finally {
                writer.dispose();
            }
            byte[] cleaned = out.toByteArray();
            return cleaned.length > 0 ? cleaned : raw;
        } catch (Exception e) {
            log.debug("[CSE] strip EXIF skipped (fallback to raw): {}", e.getMessage());
            return raw;
        }
    }

    private static String inferWriterFormat(String mime, String name) {
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        String m = mime == null ? "" : mime.toLowerCase(Locale.ROOT);
        if (m.contains("png") || n.endsWith(".png")) return "png";
        if (m.contains("webp") || n.endsWith(".webp")) return "webp";
        return "jpg";
    }

    private void recordEncryptFail(String stage) {
        try {
            if (meterRegistry != null) {
                meterRegistry.counter("cse.encrypt.fail", "stage", stage).increment();
            }
        } catch (Exception ignored) {}
    }

    /**
     * 加密上传字节流（在线图片入口走这里）
     */
    public String uploadEncryptedBytes(byte[] raw, String originalName, String mime, String bizPath) {
        String fid = OssFileMetaService.genFileId();
        String storageType = resolveStorageType();
        String bucket = resolveBucket(storageType);
        String objectKey = buildObjectKey(bizPath, fid, originalName);

        String sha256 = sha256Hex(raw);

        long encStart = System.nanoTime();
        FileEncryptionService.EncryptResult enc = encryptionService.encryptBytes(raw, fid);
        long cipherLen;
        try (InputStream cs = enc.getCipherStream()) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = cs.read(buf)) > 0) {
                bos.write(buf, 0, n);
            }
            byte[] cipherBytes = bos.toByteArray();
            cipherLen = cipherBytes.length;
            cseFileStorage.putCipher(new ByteArrayInputStream(cipherBytes), cipherLen, storageType, bucket, objectKey);
        } catch (Exception e) {
            recordEncryptFail("write");
            throw new JeecgBootException("[CSE] 写密文失败: " + e.getMessage());
        } finally {
            FileEncryptionService.clear(enc.getDekClear());
            try {
                if (meterRegistry != null) {
                    Timer.builder("cse.encrypt.duration")
                            .register(meterRegistry)
                            .record(System.nanoTime() - encStart, TimeUnit.NANOSECONDS);
                }
            } catch (Exception ignored) {}
        }

        // 缩略图（仅图片）
        String thumbKey = null;
        if (isImageMime(mime, originalName)) {
            thumbKey = uploadThumbnail(raw, fid, storageType, bucket, bizPath);
        }

        // 落库
        OssFile of = new OssFile();
        of.setFileId(fid);
        of.setFileName(originalName);
        of.setUrl(CSE_PREFIX + fid);
        of.setAlgo(FileEncryptionService.ALGO);
        of.setIvB64(Base64.getEncoder().encodeToString(enc.getIv()));
        of.setDekWrappedB64(Base64.getEncoder().encodeToString(enc.getDekWrapped()));
        of.setKekKid(enc.getKid());
        of.setMimeType(mime);
        of.setOriginSize((long) raw.length);
        of.setCipherSize(cipherLen);
        of.setPublicFlag(0);
        of.setThumbObjectKey(thumbKey);
        of.setSha256(sha256);
        of.setBizPath(bizPath);
        of.setStorageType(storageType);
        of.setBucket(bucket);
        of.setObjectKey(objectKey);
        try {
            of.setTenantId(TenantContext.getTenant());
        } catch (Exception ignored) {}
        try {
            LoginUser u = (LoginUser) SecurityUtils.getSubject().getPrincipal();
            if (u != null) {
                of.setCreateBy(u.getUsername());
            }
        } catch (Exception ignored) {}
        of.setCreateTime(java.util.Date.from(LocalDateTime.now().atZone(java.time.ZoneId.systemDefault()).toInstant()));

        metaService.save(of);

        log.info("[CSE] uploaded fid={} bizPath={} size={}→{} kek={}", fid, bizPath, raw.length, cipherLen, enc.getKid());
        return CSE_PREFIX + fid;
    }

    private String uploadThumbnail(byte[] raw, String fid, String storageType, String bucket, String bizPath) {
        try {
            int w = cseProperties.getThumbWidth();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(raw))
                    .size(w, w)
                    .outputFormat("webp")
                    .toOutputStream(out);
            byte[] thumbBytes = out.toByteArray();
            FileEncryptionService.EncryptResult enc = encryptionService.encryptBytes(thumbBytes, fid + ":thumb");
            String thumbKey = buildThumbKey(bizPath, fid);
            try (InputStream cs = enc.getCipherStream()) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = cs.read(buf)) > 0) {
                    bos.write(buf, 0, n);
                }
                byte[] cipher = bos.toByteArray();
                cseFileStorage.putCipher(new ByteArrayInputStream(cipher), cipher.length, storageType, bucket, thumbKey);
            } finally {
                FileEncryptionService.clear(enc.getDekClear());
            }
            return thumbKey;
        } catch (Exception e) {
            log.warn("[CSE] 缩略图生成失败 fid={} reason={}", fid, e.getMessage());
            return null;
        }
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            byte[] h = d.digest(data);
            StringBuilder sb = new StringBuilder(h.length * 2);
            for (byte b : h) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveStorageType() {
        SysStorageConfig c = storageConfigMapper.selectById(SysStorageConfig.ID_SINGLETON);
        if (c == null || c.getStorageType() == null) {
            return CseFileStorage.TYPE_LOCAL;
        }
        String t = SysStorageConfig.normalizeStorageType(c.getStorageType());
        if (SysStorageConfig.TYPE_ALIYUN.equals(t)) {
            return CseFileStorage.TYPE_ALIYUN;
        }
        if (SysStorageConfig.TYPE_TENCENT.equals(t)) {
            return CseFileStorage.TYPE_TENCENT;
        }
        return CseFileStorage.TYPE_LOCAL;
    }

    private String resolveBucket(String storageType) {
        if (CseFileStorage.TYPE_LOCAL.equals(storageType)) {
            return "";
        }
        SysStorageConfig c = storageConfigMapper.selectById(SysStorageConfig.ID_SINGLETON);
        if (c == null) {
            return "";
        }
        if (CseFileStorage.TYPE_ALIYUN.equals(storageType)) {
            return c.getAliyunBucket() == null ? "" : c.getAliyunBucket().trim();
        }
        if (CseFileStorage.TYPE_TENCENT.equals(storageType)) {
            return c.getTencentBucket() == null ? "" : c.getTencentBucket().trim();
        }
        return "";
    }

    private static String buildObjectKey(String bizPath, String fid, String originalName) {
        String safeBiz = (bizPath == null) ? "" : bizPath.replace("\\", "/").replaceAll("^/+", "").replaceAll("/+$", "");
        String prefix = safeBiz.isEmpty() ? "cse" : safeBiz + "/cse";
        return prefix + "/" + fid + CSE_SUFFIX;
    }

    private static String buildThumbKey(String bizPath, String fid) {
        String safeBiz = (bizPath == null) ? "" : bizPath.replace("\\", "/").replaceAll("^/+", "").replaceAll("/+$", "");
        String prefix = safeBiz.isEmpty() ? "cse" : safeBiz + "/cse";
        return prefix + "/" + fid + ".thumb" + CSE_SUFFIX;
    }

    private static boolean isImageMime(String mime, String name) {
        if (mime != null && mime.toLowerCase(Locale.ROOT).startsWith("image/")) {
            return true;
        }
        if (name == null) {
            return false;
        }
        String n = name.toLowerCase(Locale.ROOT);
        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") || n.endsWith(".webp")
                || n.endsWith(".bmp") || n.endsWith(".gif");
    }

}
