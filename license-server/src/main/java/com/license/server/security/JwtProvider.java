package com.license.server.security;

import com.license.server.config.LicenseServerProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final long accessTokenExpire;
    private final long refreshTokenExpire;
    private final Set<String> blacklist = ConcurrentHashMap.newKeySet();

    public JwtProvider(LicenseServerProperties properties) {
        String masterKey = properties.getMasterKey();
        if (masterKey == null || masterKey.isBlank()) {
            masterKey = "default-dev-key-not-for-production-use-0123456789abcdef";
        }
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(("jwt-" + masterKey).getBytes(StandardCharsets.UTF_8));
            this.key = Keys.hmacShaKeyFor(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive JWT key", e);
        }
        this.accessTokenExpire = properties.getJwt().getAccessTokenExpire();
        this.refreshTokenExpire = properties.getJwt().getRefreshTokenExpire();
    }

    public String createAccessToken(Long userId, String username) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpire * 1000))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(Long userId, String username) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpire * 1000))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        if (blacklist.contains(token)) {
            throw new JwtException("Token has been revoked");
        }
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public void blacklistToken(String token) {
        blacklist.add(token);
    }

    public long getAccessTokenExpire() {
        return accessTokenExpire;
    }
}
