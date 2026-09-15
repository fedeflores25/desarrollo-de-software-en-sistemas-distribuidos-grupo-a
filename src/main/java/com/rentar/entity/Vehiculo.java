package com.rentar.entity;

import java.math.BigDecimal;

import com.rentar.entity.enums.EstadoVehiculo;
import com.rentar.entity.enums.TipoVehiculo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


/**Representa un vehículo perteneciente a la flota de Rentar. Se encuentra asociado a la tabla vehiculo de la base de datos.  */
@Entity
@Table(name = "vehiculo")
public class Vehiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /** Patente única del vehículo. Una vez registrado el vehículo, no debe modificarse. */
    @Column(nullable = false, unique = true, length = 20)
    private String patente;

    @Column(nullable = false, length = 50)
    private String marca;

    @Column(nullable = false, length = 50)
    private String modelo;

    @Column(nullable = false)
    private Integer anio;

    @Column(length = 30)
    private String color;


     /**Tipo del vehículo según los valores definidos en TipoVehiculo. Se persiste en la base de datos como texto. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoVehiculo tipo;

    @Column(name = "precio_diario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioDiario;


    /** Estado actual del vehículo.Se persiste en la base de datos como texto. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoVehiculo estado;


    /** Indica si el vehículo se encuentra habilitado para operar. 
        La baja del vehículo es lógica, por lo que el registro no se elimina. */
    @Column(nullable = false)
    private boolean activo;


    /** Constructor vacío requerido por JPA. */
    public Vehiculo() {
    }

    //Getters y setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public EstadoVehiculo getEstado() {
        return estado;
    }

    public void setEstado(EstadoVehiculo estado) {
        this.estado = estado;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
