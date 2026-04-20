package org.jeecg.modules.system.security.cse.service;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.oss.entity.OssFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;

/**
 * CSE 感知的图片字节读取器：把 cse://{fid} 解密成明文字节，供 AutoPoi 等导出场景直接拿到 byte[] 嵌入 Excel/PDF。
 *
 * 调用方一般只需 {@link #readBytes(String)}：
 * <pre>
 *   byte[] img = cseAwareImageReader.readBytes(record.getAvatar());
 *   if (img != null) {
 *       sheet.embedImage(img);
 *   }
 * </pre>
 *
 * 与浏览器侧的 cseDecrypt 不同：
 *  - 这里直接拿到服务器内的 KEK，无需 HKDF 二次包装
 *  - 不写明文落盘，临时缓冲区随返回字节数组释放
 *  - 不做权限校验：调用方需保证已通过业务侧鉴权
 */
@Slf4j
@Service
public class CseAwareImageReader {

    public static final String CSE_PREFIX = "cse://";

    @Autowired
    private OssFileMetaService metaService;

    @Autowired
    private CseFileStorage cseFileStorage;

    @Autowired
    private FileEncryptionService fileEncryptionService;

    /**
     * 把 url（可能是 cse:// 或普通 http(s) 直链）转成图片字节。
     * 普通直链返回 null（由调用方继续用 AutoPoi 默认 URL fetcher）。
     */
    public byte[] readBytes(String url) {
        if (url == null || !url.startsWith(CSE_PREFIX)) {
            return null;
        }
        String fid = url.substring(CSE_PREFIX.length());
        return readBytesByFid(fid);
    }

    public byte[] readBytesByFid(String fid) {
        if (fid == null || fid.isEmpty()) return null;
        OssFile file = metaService.getByFileId(fid);
        if (file == null) {
            log.warn("[CSE] readBytesByFid 未找到文件 fid={}", fid);
            return null;
        }
        byte[] iv = Base64.getDecoder().decode(file.getIvB64());
        byte[] wrapped = Base64.getDecoder().decode(file.getDekWrappedB64());
        byte[] dek = fileEncryptionService.unwrapDek(wrapped, file.getKekKid());
        try (InputStream in = cseFileStorage.openCipher(file.getStorageType(), file.getBucket(), file.getObjectKey())) {
            ByteArrayOutputStream cipher = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) cipher.write(buf, 0, n);
            return fileEncryptionService.decryptBytes(cipher.toByteArray(), iv, dek, fid);
        } catch (Exception e) {
            log.warn("[CSE] readBytes 失败 fid={} reason={}", fid, e.getMessage());
            return null;
        } finally {
            FileEncryptionService.clear(dek);
        }
    }
}
