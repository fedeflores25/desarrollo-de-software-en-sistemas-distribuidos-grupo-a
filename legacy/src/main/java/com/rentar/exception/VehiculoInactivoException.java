package com.rentar.exception;

public class VehiculoInactivoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public VehiculoInactivoException(String mensaje) {
        super(mensaje);
    }
}
