package com.rentar.vehicle.grpc.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rentar.vehicle.entity.Vehiculo;
import com.rentar.vehicle.entity.enums.EstadoVehiculo;
import com.rentar.vehicle.entity.enums.TipoVehiculo;
import com.rentar.vehicle.exception.PatenteDuplicadaException;
import com.rentar.vehicle.grpc.CreateVehicleRequest;
import com.rentar.vehicle.grpc.GetVehicleRequest;
import com.rentar.vehicle.grpc.ListAvailableVehiclesRequest;
import com.rentar.vehicle.grpc.ListVehiclesResponse;
import com.rentar.vehicle.grpc.Vehicle;
import com.rentar.vehicle.grpc.VehicleType;
import com.rentar.vehicle.service.VehicleService;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

@ExtendWith(MockitoExtension.class)
class VehicleGrpcServiceTest {

    @Mock
    private VehicleService vehicleService;

    @Test
    void getVehicleMapsTheDomainVehicleToTheGrpcResponse() {
        when(vehicleService.getById(1L)).thenReturn(vehicle(1L));
        VehicleGrpcService grpcService = new VehicleGrpcService(vehicleService);
        TestObserver<Vehicle> observer = new TestObserver<>();

        grpcService.getVehicle(GetVehicleRequest.newBuilder().setId(1L).build(), observer);

        assertEquals(1, observer.values.size());
        Vehicle response = observer.values.getFirst();
        assertEquals(1L, response.getId());
        assertEquals("ABC123", response.getPatente());
        assertEquals(VehicleType.SEDAN, response.getTipo());
        assertEquals(EstadoVehiculo.DISPONIBLE.name(), response.getEstado().name());
        assertNotNull(observer.completed);
    }

    @Test
    void createVehicleMapsDuplicatePatentToAlreadyExists() {
        when(vehicleService.create(any())).thenThrow(new PatenteDuplicadaException("Patente repetida"));
        VehicleGrpcService grpcService = new VehicleGrpcService(vehicleService);
        TestObserver<Vehicle> observer = new TestObserver<>();
        CreateVehicleRequest request = CreateVehicleRequest.newBuilder()
                .setPatente("ABC123")
                .setMarca("Toyota")
                .setModelo("Corolla")
                .setAnio(2021)
                .setTipo(VehicleType.SEDAN)
                .setPrecioDiario(45000)
                .build();

        grpcService.createVehicle(request, observer);

        StatusRuntimeException error = (StatusRuntimeException) observer.error;
        assertEquals(Status.Code.ALREADY_EXISTS, error.getStatus().getCode());
    }

    @Test
    void listAvailableVehiclesForwardsTheExcludedVehicleIds() {
        when(vehicleService.listAvailable(List.of(2L, 3L))).thenReturn(List.of(vehicle(1L)));
        VehicleGrpcService grpcService = new VehicleGrpcService(vehicleService);
        TestObserver<ListVehiclesResponse> observer = new TestObserver<>();
        ListAvailableVehiclesRequest request = ListAvailableVehiclesRequest.newBuilder()
                .addExcluirIds(2L)
                .addExcluirIds(3L)
                .build();

        grpcService.listAvailableVehicles(request, observer);

        verify(vehicleService).listAvailable(eq(List.of(2L, 3L)));
        assertEquals(1, observer.values.getFirst().getVehiculosCount());
        assertEquals(1L, observer.values.getFirst().getVehiculos(0).getId());
    }

    private Vehiculo vehicle(Long id) {
        Vehiculo vehicle = new Vehiculo();
        vehicle.setId(id);
        vehicle.setPatente("ABC123");
        vehicle.setMarca("Toyota");
        vehicle.setModelo("Corolla");
        vehicle.setAnio(2021);
        vehicle.setColor("Blanco");
        vehicle.setTipo(TipoVehiculo.SEDAN);
        vehicle.setPrecioDiario(BigDecimal.valueOf(45000));
        vehicle.setEstado(EstadoVehiculo.DISPONIBLE);
        vehicle.setActivo(true);
        return vehicle;
    }

    private static class TestObserver<T> implements StreamObserver<T> {

        private final List<T> values = new ArrayList<>();
        private Throwable error;
        private Boolean completed;

        @Override
        public void onNext(T value) {
            values.add(value);
        }

        @Override
        public void onError(Throwable throwable) {
            error = throwable;
        }

        @Override
        public void onCompleted() {
            completed = true;
        }
    }
}
