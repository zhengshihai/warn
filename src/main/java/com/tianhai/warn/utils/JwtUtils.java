package com.tianhai.warn.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

public class JwtUtils {
    private final SecretKey secretKey;
    private final long defaultExpirationMillis;

    /**
     * 构造方法
     * @param secret 密钥字符串（支持 Base64 编码）
     * @param defaultExpirationMillis 默认过期时间（毫秒）
     */
    public JwtUtils(String secret, long defaultExpirationMillis) {
        this.secretKey = parseSecretKey(secret);
        this.defaultExpirationMillis = defaultExpirationMillis;
    }

    /**
     * 生成 JWT
     * @param subject 用户主键（例如 userId）
     * @param claims 额外载荷（如角色、职位等）
     * @param expirationMillis 过期时间（毫秒），如果为 null 则使用默认值
     */
    public String generateToken(String subject, Map<String, Object> claims, Long expirationMillis) {
        long now = System.currentTimeMillis();
        long exp = now + (expirationMillis != null ? expirationMillis : defaultExpirationMillis);

        return Jwts.builder()
                .setSubject(subject)
                .addClaims(claims)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(exp))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 解析并验证 JWT
     * @param token JWT 字符串
     * @return Claims 对象
     * @throws JwtException 如果无效或过期
     */
    public Claims parseToken(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 是否是 Base64 编码字符串
     */
    private boolean isBase64(String str) {
        try {
            Base64.getDecoder().decode(str);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 根据密钥字符串生成 SecretKey
     */
    private SecretKey parseSecretKey(String secret) {
        byte[] keyBytes = isBase64(secret)
                ? Base64.getDecoder().decode(secret)
                : secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
