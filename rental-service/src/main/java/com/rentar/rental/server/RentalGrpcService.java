package com.rentar.rental.server;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.rentar.rental.entity.Reserva;
import com.rentar.rental.entity.enums.EstadoMantenimiento;
import com.rentar.rental.entity.enums.EstadoReserva;
import com.rentar.rental.exception.ReservaEstadoInvalidoException;
import com.rentar.rental.exception.ReservaInvalidaException;
import com.rentar.rental.exception.ReservaNoEncontradaException;
import com.rentar.rental.exception.VehiculoNoDisponibleException;
import com.rentar.rental.grpc.CancelReservationRequest;
import com.rentar.rental.grpc.CreateReservationRequest;
import com.rentar.rental.grpc.GetBusyVehicleIdsRequest;
import com.rentar.rental.grpc.GetBusyVehicleIdsResponse;
import com.rentar.rental.grpc.GetRentalHistoryRequest;
import com.rentar.rental.grpc.GetRentalHistoryResponse;
import com.rentar.rental.grpc.GetReservationRequest;
import com.rentar.rental.grpc.ListReservationsRequest;
import com.rentar.rental.grpc.ListReservationsResponse;
import com.rentar.rental.grpc.MaintenanceStatus;
import com.rentar.rental.grpc.RentalServiceGrpc;
import com.rentar.rental.grpc.Reservation;
import com.rentar.rental.grpc.ReservationStatus;
import com.rentar.rental.service.FiltroReserva;
import com.rentar.rental.service.IMantenimientoService;
import com.rentar.rental.service.IReservaService;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

