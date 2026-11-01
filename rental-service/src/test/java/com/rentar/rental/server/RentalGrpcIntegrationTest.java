package com.rentar.rental.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.rentar.rental.entity.Reserva;
import com.rentar.rental.entity.enums.EstadoReserva;
import com.rentar.rental.grpc.CancelReservationRequest;
import com.rentar.rental.grpc.CreateReservationRequest;
import com.rentar.rental.grpc.GetBusyVehicleIdsRequest;
import com.rentar.rental.grpc.GetRentalHistoryRequest;
import com.rentar.rental.grpc.GetReservationRequest;
import com.rentar.rental.grpc.ListReservationsRequest;
import com.rentar.rental.grpc.RentalServiceGrpc;
import com.rentar.rental.grpc.Reservation;
import com.rentar.rental.grpc.ReservationStatus;
import com.rentar.rental.repository.ReservaRepository;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/**
 * Prueba de punta a punta del Rental Service: cliente gRPC real contra el servidor, la capa
 * de servicio y la base MySQL local (hace falta MYSQL_PASSWORD y el servidor MySQL en marcha).
 * Usa ids de cliente y vehiculo propios y borra lo que crea al terminar cada prueba.
 */
@SpringBootTest(properties = { "rental.grpc.port=0", "rental.eventos.habilitado=false" })
class RentalGrpcIntegrationTest {

    private static final long CLIENTE = 910001L;
    private static final long OTRO_CLIENTE = 910002L;

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Autowired
    private RentalGrpcService rentalGrpcService;

    @Autowired
    private ReservaRepository reservaRepository;

    private static Server servidor;
    private static ManagedChannel canal;
    private static RentalServiceGrpc.RentalServiceBlockingStub cliente;

    private final List<Long> idsCreados = new ArrayList<>();

    @BeforeAll
    static void conectar(@Autowired RentalGrpcService servicio) throws Exception {
        servidor = ServerBuilder.forPort(0).addService(servicio).build().start();
        canal = ManagedChannelBuilder.forAddress("localhost", servidor.getPort())
                .usePlaintext().build();
        cliente = RentalServiceGrpc.newBlockingStub(canal).withDeadlineAfter(30, TimeUnit.SECONDS);
    }

    @AfterAll
    static void desconectar() {
        canal.shutdownNow();
        servidor.shutdownNow();
    }

    @AfterEach
    void limpiar() {
        reservaRepository.deleteAllById(idsCreados);
        idsCreados.clear();
    }

    // ==========================================
    // AUXILIARES
    // ==========================================

    /** Fecha a las 10:00, a la cantidad de dias indicada desde hoy. */
    private static String fecha(int dias) {
        return LocalDateTime.now().plusDays(dias).withHour(10).withMinute(0).withSecond(0)
                .withNano(0).format(FORMATO);
    }

    private Reservation crear(long clienteId, long vehiculoId, int desdeDias, int hastaDias) {
        Reservation r = cliente.createReservation(CreateReservationRequest.newBuilder()
                .setClienteId(clienteId)
                .setVehiculoId(vehiculoId)
                .setFechaInicio(fecha(desdeDias))
                .setFechaFin(fecha(hastaDias))
                .setPrecioDiario(100.5)
                .build());
        idsCreados.add(r.getId());
        return r;
    }

    private static Status.Code codigoDe(Runnable llamada) {
        StatusRuntimeException e = assertThrows(StatusRuntimeException.class, llamada::run);
        return e.getStatus().getCode();
    }

    // ==========================================
    // ALTA
    // ==========================================

    @Test
    void crearReservaCalculaDiasEImporte() {

        Reservation r = crear(CLIENTE, 920001L, 40, 43);

        assertTrue(r.getId() > 0);
        assertEquals(ReservationStatus.RESERVATION_STATUS_CONFIRMADA, r.getEstado());
        assertEquals(3, r.getCantidadDias());
        assertEquals(100.5, r.getPrecioDiario());
        assertEquals(301.5, r.getImporteTotal());
        assertEquals(fecha(40), r.getFechaInicio());
        assertEquals("", r.getFechaCancelacion());
    }

