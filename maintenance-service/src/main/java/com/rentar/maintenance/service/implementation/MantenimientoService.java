package com.rentar.maintenance.service.implementation;

import com.rentar.maintenance.entity.SolicitudMantenimiento;
import com.rentar.maintenance.entity.enums.EstadoMantenimiento;
import com.rentar.maintenance.repository.SolicitudMantenimientoRepository;
import com.rentar.maintenance.service.IMantenimientoService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MantenimientoService implements IMantenimientoService {

    private final SolicitudMantenimientoRepository solicitudRepository;
    private final Set<Long> vehiculosNoRealizados;

    public MantenimientoService(SolicitudMantenimientoRepository solicitudRepository,
                                @Value("${maintenance.resultado.no-realizado.vehicle-ids:}") String vehiculosNoRealizados) {
        this.solicitudRepository = solicitudRepository;
        this.vehiculosNoRealizados = parsearIds(vehiculosNoRealizados);
    }

    @Override
    @Transactional
    public Optional<SolicitudMantenimiento> registrarYProcesar(Long reservaId, Long vehiculoId,
                                                                 LocalDateTime fechaInicio, LocalDateTime fechaFin,
                                                                 String eventId, String correlationId) {
        if (solicitudRepository.existsByEventId(eventId) || solicitudRepository.findByReservaId(reservaId).isPresent()) {
            return Optional.empty();
        }

        SolicitudMantenimiento solicitud = new SolicitudMantenimiento(
                reservaId, vehiculoId, fechaInicio, fechaFin, eventId, correlationId);
        solicitud.actualizarEstado(resultadoPara(vehiculoId));
        return Optional.of(solicitudRepository.save(solicitud));
    }

    private EstadoMantenimiento resultadoPara(Long vehiculoId) {
        return vehiculosNoRealizados.contains(vehiculoId)
                ? EstadoMantenimiento.NO_REALIZADO
                : EstadoMantenimiento.REALIZADO;
    }

    private Set<Long> parsearIds(String valores) {
        if (valores == null || valores.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(valores.split(","))
                .map(String::trim)
                .filter(valor -> !valor.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toUnmodifiableSet());
    }
}
