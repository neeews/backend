package com.example.neeews.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtUtil {

    private static final String TYPE_CLAIM = "type";
    private static final String ACCESS_TYPE = "access";
    private static final String REFRESH_TYPE = "refresh";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-expiration}")
    private long accessExpiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    public String generateAccessToken(String email) {
        return buildToken(email, ACCESS_TYPE, accessExpiration);
    }

    public String generateRefreshToken(String email) {
        return buildToken(email, REFRESH_TYPE, refreshExpiration);
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    // 두 토큰은 서명 키가 같아 서명만 보면 리프레시 토큰으로도 API가 통과된다. 종류까지 확인해야 한다.
    public boolean isAccessToken(String token) {
        return parseValidClaims(token)
                .map(claims -> ACCESS_TYPE.equals(claims.get(TYPE_CLAIM, String.class)))
                .orElse(false);
    }

    // type 클레임을 넣기 전에 발급된 리프레시 토큰은 type이 없다. 진위는 DB 대조로 가리므로 함께 받아
    // 배포 직후 전원이 로그아웃되지 않게 한다. 리프레시 만료(7일)가 지나면 null 허용은 지워도 된다.
    public boolean isRefreshToken(String token) {
        return parseValidClaims(token)
                .map(claims -> {
                    String type = claims.get(TYPE_CLAIM, String.class);
                    return type == null || REFRESH_TYPE.equals(type);
                })
                .orElse(false);
    }

    private String buildToken(String subject, String type, long expiration) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .claim(TYPE_CLAIM, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiration))
                .signWith(signingKey())
                .compact();
    }

    private Optional<Claims> parseValidClaims(String token) {
        try {
            return Optional.of(parseClaims(token));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
