package com.rentar.customer.server;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rentar.customer.entity.Cliente;
import com.rentar.customer.exception.ClienteNoEncontradoException;

import com.rentar.customer.grpc.Customer;
import com.rentar.customer.grpc.GetCustomerRequest;
import com.rentar.customer.grpc.ListCustomersRequest;
import com.rentar.customer.grpc.ListCustomersResponse;
import com.rentar.customer.grpc.VerifyCustomerRequest;
import com.rentar.customer.grpc.VerifyCustomerResponse;

import com.rentar.customer.service.IClienteService;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import jakarta.validation.Validator;

@ExtendWith(MockitoExtension.class)
class CustomerGrpcServiceTest {

    @Mock
    private IClienteService clienteService;

    @Mock
    private Validator validator;

    @InjectMocks
    private CustomerGrpcService grpcService;

    // ==========================================
    // TEST 1 - VERIFICAR CLIENTE ACTIVO
    // ==========================================

    @Test
    void verificarClienteActivo() {

        Cliente ana = crearCliente(
                1L, "90000001", "Ana", true);

        when(clienteService.buscarPorId(1L))
                .thenReturn(ana);

        StreamObserver<VerifyCustomerResponse> observer =
                crearObserver();

        VerifyCustomerRequest request =
                VerifyCustomerRequest.newBuilder()
                        .setId(1L)
                        .build();

        grpcService.verifyCustomer(request, observer);

        ArgumentCaptor<VerifyCustomerResponse> captor =
                ArgumentCaptor.forClass(
                        VerifyCustomerResponse.class);

        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();

        assertTrue(captor.getValue().getExiste());
        assertTrue(captor.getValue().getActivo());
    }

    // ==========================================
    // TEST 2 - VERIFICAR CLIENTE INEXISTENTE
    // ==========================================

    @Test
    void verificarClienteInexistente() {

        when(clienteService.buscarPorId(999L))
                .thenThrow(
                        new ClienteNoEncontradoException(
                                "Cliente no encontrado"));

        StreamObserver<VerifyCustomerResponse> observer =
                crearObserver();

        VerifyCustomerRequest request =
                VerifyCustomerRequest.newBuilder()
                        .setId(999L)
                        .build();

        grpcService.verifyCustomer(request, observer);

        ArgumentCaptor<VerifyCustomerResponse> captor =
                ArgumentCaptor.forClass(
                        VerifyCustomerResponse.class);

        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();

        assertFalse(captor.getValue().getExiste());
        assertFalse(captor.getValue().getActivo());
    }

    // ==========================================
    // TEST 3 - VERIFY CUSTOMER CON ID INVALIDO
    // ==========================================

    @Test
    void verificarIdInvalido() {

        StreamObserver<VerifyCustomerResponse> observer =
                crearObserver();

        VerifyCustomerRequest request =
                VerifyCustomerRequest.newBuilder()
                        .setId(0L)
                        .build();

        grpcService.verifyCustomer(request, observer);

        ArgumentCaptor<Throwable> captor =
                ArgumentCaptor.forClass(Throwable.class);

        verify(observer).onError(captor.capture());

        assertEquals(
                Status.Code.INVALID_ARGUMENT,
                Status.fromThrowable(
                        captor.getValue()).getCode());

        verify(clienteService, never())
                .buscarPorId(anyLong());
    }

    // ==========================================
    // TEST 4 - GET CUSTOMER EXISTENTE
    // ==========================================

    @Test
    void consultarClienteExistente() {

        Cliente cliente = crearCliente(
                1L, "90000001", "Ana", true);

        when(clienteService.buscarPorId(1L))
                .thenReturn(cliente);

        StreamObserver<Customer> observer =
                crearObserver();

        GetCustomerRequest request =
                GetCustomerRequest.newBuilder()
                        .setId(1L)
                        .build();

        grpcService.getCustomer(request, observer);

        ArgumentCaptor<Customer> captor =
                ArgumentCaptor.forClass(Customer.class);

        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();

        Customer respuesta = captor.getValue();

        assertEquals(1L, respuesta.getId());
        assertEquals("90000001", respuesta.getDocumento());
        assertEquals("Ana", respuesta.getNombre());
        assertEquals("Prueba", respuesta.getApellido());
        assertEquals(
                "ana.prueba@example.com",
                respuesta.getEmail());
        assertTrue(respuesta.getActivo());
    }

    // ==========================================
    // TEST 5 - GET CUSTOMER NO ENCONTRADO
    // ==========================================

