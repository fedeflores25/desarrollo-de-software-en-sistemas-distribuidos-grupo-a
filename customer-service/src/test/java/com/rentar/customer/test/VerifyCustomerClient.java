package com.rentar.customer.test;

import java.util.concurrent.TimeUnit;

import com.rentar.customer.grpc.CustomerServiceGrpc;
import com.rentar.customer.grpc.VerifyCustomerRequest;
import com.rentar.customer.grpc.VerifyCustomerResponse;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class VerifyCustomerClient {

    public static void main(String[] args) {

        // Conexion al Customer Service
        ManagedChannel canal = ManagedChannelBuilder
                .forAddress("localhost", 9092)
                .usePlaintext()
                .build();

        try {

            // Crear el cliente gRPC
            var clienteGrpc = CustomerServiceGrpc
                    .newBlockingStub(canal)
                    .withDeadlineAfter(5, TimeUnit.SECONDS);

            // Solicitud para verificar un cliente
            VerifyCustomerRequest solicitud =
                    VerifyCustomerRequest.newBuilder()
                            .setId(1L)
                            .build();

            // Enviar solicitud al servidor
            VerifyCustomerResponse respuesta =
                    clienteGrpc.verifyCustomer(solicitud);

            // Mostrar la respuesta
            System.out.println("=== RESPUESTA GRPC ===");
            System.out.println("Existe: " + respuesta.getExiste());
            System.out.println("Activo: " + respuesta.getActivo());

        } finally {
            canal.shutdownNow();
        }
    }
}