package com.rentar.rental.exception;

/** La reserva solicitada no existe. */
public class ReservaNoEncontradaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReservaNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
