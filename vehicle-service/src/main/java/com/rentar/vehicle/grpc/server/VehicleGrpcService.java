package com.rentar.vehicle.grpc.server;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import com.rentar.vehicle.dto.VehicleCreateRequest;
import com.rentar.vehicle.dto.VehicleUpdateRequest;
import com.rentar.vehicle.dto.SolicitudVehiculoExternoRequest;
import com.rentar.vehicle.entity.Vehiculo;
import com.rentar.vehicle.entity.SolicitudVehiculoExterno;
import com.rentar.vehicle.entity.enums.EstadoVehiculo;
import com.rentar.vehicle.entity.enums.TipoVehiculo;
import com.rentar.vehicle.exception.PatenteDuplicadaException;
import com.rentar.vehicle.exception.VehiculoInactivoException;
import com.rentar.vehicle.exception.VehiculoNoEncontradoException;
import com.rentar.vehicle.exception.SolicitudVehiculoExternoNoEncontradaException;

import com.rentar.vehicle.grpc.CreateVehicleRequest;
import com.rentar.vehicle.grpc.DeactivateVehicleRequest;
import com.rentar.vehicle.grpc.GetVehicleRequest;
import com.rentar.vehicle.grpc.GetVehicleByPatenteRequest;
import com.rentar.vehicle.grpc.ListAvailableVehiclesRequest;
import com.rentar.vehicle.grpc.ListVehiclesRequest;
import com.rentar.vehicle.grpc.ListVehiclesResponse;
import com.rentar.vehicle.grpc.UpdateVehicleRequest;
import com.rentar.vehicle.grpc.UpdateVehicleStatusRequest;
import com.rentar.vehicle.grpc.Vehicle;
import com.rentar.vehicle.grpc.VehicleFilter;
import com.rentar.vehicle.grpc.VehicleServiceGrpc;
import com.rentar.vehicle.grpc.VehicleStatus;
import com.rentar.vehicle.grpc.VehicleType;

import com.rentar.vehicle.grpc.CreateExternalVehicleApplicationRequest;
import com.rentar.vehicle.grpc.GetExternalVehicleApplicationRequest;
import com.rentar.vehicle.grpc.ExternalVehicleApplication;

import com.rentar.vehicle.service.VehicleService;
import com.rentar.vehicle.service.SolicitudVehiculoExternoService;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

@Service
public class VehicleGrpcService extends VehicleServiceGrpc.VehicleServiceImplBase {

    private final VehicleService vehicleService;
    private final SolicitudVehiculoExternoService solicitudService;

    public VehicleGrpcService(
            VehicleService vehicleService,
            SolicitudVehiculoExternoService solicitudService) {

        this.vehicleService = vehicleService;
        this.solicitudService = solicitudService;
    }

    // -------------------------------------------------------
    // HITO 2 - OPERACIONES EXISTENTES
    // -------------------------------------------------------

    @Override
    public void listVehicles(
            ListVehiclesRequest request,
            StreamObserver<ListVehiclesResponse> responseObserver) {

        respond(responseObserver, () -> {
            List<Vehiculo> vehicles = vehicleService.list();

            if (request.getIdsCount() > 0) {
                Set<Long> ids = new HashSet<>(request.getIdsList());
                vehicles = vehicles.stream()
                        .filter(vehicle -> ids.contains(vehicle.getId()))
                        .toList();
            }

            if (request.hasSoloActivos() && request.getSoloActivos()) {
                vehicles = vehicles.stream()
                        .filter(Vehiculo::isActivo)
                        .toList();
            }

            if (request.hasFiltro()) {
                vehicles = applyFilter(vehicles, request.getFiltro());
            }

            return toListResponse(vehicles);
        });
    }

    @Override
    public void getVehicle(
            GetVehicleRequest request,
            StreamObserver<Vehicle> responseObserver) {

        respond(responseObserver,
                () -> toGrpcVehicle(
                        vehicleService.getById(request.getId())));
    }

    @Override
    public void getVehicleByPatente(
            GetVehicleByPatenteRequest request,
            StreamObserver<Vehicle> responseObserver) {

        respond(responseObserver, () -> {
            if (request.getPatente().isBlank()) {
                throw new IllegalArgumentException(
                        "La patente es obligatoria");
            }

            return toGrpcVehicle(
                    vehicleService.getByPatente(request.getPatente()));
        });
    }

