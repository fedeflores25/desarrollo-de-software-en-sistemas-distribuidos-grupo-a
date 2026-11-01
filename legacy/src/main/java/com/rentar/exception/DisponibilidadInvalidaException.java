package com.rentar.exception;

public class DisponibilidadInvalidaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DisponibilidadInvalidaException(String mensaje) {
        super(mensaje);
    }
}