package com.notice.system.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;

public final class JwtUtil {

    private JwtUtil() {}

    // Token 密钥（>= 32 字节）
    private static final String SECRET = "notice-system-secret-key-1234567890abcd";

    // 有效期：7 天
    private static final long EXPIRATION = 7L * 24 * 60 * 60 * 1000;

    public static String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public static String parseToken(String token) {
        // 兼容 "Bearer xxx"
        String raw = stripBearer(token);

        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(raw)
                .getBody()
                .getSubject();
    }

    private static String stripBearer(String token) {
        if (token == null) return null;
        String t = token.trim();
        if (t.regionMatches(true, 0, SecurityConstants.TOKEN_PREFIX, 0, SecurityConstants.TOKEN_PREFIX.length())) {
            return t.substring(SecurityConstants.TOKEN_PREFIX.length()).trim();
        }
        return t;
    }
}

