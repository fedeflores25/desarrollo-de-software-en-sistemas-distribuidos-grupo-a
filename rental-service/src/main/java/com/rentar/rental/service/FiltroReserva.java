package com.rentar.rental.service;

import java.time.LocalDateTime;
import java.util.List;

import com.rentar.rental.entity.enums.EstadoReserva;

/**
 * Filtros opcionales para consultar reservas: un valor nulo (o una lista vacia) no filtra.
 * El rango de fechas se aplica sobre la fecha de inicio de la reserva.
 */
public record FiltroReserva(
        Long clienteId,
        List<Long> vehiculoIds,
        EstadoReserva estado,
        LocalDateTime desde,
        LocalDateTime hasta) {
}
