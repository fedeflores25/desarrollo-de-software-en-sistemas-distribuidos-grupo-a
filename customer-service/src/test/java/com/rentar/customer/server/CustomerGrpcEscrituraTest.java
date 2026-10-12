package com.rentar.customer.server;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Collections;

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

import com.rentar.customer.grpc.Customer;
import com.rentar.customer.grpc.CreateCustomerRequest;
import com.rentar.customer.grpc.UpdateCustomerRequest;
import com.rentar.customer.grpc.DeactivateCustomerRequest;

import com.rentar.customer.service.IClienteService;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import jakarta.validation.Validator;

@ExtendWith(MockitoExtension.class)
class CustomerGrpcEscrituraTest {

    @Mock
    private IClienteService clienteService;

    @Mock
    private Validator validator;

    @InjectMocks
    private CustomerGrpcService grpcService;

    // ==========================================
    // TEST 9 - CREAR CLIENTE CORRECTAMENTE
    // ==========================================

    @Test
    void crearClienteCorrectamente() {

        CreateCustomerRequest request = solicitudValida();

        Cliente clienteCreado = crearClienteSimulado();

        when(validator.validate(any(ClienteRequest.class)))
                .thenReturn(Collections.emptySet());

        when(clienteService.crear(any(ClienteRequest.class)))
                .thenReturn(clienteCreado);

        StreamObserver<Customer> observer = crearObserver();

        grpcService.createCustomer(request, observer);

        ArgumentCaptor<Customer> captor =
                ArgumentCaptor.forClass(Customer.class);

        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();
        verify(observer, never()).onError(any());

        Customer respuesta = captor.getValue();

        assertEquals(3L, respuesta.getId());
        assertEquals("90000003", respuesta.getDocumento());
        assertEquals("Camila", respuesta.getNombre());
        assertTrue(respuesta.getActivo());

        ArgumentCaptor<ClienteRequest> dtoCaptor =
                ArgumentCaptor.forClass(ClienteRequest.class);

        verify(clienteService).crear(dtoCaptor.capture());

        assertEquals(
                LocalDate.of(2000, 3, 20),
                dtoCaptor.getValue().getFechaNacimiento());
    }

    // ==========================================
    // TEST 10 - DOCUMENTO DUPLICADO
    // ==========================================

    @Test
    void crearClienteConDocumentoDuplicado() {

        CreateCustomerRequest request = solicitudValida();

        when(validator.validate(any(ClienteRequest.class)))
                .thenReturn(Collections.emptySet());

        when(clienteService.crear(any(ClienteRequest.class)))
                .thenThrow(new ClienteDuplicadoException(
                        "Ya existe un cliente con ese documento"));

        StreamObserver<Customer> observer = crearObserver();

        grpcService.createCustomer(request, observer);

        ArgumentCaptor<Throwable> captor =
                ArgumentCaptor.forClass(Throwable.class);

        verify(observer).onError(captor.capture());

        assertEquals(
                Status.Code.ALREADY_EXISTS,
                Status.fromThrowable(
                        captor.getValue()).getCode());

        verify(observer, never()).onCompleted();
    }

    // ==========================================
    // TEST 11 - FECHA INVALIDA
    // ==========================================

    @Test
    void crearClienteConFechaInvalida() {

        CreateCustomerRequest request =
                CreateCustomerRequest.newBuilder()
                        .setDocumento("90000004")
                        .setNombre("Daniel")
                        .setApellido("Prueba")
                        .setEmail("daniel.prueba@example.com")
                        .setFechaNacimiento("fecha-invalida")
                        .build();

        StreamObserver<Customer> observer = crearObserver();

        grpcService.createCustomer(request, observer);

        ArgumentCaptor<Throwable> captor =
                ArgumentCaptor.forClass(Throwable.class);

        verify(observer).onError(captor.capture());

        assertEquals(
                Status.Code.INVALID_ARGUMENT,
                Status.fromThrowable(
                        captor.getValue()).getCode());

        verify(clienteService, never())
                .crear(any(ClienteRequest.class));
    }

    // ==========================================
    // TEST 12 - MODIFICAR CLIENTE CORRECTAMENTE
    // ==========================================

    @Test
    void modificarClienteCorrectamente() {

        UpdateCustomerRequest request =
                solicitudActualizacionValida();

        Cliente clienteActualizado = crearClienteSimulado();

        clienteActualizado.setEmail(
                "camila.actualizada@example.com");

        clienteActualizado.setTelefono("1199998888");

        when(validator.validate(any(ClienteRequest.class)))
                .thenReturn(Collections.emptySet());

        when(clienteService.modificar(
                eq(3L), any(ClienteRequest.class)))
                .thenReturn(clienteActualizado);

        StreamObserver<Customer> observer = crearObserver();

        grpcService.updateCustomer(request, observer);

        ArgumentCaptor<Customer> captor =
                ArgumentCaptor.forClass(Customer.class);

        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();
        verify(observer, never()).onError(any());

        Customer respuesta = captor.getValue();

        assertEquals(3L, respuesta.getId());
        assertEquals("Camila", respuesta.getNombre());

        assertEquals(
                "camila.actualizada@example.com",
                respuesta.getEmail());

        assertEquals("1199998888", respuesta.getTelefono());
        assertTrue(respuesta.getActivo());

        // Verificar los datos enviados a la capa de negocio
        ArgumentCaptor<ClienteRequest> dtoCaptor =
                ArgumentCaptor.forClass(ClienteRequest.class);

        verify(clienteService).modificar(
                eq(3L), dtoCaptor.capture());

        assertEquals(
                "camila.actualizada@example.com",
                dtoCaptor.getValue().getEmail());
    }

