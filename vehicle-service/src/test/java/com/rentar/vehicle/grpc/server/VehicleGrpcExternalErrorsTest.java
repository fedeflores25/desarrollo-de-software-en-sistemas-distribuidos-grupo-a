package com.rentar.vehicle.grpc.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rentar.vehicle.exception.SolicitudVehiculoExternoNoEncontradaException;
import com.rentar.vehicle.grpc.CreateExternalVehicleApplicationRequest;
import com.rentar.vehicle.grpc.ExternalVehicleApplication;
import com.rentar.vehicle.grpc.GetExternalVehicleApplicationRequest;
import com.rentar.vehicle.service.SolicitudVehiculoExternoService;
import com.rentar.vehicle.service.VehicleService;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;

@ExtendWith(MockitoExtension.class)
class VehicleGrpcExternalErrorsTest {

    @Mock
    private VehicleService vehicleService;

    @Mock
    private SolicitudVehiculoExternoService solicitudService;

    @Test
    void getExternalVehicleApplicationRejectsInvalidId() {

        VehicleGrpcService grpcService =
                new VehicleGrpcService(
                        vehicleService,
                        solicitudService);

        TestObserver<ExternalVehicleApplication> observer =
                new TestObserver<>();

        GetExternalVehicleApplicationRequest request =
                GetExternalVehicleApplicationRequest.newBuilder()
                        .setId(-1L)
                        .build();

        grpcService.getExternalVehicleApplication(
                request,
                observer);

        assertGrpcError(
                observer,
                Status.Code.INVALID_ARGUMENT);

        verifyNoInteractions(
                vehicleService,
                solicitudService);
    }

    @Test
    void getExternalVehicleApplicationReturnsNotFound() {

        when(solicitudService.getById(9999L))
                .thenThrow(
                        new SolicitudVehiculoExternoNoEncontradaException(
                                "No existe una solicitud externa con id 9999"));

        VehicleGrpcService grpcService =
                new VehicleGrpcService(
                        vehicleService,
                        solicitudService);

        TestObserver<ExternalVehicleApplication> observer =
                new TestObserver<>();

        GetExternalVehicleApplicationRequest request =
                GetExternalVehicleApplicationRequest.newBuilder()
                        .setId(9999L)
                        .build();

        grpcService.getExternalVehicleApplication(
                request,
                observer);

        assertGrpcError(
                observer,
                Status.Code.NOT_FOUND);

        verify(solicitudService).getById(9999L);
    }

    @Test
    void createExternalVehicleApplicationRejectsEmptyPatente() {

        VehicleGrpcService grpcService =
                new VehicleGrpcService(
                        vehicleService,
                        solicitudService);

        TestObserver<ExternalVehicleApplication> observer =
                new TestObserver<>();

        CreateExternalVehicleApplicationRequest request =
                CreateExternalVehicleApplicationRequest.newBuilder()
                        .setClienteId(2L)
                        .setDocumento("30222333")
                        .setPatente("")
                        .build();

        grpcService.createExternalVehicleApplication(
                request,
                observer);

        assertGrpcError(
                observer,
                Status.Code.INVALID_ARGUMENT);

        verifyNoInteractions(
                vehicleService,
                solicitudService);
    }

    private void assertGrpcError(
            TestObserver<?> observer,
            Status.Code expectedCode) {

        assertNotNull(observer.error);

        StatusRuntimeException exception =
                assertInstanceOf(
                        StatusRuntimeException.class,
                        observer.error);

        assertEquals(
                expectedCode,
                exception.getStatus().getCode());

        assertEquals(0, observer.values.size());
        assertFalse(observer.completed);
    }

    private static class TestObserver<T>
            implements StreamObserver<T> {

        private final List<T> values = new ArrayList<>();

        private Throwable error;

        private boolean completed = false;

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