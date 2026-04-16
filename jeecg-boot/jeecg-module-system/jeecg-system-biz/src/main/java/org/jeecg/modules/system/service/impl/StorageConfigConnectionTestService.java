package org.jeecg.modules.system.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.config.JeecgBaseConfig;
import org.jeecg.modules.system.entity.SysStorageConfig;
import org.jeecg.modules.system.model.StorageConfigSaveDTO;
import org.jeecg.modules.system.storage.StorageCredentialCrypto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * 保存前检测存储是否可用（本地可写；云存储：Bucket 存在性 + 小对象上传/删除探针，验证密钥与写权限）
 */
@Slf4j
@Service
public class StorageConfigConnectionTestService {

    private static final String PROBE_PREFIX = ".jeecg-healthcheck/";
    private static final byte[] PROBE_BYTES = new byte[]{0x42};

    @Autowired
    private JeecgBaseConfig jeecgBaseConfig;

    /**
     * @param dto      与保存接口一致；密钥可为空则尝试用库中已保存密文解密
     * @param existing 单行配置，可为 null
     */
    public void assertConnectionOk(StorageConfigSaveDTO dto, SysStorageConfig existing) {
        if (dto == null || oConvertUtils.isEmpty(dto.getStorageType())) {
            throw new JeecgBootException("storageType 不能为空");
        }
        String st = dto.getStorageType().trim().toUpperCase();
        if (SysStorageConfig.TYPE_SYSTEM.equals(st)) {
            testSystemLocal();
            return;
        }
        if (SysStorageConfig.TYPE_ALIYUN.equals(st)) {
            testAliyun(dto, existing);
            return;
        }
        if (SysStorageConfig.TYPE_TENCENT.equals(st)) {
            testTencent(dto, existing);
            return;
        }
        throw new JeecgBootException("不支持的存储类型");
    }

    private void testSystemLocal() {
        if (jeecgBaseConfig.getPath() == null || oConvertUtils.isEmpty(jeecgBaseConfig.getPath().getUpload())) {
            throw new JeecgBootException("未配置 jeecg.path.upload，无法使用系统本地存储");
        }
        File dir = new File(jeecgBaseConfig.getPath().getUpload());
        try {
            if (!dir.exists() && !dir.mkdirs()) {
                throw new JeecgBootException("无法创建上传目录: " + dir.getAbsolutePath());
            }
            if (!dir.isDirectory()) {
                throw new JeecgBootException("上传路径不是目录: " + dir.getAbsolutePath());
            }
            if (!Files.isWritable(dir.toPath())) {
                throw new JeecgBootException("上传目录不可写: " + dir.getAbsolutePath());
            }
        } catch (JeecgBootException e) {
            throw e;
        } catch (Exception e) {
            log.error("[StorageTest] local path check failed", e);
            throw new JeecgBootException("本地目录检测失败: " + e.getMessage());
        }
    }

    private void testAliyun(StorageConfigSaveDTO dto, SysStorageConfig existing) {
        String endpoint = trimOrNull(dto.getAliyunEndpoint());
        String bucket = trimOrNull(dto.getAliyunBucket());
        String ak = trimOrNull(dto.getAliyunAccessKeyId());
        String secret = resolveAliyunSecretPlain(dto, existing);
        if (oConvertUtils.isEmpty(endpoint) || oConvertUtils.isEmpty(bucket) || oConvertUtils.isEmpty(ak) || oConvertUtils.isEmpty(secret)) {
            throw new JeecgBootException("请先填写完整的 Endpoint、Bucket、AccessKey 与 Secret（或保留已配置密钥时 Secret 留空）");
        }
        boolean transferAccel = Boolean.TRUE.equals(dto.getAliyunTransferAccel());
        String endpointForClient = transferAccel ? "https://oss-accelerate.aliyuncs.com" : endpointWithScheme(endpoint);
        OSS oss = new OSSClientBuilder().build(endpointForClient, ak, secret);
        String probeKey = PROBE_PREFIX + "write-" + System.currentTimeMillis() + ".bin";
        try {
            try {
                if (!oss.doesBucketExist(bucket)) {
                    throw new JeecgBootException("阿里云 OSS：Bucket 不存在，或当前密钥无列举权限（请核对地域 Endpoint 与 Bucket 名称）");
                }
            } catch (JeecgBootException e) {
                throw e;
            } catch (Exception e) {
                log.warn("[StorageTest] Aliyun bucket check failed: {}", e.getMessage());
                throw new JeecgBootException("阿里云连接失败（Bucket 检测）: " + unwrapMsg(e));
            }
            try (InputStream in = new ByteArrayInputStream(PROBE_BYTES)) {
                com.aliyun.oss.model.ObjectMetadata meta = new com.aliyun.oss.model.ObjectMetadata();
                meta.setContentLength(PROBE_BYTES.length);
                oss.putObject(bucket, probeKey, in, meta);
            }
        } catch (JeecgBootException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[StorageTest] Aliyun put probe failed: {}", e.getMessage());
            throw new JeecgBootException("阿里云 OSS：上传测试失败（请确认密钥具有 PutObject 权限，且策略未禁止前缀 " + PROBE_PREFIX + "）: " + unwrapMsg(e));
        } finally {
            try {
                oss.deleteObject(bucket, probeKey);
            } catch (Exception ex) {
                log.debug("[StorageTest] Aliyun probe delete: {}", ex.getMessage());
            }
            oss.shutdown();
        }
    }

