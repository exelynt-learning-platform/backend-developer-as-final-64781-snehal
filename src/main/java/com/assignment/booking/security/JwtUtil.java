package com.assignment.booking.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtUtil {

    private static final String PLACEHOLDER_SECRET = "change-this-super-secret-key-in-production-min-32-chars";

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${app.jwt.require-strong-secret:false}")
    private boolean requireStrongSecret;

    @PostConstruct
    void warnIfUsingPlaceholderSecret() {
        if (PLACEHOLDER_SECRET.equals(secret)) {
            if (requireStrongSecret) {
                throw new IllegalStateException(
                        "app.jwt.secret is still the example placeholder. Set JWT_SECRET to a unique, "
                                + "random 32+ character value (app.jwt.require-strong-secret is enabled).");
            }
            log.warn("=================================================================================");
            log.warn("SECURITY WARNING: app.jwt.secret is still set to the example placeholder value.");
            log.warn("Set the JWT_SECRET environment variable to a unique, random 32+ character value");
            log.warn("before deploying this application anywhere other than local development.");
            log.warn("=================================================================================");
        }
    }

    private SecretKey getSigningKey()  {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return extractedUsername.equals(username) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
