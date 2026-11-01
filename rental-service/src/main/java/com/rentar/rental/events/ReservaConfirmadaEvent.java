package com.rentar.rental.events;

import java.time.LocalDateTime;

/**
 * Evento interno que se dispara cuando se confirma una reserva. Un listener lo recibe
 * despues de que la transaccion se guarda y recien ahi publica el evento en RabbitMQ.
 */
public record ReservaConfirmadaEvent(
        Long reservaId,
        Long vehiculoId,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin) {
}
