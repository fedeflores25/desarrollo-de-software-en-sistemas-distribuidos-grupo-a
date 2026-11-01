package com.rentar.vehicle.events;

public record VehiculoExternoValidacionCompletadaEvent(
        Long vehiculoExternoId,
        String correlationId) {
}