package com.rentar.rental.test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import com.rentar.rental.grpc.CancelReservationRequest;
import com.rentar.rental.grpc.CreateReservationRequest;
import com.rentar.rental.grpc.GetBusyVehicleIdsRequest;
import com.rentar.rental.grpc.GetRentalHistoryRequest;
import com.rentar.rental.grpc.GetRentalHistoryResponse;
import com.rentar.rental.grpc.GetReservationRequest;
import com.rentar.rental.grpc.ListReservationsRequest;
import com.rentar.rental.grpc.ListReservationsResponse;
import com.rentar.rental.grpc.RentalServiceGrpc;
import com.rentar.rental.grpc.Reservation;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

/**
 * Cliente de prueba para ejecutar a mano contra el Rental Service levantado (puerto 9093).
 * Recorre el flujo completo y deja la reserva de prueba cancelada, asi se puede repetir.
 */
public class RentalGrpcClient {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final long CLIENTE_ID = 1L;
    private static final long VEHICULO_ID = 1L;

    public static void main(String[] args) {

        ManagedChannel canal = ManagedChannelBuilder
                .forAddress("localhost", 9093)
                .usePlaintext()
                .build();

        try {

            var cliente = RentalServiceGrpc
                    .newBlockingStub(canal)
                    .withDeadlineAfter(10, TimeUnit.SECONDS);

            String inicio = fecha(30);
            String fin = fecha(33);

            // 1. Crear una reserva.
            System.out.println("=== 1. CREAR RESERVA ===");
            Reservation creada = cliente.createReservation(
                    CreateReservationRequest.newBuilder()
                            .setClienteId(CLIENTE_ID)
                            .setVehiculoId(VEHICULO_ID)
                            .setFechaInicio(inicio)
                            .setFechaFin(fin)
                            .setPrecioDiario(15000.0)
                            .build());
            mostrar(creada);

            // 2. Crear otra que se solapa: debe fallar.
            System.out.println("\n=== 2. CREAR RESERVA SOLAPADA (error esperado) ===");
            try {
                cliente.createReservation(
                        CreateReservationRequest.newBuilder()
                                .setClienteId(2L)
                                .setVehiculoId(VEHICULO_ID)
                                .setFechaInicio(fecha(31))
                                .setFechaFin(fecha(34))
                                .setPrecioDiario(15000.0)
                                .build());
                System.out.println("ERROR: la reserva solapada no deberia haberse creado");
            } catch (StatusRuntimeException e) {
                System.out.println("Error gRPC: " + e.getStatus());
            }

            // 3. Consultar por id.
            System.out.println("\n=== 3. CONSULTAR POR ID ===");
            mostrar(cliente.getReservation(
                    GetReservationRequest.newBuilder().setId(creada.getId()).build()));

            // 4. Listar las reservas del cliente.
            System.out.println("\n=== 4. LISTAR RESERVAS DEL CLIENTE ===");
            ListReservationsResponse lista = cliente.listReservations(
                    ListReservationsRequest.newBuilder().setClienteId(CLIENTE_ID).build());
            System.out.println("Reservas encontradas: " + lista.getReservasCount());
            lista.getReservasList().forEach(RentalGrpcClient::mostrarLinea);

            // 5. Vehiculos ocupados en el periodo.
            System.out.println("\n=== 5. VEHICULOS OCUPADOS EN EL PERIODO ===");
            System.out.println("IDs: " + cliente.getBusyVehicleIds(
                    GetBusyVehicleIdsRequest.newBuilder()
                            .setFechaInicio(inicio)
                            .setFechaFin(fin)
                            .build()).getVehiculoIdsList());

            // 6. Cancelar.
            System.out.println("\n=== 6. CANCELAR RESERVA ===");
            mostrar(cliente.cancelReservation(
                    CancelReservationRequest.newBuilder().setId(creada.getId()).build()));

            // 7. Historial del cliente.
            System.out.println("\n=== 7. HISTORIAL DEL CLIENTE ===");
            GetRentalHistoryResponse historial = cliente.getRentalHistory(
                    GetRentalHistoryRequest.newBuilder().setClienteId(CLIENTE_ID).build());
            System.out.println("Reservas en el historial: " + historial.getReservasCount());
            historial.getReservasList().forEach(RentalGrpcClient::mostrarLinea);

            // 8. Consultar una reserva que no existe.
            System.out.println("\n=== 8. RESERVA INEXISTENTE (error esperado) ===");
            try {
                cliente.getReservation(GetReservationRequest.newBuilder().setId(999999L).build());
            } catch (StatusRuntimeException e) {
                System.out.println("Error gRPC: " + e.getStatus());
            }

        } catch (StatusRuntimeException e) {

            System.out.println("Error gRPC: " + e.getStatus());

        } finally {
            canal.shutdownNow();
        }
    }

    private static String fecha(int diasDesdeHoy) {
        return LocalDateTime.now().plusDays(diasDesdeHoy)
                .withHour(10).withMinute(0).withSecond(0).withNano(0).format(FORMATO);
    }

    private static void mostrar(Reservation r) {
        System.out.println("ID: " + r.getId());
        System.out.println("Cliente: " + r.getClienteId());
        System.out.println("Vehiculo: " + r.getVehiculoId());
        System.out.println("Inicio: " + r.getFechaInicio());
        System.out.println("Fin: " + r.getFechaFin());
        System.out.println("Precio diario: " + r.getPrecioDiario());
        System.out.println("Dias: " + r.getCantidadDias());
        System.out.println("Importe total: " + r.getImporteTotal());
        System.out.println("Estado: " + r.getEstado());
        System.out.println("Alta: " + r.getFechaAlta());
        System.out.println("Cancelacion: "
                + (r.getFechaCancelacion().isEmpty() ? "-" : r.getFechaCancelacion()));
    }

    private static void mostrarLinea(Reservation r) {
        System.out.println("  #" + r.getId() + " vehiculo " + r.getVehiculoId()
                + " | " + r.getFechaInicio() + " -> " + r.getFechaFin()
                + " | " + r.getEstado() + " | $" + r.getImporteTotal());
    }
}