    private void testTencent(StorageConfigSaveDTO dto, SysStorageConfig existing) {
        String regionId = trimOrNull(dto.getTencentRegion());
        String bucket = trimOrNull(dto.getTencentBucket());
        String sid = trimOrNull(dto.getTencentSecretId());
        String skey = resolveTencentSecretPlain(dto, existing);
        if (oConvertUtils.isEmpty(regionId) || oConvertUtils.isEmpty(bucket) || oConvertUtils.isEmpty(sid) || oConvertUtils.isEmpty(skey)) {
            throw new JeecgBootException("请先填写完整的 Region、Bucket、SecretId 与 SecretKey（或保留已配置密钥时 SecretKey 留空）");
        }
        COSCredentials cred = new BasicCOSCredentials(sid, skey);
        ClientConfig clientConfig = new ClientConfig(new Region(regionId));
        if (Boolean.TRUE.equals(dto.getTencentGlobalAccel())) {
            clientConfig.setEndPointSuffix("cos.accelerate.myqcloud.com");
        }
        COSClient cosClient = new COSClient(cred, clientConfig);
        String probeKey = PROBE_PREFIX + "write-" + System.currentTimeMillis() + ".bin";
        try {
            try {
                if (!cosClient.doesBucketExist(bucket)) {
                    throw new JeecgBootException("腾讯云 COS：Bucket 不存在，或当前密钥无列举权限（请核对 Region 与 Bucket 名称）");
                }
            } catch (JeecgBootException e) {
                throw e;
            } catch (Exception e) {
                log.warn("[StorageTest] Tencent bucket check failed: {}", e.getMessage());
                throw new JeecgBootException("腾讯云连接失败（Bucket 检测）: " + unwrapMsg(e));
            }
            try (InputStream in = new ByteArrayInputStream(PROBE_BYTES)) {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(PROBE_BYTES.length);
                PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, probeKey, in, metadata);
                cosClient.putObject(putObjectRequest);
            }
        } catch (JeecgBootException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[StorageTest] Tencent put probe failed: {}", e.getMessage());
            throw new JeecgBootException("腾讯云 COS：上传测试失败（请确认密钥具有 PutObject 权限，且策略未禁止前缀 " + PROBE_PREFIX + "）: " + unwrapMsg(e));
        } finally {
            try {
                cosClient.deleteObject(bucket, probeKey);
            } catch (Exception ex) {
                log.debug("[StorageTest] Tencent probe delete: {}", ex.getMessage());
            }
            cosClient.shutdown();
        }
    }

    private static String resolveAliyunSecretPlain(StorageConfigSaveDTO dto, SysStorageConfig existing) {
        if (dto.getAliyunAccessKeySecret() != null && !dto.getAliyunAccessKeySecret().isEmpty()) {
            return dto.getAliyunAccessKeySecret();
        }
        if (existing != null && oConvertUtils.isNotEmpty(existing.getAliyunSecretCipher())) {
            return StorageCredentialCrypto.decryptPlain(existing.getAliyunSecretCipher());
        }
        return null;
    }

    private static String resolveTencentSecretPlain(StorageConfigSaveDTO dto, SysStorageConfig existing) {
        if (dto.getTencentSecretKey() != null && !dto.getTencentSecretKey().isEmpty()) {
            return dto.getTencentSecretKey();
        }
        if (existing != null && oConvertUtils.isNotEmpty(existing.getTencentSecretKeyCipher())) {
            return StorageCredentialCrypto.decryptPlain(existing.getTencentSecretKeyCipher());
        }
        return null;
    }

    private static String unwrapMsg(Throwable e) {
        if (e == null) {
            return "";
        }
        String m = e.getMessage();
        if (oConvertUtils.isNotEmpty(m)) {
            return m;
        }
        if (e.getCause() != null) {
            return unwrapMsg(e.getCause());
        }
        return e.getClass().getSimpleName();
    }

    private static String trimOrNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    /** 与 AliyunDynamicOssUpload 一致 */
    private static String endpointWithScheme(String endpoint) {
        if (endpoint == null) {
            return null;
        }
        String e = endpoint.trim();
        if (e.isEmpty()) {
            return e;
        }
        String lower = e.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return e;
        }
        return "https://" + e;
    }
}
