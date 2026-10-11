package com.rentar.customer.test;

import java.util.concurrent.TimeUnit;

import com.rentar.customer.grpc.Customer;
import com.rentar.customer.grpc.CustomerServiceGrpc;
import com.rentar.customer.grpc.CreateCustomerRequest;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class CreateCustomerSuccessClient {

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

            // Preparar un cliente nuevo con datos unicos
            CreateCustomerRequest solicitud =
                    CreateCustomerRequest.newBuilder()
                            .setDocumento("90000005")
                            .setNombre("Elena")
                            .setApellido("Prueba")
                            .setEmail("elena.prueba@example.com")
                            .setTelefono("1156789012")
                            .setFechaNacimiento("2001-04-13")
                            .build();

            System.out.println("=== PRUEBA DE CREACION DE CLIENTE ===");
            System.out.println("Cliente a registrar: Elena");
            System.out.println("Documento: " + solicitud.getDocumento());
            System.out.println("Email: " + solicitud.getEmail());
            System.out.println();

            // Registrar el cliente mediante gRPC
            Customer respuesta =
                    clienteGrpc.createCustomer(solicitud);

            // Verificar los datos devueltos por el servidor
            if (respuesta.getId() > 0
                    && respuesta.getDocumento()
                            .equals(solicitud.getDocumento())
                    && respuesta.getNombre()
                            .equals(solicitud.getNombre())
                    && respuesta.getEmail()
                            .equals(solicitud.getEmail())
                    && respuesta.getActivo()) {

                System.out.println("=== PRUEBA EXITOSA ===");
                System.out.println("Cliente registrado correctamente.");
                System.out.println();

                System.out.println("ID asignado: " + respuesta.getId());
                System.out.println("Documento: " + respuesta.getDocumento());
                System.out.println("Nombre: " + respuesta.getNombre());
                System.out.println("Apellido: " + respuesta.getApellido());
                System.out.println("Email: " + respuesta.getEmail());
                System.out.println("Telefono: " + respuesta.getTelefono());
                System.out.println("Fecha de nacimiento: "
                        + respuesta.getFechaNacimiento());
                System.out.println("Activo: " + respuesta.getActivo());

            } else {

                System.out.println("=== PRUEBA FALLIDA ===");
                System.out.println("El servidor devolvio datos inesperados.");
                System.out.println("Respuesta: " + respuesta);

            }

        } catch (StatusRuntimeException e) {

            System.out.println("=== PRUEBA FALLIDA ===");
            System.out.println("No se pudo registrar el cliente.");
            System.out.println("Codigo gRPC: "
                    + e.getStatus().getCode());
            System.out.println("Detalle: "
                    + e.getStatus().getDescription());

        } finally {

            canal.shutdownNow();
        }
    }
}