@Service
public class RentalGrpcService extends RentalServiceGrpc.RentalServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(RentalGrpcService.class);

    // Fechas ISO-8601 sin zona horaria, siempre con segundos (ej: 2026-10-05T10:00:00).
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final IReservaService reservaService;
    private final IMantenimientoService mantenimientoService;

    public RentalGrpcService(IReservaService reservaService, IMantenimientoService mantenimientoService) {
        this.reservaService = reservaService;
        this.mantenimientoService = mantenimientoService;
    }

    // CREAR RESERVA

    @Override
    public void createReservation(
            CreateReservationRequest request,
            StreamObserver<Reservation> responseObserver) {

        responder(responseObserver, () -> {

            validarId(request.getClienteId(), "cliente_id");
            validarId(request.getVehiculoId(), "vehiculo_id");

            Reserva reserva = reservaService.crear(
                    request.getClienteId(),
                    request.getVehiculoId(),
                    parsearFecha(request.getFechaInicio(), "fecha_inicio"),
                    parsearFecha(request.getFechaFin(), "fecha_fin"),
                    BigDecimal.valueOf(request.getPrecioDiario()));

            return aProto(reserva);
        });
    }

    // CANCELAR RESERVA

    @Override
    public void cancelReservation(
            CancelReservationRequest request,
            StreamObserver<Reservation> responseObserver) {

        responder(responseObserver, () -> {

            validarId(request.getId(), "id");

            return aProto(reservaService.cancelar(request.getId()));
        });
    }

    // CONSULTAR UNA RESERVA POR ID

    @Override
    public void getReservation(
            GetReservationRequest request,
            StreamObserver<Reservation> responseObserver) {

        responder(responseObserver, () -> {

            validarId(request.getId(), "id");

            return aProto(reservaService.buscarPorId(request.getId()));
        });
    }

    // LISTAR RESERVAS CON FILTROS

    @Override
    public void listReservations(
            ListReservationsRequest request,
            StreamObserver<ListReservationsResponse> responseObserver) {

        responder(responseObserver, () -> {

            FiltroReserva filtro = new FiltroReserva(
                    request.hasClienteId() ? request.getClienteId() : null,
                    request.getVehiculoIdsList(),
                    request.hasEstado() ? aEstado(request.getEstado()) : null,
                    request.hasDesde() ? parsearFecha(request.getDesde(), "desde") : null,
                    request.hasHasta() ? parsearFecha(request.getHasta(), "hasta") : null);

            return aRespuesta(reservaService.listar(filtro));
        });
    }

    // HISTORIAL DE UN CLIENTE

    @Override
    public void getRentalHistory(
            GetRentalHistoryRequest request,
            StreamObserver<GetRentalHistoryResponse> responseObserver) {

        responder(responseObserver, () -> {

            validarId(request.getClienteId(), "cliente_id");

            ListReservationsResponse reservas =
                    aRespuesta(reservaService.historial(request.getClienteId()));

            return GetRentalHistoryResponse.newBuilder()
                    .addAllReservas(reservas.getReservasList())
                    .build();
        });
    }

    // VEHICULOS OCUPADOS EN UN PERIODO

    @Override
    public void getBusyVehicleIds(
            GetBusyVehicleIdsRequest request,
            StreamObserver<GetBusyVehicleIdsResponse> responseObserver) {

        responder(responseObserver, () -> {

            List<Long> ids = reservaService.idsVehiculosOcupados(
                    parsearFecha(request.getFechaInicio(), "fecha_inicio"),
                    parsearFecha(request.getFechaFin(), "fecha_fin"));

            return GetBusyVehicleIdsResponse.newBuilder()
                    .addAllVehiculoIds(ids)
                    .build();
        });
    }

    // RESPUESTA Y MAPEO DE ERRORES A STATUS GRPC

    private <T> void responder(StreamObserver<T> observer, Supplier<T> accion) {

        T resultado;

        try {
            resultado = accion.get();

        } catch (ReservaInvalidaException e) {
            observer.onError(Status.INVALID_ARGUMENT
                    .withDescription(e.getMessage()).asRuntimeException());
            return;

        } catch (ReservaNoEncontradaException e) {
            observer.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage()).asRuntimeException());
            return;

        } catch (VehiculoNoDisponibleException | ReservaEstadoInvalidoException e) {
            observer.onError(Status.FAILED_PRECONDITION
                    .withDescription(e.getMessage()).asRuntimeException());
            return;

        } catch (Exception e) {
            log.error("Error inesperado en el Rental Service", e);
            observer.onError(Status.INTERNAL
                    .withDescription("Error interno del servicio de reservas")
                    .asRuntimeException());
            return;
        }

        observer.onNext(resultado);
        observer.onCompleted();
    }

    // VALIDACION Y CONVERSIONES

    private void validarId(long id, String campo) {
        if (id <= 0) {
            throw new ReservaInvalidaException("El campo " + campo + " debe ser mayor a cero");
        }
    }

    private LocalDateTime parsearFecha(String valor, String campo) {

        if (valor == null || valor.isBlank()) {
            throw new ReservaInvalidaException("El campo " + campo + " es obligatorio");
        }

        try {
            return LocalDateTime.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new ReservaInvalidaException(
                    "El campo " + campo + " no tiene formato ISO-8601 (ej: 2026-10-05T10:00:00)");
        }
    }

    // Sin estado informado (UNSPECIFIED) no se filtra por estado.
    private EstadoReserva aEstado(ReservationStatus estado) {
        return switch (estado) {
            case RESERVATION_STATUS_CONFIRMADA -> EstadoReserva.CONFIRMADA;
            case RESERVATION_STATUS_CANCELADA -> EstadoReserva.CANCELADA;
            case RESERVATION_STATUS_FINALIZADA -> EstadoReserva.FINALIZADA;
            case RESERVATION_STATUS_UNSPECIFIED -> null;
            case UNRECOGNIZED -> throw new ReservaInvalidaException("El estado indicado no es valido");
        };
    }

    private ReservationStatus aProto(EstadoReserva estado) {
        return switch (estado) {
            case CONFIRMADA -> ReservationStatus.RESERVATION_STATUS_CONFIRMADA;
            case CANCELADA -> ReservationStatus.RESERVATION_STATUS_CANCELADA;
            case FINALIZADA -> ReservationStatus.RESERVATION_STATUS_FINALIZADA;
        };
    }

    private Reservation aProto(Reserva reserva) {
        return aProto(reserva, mantenimientoService.estados(List.of(reserva.getId())));
    }

    private MaintenanceStatus aMantenimientoProto(EstadoMantenimiento estado) {

        if (estado == null) {
            return MaintenanceStatus.MAINTENANCE_STATUS_UNSPECIFIED;
        }

        return switch (estado) {
            case PENDIENTE -> MaintenanceStatus.MAINTENANCE_STATUS_PENDIENTE;
            case REALIZADO -> MaintenanceStatus.MAINTENANCE_STATUS_REALIZADO;
            case NO_REALIZADO -> MaintenanceStatus.MAINTENANCE_STATUS_NO_REALIZADO;
        };
    }

    private Reservation aProto(Reserva reserva, Map<Long, EstadoMantenimiento> mantenimientos) {

        Reservation.Builder builder = Reservation.newBuilder()
                .setId(reserva.getId())
                .setClienteId(reserva.getClienteId())
                .setVehiculoId(reserva.getVehiculoId())
                .setFechaInicio(reserva.getFechaInicio().format(FORMATO_FECHA))
                .setFechaFin(reserva.getFechaFin().format(FORMATO_FECHA))
                .setPrecioDiario(reserva.getPrecioDiario().doubleValue())
                .setCantidadDias(reserva.getCantidadDias())
                .setImporteTotal(reserva.getImporteTotal().doubleValue())
                .setEstado(aProto(reserva.getEstado()))
                .setFechaAlta(reserva.getFechaAlta().format(FORMATO_FECHA))
                .setMantenimiento(aMantenimientoProto(mantenimientos.get(reserva.getId())));

        if (reserva.getFechaCancelacion() != null) {
            builder.setFechaCancelacion(reserva.getFechaCancelacion().format(FORMATO_FECHA));
        }

        return builder.build();
    }

    private ListReservationsResponse aRespuesta(List<Reserva> reservas) {

        ListReservationsResponse.Builder builder = ListReservationsResponse.newBuilder();

        // Una sola consulta con el mantenimiento de todas las reservas de la lista.
        Map<Long, EstadoMantenimiento> mantenimientos = mantenimientoService.estados(
                reservas.stream().map(Reserva::getId).toList());

        for (Reserva reserva : reservas) {
            builder.addReservas(aProto(reserva, mantenimientos));
        }

        return builder.build();
    }
}