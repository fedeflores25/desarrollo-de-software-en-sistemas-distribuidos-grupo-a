package com.rentar.rental.server;

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

    private final RentalGrpcService rentalGrpcService;
    private final int puerto;

    private Server server;

    public GrpcServerLifecycle(
            RentalGrpcService rentalGrpcService,
            @Value("${rental.grpc.port:9093}") int puerto) {

        this.rentalGrpcService = rentalGrpcService;
        this.puerto = puerto;
    }

    @PostConstruct
    public void iniciar() throws IOException {

        server = ServerBuilder
                .forPort(puerto)
                .addService(rentalGrpcService)
                .build()
                .start();

        System.out.println(
                "Rental Service gRPC iniciado en puerto " + puerto);

        // Mantiene el proceso vivo: la app no levanta servidor web.
        Thread espera = new Thread(() -> {
            try {
                server.awaitTermination();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "rental-grpc-server");

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