    @Test
    void crearReservaSolapadaDevuelveFailedPrecondition() {

        crear(CLIENTE, 920002L, 40, 45);

        // Se solapa con el tramo final de la reserva anterior.
        assertEquals(Status.Code.FAILED_PRECONDITION,
                codigoDe(() -> crear(OTRO_CLIENTE, 920002L, 44, 48)));

        // Otro vehiculo en el mismo periodo no tiene problema.
        crear(OTRO_CLIENTE, 920003L, 44, 48);
    }

    @Test
    void reservasContiguasNoSeSolapan() {

        crear(CLIENTE, 920004L, 40, 42);

        // Empieza justo cuando termina la anterior.
        crear(OTRO_CLIENTE, 920004L, 42, 44);
    }

    @Test
    void crearConDatosInvalidosDevuelveInvalidArgument() {

        // Fin anterior al inicio.
        assertEquals(Status.Code.INVALID_ARGUMENT, codigoDe(() -> crear(CLIENTE, 920005L, 10, 8)));

        // Inicio en el pasado.
        assertEquals(Status.Code.INVALID_ARGUMENT, codigoDe(() -> crear(CLIENTE, 920005L, -2, 3)));

        // Formato de fecha incorrecto.
        assertEquals(Status.Code.INVALID_ARGUMENT, codigoDe(() -> cliente.createReservation(
                CreateReservationRequest.newBuilder()
                        .setClienteId(CLIENTE).setVehiculoId(920005L)
                        .setFechaInicio("10/11/2026").setFechaFin(fecha(12))
                        .setPrecioDiario(100.5).build())));

        // Cliente sin informar.
        assertEquals(Status.Code.INVALID_ARGUMENT, codigoDe(() -> cliente.createReservation(
                CreateReservationRequest.newBuilder()
                        .setVehiculoId(920005L)
                        .setFechaInicio(fecha(10)).setFechaFin(fecha(12))
                        .setPrecioDiario(100.5).build())));
    }

    // ==========================================
    // VALIDACION ESTRICTA DE FECHAS
    // ==========================================

    @Test
    void fechasSinSegundosDevuelvenInvalidArgument() {

        // Se omiten los segundos de una fecha futura valida.
        String sinSegundos = fecha(20).substring(0, 16);

        // La creacion de una reserva debe rechazar la fecha incompleta.
        assertEquals(Status.Code.INVALID_ARGUMENT, codigoDe(() -> cliente.createReservation(
                CreateReservationRequest.newBuilder()
                        .setClienteId(CLIENTE)
                        .setVehiculoId(920015L)
                        .setFechaInicio(sinSegundos)
                        .setFechaFin(fecha(22))
                        .setPrecioDiario(100.5)
                        .build())));

        // La consulta de disponibilidad debe aplicar el mismo formato.
        assertEquals(Status.Code.INVALID_ARGUMENT, codigoDe(() -> cliente.getBusyVehicleIds(
                GetBusyVehicleIdsRequest.newBuilder()
                        .setFechaInicio(sinSegundos)
                        .setFechaFin(fecha(22))
                        .build())));

        // Los filtros de listado tambien deben rechazar fechas sin segundos.
        assertEquals(Status.Code.INVALID_ARGUMENT, codigoDe(() -> cliente.listReservations(
                ListReservationsRequest.newBuilder()
                        .setClienteId(CLIENTE)
                        .setDesde(sinSegundos)
                        .build())));
    }

