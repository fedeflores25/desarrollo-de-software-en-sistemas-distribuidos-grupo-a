package com.rentar.rental.exception;

/** El vehiculo ya tiene una reserva confirmada que se solapa con el periodo pedido. */
public class VehiculoNoDisponibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public VehiculoNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
