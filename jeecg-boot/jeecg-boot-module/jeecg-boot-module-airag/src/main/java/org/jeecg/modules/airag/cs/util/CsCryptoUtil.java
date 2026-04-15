package org.jeecg.modules.airag.cs.util;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.cs.config.CsCryptoConfig;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
public class CsCryptoUtil {

    private static final String ENC_PREFIX = "ENC:";
    private static final String AES_ALGORITHM = "AES";
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

    private final SecretKeySpec storageKeySpec;
    private final IvParameterSpec storageIvSpec;
    private final SecretKeySpec transportKeySpec;
    private final IvParameterSpec transportIvSpec;

    public CsCryptoUtil(CsCryptoConfig config) {
        this.storageKeySpec = new SecretKeySpec(config.getStorageKey().getBytes(StandardCharsets.UTF_8), AES_ALGORITHM);
        this.storageIvSpec = new IvParameterSpec(config.getStorageIv().getBytes(StandardCharsets.UTF_8));
        this.transportKeySpec = new SecretKeySpec(config.getTransportKey().getBytes(StandardCharsets.UTF_8), AES_ALGORITHM);
        this.transportIvSpec = new IvParameterSpec(config.getTransportIv().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 存储加密：返回 "ENC:" + Base64(AES-S(plaintext))
     */
    public String encryptStorage(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, storageKeySpec, storageIvSpec);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return ENC_PREFIX + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("存储加密失败", e);
            return plaintext;
        }
    }

    /**
     * 存储解密：有 ENC: 前缀则解密，否则原样返回（兼容历史明文数据）
     */
    public String decryptStorage(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        if (!ciphertext.startsWith(ENC_PREFIX)) {
            return ciphertext;
        }
        try {
            String base64Part = ciphertext.substring(ENC_PREFIX.length());
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, storageKeySpec, storageIvSpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(base64Part));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("存储解密失败", e);
            return ciphertext;
        }
    }

    /**
     * 传输加密：返回 Base64(AES-T(data))
     */
    public String encryptTransport(String data) {
        if (data == null || data.isEmpty()) {
            return data;
        }
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, transportKeySpec, transportIvSpec);
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("传输加密失败", e);
            return data;
        }
    }

    /**
     * 传输解密：AES-T 解密，失败则原样返回（容错）
     */
    public String decryptTransport(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, transportKeySpec, transportIvSpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("传输解密失败", e);
            return ciphertext;
        }
    }
}
