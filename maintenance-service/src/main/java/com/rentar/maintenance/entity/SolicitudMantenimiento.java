package com.rentar.maintenance.entity;

import com.rentar.maintenance.entity.enums.EstadoMantenimiento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Solicitud generada de forma asincronica al confirmarse una reserva. */
@Entity
@Table(name = "solicitud_mantenimiento")
public class SolicitudMantenimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reserva_id", nullable = false, unique = true)
    private Long reservaId;

    @Column(name = "vehiculo_id", nullable = false)
    private Long vehiculoId;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoMantenimiento estado;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    protected SolicitudMantenimiento() {
    }

    public SolicitudMantenimiento(Long reservaId, Long vehiculoId, LocalDateTime fechaInicio,
                                  LocalDateTime fechaFin, String eventId, String correlationId) {
        LocalDateTime ahora = LocalDateTime.now();
        this.reservaId = reservaId;
        this.vehiculoId = vehiculoId;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = EstadoMantenimiento.PENDIENTE;
        this.eventId = eventId;
        this.correlationId = correlationId;
        this.creadoEn = ahora;
        this.actualizadoEn = ahora;
    }

    public Long getId() { return id; }
    public Long getReservaId() { return reservaId; }
    public Long getVehiculoId() { return vehiculoId; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public LocalDateTime getFechaFin() { return fechaFin; }
    public EstadoMantenimiento getEstado() { return estado; }
    public String getEventId() { return eventId; }
    public String getCorrelationId() { return correlationId; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }

    public void actualizarEstado(EstadoMantenimiento nuevoEstado) {
        this.estado = nuevoEstado;
        this.actualizadoEn = LocalDateTime.now();
    }
}
