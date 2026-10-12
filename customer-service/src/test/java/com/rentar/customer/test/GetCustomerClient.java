package com.rentar.customer.test;

import java.util.concurrent.TimeUnit;

import com.rentar.customer.grpc.Customer;
import com.rentar.customer.grpc.CustomerServiceGrpc;
import com.rentar.customer.grpc.GetCustomerRequest;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class GetCustomerClient {

    public static void main(String[] args) {

        // Conexion al servidor gRPC
        ManagedChannel canal = ManagedChannelBuilder
                .forAddress("localhost", 9092)
                .usePlaintext()
                .build();

        try {

            // Crear cliente gRPC
            var clienteGrpc = CustomerServiceGrpc
                    .newBlockingStub(canal)
                    .withDeadlineAfter(5, TimeUnit.SECONDS);

            // Buscar un cliente por su ID
            GetCustomerRequest solicitud =
                    GetCustomerRequest.newBuilder()
                            .setId(1L)
                            .build();

            // Enviar solicitud y recibir respuesta
            Customer respuesta =
                    clienteGrpc.getCustomer(solicitud);

            // Mostrar los datos del cliente
            System.out.println("=== CLIENTE ENCONTRADO ===");
            System.out.println("ID: " + respuesta.getId());
            System.out.println("Documento: " + respuesta.getDocumento());
            System.out.println("Nombre: " + respuesta.getNombre());
            System.out.println("Apellido: " + respuesta.getApellido());
            System.out.println("Email: " + respuesta.getEmail());
            System.out.println("Telefono: " + respuesta.getTelefono());
            System.out.println("Fecha nacimiento: " + respuesta.getFechaNacimiento());
            System.out.println("Activo: " + respuesta.getActivo());

        } catch (StatusRuntimeException e) {

            System.out.println("Error gRPC: " + e.getStatus());

        } finally {
            canal.shutdownNow();
        }
    }
}