package org.jeecg.modules.system.storage;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.PutObjectResult;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.constant.SymbolConstant;
import org.jeecg.common.util.CommonUtils;
import org.jeecg.common.util.filter.SsrfFileTypeFilter;
import org.jeecg.common.util.filter.StrAttackFilter;
import org.jeecg.common.util.oConvertUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 使用数据库中的 OSS 参数上传（独立 OSS 客户端，不污染静态 OssBootUtil）
 */
@Slf4j
public final class AliyunDynamicOssUpload {

    private AliyunDynamicOssUpload() {
    }

    /** @param transferAccelerate 开启后 SDK 使用 oss-accelerate.aliyuncs.com */
    public static String uploadMultipart(MultipartFile file, String fileDir,
                                        String endpoint, String bucket, String accessKeyId, String accessKeySecret,
                                        String staticDomain, boolean transferAccelerate) throws Exception {
        SsrfFileTypeFilter.checkUploadFileType(file);
        String orgName = file.getOriginalFilename();
        if ("".equals(orgName)) {
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
        String objectKey = fileDir + fileName;
        try (InputStream in = new BufferedInputStream(file.getInputStream())) {
            return putStream(in, file.getSize(), objectKey, endpoint, bucket, accessKeyId, accessKeySecret, staticDomain, transferAccelerate);
        }
    }

    public static String uploadBytes(byte[] data, String fileDir, String fileName,
                                     String endpoint, String bucket, String accessKeyId, String accessKeySecret,
                                     String staticDomain, boolean transferAccelerate) throws Exception {
        if (!fileDir.endsWith(SymbolConstant.SINGLE_SLASH)) {
            fileDir = fileDir.concat(SymbolConstant.SINGLE_SLASH);
        }
        fileDir = StrAttackFilter.filter(fileDir);
        String objectKey = fileDir + fileName;
        try (InputStream in = new ByteArrayInputStream(data)) {
            return putStream(in, data.length, objectKey, endpoint, bucket, accessKeyId, accessKeySecret, staticDomain, transferAccelerate);
        }
    }

    private static String putStream(InputStream in, long contentLength, String objectKey,
                                    String endpoint, String bucket, String accessKeyId, String accessKeySecret,
                                    String staticDomain, boolean transferAccelerate) {
        String endpointForClient = transferAccelerate ? "https://oss-accelerate.aliyuncs.com" : endpointWithScheme(endpoint);
        OSS oss = new OSSClientBuilder().build(endpointForClient, accessKeyId, accessKeySecret);
        try {
            com.aliyun.oss.model.ObjectMetadata meta = new com.aliyun.oss.model.ObjectMetadata();
            meta.setContentLength(contentLength);
            PutObjectResult result = oss.putObject(bucket, objectKey, in, meta);
            String filePath;
            if (oConvertUtils.isNotEmpty(staticDomain) && staticDomain.toLowerCase().startsWith(CommonConstant.STR_HTTP)) {
                String dom = staticDomain.endsWith("/") ? staticDomain.substring(0, staticDomain.length() - 1) : staticDomain;
                filePath = dom + SymbolConstant.SINGLE_SLASH + objectKey;
            } else {
                String host = transferAccelerate ? "oss-accelerate.aliyuncs.com" : endpointHostOnly(endpoint);
                filePath = "https://" + bucket + "." + host + SymbolConstant.SINGLE_SLASH + objectKey;
            }
            if (result != null) {
                log.debug("OSS dynamic upload ok: {}", objectKey);
            }
            return filePath;
        } finally {
            oss.shutdown();
        }
    }

    /** SDK 需要带协议的 Endpoint */
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

    /** 虚拟主机风格访问 URL 仅使用域名部分，避免 endpoint 含 https:// 时拼出非法地址 */
    private static String endpointHostOnly(String endpoint) {
        if (endpoint == null) {
            return "";
        }
        String e = endpoint.trim();
        String lower = e.toLowerCase();
        if (lower.startsWith("https://")) {
            return e.substring(8);
        }
        if (lower.startsWith("http://")) {
            return e.substring(7);
        }
        return e;
    }
}
