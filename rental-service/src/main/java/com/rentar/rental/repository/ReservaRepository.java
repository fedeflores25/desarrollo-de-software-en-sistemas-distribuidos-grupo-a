package com.rentar.rental.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /** Ids de los vehiculos con alguna reserva en el estado indicado que se solape con el periodo. */
    @Query("""
            SELECT DISTINCT r.vehiculoId FROM Reserva r
            WHERE r.estado = :estado
              AND r.fechaInicio < :fin
              AND r.fechaFin > :inicio
            """)
    List<Long> findVehiculoIdsOcupados(
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin,
            @Param("estado") EstadoReserva estado);

    /**
     * Pasa a nuevoEstado las reservas en estadoActual cuyo periodo ya termino.
     * Se usa para marcar como FINALIZADA las reservas CONFIRMADAS vencidas.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Reserva r SET r.estado = :nuevoEstado
            WHERE r.estado = :estadoActual AND r.fechaFin < :ahora
            """)
    int actualizarEstadoVencidas(
            @Param("estadoActual") EstadoReserva estadoActual,
            @Param("nuevoEstado") EstadoReserva nuevoEstado,
            @Param("ahora") LocalDateTime ahora);

    /** Reservas de un cliente en alguno de los estados indicados, las mas recientes primero. */
    @Query("""
            SELECT r FROM Reserva r
            WHERE r.clienteId = :clienteId AND r.estado IN :estados
            ORDER BY r.fechaInicio DESC, r.id DESC
            """)
    List<Reserva> findByClienteIdAndEstadoIn(
            @Param("clienteId") Long clienteId,
            @Param("estados") Collection<EstadoReserva> estados);

    /**
     * Busqueda con filtros opcionales: un parametro nulo no filtra. filtrarVehiculos indica
     * si vehiculoIds se aplica (una coleccion vacia no debe pasarse al IN).
     */
    @Query("""
            SELECT r FROM Reserva r
            WHERE (:clienteId IS NULL OR r.clienteId = :clienteId)
              AND (:filtrarVehiculos = false OR r.vehiculoId IN :vehiculoIds)
              AND (:estado IS NULL OR r.estado = :estado)
              AND (cast(:desde as java.time.LocalDateTime) IS NULL OR r.fechaInicio >= :desde)
              AND (cast(:hasta as java.time.LocalDateTime) IS NULL OR r.fechaInicio <= :hasta)
            ORDER BY r.fechaInicio DESC, r.id DESC
            """)
    List<Reserva> buscarConFiltros(
            @Param("clienteId") Long clienteId,
            @Param("filtrarVehiculos") boolean filtrarVehiculos,
            @Param("vehiculoIds") Collection<Long> vehiculoIds,
            @Param("estado") EstadoReserva estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
