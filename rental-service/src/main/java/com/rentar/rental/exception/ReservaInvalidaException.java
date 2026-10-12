package com.rentar.rental.exception;

/** Los datos de la reserva no cumplen una regla de negocio (por ejemplo, fechas incoherentes). */
public class ReservaInvalidaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReservaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
