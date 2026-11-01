package com.rentar.dto;

import java.time.LocalDate;

public class ClienteResponse {

    private Long id;
    private String documento;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private LocalDate fechaNacimiento;
    private boolean activo;

    public ClienteResponse(
            Long id,
            String documento,
            String nombre,
            String apellido,
            String email,
            String telefono,
            LocalDate fechaNacimiento,
            boolean activo) {

        this.id = id;
        this.documento = documento;
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.telefono = telefono;
        this.fechaNacimiento = fechaNacimiento;
        this.activo = activo;
    }

    public Long getId() {
        return id;
    }

    public String getDocumento() {
        return documento;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public boolean isActivo() {
        return activo;
    }
}