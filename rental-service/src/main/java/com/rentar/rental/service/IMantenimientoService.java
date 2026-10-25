package com.rentar.rental.service;

import java.util.Collection;
import java.util.Map;

import com.rentar.rental.entity.enums.EstadoMantenimiento;

public interface IMantenimientoService {

    /** Deja registrado el mantenimiento de una reserva recien confirmada, en estado PENDIENTE. */
    void registrarPendiente(Long reservaId);

    /**
     * Guarda el resultado informado por el Maintenance Service. Solo se aplica si el
     * mantenimiento sigue PENDIENTE, asi un evento duplicado no cambia nada.
     *
     * @return true si el resultado se guardo; false si se ignoro (duplicado o reserva sin registro)
     */
    boolean registrarResultado(String eventId, Long reservaId, EstadoMantenimiento resultado);

    /** Estado del mantenimiento de cada reserva. Las reservas sin registro no figuran en el mapa. */
    Map<Long, EstadoMantenimiento> estados(Collection<Long> reservaIds);
}
