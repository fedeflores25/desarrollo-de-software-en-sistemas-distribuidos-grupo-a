package com.rentar.exception;

public class ClienteInactivoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ClienteInactivoException(String mensaje) {
        super(mensaje);
    }
}
