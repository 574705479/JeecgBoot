package org.jeecg.common.license.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

public class LicenseCacheManager {

    private static final Logger log = LoggerFactory.getLogger(LicenseCacheManager.class);
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int PBKDF2_ITERATIONS = 10000;

    private final ObjectMapper objectMapper;
    private final SecretKey encryptionKey;
    private final Path cacheFile;
    private final Path tmpFile;

    public LicenseCacheManager(String cacheDir, String appId, String appSecret, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        File dir = new File(cacheDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.cacheFile = Path.of(cacheDir, appId + ".dat");
        this.tmpFile = Path.of(cacheDir, appId + ".dat.tmp");
        this.encryptionKey = deriveKey(appSecret, appId);
    }

    public void save(LicenseInfo info) {
        try {
            String json = objectMapper.writeValueAsString(info);
            byte[] encrypted = encrypt(json.getBytes(StandardCharsets.UTF_8));
            Files.write(tmpFile, encrypted);
            Files.move(tmpFile, cacheFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            log.debug("[License] Cache saved successfully");
        } catch (Exception e) {
            log.warn("[License] Failed to save cache: {}", e.getMessage());
        }
    }

    public LicenseInfo load() {
        try {
            if (Files.exists(cacheFile)) {
                byte[] encrypted = Files.readAllBytes(cacheFile);
                byte[] decrypted = decrypt(encrypted);
                return objectMapper.readValue(decrypted, LicenseInfo.class);
            }
        } catch (Exception e) {
            log.warn("[License] Failed to load cache from main file: {}", e.getMessage());
            try {
                if (Files.exists(tmpFile)) {
                    byte[] encrypted = Files.readAllBytes(tmpFile);
                    byte[] decrypted = decrypt(encrypted);
                    return objectMapper.readValue(decrypted, LicenseInfo.class);
                }
            } catch (Exception ex) {
                log.warn("[License] Failed to load cache from tmp file: {}", ex.getMessage());
            }
        }
        return null;
    }

    public void clear() {
        try {
            Files.deleteIfExists(cacheFile);
            Files.deleteIfExists(tmpFile);
        } catch (Exception e) {
            log.warn("[License] Failed to clear cache: {}", e.getMessage());
        }
    }

    private SecretKey deriveKey(String appSecret, String salt) {
        try {
            KeySpec spec = new PBEKeySpec(appSecret.toCharArray(), salt.getBytes(StandardCharsets.UTF_8), PBKDF2_ITERATIONS, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("[License] Failed to derive encryption key", e);
        }
    }

    private byte[] encrypt(byte[] data) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] ciphertext = cipher.doFinal(data);
        ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        buffer.put(iv);
        buffer.put(ciphertext);
        return buffer.array();
    }

    private byte[] decrypt(byte[] data) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
        return cipher.doFinal(ciphertext);
    }
}
