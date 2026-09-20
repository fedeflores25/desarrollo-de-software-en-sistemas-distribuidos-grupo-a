package com.rentar.exception;

public class PatenteDuplicadaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PatenteDuplicadaException(String mensaje) {
        super(mensaje);
    }
}
