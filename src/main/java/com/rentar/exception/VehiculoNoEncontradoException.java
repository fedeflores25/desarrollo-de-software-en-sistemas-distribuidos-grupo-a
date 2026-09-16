package com.rentar.exception;

/**
 * Nota: si al integrar el ABM de vehículos (equipo: Federico) ya existe una excepción
 * equivalente, unificar en una sola y eliminar esta duplicada.
 */
public class VehiculoNoEncontradoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public VehiculoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
