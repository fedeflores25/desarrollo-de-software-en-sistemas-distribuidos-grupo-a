package com.rentar.maintenance.service;

import com.rentar.maintenance.entity.SolicitudMantenimiento;
import java.time.LocalDateTime;
import java.util.Optional;

public interface IMantenimientoService {

    Optional<SolicitudMantenimiento> registrarYProcesar(Long reservaId, Long vehiculoId,
                                                         LocalDateTime fechaInicio, LocalDateTime fechaFin,
                                                         String eventId, String correlationId);
}
