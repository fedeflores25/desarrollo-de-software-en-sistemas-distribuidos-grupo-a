package com.rentar.vehicle.grpc.client;

import com.rentar.vehicle.grpc.CreateVehicleRequest;
import com.rentar.vehicle.grpc.DeactivateVehicleRequest;
import com.rentar.vehicle.grpc.GetVehicleRequest;
import com.rentar.vehicle.grpc.GetVehicleByPatenteRequest;
import com.rentar.vehicle.grpc.ListAvailableVehiclesRequest;
import com.rentar.vehicle.grpc.UpdateVehicleRequest;
import com.rentar.vehicle.grpc.UpdateVehicleStatusRequest;
import com.rentar.vehicle.grpc.Vehicle;
import com.rentar.vehicle.grpc.VehicleServiceGrpc;
import com.rentar.vehicle.grpc.VehicleStatus;
import com.rentar.vehicle.grpc.VehicleType;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Cliente de consola para realizar pruebas manuales contra Vehicle Service gRPC.
 */
public final class VehicleGrpcClient {

    private final VehicleServiceGrpc.VehicleServiceBlockingStub stub;
    private final Scanner scanner;

    private VehicleGrpcClient(VehicleServiceGrpc.VehicleServiceBlockingStub stub, Scanner scanner) {
        this.stub = stub;
        this.scanner = scanner;
    }

