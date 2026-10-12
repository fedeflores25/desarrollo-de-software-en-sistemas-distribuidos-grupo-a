package com.rentar.rental.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.rentar.rental.entity.Reserva;

public interface IReservaService {

    /**
     * Crea una reserva CONFIRMADA. El cliente y el vehiculo ya fueron verificados por el
     * Gateway; aca se validan las fechas y el solapamiento, y se calculan dias e importe.
     */
    Reserva crear(Long clienteId, Long vehiculoId, LocalDateTime fechaInicio,
            LocalDateTime fechaFin, BigDecimal precioDiario);

    /** Cancela una reserva CONFIRMADA cuyo periodo todavia no comenzo. */
    Reserva cancelar(Long id);

    Reserva buscarPorId(Long id);

    /** Consulta con filtros opcionales. */
    List<Reserva> listar(FiltroReserva filtro);

    /** Historial del cliente: reservas finalizadas y canceladas. */
    List<Reserva> historial(Long clienteId);

    /** Ids de los vehiculos con una reserva confirmada que se solapa con el periodo. */
    List<Long> idsVehiculosOcupados(LocalDateTime fechaInicio, LocalDateTime fechaFin);
}
