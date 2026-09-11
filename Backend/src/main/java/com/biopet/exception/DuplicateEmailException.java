package com.biopet.exception;

public class DuplicateEmailException extends RuntimeException {
    /**
     * Creates the exception for a registration attempt with an email already in use.
     *
     * @param email the duplicated email address, included in the message
     */
    public DuplicateEmailException(String email) {
        super("El email ya se encuentra registrado: " + email);
    }
}