    @Override
    public void createVehicle(
            CreateVehicleRequest request,
            StreamObserver<Vehicle> responseObserver) {

        respond(responseObserver, () -> {
            VehicleCreateRequest createRequest = toCreateRequest(request);
            return toGrpcVehicle(vehicleService.create(createRequest));
        });
    }

    @Override
    public void updateVehicle(
            UpdateVehicleRequest request,
            StreamObserver<Vehicle> responseObserver) {

        respond(responseObserver, () -> {
            VehicleUpdateRequest updateRequest = toUpdateRequest(request);
            return toGrpcVehicle(
                    vehicleService.update(request.getId(), updateRequest));
        });
    }

    @Override
    public void deactivateVehicle(
            DeactivateVehicleRequest request,
            StreamObserver<Vehicle> responseObserver) {

        respond(responseObserver,
                () -> toGrpcVehicle(
                        vehicleService.deactivate(request.getId())));
    }

    @Override
    public void updateVehicleStatus(
            UpdateVehicleStatusRequest request,
            StreamObserver<Vehicle> responseObserver) {

        respond(responseObserver, () -> {
            EstadoVehiculo estado = toDomainStatus(request.getEstado());

            return toGrpcVehicle(
                    vehicleService.updateStatus(request.getId(), estado));
        });
    }

    @Override
    public void listAvailableVehicles(
            ListAvailableVehiclesRequest request,
            StreamObserver<ListVehiclesResponse> responseObserver) {

        respond(responseObserver, () -> {
            List<Vehiculo> vehicles =
                    vehicleService.listAvailable(request.getExcluirIdsList());

            if (request.hasFiltro()) {
                vehicles = applyFilter(vehicles, request.getFiltro());
            }

            return toListResponse(vehicles);
        });
    }

    // -------------------------------------------------------
    // HITO 3 - INCORPORACION DE VEHICULOS EXTERNOS
    // -------------------------------------------------------

    @Override
    public void createExternalVehicleApplication(
            CreateExternalVehicleApplicationRequest request,
            StreamObserver<ExternalVehicleApplication> responseObserver) {

        respond(responseObserver, () -> {

            SolicitudVehiculoExternoRequest solicitudRequest =
                    toSolicitudExternaRequest(request);

            SolicitudVehiculoExterno solicitud =
                    solicitudService.create(solicitudRequest);

            return toGrpcSolicitudExterna(solicitud);
        });
    }

    @Override
    public void getExternalVehicleApplication(
            GetExternalVehicleApplicationRequest request,
            StreamObserver<ExternalVehicleApplication> responseObserver) {

        respond(responseObserver, () -> {

            if (request.getId() <= 0) {
                throw new IllegalArgumentException(
                        "El id de la solicitud debe ser mayor a cero");
            }

            SolicitudVehiculoExterno solicitud =
                    solicitudService.getById(request.getId());

            return toGrpcSolicitudExterna(solicitud);
        });
    }

    private SolicitudVehiculoExternoRequest toSolicitudExternaRequest(
            CreateExternalVehicleApplicationRequest request) {

        if (request.getClienteId() <= 0) {
            throw new IllegalArgumentException(
                    "El clienteId debe ser mayor a cero");
        }

        if (request.getDocumento().isBlank()) {
            throw new IllegalArgumentException(
                    "El documento es obligatorio");
        }

        validateVehicleData(
                request.getPatente(),
                request.getMarca(),
                request.getModelo(),
                request.getAnio(),
                request.getPrecioDiario());

        SolicitudVehiculoExternoRequest result =
                new SolicitudVehiculoExternoRequest();

        result.setClienteId(request.getClienteId());
        result.setDocumento(request.getDocumento());
        result.setPatente(request.getPatente());
        result.setMarca(request.getMarca());
        result.setModelo(request.getModelo());
        result.setAnio(request.getAnio());
        result.setColor(request.getColor());
        result.setTipo(toDomainType(request.getTipo()));
        result.setPrecioDiario(
                BigDecimal.valueOf(request.getPrecioDiario()));

        return result;
    }

