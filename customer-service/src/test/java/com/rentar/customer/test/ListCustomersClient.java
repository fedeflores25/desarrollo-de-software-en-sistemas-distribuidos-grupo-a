package com.rentar.customer.test;

import java.util.concurrent.TimeUnit;

import com.rentar.customer.grpc.Customer;
import com.rentar.customer.grpc.CustomerServiceGrpc;
import com.rentar.customer.grpc.ListCustomersRequest;
import com.rentar.customer.grpc.ListCustomersResponse;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class ListCustomersClient {

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

            // Solicitar solamente el cliente con ID 2
            ListCustomersRequest solicitud =
                    ListCustomersRequest.newBuilder()
                            .addIds(1L)
                            .build();

            // Enviar solicitud
            ListCustomersResponse respuesta =
                    clienteGrpc.listCustomers(solicitud);

            System.out.println("=== CLIENTES FILTRADOS POR ID ===");

            System.out.println("Total de clientes: "
                    + respuesta.getClientesCount());

            for (Customer cliente : respuesta.getClientesList()) {

                System.out.println("-------------------------");
                System.out.println("ID: " + cliente.getId());
                System.out.println("Documento: " + cliente.getDocumento());
                System.out.println("Nombre: " + cliente.getNombre());
                System.out.println("Apellido: " + cliente.getApellido());
                System.out.println("Email: " + cliente.getEmail());
                System.out.println("Activo: " + cliente.getActivo());
            }

        } catch (StatusRuntimeException e) {

            System.out.println("Error gRPC: " + e.getStatus());

        } finally {
            canal.shutdownNow();
        }
    }
}