package com.rentar.exception;

public class ReservaNoEncontradaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReservaNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
