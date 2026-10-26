package com.rentar.vehicle.exception;

public class SolicitudVehiculoExternoNoEncontradaException
        extends RuntimeException {

    public SolicitudVehiculoExternoNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}