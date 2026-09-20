package com.rentar.exception;

public class VehiculoNoEncontradoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public VehiculoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
