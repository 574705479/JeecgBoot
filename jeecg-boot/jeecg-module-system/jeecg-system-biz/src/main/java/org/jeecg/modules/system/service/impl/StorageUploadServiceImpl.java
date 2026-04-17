package org.jeecg.modules.system.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.CommonUtils;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.common.util.storage.IStorageConfigCacheInvalidator;
import org.jeecg.common.util.storage.IStorageUploadService;
import org.jeecg.modules.system.entity.SysStorageConfig;
import org.jeecg.modules.system.mapper.SysStorageConfigMapper;
import org.jeecg.modules.system.storage.AliyunDynamicOssUpload;
import org.jeecg.modules.system.storage.CosClientPool;
import org.jeecg.modules.system.storage.OssClientPool;
import org.jeecg.modules.system.storage.StorageCredentialCrypto;
import org.jeecg.modules.system.storage.TencentCosUpload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.jeecgframework.poi.util.PoiPublicUtil;

/**
 * 全站统一上传实现
 */
@Slf4j
@Service
public class StorageUploadServiceImpl implements IStorageUploadService, IStorageConfigCacheInvalidator {

    private enum Mode {
        YML_LOCAL, YML_ALIOSS, DB_SYSTEM, DB_ALIYUN, DB_TENCENT
    }

    /** 配置缓存 TTL：60s */
    private static final long CONFIG_CACHE_TTL_MS = 60_000L;

    private volatile SysStorageConfig cachedConfig;
    private volatile long cachedConfigAt = 0L;
    private final Object cacheLock = new Object();

    @Value("${jeecg.path.upload}")
    private String uploadpath;

    @Value("${jeecg.uploadType}")
    private String ymlUploadType;

    @Autowired
    private SysStorageConfigMapper storageConfigMapper;

    @Autowired
    private OssClientPool ossClientPool;

    @Autowired
    private CosClientPool cosClientPool;

    @Override
    public void invalidate() {
        synchronized (cacheLock) {
            cachedConfig = null;
            cachedConfigAt = 0L;
        }
        try {
            ossClientPool.invalidateAll();
        } catch (Exception e) {
            log.warn("[StorageUpload] ossClientPool.invalidateAll failed: {}", e.getMessage());
        }
        try {
            cosClientPool.invalidateAll();
        } catch (Exception e) {
            log.warn("[StorageUpload] cosClientPool.invalidateAll failed: {}", e.getMessage());
        }
        log.info("[StorageUpload] storage config cache invalidated");
    }

    /**
     * 60s TTL 缓存读取存储配置。
     * 注意：null 也是合法缓存值（DB 中尚无记录），需要单独标记 cachedConfigAt 以区分"未读取"与"已读取且为空"。
     */
    private SysStorageConfig loadConfigCached() {
        long now = System.currentTimeMillis();
        if (cachedConfigAt > 0 && (now - cachedConfigAt) < CONFIG_CACHE_TTL_MS) {
            return cachedConfig;
        }
        synchronized (cacheLock) {
            now = System.currentTimeMillis();
            if (cachedConfigAt > 0 && (now - cachedConfigAt) < CONFIG_CACHE_TTL_MS) {
                return cachedConfig;
            }
            cachedConfig = storageConfigMapper.selectById(SysStorageConfig.ID_SINGLETON);
            cachedConfigAt = now;
            return cachedConfig;
        }
    }

