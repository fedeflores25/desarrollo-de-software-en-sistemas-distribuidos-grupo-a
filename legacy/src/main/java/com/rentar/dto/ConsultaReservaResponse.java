package com.rentar.dto;

import java.math.BigDecimal;

public class ConsultaReservaResponse {
    
    private String cliente;
    private String vehiculo;
    private String patente;
    private String fechaInicio;
    private String fechaFinalizacion;
    private BigDecimal precioDiario;
    private BigDecimal importeTotal;
    private String estado;

    
    public ConsultaReservaResponse(String cliente, String vehiculo, String patente, String fechaInicio,
            String fechaFinalizacion, BigDecimal precioDiario, BigDecimal importeTotal, String estado) {
        this.cliente = cliente;
        this.vehiculo = vehiculo;
        this.patente = patente;
        this.fechaInicio = fechaInicio;
        this.fechaFinalizacion = fechaFinalizacion;
        this.precioDiario = precioDiario;
        this.importeTotal = importeTotal;
        this.estado = estado;
    }

    public ConsultaReservaResponse() {
    }
    public String getCliente() {
        return cliente;
    }
    public void setCliente(String cliente) {
        this.cliente = cliente;
    }
    public String getVehiculo() {
        return vehiculo;
    }
    public void setVehiculo(String vehiculo) {
        this.vehiculo = vehiculo;
    }
    public String getPatente() {
        return patente;
    }
    public void setPatente(String patente) {
        this.patente = patente;
    }
    public String getFechaInicio() {
        return fechaInicio;
    }
    public void setFechaInicio(String fechaInicio) {
        this.fechaInicio = fechaInicio;
    }
    public String getFechaFinalizacion() {
        return fechaFinalizacion;
    }
    public void setFechaFinalizacion(String fechaFinalizacion) {
        this.fechaFinalizacion = fechaFinalizacion;
    }
    public BigDecimal getPrecioDiario() {
        return precioDiario;
    }
    public void setPrecioDiario(BigDecimal precioDiario) {
        this.precioDiario = precioDiario;
    }
    public BigDecimal getImporteTotal() {
        return importeTotal;
    }
    public void setImporteTotal(BigDecimal importeTotal) {
        this.importeTotal = importeTotal;
    }
    public String getEstado() {
        return estado;
    }
    public void setEstado(String estado) {
        this.estado = estado;
    }

    
}
