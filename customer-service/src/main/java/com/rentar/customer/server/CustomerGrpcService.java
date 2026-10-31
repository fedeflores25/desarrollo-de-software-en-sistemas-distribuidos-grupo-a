package com.rentar.customer.server;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.rentar.customer.dto.ClienteRequest;
import com.rentar.customer.dto.UsuarioAutenticado;
import com.rentar.customer.entity.Cliente;
import com.rentar.customer.exception.ClienteDuplicadoException;
import com.rentar.customer.exception.ClienteNoEncontradoException;

import com.rentar.customer.grpc.Customer;
import com.rentar.customer.grpc.CustomerServiceGrpc;
import com.rentar.customer.grpc.AuthenticateUserRequest;
import com.rentar.customer.grpc.AuthenticateUserResponse;
import com.rentar.customer.grpc.CreateCustomerRequest;
import com.rentar.customer.grpc.DeactivateCustomerRequest;
import com.rentar.customer.grpc.GetCustomerRequest;
import com.rentar.customer.grpc.ListCustomersRequest;
import com.rentar.customer.grpc.ListCustomersResponse;
import com.rentar.customer.grpc.UpdateCustomerRequest;
import com.rentar.customer.grpc.VerifyCustomerRequest;
import com.rentar.customer.grpc.VerifyCustomerResponse;
import com.rentar.customer.grpc.UserRole;

