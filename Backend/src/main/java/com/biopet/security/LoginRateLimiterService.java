package com.biopet.security;

import com.biopet.exception.RateLimitExceededException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimiterService {

    private static final String UNKNOWN_KEY = "desconocida";

    private final int maxAttempts;
    private final Duration window;
    private final Duration blockDuration;
    private final Clock clock;

    private final ConcurrentHashMap<String, State> states = new ConcurrentHashMap<>();

    /**
     * Creates the limiter with the configured thresholds and the system clock.
     *
     * @param maxAttempts consecutive failures allowed before blocking an address
     * @param window time window in which failures are counted
     * @param blockDuration how long a blocked address stays blocked
     */
    @Autowired
    public LoginRateLimiterService(
            @Value("${security.rate-limit.login.max-attempts:6}") int maxAttempts,
            @Value("${security.rate-limit.login.window:PT15M}") Duration window,
            @Value("${security.rate-limit.login.block-duration:PT15M}") Duration blockDuration
    ) {
        this(maxAttempts, window, blockDuration, Clock.systemUTC());
    }

    LoginRateLimiterService(int maxAttempts, Duration window, Duration blockDuration, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.blockDuration = blockDuration;
        this.clock = clock;
    }

    /**
     * Verifies that the address is not currently blocked.
     *
     * @param ip client address to check
     * @throws com.biopet.exception.RateLimitExceededException when the address is blocked
     */
    public void checkAllowed(String ip) {
        String clientKey = normalize(ip);
        State current = states.compute(clientKey, (key, state) -> cleanIfExpired(state));
        if (current != null && current.blockedUntil() != null) {
            throw new RateLimitExceededException(secondsRemaining(current.blockedUntil()));
        }
    }

    /**
     * Records a failed login for the address, blocking it once the threshold is reached.
     *
     * @param ip client address of the failed attempt
     * @throws com.biopet.exception.RateLimitExceededException when the attempt triggers a block
     */
    public void recordFailure(String ip) {
        String clientKey = normalize(ip);
        State[] result = new State[1];

        states.compute(clientKey, (key, previous) -> {
            State current = cleanIfExpired(previous);
            Instant now = clock.instant();

            int newFailures = (current == null) ? 1 : current.failures() + 1;
            Instant windowStart = (current == null) ? now : current.windowStart();
            Instant blockedUntil = (newFailures >= maxAttempts) ? now.plus(blockDuration) : null;

            State next = new State(newFailures, windowStart, blockedUntil);
            result[0] = next;
            return next;
        });

        if (result[0].blockedUntil() != null) {
            throw new RateLimitExceededException(secondsRemaining(result[0].blockedUntil()));
        }
    }

    /**
     * Clears the failure history of the address, for example after a successful login.
     *
     * @param ip client address to clear
     */
    public void reset(String ip) {
        states.remove(normalize(ip));
    }

    private State cleanIfExpired(State state) {
        if (state == null) {
            return null;
        }
        Instant now = clock.instant();
        if (state.blockedUntil() != null) {
            return now.isBefore(state.blockedUntil()) ? state : null;
        }
        if (!now.isBefore(state.windowStart().plus(window))) {
            return null;
        }
        return state;
    }

    private long secondsRemaining(Instant blockedUntil) {
        Duration remaining = Duration.between(clock.instant(), blockedUntil);
        if (remaining.isNegative() || remaining.isZero()) {
            return 1;
        }
        long seconds = (remaining.toMillis() + 999) / 1000;
        return Math.max(seconds, 1);
    }

    private String normalize(String ip) {
        if (ip == null || ip.isBlank()) {
            return UNKNOWN_KEY;
        }
        return ip.trim();
    }

    private record State(int failures, Instant windowStart, Instant blockedUntil) {
    }
}
