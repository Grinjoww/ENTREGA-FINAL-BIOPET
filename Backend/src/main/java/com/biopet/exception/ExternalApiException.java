package com.biopet.exception;

public class ExternalApiException extends RuntimeException {
    /**
     * Creates the exception with a user-facing message.
     *
     * @param mensaje description of the external service failure
     */
    public ExternalApiException(String mensaje) {
        super(mensaje);
    }

    /**
     * Creates the exception with a user-facing message and its upstream cause.
     *
     * @param mensaje description of the external service failure
     * @param causa the underlying transport or HTTP failure
     */
    public ExternalApiException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}