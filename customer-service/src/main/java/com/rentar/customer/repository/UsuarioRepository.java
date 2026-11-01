package com.rentar.customer.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rentar.customer.entity.Usuario;

/** Acceso a las credenciales de usuarios de Rentar. */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);
}
