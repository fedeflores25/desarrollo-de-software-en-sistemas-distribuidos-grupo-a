package com.rentar.exception;

/** Se lanza cuando se intenta una operación sobre una reserva cuyo estado actual no lo permite (ej: cancelar una reserva que ya comenzó). */
public class ReservaEstadoInvalidoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReservaEstadoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
