package com.biopet.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException(String email) {
        super("El email ya se encuentra registrado: " + email);
    }
}