    @Test
    void fechasInexistentesOConZonaUtcDevuelvenInvalidArgument() {

        // La validacion estricta rechaza dias inexistentes.
        assertEquals(Status.Code.INVALID_ARGUMENT, codigoDe(() -> cliente.createReservation(
                CreateReservationRequest.newBuilder()
                        .setClienteId(CLIENTE)
                        .setVehiculoId(920016L)
                        .setFechaInicio("2026-02-30T10:00:00")
                        .setFechaFin(fecha(22))
                        .setPrecioDiario(100.5)
                        .build())));

        // El contrato usa fecha y hora local, sin sufijo UTC.
        assertEquals(Status.Code.INVALID_ARGUMENT, codigoDe(() -> cliente.createReservation(
                CreateReservationRequest.newBuilder()
                        .setClienteId(CLIENTE)
                        .setVehiculoId(920016L)
                        .setFechaInicio(fecha(20) + "Z")
                        .setFechaFin(fecha(22))
                        .setPrecioDiario(100.5)
                        .build())));
    }

    // ==========================================
    // CANCELACION Y CONSULTA POR ID
    // ==========================================

    @Test
    void cancelarLiberaElPeriodoYNoSePuedeCancelarDosVeces() {

        Reservation r = crear(CLIENTE, 920006L, 40, 42);

        Reservation cancelada = cliente.cancelReservation(
                CancelReservationRequest.newBuilder().setId(r.getId()).build());

        assertEquals(ReservationStatus.RESERVATION_STATUS_CANCELADA, cancelada.getEstado());
        assertFalse(cancelada.getFechaCancelacion().isEmpty());

        // La reserva sigue existiendo, solo cambio de estado.
        Reservation consultada = cliente.getReservation(
                GetReservationRequest.newBuilder().setId(r.getId()).build());
        assertEquals(ReservationStatus.RESERVATION_STATUS_CANCELADA, consultada.getEstado());

        // El periodo quedo libre para otra reserva.
        crear(OTRO_CLIENTE, 920006L, 40, 42);

        assertEquals(Status.Code.FAILED_PRECONDITION, codigoDe(() -> cliente.cancelReservation(
                CancelReservationRequest.newBuilder().setId(r.getId()).build())));
    }

    @Test
    void reservaInexistenteDevuelveNotFound() {

        assertEquals(Status.Code.NOT_FOUND, codigoDe(() -> cliente.getReservation(
                GetReservationRequest.newBuilder().setId(999999999L).build())));
        assertEquals(Status.Code.NOT_FOUND, codigoDe(() -> cliente.cancelReservation(
                CancelReservationRequest.newBuilder().setId(999999999L).build())));
    }

    // ==========================================
    // VEHICULOS OCUPADOS
    // ==========================================

    @Test
    void vehiculosOcupadosSoloIncluyeLosQueSeSolapanConElPeriodo() {

        crear(CLIENTE, 920007L, 50, 53);

        List<Long> ocupadosEnPeriodo = cliente.getBusyVehicleIds(
                GetBusyVehicleIdsRequest.newBuilder()
                        .setFechaInicio(fecha(52)).setFechaFin(fecha(55)).build())
                .getVehiculoIdsList();
        assertTrue(ocupadosEnPeriodo.contains(920007L));

        List<Long> ocupadosDespues = cliente.getBusyVehicleIds(
                GetBusyVehicleIdsRequest.newBuilder()
                        .setFechaInicio(fecha(53)).setFechaFin(fecha(56)).build())
                .getVehiculoIdsList();
        assertFalse(ocupadosDespues.contains(920007L));
    }

    @Test
    void reservaCanceladaNoOcupaElVehiculo() {

        Reservation r = crear(CLIENTE, 920008L, 50, 53);
        cliente.cancelReservation(CancelReservationRequest.newBuilder().setId(r.getId()).build());

        List<Long> ocupados = cliente.getBusyVehicleIds(
                GetBusyVehicleIdsRequest.newBuilder()
                        .setFechaInicio(fecha(50)).setFechaFin(fecha(53)).build())
                .getVehiculoIdsList();

        assertFalse(ocupados.contains(920008L));
    }

