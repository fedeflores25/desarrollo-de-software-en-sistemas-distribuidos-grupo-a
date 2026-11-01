package com.rentar.service.implementation;

import org.springframework.stereotype.Service;

import com.rentar.repository.UsuarioRepository;
import com.rentar.service.IUsuarioService;

/**Implementa las operaciones relacionadas con los usuarios del sistema.
 Utiliza UsuarioRepository para acceder a los datos de los usuarios. */
@Service
public class UsuarioService implements IUsuarioService {

    private final UsuarioRepository usuarioRepository;

    /**Recibe el repositorio que utilizará el servicio para acceder a los usuarios. */
    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }
}
