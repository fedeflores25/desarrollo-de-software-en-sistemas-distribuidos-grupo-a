package com.rentar.customer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.rentar.customer.entity.Cliente;
import com.rentar.customer.entity.Usuario;
import com.rentar.customer.entity.enums.RolUsuario;
import com.rentar.customer.repository.UsuarioRepository;
import com.rentar.customer.service.implementation.UsuarioService;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final String HASH_CLIENTE =
            "$2b$10$Bty.JRWdP14LKi2v4vlAXOhFYhaOju4/gWoJOUxmYKYgFNJ05dAHq";

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void autenticaClienteActivoConContrasenaCorrecta() {
        Usuario usuario = usuarioCliente(1L, true);
        when(usuarioRepository.findByUsername("cliente"))
                .thenReturn(Optional.of(usuario));

        var resultado = usuarioService.autenticar("cliente", "C1234!");

        assertTrue(resultado.isPresent());
        assertEquals(RolUsuario.CLIENTE, resultado.get().rol());
        assertEquals(1L, resultado.get().clienteId());
    }

    @Test
    void rechazaContrasenaIncorrecta() {
        when(usuarioRepository.findByUsername("cliente"))
                .thenReturn(Optional.of(usuarioCliente(1L, true)));

        assertFalse(usuarioService.autenticar("cliente", "incorrecta").isPresent());
    }

    @Test
    void rechazaClienteInactivo() {
        when(usuarioRepository.findByUsername("cliente"))
                .thenReturn(Optional.of(usuarioCliente(1L, false)));

        assertFalse(usuarioService.autenticar("cliente", "C1234!").isPresent());
    }

    @Test
    void reconoceAdministradorSinClienteAsociado() {
        Usuario admin = new Usuario();
        admin.setUsername("admin");
        admin.setPasswordHash(new BCryptPasswordEncoder().encode("A1234!"));
        admin.setRol(RolUsuario.ADMIN);
        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        var resultado = usuarioService.autenticar("admin", "A1234!");

        assertTrue(resultado.isPresent());
        assertEquals(RolUsuario.ADMIN, resultado.get().rol());
        assertEquals(null, resultado.get().clienteId());
    }

    private Usuario usuarioCliente(Long clienteId, boolean activo) {
        Cliente cliente = new Cliente();
        cliente.setId(clienteId);
        cliente.setActivo(activo);

        Usuario usuario = new Usuario();
        usuario.setUsername("cliente");
        usuario.setPasswordHash(HASH_CLIENTE);
        usuario.setRol(RolUsuario.CLIENTE);
        usuario.setCliente(cliente);
        return usuario;
    }
}
