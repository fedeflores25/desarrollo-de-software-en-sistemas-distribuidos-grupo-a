package com.rentar.customer.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rentar.customer.dto.ClienteRequest;
import com.rentar.customer.entity.Cliente;
import com.rentar.customer.exception.ClienteDuplicadoException;
import com.rentar.customer.exception.ClienteNoEncontradoException;
import com.rentar.customer.repository.ClienteRepository;
import com.rentar.customer.service.implementation.ClienteService;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    // ==========================================
    // TEST 1 - CREAR CLIENTE CORRECTAMENTE
    // ==========================================

    @Test
    void crearClienteCorrectamente() {

        ClienteRequest request = solicitudValida();

        when(clienteRepository.save(any(Cliente.class)))
                .thenAnswer(invocacion ->
                        invocacion.getArgument(0));

        Cliente resultado = clienteService.crear(request);

        assertEquals("90000003", resultado.getDocumento());
        assertEquals("Camila", resultado.getNombre());
        assertEquals("Prueba", resultado.getApellido());
        assertEquals("camila.prueba@example.com",
                resultado.getEmail());
        assertTrue(resultado.getActivo());

        ArgumentCaptor<Cliente> captor =
                ArgumentCaptor.forClass(Cliente.class);

        verify(clienteRepository).save(captor.capture());

        assertEquals(
                LocalDate.of(2000, 3, 20),
                captor.getValue().getFechaNacimiento());
    }

    // ==========================================
    // TEST 2 - DOCUMENTO DUPLICADO AL CREAR
    // ==========================================

    @Test
    void crearClienteConDocumentoDuplicado() {

        ClienteRequest request = solicitudValida();

        when(clienteRepository.existsByDocumento("90000003"))
                .thenReturn(true);

        assertThrows(
                ClienteDuplicadoException.class,
                () -> clienteService.crear(request));

        verify(clienteRepository, never())
                .save(any(Cliente.class));
    }

    // ==========================================
    // TEST 3 - EMAIL DUPLICADO AL CREAR
    // ==========================================

    @Test
    void crearClienteConEmailDuplicado() {

        ClienteRequest request = solicitudValida();

        when(clienteRepository.existsByEmail(
                "camila.prueba@example.com"))
                .thenReturn(true);

        assertThrows(
                ClienteDuplicadoException.class,
                () -> clienteService.crear(request));

        verify(clienteRepository, never())
                .save(any(Cliente.class));
    }

    // ==========================================
    // TEST 4 - BUSCAR CLIENTE INEXISTENTE
    // ==========================================

    @Test
    void buscarClienteInexistente() {

        when(clienteRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ClienteNoEncontradoException.class,
                () -> clienteService.buscarPorId(999L));
    }

    // ==========================================
    // TEST 5 - MODIFICAR CLIENTE CORRECTAMENTE
    // ==========================================

    @Test
    void modificarClienteCorrectamente() {

        Cliente cliente = crearClienteSimulado();
        ClienteRequest request = solicitudValida();

        request.setNombre("Camila Actualizada");
        request.setEmail("camila.actualizada@example.com");

        when(clienteRepository.findById(3L))
                .thenReturn(Optional.of(cliente));

        when(clienteRepository.save(any(Cliente.class)))
                .thenAnswer(invocacion ->
                        invocacion.getArgument(0));

        Cliente resultado =
                clienteService.modificar(3L, request);

        assertEquals(3L, resultado.getId());
        assertEquals("Camila Actualizada",
                resultado.getNombre());
        assertEquals("camila.actualizada@example.com",
                resultado.getEmail());
        assertTrue(resultado.getActivo());

        verify(clienteRepository).save(cliente);
    }

    // ==========================================
    // TEST 6 - DOCUMENTO DUPLICADO AL MODIFICAR
    // ==========================================

    @Test
    void modificarClienteConDocumentoDuplicado() {

        Cliente cliente = crearClienteSimulado();
        ClienteRequest request = solicitudValida();

        request.setDocumento("90000001");

        when(clienteRepository.findById(3L))
                .thenReturn(Optional.of(cliente));

        when(clienteRepository.existsByDocumento("90000001"))
                .thenReturn(true);

        assertThrows(
                ClienteDuplicadoException.class,
                () -> clienteService.modificar(3L, request));

        verify(clienteRepository, never())
                .save(any(Cliente.class));
    }

    // ==========================================
    // TEST 7 - EMAIL DUPLICADO AL MODIFICAR
    // ==========================================

    @Test
    void modificarClienteConEmailDuplicado() {

        Cliente cliente = crearClienteSimulado();
        ClienteRequest request = solicitudValida();

        request.setEmail("ana.prueba@example.com");

        when(clienteRepository.findById(3L))
                .thenReturn(Optional.of(cliente));

        when(clienteRepository.existsByEmail(
                "ana.prueba@example.com"))
                .thenReturn(true);

        assertThrows(
                ClienteDuplicadoException.class,
                () -> clienteService.modificar(3L, request));

        verify(clienteRepository, never())
                .save(any(Cliente.class));
    }

    // ==========================================
    // TEST 8 - DESACTIVAR CLIENTE
    // ==========================================

    @Test
    void desactivarClienteCorrectamente() {

        Cliente cliente = crearClienteSimulado();

        when(clienteRepository.findById(3L))
                .thenReturn(Optional.of(cliente));

        clienteService.darDeBaja(3L);

        assertFalse(cliente.getActivo());

        verify(clienteRepository).save(cliente);
    }

    // ==========================================
    // TEST 9 - LISTAR CLIENTES
    // ==========================================

    @Test
    void listarClientesCorrectamente() {

        Cliente cliente = crearClienteSimulado();

        when(clienteRepository.findAll())
                .thenReturn(List.of(cliente));

        List<Cliente> resultado = clienteService.listar();

        assertEquals(1, resultado.size());
        assertEquals("Camila", resultado.get(0).getNombre());

        verify(clienteRepository).findAll();
    }

    // ==========================================
    // SOLICITUD VALIDA DE CLIENTE
    // ==========================================

    private ClienteRequest solicitudValida() {

        ClienteRequest request = new ClienteRequest();

        request.setDocumento("90000003");
        request.setNombre("Camila");
        request.setApellido("Prueba");
        request.setEmail("camila.prueba@example.com");
        request.setTelefono("1134567890");
        request.setFechaNacimiento(
                LocalDate.of(2000, 3, 20));

        return request;
    }

    // ==========================================
    // CLIENTE SIMULADO
    // ==========================================

    private Cliente crearClienteSimulado() {

        Cliente cliente = new Cliente();

        cliente.setId(3L);
        cliente.setDocumento("90000003");
        cliente.setNombre("Camila");
        cliente.setApellido("Prueba");
        cliente.setEmail("camila.prueba@example.com");
        cliente.setTelefono("1134567890");
        cliente.setFechaNacimiento(
                LocalDate.of(2000, 3, 20));
        cliente.setActivo(true);

        return cliente;
    }
}