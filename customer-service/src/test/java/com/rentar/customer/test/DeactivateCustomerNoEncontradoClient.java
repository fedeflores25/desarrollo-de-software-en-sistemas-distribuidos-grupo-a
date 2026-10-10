package com.rentar.customer.test;

import java.util.concurrent.TimeUnit;

import com.rentar.customer.grpc.Customer;
import com.rentar.customer.grpc.CustomerServiceGrpc;
import com.rentar.customer.grpc.DeactivateCustomerRequest;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class DeactivateCustomerNoEncontradoClient {

    public static void main(String[] args) {

        ManagedChannel canal = ManagedChannelBuilder
                .forAddress("localhost", 9092)
                .usePlaintext()
                .build();

        try {

            var clienteGrpc = CustomerServiceGrpc
                    .newBlockingStub(canal)
                    .withDeadlineAfter(5, TimeUnit.SECONDS);

            // ID de un cliente que no existe
            DeactivateCustomerRequest solicitud =
                    DeactivateCustomerRequest.newBuilder()
                            .setId(999999L)
                            .build();

            Customer respuesta =
                    clienteGrpc.deactivateCustomer(solicitud);

            System.out.println("PRUEBA FALLIDA");
            System.out.println(
                    "Se desactivo el cliente: " + respuesta.getId());

        } catch (StatusRuntimeException e) {

            if (e.getStatus().getCode() == Status.Code.NOT_FOUND) {

                System.out.println("=== PRUEBA EXITOSA ===");
                System.out.println("Cliente inexistente");
                System.out.println("Codigo gRPC: NOT_FOUND");

            } else {

                System.out.println("=== PRUEBA FALLIDA ===");
                System.out.println("Error inesperado: "
                        + e.getStatus());
            }

        } finally {
            canal.shutdownNow();
        }
    }
}