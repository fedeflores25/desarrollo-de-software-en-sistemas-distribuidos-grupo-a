package com.rentar.vehicle.events;

/** Evento interno emitido al registrar una solicitud externa. */
public record VehiculoExternoRegistradoEvent(
        Long vehiculoExternoId,
        Long clienteId,
        String patente) {
}