import com.rentar.customer.service.IClienteService;
import com.rentar.customer.service.IUsuarioService;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@Service
public class CustomerGrpcService
        extends CustomerServiceGrpc.CustomerServiceImplBase {

    private static final Logger log =
            LoggerFactory.getLogger(CustomerGrpcService.class);

    private final IClienteService clienteService;
    private final IUsuarioService usuarioService;
    private final Validator validator;

    public CustomerGrpcService(
            IClienteService clienteService,
            IUsuarioService usuarioService,
            Validator validator) {

        this.clienteService = clienteService;
        this.usuarioService = usuarioService;
        this.validator = validator;
    }

    // ==========================================
    // AUTENTICAR USUARIO
    // ==========================================

    @Override
    public void authenticateUser(
            AuthenticateUserRequest request,
            StreamObserver<AuthenticateUserResponse> responseObserver) {

        var usuario = usuarioService.autenticar(
                request.getUsername(), request.getPassword());

        AuthenticateUserResponse.Builder respuesta = AuthenticateUserResponse.newBuilder()
                .setAutenticado(usuario.isPresent());

        usuario.ifPresent(autenticado -> completarAutenticacion(respuesta, autenticado));

        responseObserver.onNext(respuesta.build());
        responseObserver.onCompleted();
    }

    private void completarAutenticacion(
            AuthenticateUserResponse.Builder respuesta,
            UsuarioAutenticado usuario) {

        respuesta.setUsername(usuario.username())
                .setRol(usuario.rol().name().equals("ADMIN")
                        ? UserRole.USER_ROLE_ADMIN : UserRole.USER_ROLE_CLIENTE);

        if (usuario.clienteId() != null) {
            respuesta.setClienteId(usuario.clienteId());
        }
    }

    // ==========================================
    // VERIFICAR EXISTENCIA Y ESTADO DEL CLIENTE
    // ==========================================

    @Override
    public void verifyCustomer(
            VerifyCustomerRequest request,
            StreamObserver<VerifyCustomerResponse> responseObserver) {

        if (request.getId() <= 0) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("El ID debe ser mayor a cero")
                            .asRuntimeException());
            return;
        }

        boolean existe = false;
        boolean activo = false;

        try {

            Cliente cliente = clienteService.buscarPorId(request.getId());

            existe = true;
            activo = Boolean.TRUE.equals(cliente.getActivo());

        } catch (ClienteNoEncontradoException e) {
            // Cliente inexistente: existe=false, activo=false

        } catch (Exception e) {

            log.error("Error al verificar cliente", e);

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Error al verificar el cliente")
                            .asRuntimeException());
            return;
        }

        VerifyCustomerResponse respuesta =
                VerifyCustomerResponse.newBuilder()
                        .setExiste(existe)
                        .setActivo(activo)
                        .build();

        responseObserver.onNext(respuesta);
        responseObserver.onCompleted();
    }

    // ==========================================
    // CONSULTAR UN CLIENTE POR ID
    // ==========================================

    @Override
    public void getCustomer(
            GetCustomerRequest request,
            StreamObserver<Customer> responseObserver) {

        if (request.getId() <= 0) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("El ID debe ser mayor a cero")
                            .asRuntimeException());
            return;
        }

        try {

            Cliente cliente = clienteService.buscarPorId(request.getId());

            Customer respuesta = convertirAProto(cliente);

            responseObserver.onNext(respuesta);
            responseObserver.onCompleted();

        } catch (ClienteNoEncontradoException e) {

            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription(e.getMessage())
                            .asRuntimeException());

        } catch (Exception e) {

            log.error("Error al consultar cliente", e);

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Error al consultar el cliente")
                            .asRuntimeException());
        }
    }

    // ==========================================
    // CONSULTAR LISTA DE CLIENTES
    // ==========================================

    @Override
    public void listCustomers(
            ListCustomersRequest request,
            StreamObserver<ListCustomersResponse> responseObserver) {

        if (request.getIdsList().stream().anyMatch(id -> id <= 0)) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("Los IDs deben ser mayores a cero")
                            .asRuntimeException());
            return;
        }

        try {

            var clientes = clienteService.listar();

            ListCustomersResponse.Builder respuesta =
                    ListCustomersResponse.newBuilder();

            for (Cliente cliente : clientes) {

                // Filtrar por IDs
                if (request.getIdsCount() > 0
                        && !request.getIdsList().contains(cliente.getId())) {
                    continue;
                }

                // Filtrar clientes activos
                if (request.hasSoloActivos()
                        && request.getSoloActivos()
                        && !Boolean.TRUE.equals(cliente.getActivo())) {
                    continue;
                }

                respuesta.addClientes(convertirAProto(cliente));
            }

            responseObserver.onNext(respuesta.build());
            responseObserver.onCompleted();

        } catch (Exception e) {

            log.error("Error al listar clientes", e);

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Error al consultar clientes")
                            .asRuntimeException());
        }
    }

    // ==========================================
    // CREAR UN NUEVO CLIENTE
    // ==========================================

    @Override
    public void createCustomer(
            CreateCustomerRequest request,
            StreamObserver<Customer> responseObserver) {

        ClienteRequest clienteRequest;

        try {

            clienteRequest = convertirYValidar(
                    request.getDocumento(),
                    request.getNombre(),
                    request.getApellido(),
                    request.getEmail(),
                    request.getTelefono(),
                    request.getFechaNacimiento());

        } catch (IllegalArgumentException e) {

            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asRuntimeException());
            return;
        }

        try {

            Cliente nuevoCliente = clienteService.crear(clienteRequest);

            responseObserver.onNext(convertirAProto(nuevoCliente));
            responseObserver.onCompleted();

        } catch (ClienteDuplicadoException e) {

            responseObserver.onError(
                    Status.ALREADY_EXISTS
                            .withDescription(e.getMessage())
                            .asRuntimeException());

        } catch (Exception e) {

            log.error("Error al crear cliente", e);

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Error al crear el cliente")
                            .asRuntimeException());
        }
    }

    // ==========================================
    // MODIFICAR UN CLIENTE EXISTENTE
    // ==========================================

    @Override
    public void updateCustomer(
            UpdateCustomerRequest request,
            StreamObserver<Customer> responseObserver) {

        if (request.getId() <= 0) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("El ID debe ser mayor a cero")
                            .asRuntimeException());
            return;
        }

        ClienteRequest clienteRequest;

        try {

            clienteRequest = convertirYValidar(
                    request.getDocumento(),
                    request.getNombre(),
                    request.getApellido(),
                    request.getEmail(),
                    request.getTelefono(),
                    request.getFechaNacimiento());

        } catch (IllegalArgumentException e) {

            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription(e.getMessage())
                            .asRuntimeException());
            return;
        }

        try {

            Cliente clienteActualizado =
                    clienteService.modificar(
                            request.getId(),
                            clienteRequest);

            responseObserver.onNext(
                    convertirAProto(clienteActualizado));

            responseObserver.onCompleted();

        } catch (ClienteNoEncontradoException e) {

            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription(e.getMessage())
                            .asRuntimeException());

        } catch (ClienteDuplicadoException e) {

            responseObserver.onError(
                    Status.ALREADY_EXISTS
                            .withDescription(e.getMessage())
                            .asRuntimeException());

        } catch (Exception e) {

            log.error("Error al modificar cliente", e);

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Error al modificar el cliente")
                            .asRuntimeException());
        }
    }

    // ==========================================
    // DESACTIVAR UN CLIENTE
    // ==========================================

    @Override
    public void deactivateCustomer(
            DeactivateCustomerRequest request,
            StreamObserver<Customer> responseObserver) {

        // Validar ID
        if (request.getId() <= 0) {
            responseObserver.onError(
                    Status.INVALID_ARGUMENT
                            .withDescription("El ID debe ser mayor a cero")
                            .asRuntimeException());
            return;
        }

        try {

            // Realizar baja logica
            clienteService.darDeBaja(request.getId());

            // Consultar el cliente con su nuevo estado
            Cliente clienteDesactivado =
                    clienteService.buscarPorId(request.getId());

            // Convertir a Protobuf
            Customer respuesta = convertirAProto(clienteDesactivado);

            responseObserver.onNext(respuesta);
            responseObserver.onCompleted();

        } catch (ClienteNoEncontradoException e) {

            responseObserver.onError(
                    Status.NOT_FOUND
                            .withDescription(e.getMessage())
                            .asRuntimeException());

        } catch (Exception e) {

            log.error("Error al desactivar cliente", e);

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Error al desactivar el cliente")
                            .asRuntimeException());
        }
    }

    // ==========================================
    // CONVERTIR Y VALIDAR DATOS DEL CLIENTE
    // ==========================================

    private ClienteRequest convertirYValidar(
            String documento,
            String nombre,
            String apellido,
            String email,
            String telefono,
            String fechaNacimiento) {

        ClienteRequest clienteRequest = new ClienteRequest();

        clienteRequest.setDocumento(documento);
        clienteRequest.setNombre(nombre);
        clienteRequest.setApellido(apellido);
        clienteRequest.setEmail(email);
        clienteRequest.setTelefono(telefono);

        // Convertir fecha si fue informada
        if (fechaNacimiento != null
                && !fechaNacimiento.isBlank()) {

            try {

                LocalDate fecha = LocalDate.parse(fechaNacimiento);

                clienteRequest.setFechaNacimiento(fecha);

            } catch (DateTimeParseException e) {

                throw new IllegalArgumentException(
                        "La fecha debe tener formato AAAA-MM-DD");
            }
        }

        // Validar datos
        Set<ConstraintViolation<ClienteRequest>> errores =
                validator.validate(clienteRequest);

        if (!errores.isEmpty()) {

            String mensaje = errores.stream()
                    .map(ConstraintViolation::getMessage)
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining("; "));

            throw new IllegalArgumentException(mensaje);
        }

        return clienteRequest;
    }

    // ==========================================
    // CONVERTIR ENTIDAD JAVA A PROTOBUF
    // ==========================================

    private Customer convertirAProto(Cliente cliente) {

        return Customer.newBuilder()
                .setId(cliente.getId())
                .setDocumento(cliente.getDocumento())
                .setNombre(cliente.getNombre())
                .setApellido(cliente.getApellido())
                .setEmail(cliente.getEmail())
                .setTelefono(
                        cliente.getTelefono() != null
                                ? cliente.getTelefono() : "")
                .setFechaNacimiento(
                        cliente.getFechaNacimiento() != null
                                ? cliente.getFechaNacimiento().toString() : "")
                .setActivo(Boolean.TRUE.equals(cliente.getActivo()))
                .build();
    }
}
