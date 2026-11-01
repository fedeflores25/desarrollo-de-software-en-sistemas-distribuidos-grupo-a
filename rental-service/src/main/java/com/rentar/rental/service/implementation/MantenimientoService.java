package com.rentar.rental.service.implementation;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rentar.rental.entity.ReservaMantenimiento;
import com.rentar.rental.entity.enums.EstadoMantenimiento;
import com.rentar.rental.repository.ReservaMantenimientoRepository;
import com.rentar.rental.service.IMantenimientoService;

@Service
public class MantenimientoService implements IMantenimientoService {

    private static final Logger log = LoggerFactory.getLogger(MantenimientoService.class);

    private final ReservaMantenimientoRepository repository;

    public MantenimientoService(ReservaMantenimientoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void registrarPendiente(Long reservaId) {

        ReservaMantenimiento mantenimiento = new ReservaMantenimiento();
        mantenimiento.setReservaId(reservaId);
        mantenimiento.setEstado(EstadoMantenimiento.PENDIENTE);
        mantenimiento.setFechaActualizacion(LocalDateTime.now());

        repository.save(mantenimiento);
    }

    @Override
    @Transactional
    public boolean registrarResultado(String eventId, Long reservaId, EstadoMantenimiento resultado) {

        if (resultado == null || resultado == EstadoMantenimiento.PENDIENTE) {
            log.warn("Resultado de mantenimiento invalido para la reserva {}: {}", reservaId, resultado);
            return false;
        }

        Optional<ReservaMantenimiento> encontrado = repository.findById(reservaId);

        if (encontrado.isEmpty()) {
            log.warn("Llego un resultado de mantenimiento para la reserva {}, que no tiene registro", reservaId);
            return false;
        }

        ReservaMantenimiento mantenimiento = encontrado.get();

        if (mantenimiento.getEstado() != EstadoMantenimiento.PENDIENTE) {
            log.info("Resultado repetido para la reserva {} (evento {}): se ignora", reservaId, eventId);
            return false;
        }

        mantenimiento.setEstado(resultado);
        mantenimiento.setFechaActualizacion(LocalDateTime.now());
        mantenimiento.setUltimoEventId(eventId);

        repository.save(mantenimiento);

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, EstadoMantenimiento> estados(Collection<Long> reservaIds) {

        Map<Long, EstadoMantenimiento> estados = new HashMap<>();

        if (reservaIds == null || reservaIds.isEmpty()) {
            return estados;
        }

        for (ReservaMantenimiento m : repository.findAllById(reservaIds)) {
            estados.put(m.getReservaId(), m.getEstado());
        }

        return estados;
    }
}
