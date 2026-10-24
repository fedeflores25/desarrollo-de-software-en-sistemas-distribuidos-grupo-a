package com.rentar.maintenance.repository;

import com.rentar.maintenance.entity.SolicitudMantenimiento;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitudMantenimientoRepository extends JpaRepository<SolicitudMantenimiento, Long> {

    boolean existsByEventId(String eventId);

    Optional<SolicitudMantenimiento> findByReservaId(Long reservaId);
}
