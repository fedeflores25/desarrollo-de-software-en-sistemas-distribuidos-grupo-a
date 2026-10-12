package com.rentar.vehicle.grpc.server;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class GrpcServerLifecycle {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcServerLifecycle.class);

    private final VehicleGrpcService vehicleGrpcService;
    private final int port;
    private Server server;

    public GrpcServerLifecycle(
            VehicleGrpcService vehicleGrpcService,
            @Value("${vehicle.grpc.port}") int port) {
        this.vehicleGrpcService = vehicleGrpcService;
        this.port = port;
    }

    @PostConstruct
    void start() throws IOException {
        server = NettyServerBuilder.forPort(port)
                .addService(vehicleGrpcService)
                .build()
                .start();
        LOGGER.info("Vehicle Service gRPC listening on port {}", port);
    }

    @PreDestroy
    void stop() {
        if (server != null) {
            server.shutdown();
        }
    }
}
