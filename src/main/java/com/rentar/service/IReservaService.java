package com.rentar.service;

import com.rentar.dto.ReservaRequest;
import com.rentar.entity.Reserva;

public interface IReservaService {

    /** Alta de una reserva. Valida cliente/vehículo, disponibilidad y fechas; calcula el importe total. */
    Reserva crear(ReservaRequest request);

    /** Cancelación de una reserva. Solo permitida si todavía no comenzó el período reservado. */
    Reserva cancelar(Long id);

    Reserva buscarPorId(Long id);
}
