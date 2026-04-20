package org.jeecg.modules.system.security.cse.service;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.modules.system.entity.SysStorageConfig;
import org.jeecg.modules.system.mapper.SysStorageConfigMapper;
import org.jeecg.modules.system.storage.CosClientPool;
import org.jeecg.modules.system.storage.OssClientPool;
import org.jeecg.modules.system.storage.StorageCredentialCrypto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * CSE 密文对象的存取（按 storageType + bucket + objectKey）
 */
@Slf4j
@Component
public class CseFileStorage {

    public static final String TYPE_LOCAL = "local";
    public static final String TYPE_ALIYUN = "aliyun";
    public static final String TYPE_TENCENT = "tencent";

    @Value("${jeecg.path.upload}")
    private String uploadpath;

    @Autowired
    private SysStorageConfigMapper storageConfigMapper;

    @Autowired
    private OssClientPool ossClientPool;

    @Autowired
    private CosClientPool cosClientPool;

    /**
     * 把密文流写入指定存储位置，返回真实 objectKey（含 .cse 后缀）
     */
    public PutResult putCipher(InputStream cipherStream, long contentLength, String storageType,
                               String bucket, String objectKey) {
        try {
            switch (storageType) {
                case TYPE_LOCAL:
                    Path uploadRoot = Paths.get(uploadpath).toAbsolutePath().normalize();
                    Path target = uploadRoot.resolve(objectKey).normalize();
                    if (!target.startsWith(uploadRoot)) {
                        throw new JeecgBootException("非法 objectKey");
                    }
                    Files.createDirectories(target.getParent());
                    try (OutputStream os = new FileOutputStream(target.toFile())) {
                        byte[] buf = new byte[8192];
                        int n;
                        long total = 0;
                        while ((n = cipherStream.read(buf)) > 0) {
                            os.write(buf, 0, n);
                            total += n;
                        }
                        return new PutResult(objectKey, total);
                    }
                case TYPE_ALIYUN: {
                    SysStorageConfig c = requireConfig();
                    String ask = StorageCredentialCrypto.decryptPlain(c.getAliyunSecretCipher());
                    boolean accel = Boolean.TRUE.equals(c.getAliyunTransferAccel());
                    OSS oss = ossClientPool.acquire(trim(c.getAliyunEndpoint()), trim(c.getAliyunAccessKeyId()), ask, accel);
                    ObjectMetadata meta = new ObjectMetadata();
                    if (contentLength > 0) {
                        meta.setContentLength(contentLength);
                    }
                    oss.putObject(bucket, objectKey, cipherStream, meta);
                    return new PutResult(objectKey, contentLength);
                }
                case TYPE_TENCENT: {
                    SysStorageConfig t = requireConfig();
                    String tsk = StorageCredentialCrypto.decryptPlain(t.getTencentSecretKeyCipher());
                    boolean accel = Boolean.TRUE.equals(t.getTencentGlobalAccel());
                    COSClient cos = cosClientPool.acquire(trim(t.getTencentRegion()), trim(t.getTencentSecretId()), tsk, accel);
                    com.qcloud.cos.model.ObjectMetadata m = new com.qcloud.cos.model.ObjectMetadata();
                    if (contentLength > 0) {
                        m.setContentLength(contentLength);
                    }
                    PutObjectRequest req = new PutObjectRequest(bucket, objectKey, cipherStream, m);
                    cos.putObject(req);
                    return new PutResult(objectKey, contentLength);
                }
                default:
                    throw new JeecgBootException("未知 storageType: " + storageType);
            }
        } catch (JeecgBootException e) {
            throw e;
        } catch (Exception e) {
            throw new JeecgBootException("[CSE] 写密文失败: " + e.getMessage());
        }
    }

    /**
     * 读密文流（调用方负责 close）
     */
    public InputStream openCipher(String storageType, String bucket, String objectKey) {
        try {
            switch (storageType) {
                case TYPE_LOCAL:
                    Path uploadRoot = Paths.get(uploadpath).toAbsolutePath().normalize();
                    Path target = uploadRoot.resolve(objectKey).normalize();
                    if (!target.startsWith(uploadRoot)) {
                        throw new JeecgBootException("非法 objectKey");
                    }
                    File f = target.toFile();
                    if (!f.exists() || !f.isFile()) {
                        throw new JeecgBootException("文件不存在");
                    }
                    return new FileInputStream(f);
                case TYPE_ALIYUN: {
                    SysStorageConfig c = requireConfig();
                    String ask = StorageCredentialCrypto.decryptPlain(c.getAliyunSecretCipher());
                    boolean accel = Boolean.TRUE.equals(c.getAliyunTransferAccel());
                    OSS oss = ossClientPool.acquire(trim(c.getAliyunEndpoint()), trim(c.getAliyunAccessKeyId()), ask, accel);
                    OSSObject obj = oss.getObject(bucket, objectKey);
                    return obj.getObjectContent();
                }
                case TYPE_TENCENT: {
                    SysStorageConfig t = requireConfig();
                    String tsk = StorageCredentialCrypto.decryptPlain(t.getTencentSecretKeyCipher());
                    boolean accel = Boolean.TRUE.equals(t.getTencentGlobalAccel());
                    COSClient cos = cosClientPool.acquire(trim(t.getTencentRegion()), trim(t.getTencentSecretId()), tsk, accel);
                    COSObject obj = cos.getObject(bucket, objectKey);
                    return obj.getObjectContent();
                }
                default:
                    throw new JeecgBootException("未知 storageType: " + storageType);
            }
        } catch (JeecgBootException e) {
            throw e;
        } catch (Exception e) {
            throw new JeecgBootException("[CSE] 读密文失败: " + e.getMessage());
        }
    }

    public static class PutResult {
        public final String objectKey;
        public final long size;

        public PutResult(String objectKey, long size) {
            this.objectKey = objectKey;
            this.size = size;
        }
    }

    private SysStorageConfig requireConfig() {
        SysStorageConfig c = storageConfigMapper.selectById(SysStorageConfig.ID_SINGLETON);
        if (c == null) {
            throw new JeecgBootException("存储配置不存在");
        }
        return c;
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
