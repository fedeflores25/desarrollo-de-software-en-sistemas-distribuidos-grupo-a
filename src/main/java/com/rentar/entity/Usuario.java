package com.rentar.entity;

import com.rentar.entity.enums.RolUsuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**Representa un usuario que puede acceder al sistema Rentar.
 Puede corresponder a un administrador o estar asociado a un cliente. */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**Nombre utilizado por el usuario para identificarse en el sistema. */
    @Column(nullable = false, unique = true)
    private String username;

    /**Contraseña del usuario almacenada de forma segura mediante hash. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**Define si el usuario tiene rol de administrador o cliente. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolUsuario rol;

    /**Relaciona un usuario CLIENTE con su registro correspondiente de Cliente.
     Para los usuarios ADMIN esta relación puede quedar vacía. */
    @OneToOne
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    /**Constructor vacío requerido por JPA. */
    public Usuario() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public RolUsuario getRol() {
        return rol;
    }

    public void setRol(RolUsuario rol) {
        this.rol = rol;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }
}