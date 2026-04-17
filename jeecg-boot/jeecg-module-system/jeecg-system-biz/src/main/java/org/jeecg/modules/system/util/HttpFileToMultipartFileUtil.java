package org.jeecg.modules.system.util;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.MyCommonsMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.Locale;

/**
 * @Description: http 文件转 MultipartFile（含 SSRF 防护与下载大小上限）
 * @author: wangshuai
 * @date: 2025/11/5 17:55
 */
@Slf4j
public class HttpFileToMultipartFileUtil {

    /** 远程下载最大字节数（20MB），防止恶意远端撑爆内存。 */
    private static final long MAX_DOWNLOAD_BYTES = 20L * 1024 * 1024;

    /** 云元数据服务地址，必须显式拒绝（即使是公网 IP 也不允许）。 */
    private static final String CLOUD_META_IP = "169.254.169.254";

    /**
     * 获取
     *
     * @param fileUrl
     * @param filename
     * @return
     * @throws Exception
     */
    public static MultipartFile httpFileToMultipartFile(String fileUrl, String filename) throws Exception {
        byte[] bytes = downloadImageData(fileUrl);
        return convertByteToMultipartFile(bytes, filename);
    }

    /**
     * 下载图片数据（带 SSRF 防护：协议白名单 + DNS 解析 IP 校验 + 禁重定向 + 大小上限）
     */
    private static byte[] downloadImageData(String fileUrl) throws IOException {
        URL url = new URL(fileUrl);

        String protocol = url.getProtocol() == null ? "" : url.getProtocol().toLowerCase(Locale.ROOT);
        if (!"http".equals(protocol) && !"https".equals(protocol)) {
            throw new IOException("仅支持 http/https 协议: " + protocol);
        }

        String host = url.getHost();
        if (host == null || host.isEmpty()) {
            throw new IOException("URL 缺少 host");
        }

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (Exception e) {
            throw new IOException("无法解析域名: " + host);
        }
        if (addresses == null || addresses.length == 0) {
            throw new IOException("无法解析域名: " + host);
        }
        for (InetAddress addr : addresses) {
            if (CLOUD_META_IP.equals(addr.getHostAddress())) {
                throw new IOException("禁止访问云元数据服务: " + addr.getHostAddress());
            }
            if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()
                    || addr.isLinkLocalAddress() || addr.isSiteLocalAddress()
                    || addr.isMulticastAddress()) {
                throw new IOException("禁止访问内网地址: " + addr.getHostAddress());
            }
        }

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        connection.setRequestProperty("Accept", "image/*");

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP 请求失败，响应码: " + responseCode);
            }
            try (InputStream inputStream = connection.getInputStream();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                long total = 0L;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    total += bytesRead;
                    if (total > MAX_DOWNLOAD_BYTES) {
                        throw new IOException("远程文件超过 " + (MAX_DOWNLOAD_BYTES / 1024 / 1024) + "MB 上限");
                    }
                    outputStream.write(buffer, 0, bytesRead);
                }
                return outputStream.toByteArray();
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * byte 转 MultipartFile（直接基于内存构造，避免临时文件遗留）
     */
    private static MultipartFile convertByteToMultipartFile(byte[] data, String fileName) throws IOException {
        return new MyCommonsMultipartFile(new ByteArrayInputStream(data), fileName, "application/octet-stream");
    }
}