    @Override
    public String upload(MultipartFile file, String bizPath) {
        Mode mode = resolveMode();
        String path = normalizeBizParam(bizPath, objectStorage(mode));
        try {
            switch (mode) {
                case YML_LOCAL:
                case DB_SYSTEM:
                    return CommonUtils.uploadLocal(file, path, uploadpath);
                case YML_ALIOSS:
                    return CommonUtils.upload(file, path, CommonConstant.UPLOAD_TYPE_OSS);
                case DB_ALIYUN:
                    SysStorageConfig c = requireConfig();
                    String ask = StorageCredentialCrypto.decryptPlain(c.getAliyunSecretCipher());
                    validateAliyun(c, ask);
                    boolean aliyunAccel = Boolean.TRUE.equals(c.getAliyunTransferAccel());
                    return AliyunDynamicOssUpload.uploadMultipart(file, path,
                            trim(c.getAliyunEndpoint()), trim(c.getAliyunBucket()),
                            trim(c.getAliyunAccessKeyId()), ask,
                            trim(c.getAliyunStaticDomain()), aliyunAccel);
                case DB_TENCENT:
                    SysStorageConfig t = requireConfig();
                    String tsk = StorageCredentialCrypto.decryptPlain(t.getTencentSecretKeyCipher());
                    validateTencent(t, tsk);
                    boolean cosAccel = Boolean.TRUE.equals(t.getTencentGlobalAccel());
                    return TencentCosUpload.uploadMultipart(file, path,
                            trim(t.getTencentRegion()), trim(t.getTencentBucket()),
                            trim(t.getTencentSecretId()), tsk,
                            trim(t.getTencentDomain()), cosAccel);
                default:
                    throw new JeecgBootException("未支持的存储模式");
            }
        } catch (JeecgBootException e) {
            throw e;
        } catch (Exception e) {
            log.error("[StorageUpload] upload failed", e);
            throw new JeecgBootException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public String uploadOnlineImage(byte[] data, String uploadRootPath, String bizPath) {
        Mode mode = resolveMode();
        String path = normalizeBizParam(bizPath, objectStorage(mode));

        String fileName = "image" + Math.round(Math.random() * 100000000000L);
        fileName += "." + PoiPublicUtil.getFileExtendName(data);
        try {
            switch (mode) {
                case YML_LOCAL:
                case DB_SYSTEM:
                    return uploadOnlineLocal(data, uploadRootPath, path, fileName);
                case YML_ALIOSS:
                    java.io.InputStream in2 = new java.io.ByteArrayInputStream(data);
                    String rel2 = path + "/" + fileName;
                    return org.jeecg.common.util.oss.OssBootUtil.upload(in2, rel2);
                case DB_ALIYUN:
                    SysStorageConfig c = requireConfig();
                    String ask = StorageCredentialCrypto.decryptPlain(c.getAliyunSecretCipher());
                    validateAliyun(c, ask);
                    boolean aliyunAccel2 = Boolean.TRUE.equals(c.getAliyunTransferAccel());
                    return AliyunDynamicOssUpload.uploadBytes(data, path, fileName,
                            trim(c.getAliyunEndpoint()), trim(c.getAliyunBucket()),
                            trim(c.getAliyunAccessKeyId()), ask,
                            trim(c.getAliyunStaticDomain()), aliyunAccel2);
                case DB_TENCENT:
                    SysStorageConfig t = requireConfig();
                    String tsk = StorageCredentialCrypto.decryptPlain(t.getTencentSecretKeyCipher());
                    validateTencent(t, tsk);
                    boolean cosAccel2 = Boolean.TRUE.equals(t.getTencentGlobalAccel());
                    return TencentCosUpload.uploadBytes(data, path, fileName,
                            trim(t.getTencentRegion()), trim(t.getTencentBucket()),
                            trim(t.getTencentSecretId()), tsk,
                            trim(t.getTencentDomain()), cosAccel2);
                default:
                    throw new JeecgBootException("未支持的存储模式");
            }
        } catch (JeecgBootException e) {
            throw e;
        } catch (Exception e) {
            log.error("[StorageUpload] uploadOnlineImage failed", e);
            throw new JeecgBootException("上传失败: " + e.getMessage());
        }
    }

    private static String uploadOnlineLocal(byte[] data, String basePath, String bizPath, String fileName) throws Exception {
        java.nio.file.Path uploadRoot = java.nio.file.Paths.get(basePath).toAbsolutePath().normalize();
        String safeBiz = (bizPath == null) ? "" : bizPath.replace("\\", "/");
        java.nio.file.Path bizDir = uploadRoot.resolve(safeBiz).normalize();
        if (!bizDir.startsWith(uploadRoot)) {
            throw new JeecgBootException("非法的上传业务路径");
        }
        java.io.File file = bizDir.toFile();
        if (!file.exists()) {
            file.mkdirs();
        }
        java.nio.file.Path savePath = bizDir.resolve(fileName).normalize();
        if (!savePath.startsWith(uploadRoot)) {
            throw new JeecgBootException("非法的上传文件名");
        }
        org.springframework.util.FileCopyUtils.copy(data, savePath.toFile());
        String dbpath = oConvertUtils.isNotEmpty(bizPath) ? bizPath + java.io.File.separator + fileName : fileName;
        return dbpath.replace("\\", "/");
    }

    @Override
    public boolean isEffectiveLocal() {
        Mode m = resolveMode();
        return m == Mode.YML_LOCAL || m == Mode.DB_SYSTEM;
    }

    private SysStorageConfig requireConfig() {
        SysStorageConfig c = loadConfigCached();
        if (c == null) {
            throw new JeecgBootException("存储配置不存在，请先在系统管理保存存储桶配置");
        }
        return c;
    }

    private Mode resolveMode() {
        SysStorageConfig row = loadConfigCached();
        if (row == null) {
            return modeFromYml(ymlUploadType);
        }
        String st = SysStorageConfig.normalizeStorageType(row.getStorageType());
        if (oConvertUtils.isEmpty(st)) {
            return modeFromYml(ymlUploadType);
        }
        if (SysStorageConfig.TYPE_SYSTEM.equals(st)) {
            return Mode.DB_SYSTEM;
        }
        if (SysStorageConfig.TYPE_ALIYUN.equals(st)) {
            return Mode.DB_ALIYUN;
        }
        if (SysStorageConfig.TYPE_TENCENT.equals(st)) {
            return Mode.DB_TENCENT;
        }
        return modeFromYml(ymlUploadType);
    }

    private static Mode modeFromYml(String ut) {
        if (ut == null || ut.trim().isEmpty()) {
            return Mode.YML_LOCAL;
        }
        String v = ut.trim().toLowerCase();
        switch (v) {
            case "local":
                return Mode.YML_LOCAL;
            case "alioss":
                return Mode.YML_ALIOSS;
            case "minio":
                log.warn("jeecg.uploadType=minio 已废弃，自动回退到 local。请修改 application.yml 中的 jeecg.uploadType");
                return Mode.YML_LOCAL;
            default:
                throw new IllegalStateException("不支持的 jeecg.uploadType: " + ut + "，可选值: local | alioss");
        }
    }

    private static boolean objectStorage(Mode mode) {
        return mode == Mode.YML_ALIOSS
                || mode == Mode.DB_ALIYUN || mode == Mode.DB_TENCENT;
    }

    /** 空 biz 时对象存储默认前缀 upload */
    private static String normalizeBizParam(String bizPath, boolean objectStorage) {
        if (oConvertUtils.isEmpty(bizPath)) {
            return objectStorage ? "upload" : "";
        }
        return bizPath;
    }

    private static void validateAliyun(SysStorageConfig c, String secret) {
        if (oConvertUtils.isEmpty(c.getAliyunEndpoint()) || oConvertUtils.isEmpty(c.getAliyunBucket())
                || oConvertUtils.isEmpty(c.getAliyunAccessKeyId()) || oConvertUtils.isEmpty(secret)) {
            throw new JeecgBootException("阿里云 OSS 配置不完整，请补全 Endpoint、Bucket、AccessKey 与 Secret");
        }
    }

    private static void validateTencent(SysStorageConfig c, String secretKey) {
        if (oConvertUtils.isEmpty(c.getTencentRegion()) || oConvertUtils.isEmpty(c.getTencentBucket())
                || oConvertUtils.isEmpty(c.getTencentSecretId()) || oConvertUtils.isEmpty(secretKey)) {
            throw new JeecgBootException("腾讯云 COS 配置不完整，请补全 Region、Bucket、SecretId 与 SecretKey");
        }
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
