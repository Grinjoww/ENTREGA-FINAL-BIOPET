package com.biopet.exception;

public class RateLimitExceededException extends RuntimeException {
    private final long secondsRemaining;

    public RateLimitExceededException(long secondsRemaining) {
        super("Se ha excedido el número máximo de intentos fallidos de inicio de sesión. Intente nuevamente en "
                + secondsRemaining + " segundos.");
        this.secondsRemaining = secondsRemaining;
    }

    public long getSecondsRemaining() {
        return secondsRemaining;
    }
}
