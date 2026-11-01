package com.rentar.vehicle.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rentar.vehicle.entity.SolicitudVehiculoExterno;

import jakarta.persistence.LockModeType;

public interface SolicitudVehiculoExternoRepository
        extends JpaRepository<SolicitudVehiculoExterno, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SolicitudVehiculoExterno s WHERE s.id = :id")
    Optional<SolicitudVehiculoExterno> buscarParaActualizar(
            @Param("id") Long id);
}