package com.rentar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.rentar.dto.ClienteRequest;
import com.rentar.entity.Cliente;
import com.rentar.exception.ClienteDuplicadoException;
import com.rentar.repository.ClienteRepository;
import com.rentar.service.IClienteService;

/**
 * Pruebas de Gestion de Clientes (Hito 1).
 */
@SpringBootTest
@Transactional
class ClienteServiceTest {

    @Autowired
    private IClienteService clienteService;

    @Autowired
    private ClienteRepository clienteRepository;

    // ALTA: cliente creado correctamente

    @Test
    void crear_debeGuardarClienteActivo() {

        ClienteRequest request = new ClienteRequest();
        request.setDocumento("99999999");
        request.setNombre("Valentina");
        request.setApellido("Prueba");
        request.setEmail("valentina.prueba@test.com");
        request.setTelefono("1112345678");
        request.setFechaNacimiento(LocalDate.of(2000, 5, 10));

        Cliente cliente = clienteService.crear(request);

        assertNotNull(cliente.getId());
        assertEquals("99999999", cliente.getDocumento());
        assertEquals("Valentina", cliente.getNombre());
        assertEquals("Prueba", cliente.getApellido());
        assertEquals("valentina.prueba@test.com", cliente.getEmail());
        assertTrue(cliente.getActivo());
    }

    // ALTA: documento duplicado

    @Test
    void crear_debeFallarSiDocumentoYaExiste() {

        ClienteRequest primero = new ClienteRequest();
        primero.setDocumento("88888888");
        primero.setNombre("Cliente");
        primero.setApellido("Uno");
        primero.setEmail("cliente.uno@test.com");
        primero.setTelefono("1111111111");
        primero.setFechaNacimiento(LocalDate.of(1995, 1, 1));

        clienteService.crear(primero);

        ClienteRequest segundo = new ClienteRequest();
        segundo.setDocumento("88888888");
        segundo.setNombre("Cliente");
        segundo.setApellido("Dos");
        segundo.setEmail("cliente.dos@test.com");
        segundo.setTelefono("2222222222");
        segundo.setFechaNacimiento(LocalDate.of(1996, 2, 2));

        assertThrows(
                ClienteDuplicadoException.class,
                () -> clienteService.crear(segundo));
    }

    // ALTA: email duplicado

    @Test
    void crear_debeFallarSiEmailYaExiste() {

        ClienteRequest primero = new ClienteRequest();
        primero.setDocumento("77777777");
        primero.setNombre("Cliente");
        primero.setApellido("Uno");
        primero.setEmail("email.repetido@test.com");
        primero.setTelefono("1111111111");
        primero.setFechaNacimiento(LocalDate.of(1995, 1, 1));

        clienteService.crear(primero);

        ClienteRequest segundo = new ClienteRequest();
        segundo.setDocumento("66666666");
        segundo.setNombre("Cliente");
        segundo.setApellido("Dos");
        segundo.setEmail("email.repetido@test.com");
        segundo.setTelefono("2222222222");
        segundo.setFechaNacimiento(LocalDate.of(1996, 2, 2));

        assertThrows(
                ClienteDuplicadoException.class,
                () -> clienteService.crear(segundo));
    }

    // MODIFICACION: datos actualizados correctamente

    @Test
    void modificar_debeActualizarLosDatos() {

        ClienteRequest alta = new ClienteRequest();
        alta.setDocumento("55555555");
        alta.setNombre("Nombre");
        alta.setApellido("Original");
        alta.setEmail("original@test.com");
        alta.setTelefono("1111111111");
        alta.setFechaNacimiento(LocalDate.of(1990, 3, 15));

        Cliente creado = clienteService.crear(alta);

        ClienteRequest modificacion = new ClienteRequest();
        modificacion.setDocumento("55555555");
        modificacion.setNombre("Nombre Modificado");
        modificacion.setApellido("Apellido Modificado");
        modificacion.setEmail("modificado@test.com");
        modificacion.setTelefono("3333333333");
        modificacion.setFechaNacimiento(LocalDate.of(1990, 3, 15));

        Cliente modificado = clienteService.modificar(
                creado.getId(),
                modificacion);

        assertEquals("Nombre Modificado", modificado.getNombre());
        assertEquals("Apellido Modificado", modificado.getApellido());
        assertEquals("modificado@test.com", modificado.getEmail());
        assertEquals("3333333333", modificado.getTelefono());
    }

    // BAJA: baja logica del cliente

    @Test
    void darDeBaja_debeDejarClienteInactivo() {

        ClienteRequest request = new ClienteRequest();
        request.setDocumento("44444444");
        request.setNombre("Cliente");
        request.setApellido("Baja");
        request.setEmail("cliente.baja@test.com");
        request.setTelefono("1111111111");
        request.setFechaNacimiento(LocalDate.of(1985, 7, 20));

        Cliente creado = clienteService.crear(request);

        clienteService.darDeBaja(creado.getId());

        Cliente cliente = clienteRepository
                .findById(creado.getId())
                .orElseThrow();

        assertFalse(cliente.getActivo());
        assertTrue(clienteRepository.existsById(creado.getId()));
    }
}