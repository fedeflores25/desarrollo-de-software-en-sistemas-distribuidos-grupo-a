package com.rentar.dto;

import java.math.BigDecimal;

public class HistorialResponse {
    private String vehiculo; 
    private String patente;
    private String fechaInicio;
    private String fechaFinalizacion;
    private Integer cantidadDias;
    private BigDecimal importeTotal;
    private String estado;
    
    
    public HistorialResponse(String vehiculo, String patente, String fechaInicio, String fechaFinalizacion,
            Integer cantidadDias, BigDecimal importeTotal, String estado) {
        this.vehiculo = vehiculo;
        this.patente = patente;
        this.fechaInicio = fechaInicio;
        this.fechaFinalizacion = fechaFinalizacion;
        this.cantidadDias = cantidadDias;
        this.importeTotal = importeTotal;
        this.estado = estado;
    }
    public HistorialResponse() {
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
    public Integer getCantidadDias() {
        return cantidadDias;
    }
    public void setCantidadDias(Integer cantidadDias) {
        this.cantidadDias = cantidadDias;
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