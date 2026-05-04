package com.airecruiter.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    private SecretKey getKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 256 bits (32 bytes)");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UserDetails userDetails, Map<String, Object> extraClaims) {
        return buildToken(extraClaims, userDetails.getUsername(), expiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(Map.of(), userDetails.getUsername(), refreshExpiration);
    }

    private String buildToken(Map<String, Object> claims, String subject, long ttl) {
        return Jwts.builder()
            .claims(claims)
            .subject(subject)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + ttl))
            .signWith(getKey())
            .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getPayload().getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isExpired(token);
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    private boolean isExpired(String token) {
        return parseClaims(token).getPayload().getExpiration().before(new Date());
    }

    private Jws<Claims> parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(getKey())
            .build()
            .parseSignedClaims(token);
    }
}
