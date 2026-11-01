package com.rentar.dto;

import java.math.BigDecimal;

import com.rentar.entity.enums.EstadoVehiculo;
import com.rentar.entity.enums.TipoVehiculo;

public class VehiculoResponse {

    private final Long id;
    private final String patente;
    private final String marca;
    private final String modelo;
    private final Integer anio;
    private final String color;
    private final TipoVehiculo tipo;
    private final BigDecimal precioDiario;
    private final EstadoVehiculo estado;
    private final boolean activo;

    public VehiculoResponse(
            Long id,
            String patente,
            String marca,
            String modelo,
            Integer anio,
            String color,
            TipoVehiculo tipo,
            BigDecimal precioDiario,
            EstadoVehiculo estado,
            boolean activo) {

        this.id = id;
        this.patente = patente;
        this.marca = marca;
        this.modelo = modelo;
        this.anio = anio;
        this.color = color;
        this.tipo = tipo;
        this.precioDiario = precioDiario;
        this.estado = estado;
        this.activo = activo;
    }

    public Long getId() {
        return id;
    }

    public String getPatente() {
        return patente;
    }

    public String getMarca() {
        return marca;
    }

    public String getModelo() {
        return modelo;
    }

    public Integer getAnio() {
        return anio;
    }

    public String getColor() {
        return color;
    }

    public TipoVehiculo getTipo() {
        return tipo;
    }

    public BigDecimal getPrecioDiario() {
        return precioDiario;
    }

    public EstadoVehiculo getEstado() {
        return estado;
    }

    public boolean isActivo() {
        return activo;
    }
}