    @Test
    void consultarClienteNoEncontrado() {

        when(clienteService.buscarPorId(999L))
                .thenThrow(
                        new ClienteNoEncontradoException(
                                "Cliente no encontrado"));

        StreamObserver<Customer> observer =
                crearObserver();

        GetCustomerRequest request =
                GetCustomerRequest.newBuilder()
                        .setId(999L)
                        .build();

        grpcService.getCustomer(request, observer);

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
    // TEST 6 - LISTAR TODOS LOS CLIENTES
    // ==========================================

    @Test
    void listarTodosLosClientes() {

        Cliente ana = crearCliente(
                1L, "90000001", "Ana", true);

        Cliente bruno = crearCliente(
                2L, "90000002", "Bruno", false);

        when(clienteService.listar())
                .thenReturn(List.of(ana, bruno));

        StreamObserver<ListCustomersResponse> observer =
                crearObserver();

        ListCustomersRequest request =
                ListCustomersRequest.newBuilder()
                        .build();

        grpcService.listCustomers(request, observer);

        ArgumentCaptor<ListCustomersResponse> captor =
                ArgumentCaptor.forClass(
                        ListCustomersResponse.class);

        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();

        ListCustomersResponse respuesta = captor.getValue();

        assertEquals(2, respuesta.getClientesCount());

        assertEquals(
                "Ana",
                respuesta.getClientes(0).getNombre());

        assertEquals(
                "Bruno",
                respuesta.getClientes(1).getNombre());

        assertTrue(respuesta.getClientes(0).getActivo());
        assertFalse(respuesta.getClientes(1).getActivo());
    }

    // ==========================================
    // TEST 7 - LISTAR SOLO CLIENTES ACTIVOS
    // ==========================================

    @Test
    void listarSoloClientesActivos() {

        Cliente ana = crearCliente(
                1L, "90000001", "Ana", true);

        Cliente bruno = crearCliente(
                2L, "90000002", "Bruno", false);

        when(clienteService.listar())
                .thenReturn(List.of(ana, bruno));

        StreamObserver<ListCustomersResponse> observer =
                crearObserver();

        ListCustomersRequest request =
                ListCustomersRequest.newBuilder()
                        .setSoloActivos(true)
                        .build();

        grpcService.listCustomers(request, observer);

        ArgumentCaptor<ListCustomersResponse> captor =
                ArgumentCaptor.forClass(
                        ListCustomersResponse.class);

        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();

        ListCustomersResponse respuesta = captor.getValue();

        assertEquals(1, respuesta.getClientesCount());

        assertEquals(
                "Ana",
                respuesta.getClientes(0).getNombre());

        assertTrue(respuesta.getClientes(0).getActivo());
    }

    // ==========================================
    // TEST 8 - LISTAR CLIENTES POR ID
    // ==========================================

    @Test
    void listarClientesPorId() {

        Cliente ana = crearCliente(
                1L, "90000001", "Ana", true);

        Cliente bruno = crearCliente(
                2L, "90000002", "Bruno", false);

        when(clienteService.listar())
                .thenReturn(List.of(ana, bruno));

        StreamObserver<ListCustomersResponse> observer =
                crearObserver();

        ListCustomersRequest request =
                ListCustomersRequest.newBuilder()
                        .addIds(2L)
                        .build();

        grpcService.listCustomers(request, observer);

        ArgumentCaptor<ListCustomersResponse> captor =
                ArgumentCaptor.forClass(
                        ListCustomersResponse.class);

        verify(observer).onNext(captor.capture());
        verify(observer).onCompleted();

        ListCustomersResponse respuesta = captor.getValue();

        assertEquals(1, respuesta.getClientesCount());

        assertEquals(
                2L,
                respuesta.getClientes(0).getId());

        assertEquals(
                "Bruno",
                respuesta.getClientes(0).getNombre());

        assertFalse(respuesta.getClientes(0).getActivo());
    }

    // ==========================================
    // CREAR CLIENTES SIMULADOS PARA LOS TESTS
    // ==========================================

    private Cliente crearCliente(
            Long id,
            String documento,
            String nombre,
            boolean activo) {

        Cliente cliente = new Cliente();

        cliente.setId(id);
        cliente.setDocumento(documento);
        cliente.setNombre(nombre);
        cliente.setApellido("Prueba");

        cliente.setEmail(
                nombre.toLowerCase() + ".prueba@example.com");

        cliente.setActivo(activo);

        return cliente;
    }

    // ==========================================
    // CREAR OBSERVADOR MOCK
    // ==========================================

    @SuppressWarnings("unchecked")
    private <T> StreamObserver<T> crearObserver() {

        return mock(StreamObserver.class);
    }
}