package com.edifika.telemetry.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

public final class JwtTestTokens {

    public static final String TEST_SECRET = "edifika-secret-key-must-be-at-least-32-characters-long";
    private static final SecretKey KEY = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));

    private JwtTestTokens() {}

    public static String admin() {
        return createToken(1L, "admin@edifika.com", List.of("ADMIN"));
    }

    public static String resident(long userId) {
        return createToken(userId, "resident" + userId + "@edifika.com", List.of("TENANT"));
    }

    private static String createToken(Long userId, String email, List<String> roles) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(email)
                .claims(Map.of(
                        "user_id", userId,
                        "email", email,
                        "roles", roles
                ))
                .issuedAt(new Date(now))
                .expiration(new Date(now + 3600_000))
                .signWith(KEY)
                .compact();
    }
}
