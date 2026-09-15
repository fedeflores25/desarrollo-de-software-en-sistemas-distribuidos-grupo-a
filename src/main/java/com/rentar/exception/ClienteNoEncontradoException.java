package com.rentar.exception;

public class ClienteNoEncontradoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ClienteNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}