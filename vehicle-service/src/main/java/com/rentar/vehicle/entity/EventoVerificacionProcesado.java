package com.rentar.vehicle.entity;

import com.rentar.vehicle.entity.enums.ResultadoVerificacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Evento policial ya aplicado, conservado para ignorar entregas duplicadas. */
@Entity
@Table(name = "evento_verificacion_procesado")
public class EventoVerificacionProcesado {

    @Id
    @Column(name = "event_id", length = 36)
    private String eventId;

    @Column(name = "vehiculo_id", nullable = false)
    private Long vehiculoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultadoVerificacion resultado;

    @Column(name = "procesado_en", nullable = false)
    private LocalDateTime procesadoEn;

    protected EventoVerificacionProcesado() {
    }

    public EventoVerificacionProcesado(String eventId, Long vehiculoId, ResultadoVerificacion resultado) {
        this.eventId = eventId;
        this.vehiculoId = vehiculoId;
        this.resultado = resultado;
        this.procesadoEn = LocalDateTime.now();
    }

    public String getEventId() { return eventId; }
    public Long getVehiculoId() { return vehiculoId; }
    public ResultadoVerificacion getResultado() { return resultado; }
    public LocalDateTime getProcesadoEn() { return procesadoEn; }
}
