package com.biopet.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenBlacklistService {
    private static final String PREFIX = "jwt:blacklist:";
    private final StringRedisTemplate redisTemplate;

    /**
     * Creates the blacklist backed by Redis.
     *
     * @param redisTemplate template used to store revoked token identifiers
     */
    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Marks a token as revoked until it would have expired naturally.
     *
     * <p>Already expired tokens are ignored because they are rejected by verification anyway.
     *
     * @param jti unique identifier of the token to revoke
     * @param expiresAt expiration instant of the token, bounding the blacklist entry lifetime
     */
    public void revoke(String jti, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (!ttl.isNegative() && !ttl.isZero()) {
            redisTemplate.opsForValue().set(PREFIX + jti, "revoked", ttl);
        }
    }

    /**
     * Checks whether a token was revoked.
     *
     * @param jti unique identifier of the token to check
     * @return true when the identifier is present in the blacklist
     */
    public boolean isRevoked(String jti) {
        Boolean exists = redisTemplate.hasKey(PREFIX + jti);
        return Boolean.TRUE.equals(exists);
    }
}
