package org.jeecg.modules.system.storage;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.constant.SymbolConstant;
import org.jeecg.common.util.CommonUtils;
import org.jeecg.common.util.SpringContextUtils;
import org.jeecg.common.util.filter.SsrfFileTypeFilter;
import org.jeecg.common.util.filter.StrAttackFilter;
import org.jeecg.common.util.oConvertUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 腾讯云 COS 上传（数据库配置）
 */
@Slf4j
public final class TencentCosUpload {

    private TencentCosUpload() {
    }

    /** @param globalAccelerate 开启后使用 cos.accelerate.myqcloud.com */
    public static String uploadMultipart(MultipartFile file, String fileDir,
                                         String region, String bucket, String secretId, String secretKey,
                                         String customDomain, boolean globalAccelerate) throws Exception {
        SsrfFileTypeFilter.checkUploadFileType(file);
        String orgName = file.getOriginalFilename();
        if (orgName == null || orgName.isEmpty()) {
            orgName = file.getName();
        }
        orgName = CommonUtils.getFileName(orgName);
        String fileName = orgName.indexOf(".") == -1
                ? orgName + "_" + System.currentTimeMillis()
                : orgName.substring(0, orgName.lastIndexOf(".")) + "_" + System.currentTimeMillis() + orgName.substring(orgName.lastIndexOf("."));
        if (!fileDir.endsWith(SymbolConstant.SINGLE_SLASH)) {
            fileDir = fileDir.concat(SymbolConstant.SINGLE_SLASH);
        }
        fileDir = StrAttackFilter.filter(fileDir);
        String key = fileDir + fileName;
        try (InputStream in = new BufferedInputStream(file.getInputStream())) {
            return putStream(in, file.getSize(), key, region, bucket, secretId, secretKey, customDomain, globalAccelerate);
        }
    }

    public static String uploadBytes(byte[] data, String fileDir, String fileName,
                                     String region, String bucket, String secretId, String secretKey,
                                     String customDomain, boolean globalAccelerate) throws Exception {
        if (!fileDir.endsWith(SymbolConstant.SINGLE_SLASH)) {
            fileDir = fileDir.concat(SymbolConstant.SINGLE_SLASH);
        }
        fileDir = StrAttackFilter.filter(fileDir);
        String key = fileDir + fileName;
        try (InputStream in = new ByteArrayInputStream(data)) {
            return putStream(in, data.length, key, region, bucket, secretId, secretKey, customDomain, globalAccelerate);
        }
    }

    private static String putStream(InputStream in, long contentLength, String key,
                                    String regionId, String bucket, String secretId, String secretKey,
                                    String customDomain, boolean globalAccelerate) {
        COSClient cosClient = SpringContextUtils.getBean(CosClientPool.class)
                .acquire(regionId, secretId, secretKey, globalAccelerate);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(contentLength);
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, key, in, metadata);
        cosClient.putObject(putObjectRequest);
        if (oConvertUtils.isNotEmpty(customDomain) && (customDomain.startsWith("http://") || customDomain.startsWith("https://"))) {
            String d = customDomain.endsWith("/") ? customDomain.substring(0, customDomain.length() - 1) : customDomain;
            return d + SymbolConstant.SINGLE_SLASH + key;
        }
        java.net.URL u = cosClient.getObjectUrl(bucket, key);
        return u != null ? u.toString() : "";
    }
}
