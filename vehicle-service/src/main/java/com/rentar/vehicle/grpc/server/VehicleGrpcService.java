package com.rentar.vehicle.grpc.server;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import com.rentar.vehicle.dto.VehicleCreateRequest;
import com.rentar.vehicle.dto.VehicleUpdateRequest;
import com.rentar.vehicle.entity.Vehiculo;
import com.rentar.vehicle.entity.enums.EstadoVehiculo;
import com.rentar.vehicle.entity.enums.TipoVehiculo;
import com.rentar.vehicle.exception.PatenteDuplicadaException;
import com.rentar.vehicle.exception.VehiculoInactivoException;
import com.rentar.vehicle.exception.VehiculoNoEncontradoException;
import com.rentar.vehicle.grpc.CreateVehicleRequest;
import com.rentar.vehicle.grpc.DeactivateVehicleRequest;
import com.rentar.vehicle.grpc.GetVehicleRequest;
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
import com.rentar.vehicle.service.VehicleService;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;

@Service
public class VehicleGrpcService extends VehicleServiceGrpc.VehicleServiceImplBase {

    private final VehicleService vehicleService;

    public VehicleGrpcService(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

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
                vehicles = vehicles.stream().filter(Vehiculo::isActivo).toList();
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
        respond(responseObserver, () -> toGrpcVehicle(vehicleService.getById(request.getId())));
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
            return toGrpcVehicle(vehicleService.update(request.getId(), updateRequest));
        });
    }

    @Override
    public void deactivateVehicle(
            DeactivateVehicleRequest request,
            StreamObserver<Vehicle> responseObserver) {
        respond(responseObserver, () -> toGrpcVehicle(vehicleService.deactivate(request.getId())));
    }

    @Override
    public void updateVehicleStatus(
            UpdateVehicleStatusRequest request,
            StreamObserver<Vehicle> responseObserver) {
        respond(responseObserver, () -> {
            EstadoVehiculo estado = toDomainStatus(request.getEstado());
            return toGrpcVehicle(vehicleService.updateStatus(request.getId(), estado));
        });
    }

    @Override
    public void listAvailableVehicles(
            ListAvailableVehiclesRequest request,
            StreamObserver<ListVehiclesResponse> responseObserver) {
        respond(responseObserver, () -> {
            List<Vehiculo> vehicles = vehicleService.listAvailable(request.getExcluirIdsList());
            if (request.hasFiltro()) {
                vehicles = applyFilter(vehicles, request.getFiltro());
            }
            return toListResponse(vehicles);
        });
    }

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
        result.setPrecioDiario(BigDecimal.valueOf(request.getPrecioDiario()));
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
        result.setPrecioDiario(BigDecimal.valueOf(request.getPrecioDiario()));
        return result;
    }

    private void validateVehicleData(
            String patente,
            String marca,
            String modelo,
            int anio,
            double precioDiario) {
        if (patente == null || patente.isBlank()) {
            throw new IllegalArgumentException("La patente es obligatoria");
        }
        if (marca == null || marca.isBlank()) {
            throw new IllegalArgumentException("La marca es obligatoria");
        }
        if (modelo == null || modelo.isBlank()) {
            throw new IllegalArgumentException("El modelo es obligatorio");
        }
        if (anio < 1900) {
            throw new IllegalArgumentException("El anio debe ser mayor o igual a 1900");
        }
        if (precioDiario <= 0) {
            throw new IllegalArgumentException("El precio diario debe ser mayor a cero");
        }
    }

    private List<Vehiculo> applyFilter(List<Vehiculo> vehicles, VehicleFilter filter) {
        return vehicles.stream()
                .filter(vehicle -> !filter.hasTipo()
                        || vehicle.getTipo().name().equals(filter.getTipo().name()))
                .filter(vehicle -> !filter.hasMarca()
                        || vehicle.getMarca().equalsIgnoreCase(filter.getMarca()))
                .filter(vehicle -> !filter.hasModelo()
                        || vehicle.getModelo().equalsIgnoreCase(filter.getModelo()))
                .filter(vehicle -> !filter.hasPrecioMin()
                        || vehicle.getPrecioDiario().compareTo(
                                BigDecimal.valueOf(filter.getPrecioMin())) >= 0)
                .filter(vehicle -> !filter.hasPrecioMax()
                        || vehicle.getPrecioDiario().compareTo(
                                BigDecimal.valueOf(filter.getPrecioMax())) <= 0)
                .toList();
    }

    private Vehicle toGrpcVehicle(Vehiculo vehicle) {
        return Vehicle.newBuilder()
                .setId(vehicle.getId())
                .setPatente(vehicle.getPatente())
                .setMarca(vehicle.getMarca())
                .setModelo(vehicle.getModelo())
                .setAnio(vehicle.getAnio())
                .setColor(vehicle.getColor() == null ? "" : vehicle.getColor())
                .setTipo(VehicleType.valueOf(vehicle.getTipo().name()))
                .setPrecioDiario(vehicle.getPrecioDiario().doubleValue())
                .setEstado(VehicleStatus.valueOf(vehicle.getEstado().name()))
                .setActivo(vehicle.isActivo())
                .build();
    }

    private ListVehiclesResponse toListResponse(List<Vehiculo> vehicles) {
        return ListVehiclesResponse.newBuilder()
                .addAllVehiculos(vehicles.stream().map(this::toGrpcVehicle).toList())
                .build();
    }

    private TipoVehiculo toDomainType(VehicleType type) {
        if (type == VehicleType.VEHICLE_TYPE_UNSPECIFIED) {
            throw new IllegalArgumentException("El tipo de vehiculo es obligatorio");
        }
        return TipoVehiculo.valueOf(type.name());
    }

    private EstadoVehiculo toDomainStatus(VehicleStatus status) {
        if (status == VehicleStatus.VEHICLE_STATUS_UNSPECIFIED) {
            throw new IllegalArgumentException("El estado es obligatorio");
        }
        return EstadoVehiculo.valueOf(status.name());
    }

    private <T> void respond(StreamObserver<T> responseObserver, Supplier<T> operation) {
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
        } else if (exception instanceof VehiculoNoEncontradoException) {
            status = Status.NOT_FOUND;
        } else if (exception instanceof VehiculoInactivoException) {
            status = Status.FAILED_PRECONDITION;
        } else if (exception instanceof IllegalArgumentException) {
            status = Status.INVALID_ARGUMENT;
        } else {
            status = Status.INTERNAL;
        }
        return status.withDescription(exception.getMessage()).asRuntimeException();
    }
}
