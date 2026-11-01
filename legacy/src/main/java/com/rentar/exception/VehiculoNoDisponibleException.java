package com.rentar.exception;

public class VehiculoNoDisponibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public VehiculoNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
