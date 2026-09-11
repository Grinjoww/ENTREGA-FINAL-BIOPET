package com.biopet.exception;

public class RateLimitExceededException extends RuntimeException {
    private final long secondsRemaining;

    /**
     * Creates the exception with the remaining lockout time.
     *
     * @param secondsRemaining seconds until the blocked address may retry
     */
    public RateLimitExceededException(long secondsRemaining) {
        super("Se ha excedido el número máximo de intentos fallidos de inicio de sesión. Intente nuevamente en "
                + secondsRemaining + " segundos.");
        this.secondsRemaining = secondsRemaining;
    }

    /**
     * Returns the remaining lockout time, used for the {@code Retry-After} header.
     *
     * @return seconds until the blocked address may retry
     */
    public long getSecondsRemaining() {
        return secondsRemaining;
    }
}
