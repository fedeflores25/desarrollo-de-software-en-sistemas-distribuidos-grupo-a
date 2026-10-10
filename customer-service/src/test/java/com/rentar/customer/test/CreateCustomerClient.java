package com.rentar.customer.test;

import java.util.concurrent.TimeUnit;

import com.rentar.customer.grpc.Customer;
import com.rentar.customer.grpc.CustomerServiceGrpc;
import com.rentar.customer.grpc.CreateCustomerRequest;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class CreateCustomerClient {

    public static void main(String[] args) {

        // Conexion al Customer Service
        ManagedChannel canal = ManagedChannelBuilder
                .forAddress("localhost", 9092)
                .usePlaintext()
                .build();

        try {

            // Crear cliente gRPC
            var clienteGrpc = CustomerServiceGrpc
                    .newBlockingStub(canal)
                    .withDeadlineAfter(5, TimeUnit.SECONDS);

            // Preparar un nuevo cliente con un email ya registrado
            CreateCustomerRequest solicitud =
                    CreateCustomerRequest.newBuilder()
                            .setDocumento("90000004")
                            .setNombre("Daniel")
                            .setApellido("Prueba")
                            .setEmail("ana.prueba@example.com")
                            .setTelefono("1145678901")
                            .setFechaNacimiento("1999-06-10")
                            .build();

            System.out.println("=== PRUEBA DE EMAIL DUPLICADO ===");
            System.out.println("Cliente a registrar: Daniel");
            System.out.println("Email utilizado: " + solicitud.getEmail());
            System.out.println();

            // Intentar registrar el cliente
            Customer respuesta =
                    clienteGrpc.createCustomer(solicitud);

            // Resultado incorrecto: el registro fue aceptado
            System.out.println("=== PRUEBA FALLIDA ===");
            System.out.println("El cliente fue registrado.");
            System.out.println("El servidor no rechazo el email duplicado.");
            System.out.println("ID asignado: " + respuesta.getId());
            System.out.println("Nombre: " + respuesta.getNombre());
            System.out.println("Email: " + respuesta.getEmail());

        } catch (StatusRuntimeException e) {

            if (e.getStatus().getCode() == Status.Code.ALREADY_EXISTS
                    && e.getStatus().getDescription() != null
                    && e.getStatus().getDescription().contains("email")) {

                // Resultado esperado: rechazar el email duplicado
                System.out.println("=== PRUEBA EXITOSA ===");
                System.out.println("Registro rechazado correctamente.");
                System.out.println("Motivo: el email ya pertenece a otro cliente.");
                System.out.println("Codigo gRPC: ALREADY_EXISTS");

            } else {

                // Mostrar un error diferente al esperado
                System.out.println("=== PRUEBA FALLIDA ===");
                System.out.println("El servidor devolvio un error inesperado.");
                System.out.println("Detalle: " + e.getStatus());

            }

        } finally {

            canal.shutdownNow();
        }
    }
}