package com.rentar.vehicle.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.rentar.vehicle.dto.SolicitudVehiculoExternoRequest;
import com.rentar.vehicle.entity.SolicitudVehiculoExterno;
import com.rentar.vehicle.entity.enums.EstadoIncorporacionExterna;
import com.rentar.vehicle.entity.enums.ResultadoValidacionExterna;
import com.rentar.vehicle.entity.enums.TipoVehiculo;
import com.rentar.vehicle.events.VehiculoExternoRegistradoEvent;
import com.rentar.vehicle.events.VehiculoExternoValidacionCompletadaEvent;
import com.rentar.vehicle.repository.SolicitudVehiculoExternoRepository;
import com.rentar.vehicle.repository.VehicleRepository;

@ExtendWith(MockitoExtension.class)
class SolicitudVehiculoExternoServiceImplTest {

    @Mock
    private SolicitudVehiculoExternoRepository solicitudRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SolicitudVehiculoExternoServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SolicitudVehiculoExternoServiceImpl(
                solicitudRepository,
                vehicleRepository,
                eventPublisher);
    }

    @Test
    void createGuardaSolicitudPendienteYPublicaEvento() {

        SolicitudVehiculoExternoRequest request = solicitudValida();
        request.setPatente(" ext104 ");

        when(solicitudRepository.save(
                any(SolicitudVehiculoExterno.class)))
                .thenAnswer(invocation -> {
                    SolicitudVehiculoExterno solicitud =
                            invocation.getArgument(0);
                    solicitud.setId(10L);
                    return solicitud;
                });

        SolicitudVehiculoExterno guardada = service.create(request);

        assertEquals(10L, guardada.getId());
        assertEquals("EXT104", guardada.getPatente());
        assertEquals(
                EstadoIncorporacionExterna.PENDIENTE,
                guardada.getEstado());
        assertNull(guardada.getResultadoPatente());
        assertNull(guardada.getResultadoTitularidad());
        assertNull(guardada.getVehiculoId());

        verify(vehicleRepository).existsByPatente("EXT104");

        ArgumentCaptor<VehiculoExternoRegistradoEvent> captor =
                ArgumentCaptor.forClass(
                        VehiculoExternoRegistradoEvent.class);

        verify(eventPublisher).publishEvent(captor.capture());

        assertEquals(10L, captor.getValue().vehiculoExternoId());
        assertEquals(2L, captor.getValue().clienteId());
        assertEquals("EXT104", captor.getValue().patente());
    }

    @Test
    void createRechazaPatenteDemasiadoLarga() {

        SolicitudVehiculoExternoRequest request = solicitudValida();
        request.setPatente("PATENTE_DEMASIADO_LARGA");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(request));

        verifyNoInteractions(
                vehicleRepository,
                solicitudRepository,
                eventPublisher);
    }

    @Test
    void createRechazaPrecioConMasDeDosDecimales() {

        SolicitudVehiculoExternoRequest request = solicitudValida();
        request.setPrecioDiario(new BigDecimal("45000.123"));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(request));

        verifyNoInteractions(
                vehicleRepository,
                solicitudRepository,
                eventPublisher);
    }

    @Test
    void createRechazaPrecioQueSuperaElLimiteDeMySQL() {

        SolicitudVehiculoExternoRequest request = solicitudValida();
        request.setPrecioDiario(new BigDecimal("100000000.00"));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.create(request));

        verifyNoInteractions(
                vehicleRepository,
                solicitudRepository,
                eventPublisher);
    }

    @Test
    void publicaValidacionCompletadaSoloCuandoAmbasEstanAprobadas() {

        SolicitudVehiculoExterno solicitud = entidadPendiente();
        String correlationId = correlationId(10L);

        when(solicitudRepository.buscarParaActualizar(10L))
                .thenReturn(Optional.of(solicitud));

        service.procesarResultadoTitularidad(
                10L,
                2L,
                "30222333",
                "EXT104",
                ResultadoValidacionExterna.VALIDADO,
                correlationId);

        assertEquals(
                ResultadoValidacionExterna.VALIDADO,
                solicitud.getResultadoTitularidad());

        assertNull(solicitud.getResultadoPatente());

        verify(eventPublisher, never()).publishEvent(
                any(VehiculoExternoValidacionCompletadaEvent.class));

        service.procesarResultadoPatente(
                10L,
                "EXT104",
                ResultadoValidacionExterna.VALIDADO,
                correlationId);

        assertEquals(
                ResultadoValidacionExterna.VALIDADO,
                solicitud.getResultadoPatente());

        ArgumentCaptor<VehiculoExternoValidacionCompletadaEvent> captor =
                ArgumentCaptor.forClass(
                        VehiculoExternoValidacionCompletadaEvent.class);

        verify(eventPublisher).publishEvent(captor.capture());

        assertEquals(10L, captor.getValue().vehiculoExternoId());
        assertEquals(correlationId, captor.getValue().correlationId());
    }

    @Test
    void titularidadIncorrectaRechazaLaSolicitud() {

        SolicitudVehiculoExterno solicitud = entidadPendiente();

        when(solicitudRepository.buscarParaActualizar(10L))
                .thenReturn(Optional.of(solicitud));

        service.procesarResultadoTitularidad(
                10L,
                2L,
                "DOCUMENTO_INCORRECTO",
                "EXT104",
                ResultadoValidacionExterna.VALIDADO,
                correlationId(10L));

        assertEquals(
                ResultadoValidacionExterna.RECHAZADO,
                solicitud.getResultadoTitularidad());

        assertEquals(
                EstadoIncorporacionExterna.RECHAZADA,
                solicitud.getEstado());

        verify(eventPublisher, never()).publishEvent(
                any(VehiculoExternoValidacionCompletadaEvent.class));
    }

    @Test
    void incorporarNoDuplicaUnaSolicitudYaIncorporada() {

        SolicitudVehiculoExterno solicitud = entidadPendiente();

        solicitud.setEstado(
                EstadoIncorporacionExterna.INCORPORADA);
        solicitud.setVehiculoId(13L);

        when(solicitudRepository.buscarParaActualizar(10L))
                .thenReturn(Optional.of(solicitud));

        service.incorporar(10L);

        assertEquals(
                EstadoIncorporacionExterna.INCORPORADA,
                solicitud.getEstado());

        assertEquals(13L, solicitud.getVehiculoId());

        verifyNoInteractions(vehicleRepository);
    }

    private SolicitudVehiculoExternoRequest solicitudValida() {

        SolicitudVehiculoExternoRequest request =
                new SolicitudVehiculoExternoRequest();

        request.setClienteId(2L);
        request.setDocumento("30222333");
        request.setPatente("EXT104");
        request.setMarca("Toyota");
        request.setModelo("Corolla");
        request.setAnio(2022);
        request.setColor("Blanco");
        request.setTipo(TipoVehiculo.SEDAN);
        request.setPrecioDiario(new BigDecimal("45000.00"));

        return request;
    }

    private SolicitudVehiculoExterno entidadPendiente() {

        SolicitudVehiculoExterno solicitud =
                new SolicitudVehiculoExterno();

        solicitud.setId(10L);
        solicitud.setClienteId(2L);
        solicitud.setDocumento("30222333");
        solicitud.setPatente("EXT104");
        solicitud.setMarca("Toyota");
        solicitud.setModelo("Corolla");
        solicitud.setAnio(2022);
        solicitud.setColor("Blanco");
        solicitud.setTipo(TipoVehiculo.SEDAN);
        solicitud.setPrecioDiario(new BigDecimal("45000.00"));
        solicitud.setEstado(
                EstadoIncorporacionExterna.PENDIENTE);

        return solicitud;
    }

    private String correlationId(Long solicitudId) {

        return UUID.nameUUIDFromBytes(
                ("vehiculo-externo:" + solicitudId)
                        .getBytes(StandardCharsets.UTF_8))
                .toString();
    }
}