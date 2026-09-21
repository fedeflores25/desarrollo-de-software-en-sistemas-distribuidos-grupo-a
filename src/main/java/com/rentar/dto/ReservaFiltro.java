package com.rentar.dto;

public class ReservaFiltro {
    private Long clienteId;
    private Long vehiculoId;
    private String tipoVehiculo;
    private String estado;
    private String fechaInicio;
    private String fechaFin;

    
    public ReservaFiltro() {
    }

    
    public ReservaFiltro(Long clienteId, Long vehiculoId, String tipoVehiculo, String estado, String fechaInicio,
            String fechaFin) {
        this.clienteId = clienteId;
        this.vehiculoId = vehiculoId;
        this.tipoVehiculo = tipoVehiculo;
        this.estado = estado;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }


    public Long getClienteId() {
        return clienteId;
    }
    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }
    public Long getVehiculoId() {
        return vehiculoId;
    }
    public void setVehiculoId(Long vehiculoId) {
        this.vehiculoId = vehiculoId;
    }
    public String getTipoVehiculo() {
        return tipoVehiculo;
    }
    public void setTipoVehiculo(String tipoVehiculo) {
        this.tipoVehiculo = tipoVehiculo;
    }
    public String getEstado() {
        return estado;
    }
    public void setEstado(String estado) {
        this.estado = estado;
    }
    public String getFechaInicio() {
        return fechaInicio;
    }
    public void setFechaInicio(String fechaInicio) {
        this.fechaInicio = fechaInicio;
    }
    public String getFechaFin() {
        return fechaFin;
    }
    public void setFechaFin(String fechaFin) {
        this.fechaFin = fechaFin;
    }

    
}