    // ==========================================
    // TEST 13 - MODIFICAR CON DATOS DUPLICADOS
    // ==========================================

    @Test
    void modificarClienteConEmailDuplicado() {

        UpdateCustomerRequest request =
                solicitudActualizacionValida();

        when(validator.validate(any(ClienteRequest.class)))
                .thenReturn(Collections.emptySet());

        when(clienteService.modificar(
                eq(3L), any(ClienteRequest.class)))
                .thenThrow(new ClienteDuplicadoException(
                        "Ya existe un cliente con ese email"));

        StreamObserver<Customer> observer = crearObserver();

        grpcService.updateCustomer(request, observer);

        ArgumentCaptor<Throwable> captor =
                ArgumentCaptor.forClass(Throwable.class);

        verify(observer).onError(captor.capture());

        assertEquals(
                Status.Code.ALREADY_EXISTS,
                Status.fromThrowable(
                        captor.getValue()).getCode());

        verify(observer, never()).onCompleted();
    }

    // ==========================================
    // TEST 14 - MODIFICAR CLIENTE INEXISTENTE
    // ==========================================

    @Test
    void modificarClienteInexistente() {

        UpdateCustomerRequest request =
                UpdateCustomerRequest.newBuilder()
                        .setId(999L)
                        .setDocumento("90000003")
                        .setNombre("Camila")
                        .setApellido("Prueba")
                        .setEmail("camila.actualizada@example.com")
                        .setTelefono("1199998888")
                        .setFechaNacimiento("2000-03-20")
                        .build();

        when(validator.validate(any(ClienteRequest.class)))
                .thenReturn(Collections.emptySet());

        when(clienteService.modificar(
                eq(999L), any(ClienteRequest.class)))
                .thenThrow(new ClienteNoEncontradoException(
                        "Cliente no encontrado"));

        StreamObserver<Customer> observer = crearObserver();

        grpcService.updateCustomer(request, observer);

        ArgumentCaptor<Throwable> captor =
                ArgumentCaptor.forClass(Throwable.class);

        verify(observer).onError(captor.capture());

        assertEquals(
                Status.Code.NOT_FOUND,
                Status.fromThrowable(
                        captor.getValue()).getCode());

        verify(observer, never()).onCompleted();
    }

    // ==========================================
    // TEST 15 - DESACTIVAR CLIENTE CORRECTAMENTE
    // ==========================================

    @Test
    void desactivarClienteCorrectamente() {

        Cliente cliente = crearClienteSimulado();

        // Simular el estado despues de la baja logica
        cliente.setActivo(false);

        when(clienteService.buscarPorId(3L))
                .thenReturn(cliente);

        DeactivateCustomerRequest request =
                DeactivateCustomerRequest.newBuilder()
                        .setId(3L)
                        .build();

        StreamObserver<Customer> observer = crearObserver();

        grpcService.deactivateCustomer(request, observer);

        // Verificar que se solicito la baja logica
        verify(clienteService).darDeBaja(3L);

        ArgumentCaptor<Customer> captor =
                ArgumentCaptor.forClass(Customer.class);

        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();
        verify(observer, never()).onError(any());

        Customer respuesta = captor.getValue();

        assertEquals(3L, respuesta.getId());
        assertEquals("Camila", respuesta.getNombre());
        assertFalse(respuesta.getActivo());
    }

    // ==========================================
    // TEST 16 - DESACTIVAR CLIENTE INEXISTENTE
    // ==========================================

    @Test
    void desactivarClienteInexistente() {

        doThrow(new ClienteNoEncontradoException(
                "Cliente no encontrado"))
                .when(clienteService)
                .darDeBaja(999L);

        DeactivateCustomerRequest request =
                DeactivateCustomerRequest.newBuilder()
                        .setId(999L)
                        .build();

        StreamObserver<Customer> observer = crearObserver();

        grpcService.deactivateCustomer(request, observer);

        ArgumentCaptor<Throwable> captor =
                ArgumentCaptor.forClass(Throwable.class);

        verify(observer).onError(captor.capture());

        assertEquals(
                Status.Code.NOT_FOUND,
                Status.fromThrowable(
                        captor.getValue()).getCode());

        verify(clienteService, never())
                .buscarPorId(999L);

        verify(observer, never()).onCompleted();
    }

    // ==========================================
    // SOLICITUD VALIDA PARA CREAR CLIENTES
    // ==========================================

    private CreateCustomerRequest solicitudValida() {

        return CreateCustomerRequest.newBuilder()
                .setDocumento("90000003")
                .setNombre("Camila")
                .setApellido("Prueba")
                .setEmail("camila.prueba@example.com")
                .setTelefono("1134567890")
                .setFechaNacimiento("2000-03-20")
                .build();
    }

    // ==========================================
    // SOLICITUD VALIDA PARA MODIFICAR CLIENTES
    // ==========================================

    private UpdateCustomerRequest solicitudActualizacionValida() {

        return UpdateCustomerRequest.newBuilder()
                .setId(3L)
                .setDocumento("90000003")
                .setNombre("Camila")
                .setApellido("Prueba")
                .setEmail("camila.actualizada@example.com")
                .setTelefono("1199998888")
                .setFechaNacimiento("2000-03-20")
                .build();
    }

    // ==========================================
    // CREAR CLIENTE SIMULADO
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

    // ==========================================
    // OBSERVADOR MOCK
    // ==========================================

    @SuppressWarnings("unchecked")
    private <T> StreamObserver<T> crearObserver() {

        return mock(StreamObserver.class);
    }
}