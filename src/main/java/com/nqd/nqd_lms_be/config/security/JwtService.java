package com.nqd.nqd_lms_be.config.security;

import com.nqd.nqd_lms_be.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class JwtService {

    private final String secretKeyString;
    private final int expirationDays;

    public JwtService(
            @Value("${app.jwt.secret:nqd-lms-super-secret-key-for-jwt-signing-at-least-256-bits-long-2026}") String secretKeyString,
            @Value("${app.jwt.expiration-days:7}") int expirationDays
    ) {
        this.secretKeyString = secretKeyString;
        this.expirationDays = expirationDays;
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKeyString.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            // Pad key if shorter than 256 bits (32 bytes) for HMAC-SHA256
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            return Keys.hmacShaKeyFor(padded);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(User user, Set<String> roles) {
        return generateToken(user.getId(), user.getEmail(), user.getFullName(), roles);
    }

    public String generateToken(UUID userId, String email, String fullName, Set<String> roles) {
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        long expMillis = nowMillis + (expirationDays * 24L * 60 * 60 * 1000L);
        Date exp = new Date(expMillis);

        List<String> roleList = roles != null ? new ArrayList<>(roles) : Collections.emptyList();

        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email != null ? email : "")
                .claim("name", fullName != null ? fullName : "")
                .claim("roles", roleList)
                .issuedAt(now)
                .expiration(exp)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            if (token == null || token.isBlank()) {
                return false;
            }
            Claims claims = getClaims(token);
            return claims != null && claims.getExpiration() != null && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID getUserIdFromToken(String token) {
        try {
            Claims claims = getClaims(token);
            return UUID.fromString(claims.getSubject());
        } catch (Exception e) {
            log.warn("Cannot extract userId from JWT: {}", e.getMessage());
            return null;
        }
    }

    public String getEmailFromToken(String token) {
        try {
            Claims claims = getClaims(token);
            return claims.get("email", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public Set<String> getRolesFromToken(String token) {
        try {
            Claims claims = getClaims(token);
            List<String> list = claims.get("roles", List.class);
            return list != null ? new HashSet<>(list) : Collections.emptySet();
        } catch (Exception e) {
            return Collections.emptySet();
        }
    }

    public int getExpirationDays() {
        return expirationDays;
    }
}
