package com.rentar.rental.exception;

/** El estado actual de la reserva no permite la operacion (por ejemplo, cancelar una que ya comenzo). */
public class ReservaEstadoInvalidoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReservaEstadoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
