package com.rentar.rental.entity;

import java.time.LocalDateTime;

import com.rentar.rental.entity.enums.EstadoMantenimiento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Resultado del mantenimiento preventivo de una reserva. Se crea en PENDIENTE junto con la
 * reserva y pasa a REALIZADO o NO_REALIZADO cuando llega el evento mantenimiento.resultado.
 */
@Entity
@Table(name = "reserva_mantenimiento")
public class ReservaMantenimiento {

    @Id
    @Column(name = "reserva_id")
    private Long reservaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoMantenimiento estado;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    // Id del ultimo evento procesado, para trazabilidad.
    @Column(name = "ultimo_event_id", length = 36)
    private String ultimoEventId;

    // ==========================================
    // GETTERS Y SETTERS
    // ==========================================

    public Long getReservaId() {
        return reservaId;
    }

    public void setReservaId(Long reservaId) {
        this.reservaId = reservaId;
    }

    public EstadoMantenimiento getEstado() {
        return estado;
    }

    public void setEstado(EstadoMantenimiento estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public String getUltimoEventId() {
        return ultimoEventId;
    }

    public void setUltimoEventId(String ultimoEventId) {
        this.ultimoEventId = ultimoEventId;
    }
}
