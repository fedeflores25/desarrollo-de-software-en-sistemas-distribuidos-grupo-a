package com.rentar.customer.server;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.rentar.customer.entity.Cliente;
import com.rentar.customer.grpc.Customer;
import com.rentar.customer.grpc.CustomerServiceGrpc;
import com.rentar.customer.grpc.CreateCustomerRequest;
import com.rentar.customer.grpc.DeactivateCustomerRequest;
import com.rentar.customer.grpc.GetCustomerRequest;
import com.rentar.customer.grpc.ListCustomersRequest;
import com.rentar.customer.grpc.VerifyCustomerRequest;
import com.rentar.customer.repository.ClienteRepository;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:3306/rentar_clientes_integracion?createDatabaseIfNotExist=true",
        "customer.grpc.port=0"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CustomerGrpcIntegrationTest {

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private DataSource dataSource;

    private static Server servidor;
    private static ManagedChannel canal;
    private static CustomerServiceGrpc.CustomerServiceBlockingStub clienteGrpc;

    // Guardar los IDs de los clientes creados durante cada prueba
    private final List<Long> idsCreados = new ArrayList<>();

    // ==========================================
    // INICIAR EL SERVIDOR GRPC DE INTEGRACION
    // ==========================================

    @BeforeAll
    static void iniciar(@Autowired CustomerGrpcService servicio)
            throws Exception {

        servidor = ServerBuilder
                .forPort(0)
                .addService(servicio)
                .build()
                .start();

        canal = ManagedChannelBuilder
                .forAddress("localhost", servidor.getPort())
                .usePlaintext()
                .build();

        clienteGrpc = CustomerServiceGrpc
                .newBlockingStub(canal)
                .withDeadlineAfter(10, TimeUnit.SECONDS);
    }

    // ==========================================
    // VERIFICAR LA BASE DE DATOS DE INTEGRACION
    // ==========================================

    @BeforeEach
    void comprobarBaseDeDatos() throws SQLException {

        try (Connection conexion = dataSource.getConnection()) {

            assertEquals(
                    "rentar_clientes_integracion",
                    conexion.getCatalog(),
                    "La prueba no puede modificar la base de datos habitual");
        }
    }

    // ==========================================
    // ELIMINAR LOS CLIENTES CREADOS EN CADA PRUEBA
    // ==========================================

    @AfterEach
    void limpiar() {

        if (!idsCreados.isEmpty()) {
            clienteRepository.deleteAllById(idsCreados);
            idsCreados.clear();
        }
    }

    // ==========================================
    // DETENER EL SERVIDOR GRPC DE INTEGRACION
    // ==========================================

    @AfterAll
    static void detener() throws InterruptedException {

        if (canal != null) {
            canal.shutdownNow();
            canal.awaitTermination(5, TimeUnit.SECONDS);
        }

        if (servidor != null) {
            servidor.shutdownNow();
            servidor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    // ==========================================
    // TEST 1 - CREAR Y CONSULTAR CLIENTE REAL
    // ==========================================

    @Test
    void crearYConsultarClienteCorrectamente() {

        CreateCustomerRequest solicitud = solicitudValida();

        Customer creado = crearCliente(solicitud);

        assertTrue(creado.getId() > 0);
        assertEquals(solicitud.getDocumento(), creado.getDocumento());
        assertEquals(solicitud.getNombre(), creado.getNombre());
        assertEquals(solicitud.getEmail(), creado.getEmail());
        assertEquals("2000-03-20", creado.getFechaNacimiento());
        assertTrue(creado.getActivo());

        // Verificar que el cliente se guardo en MySQL
        Cliente guardado = clienteRepository
                .findById(creado.getId())
                .orElseThrow();

        assertEquals(solicitud.getEmail(), guardado.getEmail());

        // Consultar el cliente mediante gRPC
        Customer consultado = clienteGrpc.getCustomer(
                GetCustomerRequest.newBuilder()
                        .setId(creado.getId())
                        .build());

        assertEquals(creado.getId(), consultado.getId());
        assertEquals(creado.getNombre(), consultado.getNombre());
    }

    // ==========================================
    // TEST 2 - VERIFICAR CLIENTE Y BAJA LOGICA
    // ==========================================

    @Test
    void verificarYDesactivarCliente() {

        Customer creado = crearCliente(solicitudValida());

        var verificacionInicial = clienteGrpc.verifyCustomer(
                VerifyCustomerRequest.newBuilder()
                        .setId(creado.getId())
                        .build());

        assertTrue(verificacionInicial.getExiste());
        assertTrue(verificacionInicial.getActivo());

        Customer desactivado = clienteGrpc.deactivateCustomer(
                DeactivateCustomerRequest.newBuilder()
                        .setId(creado.getId())
                        .build());

        assertFalse(desactivado.getActivo());

        var verificacionFinal = clienteGrpc.verifyCustomer(
                VerifyCustomerRequest.newBuilder()
                        .setId(creado.getId())
                        .build());

        assertTrue(verificacionFinal.getExiste());
        assertFalse(verificacionFinal.getActivo());

        // Verificar que la baja logica conserve el registro en MySQL
        Cliente guardado = clienteRepository
                .findById(creado.getId())
                .orElseThrow();

        assertFalse(guardado.getActivo());
    }

    // ==========================================
    // TEST 3 - RECHAZAR EMAIL Y DOCUMENTO DUPLICADOS
    // ==========================================

    @Test
    void rechazarDatosDuplicados() {

        CreateCustomerRequest solicitud = solicitudValida();

        crearCliente(solicitud);

        // Intentar crear un cliente con email duplicado
        CreateCustomerRequest emailDuplicado =
                solicitudValida()
                        .toBuilder()
                        .setEmail(solicitud.getEmail())
                        .build();

        StatusRuntimeException errorEmail = assertThrows(
                StatusRuntimeException.class,
                () -> clienteGrpc.createCustomer(emailDuplicado));

        assertEquals(
                Status.Code.ALREADY_EXISTS,
                errorEmail.getStatus().getCode());

        // Intentar crear un cliente con documento duplicado
        CreateCustomerRequest documentoDuplicado =
                solicitudValida()
                        .toBuilder()
                        .setDocumento(solicitud.getDocumento())
                        .build();

        StatusRuntimeException errorDocumento = assertThrows(
                StatusRuntimeException.class,
                () -> clienteGrpc.createCustomer(documentoDuplicado));

        assertEquals(
                Status.Code.ALREADY_EXISTS,
                errorDocumento.getStatus().getCode());
    }

    // ==========================================
    // TEST 4 - LISTAR CLIENTES POR ID Y ESTADO
    // ==========================================

    @Test
    void listarClientesConFiltros() {

        Customer creado = crearCliente(solicitudValida());

        var respuesta = clienteGrpc.listCustomers(
                ListCustomersRequest.newBuilder()
                        .addIds(creado.getId())
                        .setSoloActivos(true)
                        .build());

        assertEquals(1, respuesta.getClientesCount());
        assertEquals(creado.getId(), respuesta.getClientes(0).getId());

        // Desactivar el cliente
        clienteGrpc.deactivateCustomer(
                DeactivateCustomerRequest.newBuilder()
                        .setId(creado.getId())
                        .build());

        // Verificar que el cliente no aparezca entre los activos
        var activos = clienteGrpc.listCustomers(
                ListCustomersRequest.newBuilder()
                        .addIds(creado.getId())
                        .setSoloActivos(true)
                        .build());

        assertEquals(0, activos.getClientesCount());
    }

    // ==========================================
    // TEST 5 - RECHAZAR FECHA DE NACIMIENTO INVALIDA
    // ==========================================

    @Test
    void rechazarFechaDeNacimientoInvalida() {

        CreateCustomerRequest solicitud =
                solicitudValida()
                        .toBuilder()
                        .setFechaNacimiento("20/03/2000")
                        .build();

        StatusRuntimeException error = assertThrows(
                StatusRuntimeException.class,
                () -> clienteGrpc.createCustomer(solicitud));

        assertEquals(
                Status.Code.INVALID_ARGUMENT,
                error.getStatus().getCode());
    }

    // ==========================================
    // GENERAR UNA SOLICITUD DE CLIENTE CON DATOS UNICOS
    // ==========================================

    private CreateCustomerRequest solicitudValida() {

        String identificador =
                UUID.randomUUID().toString().replace("-", "");

        return CreateCustomerRequest.newBuilder()
                .setDocumento("IT" + identificador)
                .setNombre("Cliente")
                .setApellido("Integracion")
                .setEmail("it." + identificador + "@example.com")
                .setTelefono("1122334455")
                .setFechaNacimiento("2000-03-20")
                .build();
    }

    // ==========================================
    // CREAR CLIENTE Y REGISTRAR SU ID PARA ELIMINACION
    // ==========================================

    private Customer crearCliente(CreateCustomerRequest solicitud) {

        Customer creado = clienteGrpc.createCustomer(solicitud);

        idsCreados.add(creado.getId());

        return creado;
    }
}
