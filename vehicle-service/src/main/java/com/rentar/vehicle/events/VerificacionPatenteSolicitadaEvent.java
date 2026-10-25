package com.rentar.vehicle.events;

/** Evento interno emitido una vez confirmada el alta de un vehículo. */
public record VerificacionPatenteSolicitadaEvent(Long vehiculoId, String patente) {
}
