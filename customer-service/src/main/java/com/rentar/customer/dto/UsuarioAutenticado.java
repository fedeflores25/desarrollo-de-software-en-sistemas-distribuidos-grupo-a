package com.rentar.customer.dto;

import com.rentar.customer.entity.enums.RolUsuario;

/** Datos seguros que Customer Service devuelve tras una autenticacion valida. */
public record UsuarioAutenticado(
        String username,
        RolUsuario rol,
        Long clienteId) {
}
