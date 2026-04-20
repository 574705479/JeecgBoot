package org.jeecg.modules.system.security.cse.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.exception.JeecgBootException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * 文件加解密服务：AES-256-GCM 数据加密 + AES-Wrap 风格的 DEK 包装。
 * <p>
 * 设计：
 * - 数据 IV 12B；GCM tag 16B 自带在 doFinal 输出末尾
 * - DEK 32B 随机；用 KEK 走 AES/GCM/NoPadding 包装（独立 IV，存 dek_wrapped_b64 头部 12B）
 * - HKDF-SHA256 用于前端 token 派生二次包装密钥
 */
@Slf4j
@Service
public class FileEncryptionService {

    public static final String ALGO = "AES-256-GCM";
    private static final int DEK_LEN = 32;
    private static final int IV_LEN = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int STREAM_BUF = 8192;

    @Autowired
    private KekProvider kekProvider;

    @Data
    public static class EncryptResult {
        /** 加密后输入流（含 tag，不含 IV，IV 单独传） */
        private InputStream cipherStream;
        /** 12B IV */
        private byte[] iv;
        /** 用 KEK 包装后的 DEK（含 12B IV 前缀） */
        private byte[] dekWrapped;
        /** 使用的 KEK kid */
        private String kid;
        /** 明文 DEK，调用方加密结束后请尽快置零 */
        private byte[] dekClear;
    }