    private ExternalVehicleApplication toGrpcSolicitudExterna(
            SolicitudVehiculoExterno solicitud) {

        ExternalVehicleApplication.Builder builder =
                ExternalVehicleApplication.newBuilder()
                        .setId(solicitud.getId())
                        .setClienteId(solicitud.getClienteId())
                        .setPatente(solicitud.getPatente())
                        .setMarca(solicitud.getMarca())
                        .setModelo(solicitud.getModelo())
                        .setEstado(solicitud.getEstado().name())
                        .setResultadoPatente(
                                solicitud.getResultadoPatente() == null
                                        ? ""
                                        : solicitud.getResultadoPatente().name())
                        .setResultadoTitularidad(
                                solicitud.getResultadoTitularidad() == null
                                        ? ""
                                        : solicitud.getResultadoTitularidad().name());

        if (solicitud.getVehiculoId() != null) {
            builder.setVehiculoId(solicitud.getVehiculoId());
        }

        return builder.build();
    }

    // -------------------------------------------------------
    // CONVERSIONES ORIGINALES DEL HITO 2
    // -------------------------------------------------------

    private VehicleCreateRequest toCreateRequest(CreateVehicleRequest request) {

        validateVehicleData(
                request.getPatente(),
                request.getMarca(),
                request.getModelo(),
                request.getAnio(),
                request.getPrecioDiario());

        VehicleCreateRequest result = new VehicleCreateRequest();

        result.setPatente(request.getPatente());
        result.setMarca(request.getMarca());
        result.setModelo(request.getModelo());
        result.setAnio(request.getAnio());
        result.setColor(request.getColor());
        result.setTipo(toDomainType(request.getTipo()));
        result.setPrecioDiario(
                BigDecimal.valueOf(request.getPrecioDiario()));

        return result;
    }

    private VehicleUpdateRequest toUpdateRequest(UpdateVehicleRequest request) {

        validateVehicleData(
                "patente-no-modificable",
                request.getMarca(),
                request.getModelo(),
                request.getAnio(),
                request.getPrecioDiario());

        VehicleUpdateRequest result = new VehicleUpdateRequest();

        result.setMarca(request.getMarca());
        result.setModelo(request.getModelo());
        result.setAnio(request.getAnio());
        result.setColor(request.getColor());
        result.setTipo(toDomainType(request.getTipo()));
        result.setPrecioDiario(
                BigDecimal.valueOf(request.getPrecioDiario()));

        return result;
    }

    private void validateVehicleData(
            String patente,
            String marca,
            String modelo,
            int anio,
            double precioDiario) {

        if (patente == null || patente.isBlank()) {
            throw new IllegalArgumentException(
                    "La patente es obligatoria");
        }

        if (marca == null || marca.isBlank()) {
            throw new IllegalArgumentException(
                    "La marca es obligatoria");
        }

        if (modelo == null || modelo.isBlank()) {
            throw new IllegalArgumentException(
                    "El modelo es obligatorio");
        }

        if (anio < 1900) {
            throw new IllegalArgumentException(
                    "El anio debe ser mayor o igual a 1900");
        }

        if (!Double.isFinite(precioDiario) || precioDiario <= 0) {
            throw new IllegalArgumentException(
                    "El precio diario debe ser un numero mayor a cero");
        }
    }

    private List<Vehiculo> applyFilter(
            List<Vehiculo> vehicles,
            VehicleFilter filter) {

        return vehicles.stream()
                .filter(vehicle -> !filter.hasTipo()
                        || toGrpcType(vehicle.getTipo()) == filter.getTipo())
                .filter(vehicle -> !filter.hasMarca()
                        || vehicle.getMarca().equalsIgnoreCase(
                                filter.getMarca()))
                .filter(vehicle -> !filter.hasModelo()
                        || vehicle.getModelo().equalsIgnoreCase(
                                filter.getModelo()))
                .filter(vehicle -> !filter.hasPrecioMin()
                        || vehicle.getPrecioDiario().compareTo(
                                BigDecimal.valueOf(
                                        filter.getPrecioMin())) >= 0)
                .filter(vehicle -> !filter.hasPrecioMax()
                        || vehicle.getPrecioDiario().compareTo(
                                BigDecimal.valueOf(
                                        filter.getPrecioMax())) <= 0)
                .toList();
    }

