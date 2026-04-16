package org.jeecg.modules.system.storage;

import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.encryption.AesEncryptUtil;
import org.jeecg.common.util.oConvertUtils;

/**
 * 存储密钥入库存储（与 EncryptedString 同源 AES）
 */
public final class StorageCredentialCrypto {

    private StorageCredentialCrypto() {
    }

    public static String encryptPlain(String plain) {
        if (oConvertUtils.isEmpty(plain)) {
            return null;
        }
        try {
            return AesEncryptUtil.encrypt(plain);
        } catch (Exception e) {
            throw new JeecgBootException("密钥加密失败");
        }
    }

    public static String decryptPlain(String cipher) {
        if (oConvertUtils.isEmpty(cipher)) {
            return null;
        }
        try {
            return AesEncryptUtil.desEncrypt(cipher).trim();
        } catch (Exception e) {
            return null;
        }
    }
}
