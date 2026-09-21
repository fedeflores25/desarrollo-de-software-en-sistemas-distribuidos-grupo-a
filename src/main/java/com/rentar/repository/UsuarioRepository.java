package com.rentar.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rentar.entity.Usuario;

/**Repositorio de acceso a datos para la entidad Usuario. */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**Busca un usuario a partir de su nombre de usuario.
     Se utilizará más adelante para resolver el login simple. */
    Optional<Usuario> findByUsername(String username);
}