package com.rentar.customer.server;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class GrpcServerLifecycle {

    private final CustomerGrpcService customerGrpcService;
    private final int puerto;

    private Server server;

    public GrpcServerLifecycle(
            CustomerGrpcService customerGrpcService,
            @Value("${customer.grpc.port:9092}") int puerto) {

        this.customerGrpcService = customerGrpcService;
        this.puerto = puerto;
    }

    @PostConstruct
    public void iniciar() throws IOException {

        server = ServerBuilder
                .forPort(puerto)
                .addService(customerGrpcService)
                .build()
                .start();

        System.out.println(
                "Customer Service gRPC iniciado en puerto " + puerto);

        Thread espera = new Thread(() -> {
            try {
                server.awaitTermination();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "customer-grpc-server");

        espera.setDaemon(false);
        espera.start();
    }

    @PreDestroy
    public void detener() {

        if (server != null) {

            server.shutdown();

            try {
                if (!server.awaitTermination(5, TimeUnit.SECONDS)) {
                    server.shutdownNow();
                }
            } catch (InterruptedException e) {
                server.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}