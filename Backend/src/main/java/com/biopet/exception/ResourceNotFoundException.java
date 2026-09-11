package com.biopet.exception;

public class ResourceNotFoundException extends RuntimeException {
    /**
     * Creates the exception with a description of the missing resource.
     *
     * @param mensaje description of the resource that was not found
     */
    public ResourceNotFoundException(String mensaje) {
        super(mensaje);
    }
}
