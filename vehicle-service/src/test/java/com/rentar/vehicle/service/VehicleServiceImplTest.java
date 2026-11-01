package com.rentar.vehicle.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.rentar.vehicle.dto.VehicleCreateRequest;
import com.rentar.vehicle.entity.EventoVerificacionProcesado;
import com.rentar.vehicle.entity.Vehiculo;
import com.rentar.vehicle.entity.enums.EstadoVehiculo;
import com.rentar.vehicle.entity.enums.ResultadoVerificacion;
import com.rentar.vehicle.entity.enums.TipoVehiculo;
import com.rentar.vehicle.events.VerificacionPatenteSolicitadaEvent;
import com.rentar.vehicle.exception.PatenteDuplicadaException;
import com.rentar.vehicle.exception.VehiculoInactivoException;
import com.rentar.vehicle.repository.VehicleRepository;
import com.rentar.vehicle.repository.EventoVerificacionProcesadoRepository;

@ExtendWith(MockitoExtension.class)
class VehicleServiceImplTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private EventoVerificacionProcesadoRepository eventoRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    @Test
    void createInitializesVehicleAsActiveAndAvailable() {
        VehicleCreateRequest request = createRequest();
        when(vehicleRepository.existsByPatente(request.getPatente())).thenReturn(false);
        when(vehicleRepository.save(any(Vehiculo.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Vehiculo result = vehicleService.create(request);

        ArgumentCaptor<Vehiculo> vehicleCaptor = ArgumentCaptor.forClass(Vehiculo.class);
        verify(vehicleRepository).save(vehicleCaptor.capture());
        Vehiculo savedVehicle = vehicleCaptor.getValue();
        assertTrue(savedVehicle.isActivo());
        assertEquals(EstadoVehiculo.DISPONIBLE, savedVehicle.getEstado());
        assertEquals("ABC123", savedVehicle.getPatente());
        assertEquals(savedVehicle, result);
        verify(eventPublisher).publishEvent(any(VerificacionPatenteSolicitadaEvent.class));
    }

    @Test
    void createRejectsAnExistingPatent() {
        VehicleCreateRequest request = createRequest();
        when(vehicleRepository.existsByPatente(request.getPatente())).thenReturn(true);

        assertThrows(PatenteDuplicadaException.class, () -> vehicleService.create(request));
    }

    @Test
    void listAvailableExcludesInactiveReservedAndBusyVehicles() {
        Vehiculo available = vehicle(1L, EstadoVehiculo.DISPONIBLE, true);
        Vehiculo busy = vehicle(2L, EstadoVehiculo.DISPONIBLE, true);
        Vehiculo reserved = vehicle(3L, EstadoVehiculo.RESERVADO, true);
        Vehiculo inactive = vehicle(4L, EstadoVehiculo.DISPONIBLE, false);
        when(vehicleRepository.findAllByActivoTrue()).thenReturn(List.of(available, busy, reserved));

        List<Vehiculo> result = vehicleService.listAvailable(List.of(2L));

        assertEquals(List.of(available), result);
        assertFalse(result.contains(inactive));
    }

    @Test
    void updateStatusRejectsAnInactiveVehicle() {
        Vehiculo vehicle = vehicle(1L, EstadoVehiculo.DISPONIBLE, false);
        when(vehicleRepository.findById(1L)).thenReturn(java.util.Optional.of(vehicle));

        assertThrows(
                VehiculoInactivoException.class,
                () -> vehicleService.updateStatus(1L, EstadoVehiculo.RESERVADO));
    }

    @Test
    void stolenResultDeactivatesVehicleAndRecordsTheEvent() {
        Vehiculo vehicle = vehicle(7L, EstadoVehiculo.DISPONIBLE, true);
        when(eventoRepository.existsById("evento-1")).thenReturn(false);
        when(vehicleRepository.findById(7L)).thenReturn(java.util.Optional.of(vehicle));

        boolean processed = vehicleService.procesarResultadoVerificacion(
                "evento-1", 7L, "PAT7", ResultadoVerificacion.ROBADO);

        assertTrue(processed);
        assertFalse(vehicle.isActivo());
        verify(vehicleRepository).save(vehicle);
        verify(eventoRepository).save(any(EventoVerificacionProcesado.class));
    }

    @Test
    void duplicateResultDoesNotModifyVehicleAgain() {
        when(eventoRepository.existsById("evento-duplicado")).thenReturn(true);

        boolean processed = vehicleService.procesarResultadoVerificacion(
                "evento-duplicado", 8L, "PAT8", ResultadoVerificacion.VALIDADO);

        assertFalse(processed);
        verify(vehicleRepository, never()).findById(8L);
    }

    private VehicleCreateRequest createRequest() {
        VehicleCreateRequest request = new VehicleCreateRequest();
        request.setPatente("ABC123");
        request.setMarca("Toyota");
        request.setModelo("Corolla");
        request.setAnio(2021);
        request.setColor("Blanco");
        request.setTipo(TipoVehiculo.SEDAN);
        request.setPrecioDiario(BigDecimal.valueOf(45000));
        return request;
    }

    private Vehiculo vehicle(Long id, EstadoVehiculo estado, boolean activo) {
        Vehiculo vehicle = new Vehiculo();
        vehicle.setId(id);
        vehicle.setPatente("PAT" + id);
        vehicle.setMarca("Marca");
        vehicle.setModelo("Modelo");
        vehicle.setAnio(2021);
        vehicle.setTipo(TipoVehiculo.SEDAN);
        vehicle.setPrecioDiario(BigDecimal.valueOf(100));
        vehicle.setEstado(estado);
        vehicle.setActivo(activo);
        return vehicle;
    }
}
