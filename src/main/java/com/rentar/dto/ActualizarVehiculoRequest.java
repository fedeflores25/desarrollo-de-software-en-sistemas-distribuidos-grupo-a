package com.rentar.dto;

import java.math.BigDecimal;

import com.rentar.entity.enums.EstadoVehiculo;
import com.rentar.entity.enums.TipoVehiculo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ActualizarVehiculoRequest {

    @NotBlank(message = "La marca es obligatoria")
    private String marca;

    @NotBlank(message = "El modelo es obligatorio")
    private String modelo;

    @NotNull(message = "El anio es obligatorio")
    @Min(value = 1900, message = "El anio debe ser mayor o igual a 1900")
    private Integer anio;

    private String color;

    @NotNull(message = "El tipo de vehiculo es obligatorio")
    private TipoVehiculo tipo;

    @NotNull(message = "El precio diario es obligatorio")
    @Positive(message = "El precio diario debe ser mayor a cero")
    private BigDecimal precioDiario;

    @NotNull(message = "El estado es obligatorio")
    private EstadoVehiculo estado;

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public TipoVehiculo getTipo() {
        return tipo;
    }

    public void setTipo(TipoVehiculo tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getPrecioDiario() {
        return precioDiario;
    }

    public void setPrecioDiario(BigDecimal precioDiario) {
        this.precioDiario = precioDiario;
    }

    public EstadoVehiculo getEstado() {
        return estado;
    }

    public void setEstado(EstadoVehiculo estado) {
        this.estado = estado;
    }
}
