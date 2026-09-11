package com.biopet.security;

import com.biopet.exception.RateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRateLimiterServiceTest {

    private static final int MAX_ATTEMPTS = 6;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    private MutableClock reloj;
    private LoginRateLimiterService limiter;

    @BeforeEach
    void setUp() {
        reloj = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        limiter = new LoginRateLimiterService(MAX_ATTEMPTS, WINDOW, BLOCK_DURATION, reloj);
    }

    @Test
    void cincoFallosNoBloquean() {
        String ip = "203.0.113.10";

        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> limiter.recordFailure(ip));
        }

        assertDoesNotThrow(() -> limiter.checkAllowed(ip));
    }

    @Test
    void sextoFalloBloqueaYLanzaExcepcion() {
        String ip = "203.0.113.11";

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(ip);
        }

        RateLimitExceededException ex = assertThrows(RateLimitExceededException.class,
                () -> limiter.recordFailure(ip));

        assertEquals(BLOCK_DURATION.getSeconds(), ex.getSecondsRemaining());
    }

    @Test
    void ipBloqueadaSigueRechazadaConTiempoRestante() {
        String ip = "203.0.113.12";

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(ip);
        }
        assertThrows(RateLimitExceededException.class, () -> limiter.recordFailure(ip));

        reloj.avanzar(Duration.ofMinutes(5));

        RateLimitExceededException ex = assertThrows(RateLimitExceededException.class,
                () -> limiter.checkAllowed(ip));

        assertEquals(Duration.ofMinutes(10).getSeconds(), ex.getSecondsRemaining());
        assertTrue(ex.getSecondsRemaining() > 0);
    }

    @Test
    void ipsDiferentesMantienenContadoresSeparados() {
        String ipBloqueada = "203.0.113.20";
        String ipLibre = "203.0.113.21";

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(ipBloqueada);
        }
        assertThrows(RateLimitExceededException.class, () -> limiter.recordFailure(ipBloqueada));

        assertDoesNotThrow(() -> limiter.checkAllowed(ipLibre));
        assertDoesNotThrow(() -> limiter.recordFailure(ipLibre));
    }

    @Test
    void reiniciarEliminaFallosYBloqueo() {
        String ip = "203.0.113.30";

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(ip);
        }
        assertThrows(RateLimitExceededException.class, () -> limiter.recordFailure(ip));

        limiter.reset(ip);

        assertDoesNotThrow(() -> limiter.checkAllowed(ip));
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> limiter.recordFailure(ip));
        }
        assertDoesNotThrow(() -> limiter.checkAllowed(ip));
    }

    @Test
    void ventanaExpiradaReiniciaElContador() {
        String ip = "203.0.113.40";

        for (int i = 0; i < 3; i++) {
            limiter.recordFailure(ip);
        }

        reloj.avanzar(WINDOW.plusMinutes(1));

        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> limiter.recordFailure(ip));
        }
        assertThrows(RateLimitExceededException.class, () -> limiter.recordFailure(ip));
    }

    @Test
    void bloqueoExpiradoPermiteNuevoIntento() {
        String ip = "203.0.113.50";

        for (int i = 0; i < 5; i++) {
            limiter.recordFailure(ip);
        }
        assertThrows(RateLimitExceededException.class, () -> limiter.recordFailure(ip));

        reloj.avanzar(BLOCK_DURATION.plusSeconds(1));

        assertDoesNotThrow(() -> limiter.checkAllowed(ip));
        for (int i = 0; i < 5; i++) {
            assertDoesNotThrow(() -> limiter.recordFailure(ip));
        }
        assertThrows(RateLimitExceededException.class, () -> limiter.recordFailure(ip));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void avanzar(Duration duracion) {
            instant = instant.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
