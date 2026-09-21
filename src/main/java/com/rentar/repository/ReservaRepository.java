package com.rentar.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.rentar.entity.Reserva;
import com.rentar.entity.enums.EstadoReserva;


/** Repositorio de acceso a datos para la entidad Reserva. */
@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByClienteId(Long clienteId);

    /**
     * Indica si el vehículo tiene alguna reserva en el estado indicado (normalmente CONFIRMADA)
     * cuyo período se solape con [inicio, fin). Dos períodos se solapan si
     * fechaInicioExistente &lt; finNuevo Y fechaFinExistente &gt; inicioNuevo.
     */
    @Query("""
            SELECT COUNT(r) > 0 FROM Reserva r
            WHERE r.vehiculo.id = :vehiculoId
              AND r.estado = :estado
              AND r.fechaInicio < :fin
              AND r.fechaFin > :inicio
            """)
    boolean existsSolapamiento(
            @Param("vehiculoId") Long vehiculoId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estado") EstadoReserva estado);

        // Busca reservas de un cliente específico cuyos estados coincidan con los de la lista
    List<Reserva> findByClienteIdAndEstadoIn(Long clienteId, List<EstadoReserva> estados);  

    List<Reserva> findByEstadoIn(List<EstadoReserva> estados);

      
}



    
    
