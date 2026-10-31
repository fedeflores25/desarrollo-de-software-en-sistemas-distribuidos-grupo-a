package com.rentar.customer.service.implementation;

import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.rentar.customer.dto.UsuarioAutenticado;
import com.rentar.customer.entity.Usuario;
import com.rentar.customer.entity.enums.RolUsuario;
import com.rentar.customer.repository.UsuarioRepository;
import com.rentar.customer.service.IUsuarioService;

/** Verifica credenciales BCrypt y el estado del cliente asociado. */
@Service
public class UsuarioService implements IUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public Optional<UsuarioAutenticado> autenticar(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }

        return usuarioRepository.findByUsername(username.trim())
                .filter(usuario -> passwordEncoder.matches(password, usuario.getPasswordHash()))
                .filter(this::puedeIniciarSesion)
                .map(this::aUsuarioAutenticado);
    }

    private boolean puedeIniciarSesion(Usuario usuario) {
        return usuario.getRol() != RolUsuario.CLIENTE
                || (usuario.getCliente() != null && Boolean.TRUE.equals(usuario.getCliente().getActivo()));
    }

    private UsuarioAutenticado aUsuarioAutenticado(Usuario usuario) {
        Long clienteId = usuario.getCliente() == null ? null : usuario.getCliente().getId();
        return new UsuarioAutenticado(usuario.getUsername(), usuario.getRol(), clienteId);
    }
}
