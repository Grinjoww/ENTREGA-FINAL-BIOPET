package com.biopet.security;

import com.biopet.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final SecretKey key;
    private final long expirationMs;
    private final long refreshExpirationMs;
    private final String issuer;
    private final String audience;

    /**
     * Creates the token service from the configured JWT settings.
     *
     * @param secret HMAC secret of at least 32 bytes, without any default value
     * @param expirationMs lifetime of access tokens in milliseconds
     * @param refreshExpirationMs lifetime of refresh tokens in milliseconds
     * @param issuer expected token issuer
     * @param audience expected token audience
     * @throws IllegalArgumentException if the secret is shorter than 32 bytes
     */
    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-ms}") long expirationMs,
            @Value("${security.jwt.refresh-expiration-ms}") long refreshExpirationMs,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.audience}") String audience
    ) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET debe tener al menos 256 bits (32 bytes)");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
        this.issuer = issuer;
        this.audience = audience;
    }

    /**
     * Issues a short-lived access token for the given user.
     *
     * @param usuario the authenticated user the token is issued for
     * @return signed JWT with type {@code access}
     */
    public String generateAccessToken(User usuario) {
        return buildToken(usuario, expirationMs, "access");
    }

    /**
     * Issues a long-lived refresh token for the given user.
     *
     * @param usuario the authenticated user the token is issued for
     * @return signed JWT with type {@code refresh}
     */
    public String generateRefreshToken(User usuario) {
        return buildToken(usuario, refreshExpirationMs, "refresh");
    }

    private String buildToken(User usuario, long ttlMs, String tipo) {
        Instant now = Instant.now();
        Instant exp = now.plusMillis(ttlMs);
        return Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(usuario.getId()))
                .audience().add(audience).and()
                .claim("email", usuario.getEmail())
                .claim("rol", usuario.getRol().name())
                .claim("typ", tipo)
                .issuedAt(Date.from(now))
                .notBefore(Date.from(now))
                .expiration(Date.from(exp))
                .id(UUID.randomUUID().toString())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Verifies a token signature and returns its claims.
     *
     * <p>Verification enforces the configured issuer and audience, so tokens
     * from other issuers or audiences are rejected.
     *
     * @param token the compact JWT to verify
     * @return the verified token claims
     * @throws io.jsonwebtoken.JwtException if the token is invalid, expired or has a wrong issuer or audience
     */
    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Reads the email claim of a verified token.
     *
     * @param token the compact JWT to verify
     * @return the email stored in the token
     * @throws io.jsonwebtoken.JwtException if the token is invalid or expired
     */
    public String extractEmail(String token) {
        return extractClaims(token).get("email", String.class);
    }

    /**
     * Reads the unique identifier of a verified token.
     *
     * @param token the compact JWT to verify
     * @return the token identifier used for revocation tracking
     * @throws io.jsonwebtoken.JwtException if the token is invalid or expired
     */
    public String extractJti(String token) {
        return extractClaims(token).getId();
    }

    /**
     * Reads the expiration instant of a verified token.
     *
     * @param token the compact JWT to verify
     * @return when the token expires
     * @throws io.jsonwebtoken.JwtException if the token is invalid or expired
     */
    public Instant extractExpiration(String token) {
        return extractClaims(token).getExpiration().toInstant();
    }

    /**
     * Checks whether a verified token is an access token.
     *
     * @param token the compact JWT to verify
     * @return true when the token type claim is {@code access}
     * @throws io.jsonwebtoken.JwtException if the token is invalid or expired
     */
    public boolean isAccessToken(String token) {
        return "access".equals(extractClaims(token).get("typ", String.class));
    }

    /**
     * Checks whether a verified token is a refresh token.
     *
     * @param token the compact JWT to verify
     * @return true when the token type claim is {@code refresh}
     * @throws io.jsonwebtoken.JwtException if the token is invalid or expired
     */
    public boolean isRefreshToken(String token) {
        return "refresh".equals(extractClaims(token).get("typ", String.class));
    }

    /**
     * Returns the configured access token lifetime.
     *
     * @return lifetime of access tokens in milliseconds
     */
    public long getExpirationMs() {
        return expirationMs;
    }
}
