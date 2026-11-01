package com.rentar.dto;

import java.math.BigDecimal;

import com.rentar.entity.enums.TipoVehiculo;

public class VehiculoDisponibleResponse {

    private String patente;
    private String marca;
    private String modelo;
    private Integer anio;
    private String color;
    private TipoVehiculo tipo;
    private BigDecimal precioDiario;
	public String getPatente() {
		return patente;
	}
	public void setPatente(String patente) {
		this.patente = patente;
	}
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
}