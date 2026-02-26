package org.jeecg.common.license.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RsaVerifier {

    private static final Logger log = LoggerFactory.getLogger(RsaVerifier.class);
    private final PublicKey publicKey;

    public RsaVerifier(String publicKeyPem) {
        try {
            String base64 = publicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(base64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            this.publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException("[License] Failed to load public key", e);
        }
    }

    public boolean verify(String payload, String signatureBase64) {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(payload.getBytes("UTF-8"));
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            log.error("[License] RSA verification failed", e);
            return false;
        }
    }
}
