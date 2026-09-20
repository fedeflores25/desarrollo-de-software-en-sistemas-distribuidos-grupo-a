package com.rentar.exception;

/** Se lanza cuando los datos de la reserva no cumplen una regla de negocio (ej: fecha de fin anterior o igual a la de inicio). */
public class ReservaInvalidaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ReservaInvalidaException(String mensaje) {
        super(mensaje);
    }
}