    private Vehicle toGrpcVehicle(Vehiculo vehicle) {

        return Vehicle.newBuilder()
                .setId(vehicle.getId())
                .setPatente(vehicle.getPatente())
                .setMarca(vehicle.getMarca())
                .setModelo(vehicle.getModelo())
                .setAnio(vehicle.getAnio())
                .setColor(
                        vehicle.getColor() == null
                                ? ""
                                : vehicle.getColor())
                .setTipo(toGrpcType(vehicle.getTipo()))
                .setPrecioDiario(
                        vehicle.getPrecioDiario().doubleValue())
                .setEstado(toGrpcStatus(vehicle.getEstado()))
                .setActivo(vehicle.isActivo())
                .build();
    }

    private ListVehiclesResponse toListResponse(List<Vehiculo> vehicles) {

        return ListVehiclesResponse.newBuilder()
                .addAllVehiculos(
                        vehicles.stream()
                                .map(this::toGrpcVehicle)
                                .toList())
                .build();
    }

    private VehicleType toGrpcType(TipoVehiculo type) {
        return switch (type) {
            case SEDAN -> VehicleType.VEHICLE_TYPE_SEDAN;
            case SUV -> VehicleType.VEHICLE_TYPE_SUV;
            case PICKUP -> VehicleType.VEHICLE_TYPE_PICKUP;
            case COUPE -> VehicleType.VEHICLE_TYPE_COUPE;
            case HATCHBACK -> VehicleType.VEHICLE_TYPE_HATCHBACK;
        };
    }

    private VehicleStatus toGrpcStatus(EstadoVehiculo status) {
        return switch (status) {
            case DISPONIBLE -> VehicleStatus.VEHICLE_STATUS_DISPONIBLE;
            case RESERVADO -> VehicleStatus.VEHICLE_STATUS_RESERVADO;
            case EN_ALQUILER -> VehicleStatus.VEHICLE_STATUS_EN_ALQUILER;
        };
    }

    private TipoVehiculo toDomainType(VehicleType type) {
        return switch (type) {
            case VEHICLE_TYPE_SEDAN -> TipoVehiculo.SEDAN;
            case VEHICLE_TYPE_SUV -> TipoVehiculo.SUV;
            case VEHICLE_TYPE_PICKUP -> TipoVehiculo.PICKUP;
            case VEHICLE_TYPE_COUPE -> TipoVehiculo.COUPE;
            case VEHICLE_TYPE_HATCHBACK -> TipoVehiculo.HATCHBACK;
            case VEHICLE_TYPE_UNSPECIFIED, UNRECOGNIZED ->
                    throw new IllegalArgumentException(
                            "El tipo de vehiculo es obligatorio");
        };
    }

    private EstadoVehiculo toDomainStatus(VehicleStatus status) {
        return switch (status) {
            case VEHICLE_STATUS_DISPONIBLE -> EstadoVehiculo.DISPONIBLE;
            case VEHICLE_STATUS_RESERVADO -> EstadoVehiculo.RESERVADO;
            case VEHICLE_STATUS_EN_ALQUILER -> EstadoVehiculo.EN_ALQUILER;
            case VEHICLE_STATUS_UNSPECIFIED, UNRECOGNIZED ->
                    throw new IllegalArgumentException(
                            "El estado es obligatorio");
        };
    }

    private <T> void respond(
            StreamObserver<T> responseObserver,
            Supplier<T> operation) {

        try {
            responseObserver.onNext(operation.get());
            responseObserver.onCompleted();

        } catch (RuntimeException exception) {
            responseObserver.onError(toGrpcError(exception));
        }
    }

    private RuntimeException toGrpcError(RuntimeException exception) {

        Status status;

        if (exception instanceof PatenteDuplicadaException) {
            status = Status.ALREADY_EXISTS;

        } else if (exception instanceof VehiculoNoEncontradoException
                || exception instanceof SolicitudVehiculoExternoNoEncontradaException) {

            status = Status.NOT_FOUND;

        } else if (exception instanceof VehiculoInactivoException) {
            status = Status.FAILED_PRECONDITION;

        } else if (exception instanceof IllegalArgumentException) {
            status = Status.INVALID_ARGUMENT;

        } else {
            status = Status.INTERNAL;
        }

        return status.withDescription(
                exception.getMessage()).asRuntimeException();
    }
}