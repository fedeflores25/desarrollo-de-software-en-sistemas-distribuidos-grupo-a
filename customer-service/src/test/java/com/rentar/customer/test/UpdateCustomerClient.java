package com.rentar.customer.test;

import java.util.concurrent.TimeUnit;

import com.rentar.customer.grpc.Customer;
import com.rentar.customer.grpc.CustomerServiceGrpc;
import com.rentar.customer.grpc.UpdateCustomerRequest;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class UpdateCustomerClient {

    public static void main(String[] args) {

        // Conexion al servidor Customer Service
        ManagedChannel canal = ManagedChannelBuilder
                .forAddress("localhost", 9092)
                .usePlaintext()
                .build();

        try {

            // Crear cliente gRPC
            var clienteGrpc = CustomerServiceGrpc
                    .newBlockingStub(canal)
                    .withDeadlineAfter(5, TimeUnit.SECONDS);

            // Modificar los datos de Camila
            UpdateCustomerRequest solicitud =
                    UpdateCustomerRequest.newBuilder()
                            .setId(3L)
                            .setDocumento("90000003")
                            .setNombre("Camila")
                            .setApellido("Prueba")
                            .setEmail("camila.actualizada@example.com")
                            .setTelefono("1199998888")
                            .setFechaNacimiento("2000-03-20")
                            .build();

            // Enviar solicitud al servidor
            Customer respuesta =
                    clienteGrpc.updateCustomer(solicitud);

            // Mostrar los datos actualizados
            System.out.println("=== CLIENTE ACTUALIZADO ===");

            System.out.println("ID: " + respuesta.getId());
            System.out.println("Documento: " + respuesta.getDocumento());
            System.out.println("Nombre: " + respuesta.getNombre());
            System.out.println("Apellido: " + respuesta.getApellido());
            System.out.println("Email: " + respuesta.getEmail());
            System.out.println("Telefono: " + respuesta.getTelefono());
            System.out.println("Fecha nacimiento: "
                    + respuesta.getFechaNacimiento());
            System.out.println("Activo: " + respuesta.getActivo());

        } catch (StatusRuntimeException e) {

            System.out.println("Error gRPC: " + e.getStatus());

        } finally {

            canal.shutdownNow();
        }
    }
}