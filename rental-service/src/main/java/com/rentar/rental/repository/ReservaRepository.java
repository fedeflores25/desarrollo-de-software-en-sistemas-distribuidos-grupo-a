package com.rentar.rental.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rentar.rental.entity.Reserva;
import com.rentar.rental.entity.enums.EstadoReserva;

/** Acceso a datos de las reservas. */
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /**
     * Indica si el vehiculo tiene una reserva en el estado indicado (normalmente CONFIRMADA)
     * cuyo periodo se solape con [inicio, fin). Dos periodos se solapan si
     * fechaInicioExistente &lt; finNuevo y fechaFinExistente &gt; inicioNuevo.
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM Reserva r
            WHERE r.vehiculoId = :vehiculoId
              AND r.estado = :estado
              AND r.fechaInicio < :fin
              AND r.fechaFin > :inicio
            """)
    boolean existsSolapamiento(
            @Param("vehiculoId") Long vehiculoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estado") EstadoReserva estado);
}