    public static void main(String[] args) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9091)
                .usePlaintext()
                .build();

        try (Scanner scanner = new Scanner(System.in)) {
            VehicleGrpcClient client = new VehicleGrpcClient(
                    VehicleServiceGrpc.newBlockingStub(channel), scanner);
            client.runMenu();
        } finally {
            channel.shutdownNow();
        }
    }

    private void runMenu() {
        int option;
        do {
            printMenu();
            option = readInt("Opcion: ");

            try {
                switch (option) {
                    case 1 -> listVehicles();
                    case 2 -> getVehicle();
                    case 3 -> getVehicleByPatente();
                    case 4 -> createVehicle();
                    case 5 -> updateVehicle();
                    case 6 -> deactivateVehicle();
                    case 7 -> updateVehicleStatus();
                    case 8 -> listAvailableVehicles();
                    case 9 -> testDuplicatePatent();
                    case 0 -> System.out.println("Cliente finalizado.");
                    default -> System.out.println("Opcion no valida.");
                }
            } catch (StatusRuntimeException exception) {
                System.out.printf("Error gRPC: %s - %s%n", exception.getStatus().getCode(),
                        exception.getStatus().getDescription());
            } catch (IllegalArgumentException exception) {
                System.out.println("Dato invalido: " + exception.getMessage());
            }
        } while (option != 0);
    }

    private void printMenu() {
        System.out.println();
        System.out.println("=== Vehicle Service gRPC Client ===");
        System.out.println("1. Listar vehiculos");
        System.out.println("2. Consultar vehiculo por ID");
        System.out.println("3. Consultar vehiculo por patente");
        System.out.println("4. Crear vehiculo");
        System.out.println("5. Modificar vehiculo");
        System.out.println("6. Dar de baja vehiculo");
        System.out.println("7. Actualizar estado");
        System.out.println("8. Consultar disponibilidad");
        System.out.println("9. Probar patente duplicada");
        System.out.println("0. Salir");
    }

    private void listVehicles() {
        List<Vehicle> vehicles = stub.listVehicles(
                com.rentar.vehicle.grpc.ListVehiclesRequest.getDefaultInstance()).getVehiculosList();
        System.out.println("Vehiculos encontrados: " + vehicles.size());
        vehicles.forEach(this::printVehicle);
    }

    private void getVehicle() {
        Vehicle vehicle = stub.getVehicle(GetVehicleRequest.newBuilder()
                .setId(readLong("ID: "))
                .build());
        printVehicle(vehicle);
    }

    private void getVehicleByPatente() {
        Vehicle vehicle = stub.getVehicleByPatente(GetVehicleByPatenteRequest.newBuilder()
                .setPatente(readRequired("Patente: "))
                .build());
        printVehicle(vehicle);
    }

    private void createVehicle() {
        CreateVehicleRequest request = CreateVehicleRequest.newBuilder()
                .setPatente(readRequired("Patente: "))
                .setMarca(readRequired("Marca: "))
                .setModelo(readRequired("Modelo: "))
                .setAnio(readInt("Anio: "))
                .setColor(readRequired("Color: "))
                .setTipo(readVehicleType())
                .setPrecioDiario(readDouble("Precio diario: "))
                .build();
        System.out.println("Vehiculo creado:");
        printVehicle(stub.createVehicle(request));
    }

    private void updateVehicle() {
        UpdateVehicleRequest request = UpdateVehicleRequest.newBuilder()
                .setId(readLong("ID: "))
                .setMarca(readRequired("Marca: "))
                .setModelo(readRequired("Modelo: "))
                .setAnio(readInt("Anio: "))
                .setColor(readRequired("Color: "))
                .setTipo(readVehicleType())
                .setPrecioDiario(readDouble("Precio diario: "))
                .build();
        System.out.println("Vehiculo actualizado:");
        printVehicle(stub.updateVehicle(request));
    }

    private void deactivateVehicle() {
        Vehicle vehicle = stub.deactivateVehicle(DeactivateVehicleRequest.newBuilder()
                .setId(readLong("ID: "))
                .build());
        System.out.println("Vehiculo dado de baja:");
        printVehicle(vehicle);
    }

    private void updateVehicleStatus() {
        Vehicle vehicle = stub.updateVehicleStatus(UpdateVehicleStatusRequest.newBuilder()
                .setId(readLong("ID: "))
                .setEstado(readVehicleStatus())
                .build());
        System.out.println("Estado actualizado:");
        printVehicle(vehicle);
    }

    private void listAvailableVehicles() {
        String occupiedIds = read("IDs ocupados separados por coma (vacio si no hay): ");
        ListAvailableVehiclesRequest.Builder request = ListAvailableVehiclesRequest.newBuilder();
        if (!occupiedIds.isBlank()) {
            request.addAllExcluirIds(Arrays.stream(occupiedIds.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .map(Long::parseLong)
                    .collect(Collectors.toList()));
        }

        List<Vehicle> vehicles = stub.listAvailableVehicles(request.build()).getVehiculosList();
        System.out.println("Vehiculos disponibles: " + vehicles.size());
        vehicles.forEach(this::printVehicle);
    }

    private void testDuplicatePatent() {
        CreateVehicleRequest request = CreateVehicleRequest.newBuilder()
                .setPatente("ABC123")
                .setMarca("Prueba")
                .setModelo("Duplicado")
                .setAnio(2026)
                .setColor("Gris")
                .setTipo(VehicleType.SEDAN)
                .setPrecioDiario(1.0)
                .build();
        stub.createVehicle(request);
    }

    private VehicleType readVehicleType() {
        return VehicleType.valueOf(readRequired("Tipo (SEDAN, SUV, PICKUP, COUPE, HATCHBACK): ")
                .toUpperCase(Locale.ROOT));
    }

    private VehicleStatus readVehicleStatus() {
        return VehicleStatus.valueOf(readRequired("Estado (DISPONIBLE, RESERVADO, EN_ALQUILER): ")
                .toUpperCase(Locale.ROOT));
    }

    private int readInt(String label) {
        return Integer.parseInt(readRequired(label));
    }

    private long readLong(String label) {
        return Long.parseLong(readRequired(label));
    }

    private double readDouble(String label) {
        return Double.parseDouble(readRequired(label).replace(',', '.'));
    }

    private String readRequired(String label) {
        String value = read(label);
        if (value.isBlank()) {
            throw new IllegalArgumentException("el valor es obligatorio");
        }
        return value;
    }

    private String read(String label) {
        System.out.print(label);
        return scanner.nextLine().trim();
    }

    private void printVehicle(Vehicle vehicle) {
        System.out.printf("ID: %d | Patente: %s | %s %s | %d | %s | %s | $ %.2f | %s | activo=%s%n",
                vehicle.getId(), vehicle.getPatente(), vehicle.getMarca(), vehicle.getModelo(),
                vehicle.getAnio(), vehicle.getColor(), vehicle.getTipo(), vehicle.getPrecioDiario(),
                vehicle.getEstado(), vehicle.getActivo());
    }
}
