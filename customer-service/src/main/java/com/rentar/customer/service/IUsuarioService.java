package com.rentar.customer.service;

import java.util.Optional;

import com.rentar.customer.dto.UsuarioAutenticado;

/** Define la autenticacion de usuarios administrada por Customer Service. */
public interface IUsuarioService {

    Optional<UsuarioAutenticado> autenticar(String username, String password);
}
