package com.rentar.customer.test;

import java.util.concurrent.TimeUnit;

import com.rentar.customer.grpc.Customer;
import com.rentar.customer.grpc.CustomerServiceGrpc;
import com.rentar.customer.grpc.DeactivateCustomerRequest;
import com.rentar.customer.grpc.GetCustomerRequest;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class DeactivateCustomerClient {

    public static void main(String[] args) {

        // ID de Camila
        long idCliente = 3L;

        // Conexion al servidor gRPC
        ManagedChannel canal = ManagedChannelBuilder
                .forAddress("localhost", 9092)
                .usePlaintext()
                .build();

        try {

            var clienteGrpc = CustomerServiceGrpc
                    .newBlockingStub(canal)
                    .withDeadlineAfter(5, TimeUnit.SECONDS);

            // 1. Consultar estado antes de la baja
            GetCustomerRequest consulta =
                    GetCustomerRequest.newBuilder()
                            .setId(idCliente)
                            .build();

            Customer antes = clienteGrpc.getCustomer(consulta);

            System.out.println("=== ANTES DE DESACTIVAR ===");
            System.out.println("ID: " + antes.getId());
            System.out.println("Nombre: " + antes.getNombre());
            System.out.println("Activo: " + antes.getActivo());

            // 2. Desactivar el cliente mediante gRPC
            DeactivateCustomerRequest solicitud =
                    DeactivateCustomerRequest.newBuilder()
                            .setId(idCliente)
                            .build();

            Customer desactivado =
                    clienteGrpc.deactivateCustomer(solicitud);

            System.out.println();
            System.out.println("=== CLIENTE DESACTIVADO ===");
            System.out.println("ID: " + desactivado.getId());
            System.out.println("Nombre: " + desactivado.getNombre());
            System.out.println("Activo: " + desactivado.getActivo());

            // 3. Consultar nuevamente para verificar
            Customer despues = clienteGrpc.getCustomer(consulta);

            System.out.println();
            System.out.println("=== VERIFICACION FINAL ===");
            System.out.println("ID: " + despues.getId());
            System.out.println("Nombre: " + despues.getNombre());
            System.out.println("Activo: " + despues.getActivo());

        } catch (StatusRuntimeException e) {

            System.out.println("Error gRPC: " + e.getStatus());

        } finally {

            canal.shutdownNow();
        }
    }
}