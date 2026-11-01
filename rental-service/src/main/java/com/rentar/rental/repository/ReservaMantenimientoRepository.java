package com.rentar.rental.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rentar.rental.entity.ReservaMantenimiento;

/** Acceso a datos del mantenimiento asociado a cada reserva. */
public interface ReservaMantenimientoRepository extends JpaRepository<ReservaMantenimiento, Long> {
}
