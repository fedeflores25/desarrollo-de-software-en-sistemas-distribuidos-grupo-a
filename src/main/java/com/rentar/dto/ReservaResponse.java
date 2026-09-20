package com.rentar.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReservaResponse {

    private Long id;
    private Long clienteId;
    private Long vehiculoId;
    private String patente;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private BigDecimal precioDiario;
    private Integer cantidadDias;
    private BigDecimal importeTotal;
    private String estado;
    private LocalDateTime fechaAlta;
    private LocalDateTime fechaCancelacion;

    public ReservaResponse(
            Long id,
            Long clienteId,
            Long vehiculoId,
            String patente,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            BigDecimal precioDiario,
            Integer cantidadDias,
            BigDecimal importeTotal,
            String estado,
            LocalDateTime fechaAlta,
            LocalDateTime fechaCancelacion) {

        this.id = id;
        this.clienteId = clienteId;
        this.vehiculoId = vehiculoId;
        this.patente = patente;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.precioDiario = precioDiario;
        this.cantidadDias = cantidadDias;
        this.importeTotal = importeTotal;
        this.estado = estado;
        this.fechaAlta = fechaAlta;
        this.fechaCancelacion = fechaCancelacion;
    }

    public Long getId() {
        return id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public Long getVehiculoId() {
        return vehiculoId;
    }

    public String getPatente() {
        return patente;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public BigDecimal getPrecioDiario() {
        return precioDiario;
    }

    public Integer getCantidadDias() {
        return cantidadDias;
    }

    public BigDecimal getImporteTotal() {
        return importeTotal;
    }

    public String getEstado() {
        return estado;
    }

    public LocalDateTime getFechaAlta() {
        return fechaAlta;
    }

    public LocalDateTime getFechaCancelacion() {
        return fechaCancelacion;
    }
}