    // ==========================================
    // LISTADO CON FILTROS
    // ==========================================

    @Test
    void listarAplicaLosFiltrosOpcionales() {

        Reservation a = crear(CLIENTE, 920009L, 60, 62);
        Reservation b = crear(CLIENTE, 920010L, 70, 72);
        crear(OTRO_CLIENTE, 920011L, 60, 62);

        // Solo por cliente.
        assertEquals(2, cliente.listReservations(ListReservationsRequest.newBuilder()
                .setClienteId(CLIENTE).build()).getReservasCount());

        // Cliente y vehiculo.
        var porVehiculo = cliente.listReservations(ListReservationsRequest.newBuilder()
                .setClienteId(CLIENTE).addVehiculoIds(920010L).build());
        assertEquals(1, porVehiculo.getReservasCount());
        assertEquals(b.getId(), porVehiculo.getReservas(0).getId());

        // Cliente y rango de fechas sobre la fecha de inicio.
        var porFechas = cliente.listReservations(ListReservationsRequest.newBuilder()
                .setClienteId(CLIENTE).setDesde(fecha(59)).setHasta(fecha(65)).build());
        assertEquals(1, porFechas.getReservasCount());
        assertEquals(a.getId(), porFechas.getReservas(0).getId());

        // Estado: ninguna esta cancelada.
        assertEquals(0, cliente.listReservations(ListReservationsRequest.newBuilder()
                .setClienteId(CLIENTE).setEstado(ReservationStatus.RESERVATION_STATUS_CANCELADA).build())
                .getReservasCount());

        // Estado sin especificar no filtra.
        assertEquals(2, cliente.listReservations(ListReservationsRequest.newBuilder()
                .setClienteId(CLIENTE)
                .setEstado(ReservationStatus.RESERVATION_STATUS_UNSPECIFIED).build())
                .getReservasCount());
    }

    // ==========================================
    // HISTORIAL
    // ==========================================

    @Test
    void historialIncluyeCanceladasYFinalizadasPeroNoLasConfirmadasFuturas() {

        long cliente2 = 910003L;

        Reservation futura = crear(cliente2, 920012L, 80, 82);
        Reservation aCancelar = crear(cliente2, 920013L, 80, 82);
        cliente.cancelReservation(CancelReservationRequest.newBuilder().setId(aCancelar.getId()).build());

        // Reserva CONFIRMADA cuyo periodo ya termino: debe figurar como FINALIZADA.
        Reserva vencida = new Reserva();
        vencida.setClienteId(cliente2);
        vencida.setVehiculoId(920014L);
        vencida.setFechaInicio(LocalDateTime.now().minusDays(10).withNano(0));
        vencida.setFechaFin(LocalDateTime.now().minusDays(8).withNano(0));
        vencida.setPrecioDiario(new BigDecimal("100.00"));
        vencida.setCantidadDias(2);
        vencida.setImporteTotal(new BigDecimal("200.00"));
        vencida.setEstado(EstadoReserva.CONFIRMADA);
        vencida.setFechaAlta(LocalDateTime.now().minusDays(20).withNano(0));
        idsCreados.add(reservaRepository.save(vencida).getId());

        var historial = cliente.getRentalHistory(
                GetRentalHistoryRequest.newBuilder().setClienteId(cliente2).build());

        List<Long> ids = historial.getReservasList().stream().map(Reservation::getId).toList();

        assertEquals(2, historial.getReservasCount());
        assertTrue(ids.contains(aCancelar.getId()));
        assertTrue(ids.contains(vencida.getId()));
        assertFalse(ids.contains(futura.getId()));

        Reservation finalizada = historial.getReservasList().stream()
                .filter(x -> x.getId() == vencida.getId()).findFirst().orElseThrow();
        assertEquals(ReservationStatus.RESERVATION_STATUS_FINALIZADA, finalizada.getEstado());
    }
}