    /**
     * 加密一段输入流，返回密文流 + IV + 包装后的 DEK
     */
    public EncryptResult encryptStream(InputStream in, String fileId) {
        try {
            byte[] dek = new byte[DEK_LEN];
            new SecureRandom().nextBytes(dek);
            byte[] iv = new byte[IV_LEN];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(dek, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));
            // AAD 绑定 fileId，防 fid 替换
            if (fileId != null) {
                cipher.updateAAD(fileId.getBytes(StandardCharsets.UTF_8));
            }

            CipherInputStream cis = new CipherInputStream(in, cipher);

            byte[] dekWrapped = wrapDek(dek, kekProvider.getActiveKek());
            EncryptResult r = new EncryptResult();
            r.setCipherStream(cis);
            r.setIv(iv);
            r.setDekWrapped(dekWrapped);
            r.setKid(kekProvider.getActiveKid());
            r.setDekClear(dek);
            return r;
        } catch (Exception e) {
            throw new JeecgBootException("[CSE] 加密失败: " + e.getMessage());
        }
    }

    /**
     * 加密一段字节
     */
    public EncryptResult encryptBytes(byte[] data, String fileId) {
        try {
            EncryptResult r = encryptStream(new ByteArrayInputStream(data), fileId);
            try (InputStream cs = r.getCipherStream(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                byte[] buf = new byte[STREAM_BUF];
                int n;
                while ((n = cs.read(buf)) > 0) {
                    bos.write(buf, 0, n);
                }
                r.setCipherStream(new ByteArrayInputStream(bos.toByteArray()));
            }
            return r;
        } catch (Exception e) {
            throw new JeecgBootException("[CSE] 加密失败: " + e.getMessage());
        }
    }

    /**
     * 解密密文（含 GCM tag）→ 明文字节
     */
    public byte[] decryptBytes(byte[] cipherBytes, byte[] iv, byte[] dekClear, String fileId) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(dekClear, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));
            if (fileId != null) {
                cipher.updateAAD(fileId.getBytes(StandardCharsets.UTF_8));
            }
            return cipher.doFinal(cipherBytes);
        } catch (Exception e) {
            throw new JeecgBootException("[CSE] 解密失败: " + e.getMessage());
        }
    }

    /**
     * 用 KEK 包装 DEK（AES-GCM, IV 拼前缀）
     */
    public byte[] wrapDek(byte[] dek, byte[] kek) {
        try {
            byte[] iv = new byte[IV_LEN];
            new SecureRandom().nextBytes(iv);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(kek, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] wrapped = c.doFinal(dek);
            byte[] out = new byte[IV_LEN + wrapped.length];
            System.arraycopy(iv, 0, out, 0, IV_LEN);
            System.arraycopy(wrapped, 0, out, IV_LEN, wrapped.length);
            return out;
        } catch (Exception e) {
            throw new JeecgBootException("[CSE] DEK wrap 失败: " + e.getMessage());
        }
    }

    /**
     * 用指定 kid 的 KEK 解包 DEK
     */
    public byte[] unwrapDek(byte[] wrapped, String kid) {
        try {
            byte[] kek = kekProvider.getKek(kid);
            byte[] iv = Arrays.copyOfRange(wrapped, 0, IV_LEN);
            byte[] body = Arrays.copyOfRange(wrapped, IV_LEN, wrapped.length);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(kek, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, iv));
            return c.doFinal(body);
        } catch (Exception e) {
            throw new JeecgBootException("[CSE] DEK unwrap 失败: " + e.getMessage());
        }
    }

    /**
     * HKDF-SHA256 派生密钥（前后端必须使用相同实现）
     * info = "cse:dek-wrap" 固定字符串
     */
    public byte[] hkdfSha256(byte[] ikm, byte[] salt, byte[] info, int length) {
        try {
            // Extract: PRK = HMAC-SHA256(salt, IKM)
            if (salt == null || salt.length == 0) {
                salt = new byte[32];
            }
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(salt, "HmacSHA256"));
            byte[] prk = mac.doFinal(ikm);

            // Expand: T(i) = HMAC-SHA256(PRK, T(i-1) | info | i)
            int n = (length + 31) / 32;
            byte[] okm = new byte[n * 32];
            byte[] prev = new byte[0];
            for (int i = 1; i <= n; i++) {
                Mac m = Mac.getInstance("HmacSHA256");
                m.init(new SecretKeySpec(prk, "HmacSHA256"));
                m.update(prev);
                if (info != null) {
                    m.update(info);
                }
                m.update((byte) i);
                prev = m.doFinal();
                System.arraycopy(prev, 0, okm, (i - 1) * 32, 32);
            }
            return Arrays.copyOf(okm, length);
        } catch (Exception e) {
            throw new JeecgBootException("[CSE] HKDF 失败: " + e.getMessage());
        }
    }

    /**
     * 用前端 token 派生的二次密钥 + 文件 IV 包装明文 DEK 返回给前端
     * 前端用同样的派生与 IV 解出 DEK 再解密文件
     */
    public byte[] sealDekForClient(byte[] dekClear, String token, String fileId, String kid, byte[] fileIv) {
        try {
            byte[] ikm = (token + "|" + kid).getBytes(StandardCharsets.UTF_8);
            byte[] salt = fileId.getBytes(StandardCharsets.UTF_8);
            byte[] sk = hkdfSha256(ikm, salt, "cse:dek-wrap".getBytes(StandardCharsets.UTF_8), 32);
            // 用 fileIv 直接做 GCM IV（同一文件唯一，且与文件 IV 解耦——这里其实是另一段密文，独立但等同安全）
            // 为避免与文件加密的 (key,iv) 重复：用 file_iv 字节反转作派生 IV
            byte[] derivedIv = new byte[IV_LEN];
            for (int i = 0; i < IV_LEN; i++) {
                derivedIv[i] = (byte) (fileIv[IV_LEN - 1 - i] ^ 0x5A);
            }
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sk, "AES"),
                    new GCMParameterSpec(GCM_TAG_BITS, derivedIv));
            return c.doFinal(dekClear);
        } catch (Exception e) {
            throw new JeecgBootException("[CSE] DEK 二次包装失败: " + e.getMessage());
        }
    }

    public static void clear(byte[] arr) {
        if (arr != null) {
            Arrays.fill(arr, (byte) 0);
        }
    }
}
