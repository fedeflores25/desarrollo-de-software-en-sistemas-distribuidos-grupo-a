package com.rentar.vehicle.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rentar.vehicle.entity.SolicitudVehiculoExterno;
import com.rentar.vehicle.entity.enums.EstadoIncorporacionExterna;
import com.rentar.vehicle.entity.enums.ResultadoValidacionExterna;
import com.rentar.vehicle.repository.SolicitudVehiculoExternoRepository;

@ExtendWith(MockitoExtension.class)
class RecuperacionSolicitudesExternasTest {

    @Mock
    private SolicitudVehiculoExternoRepository solicitudRepository;

    @Mock
    private VehiculoExternoRegistradoPublisher registradoPublisher;

    @Mock
    private VehiculoExternoValidacionCompletadaPublisher completadaPublisher;

    private RecuperacionSolicitudesExternas recuperacion;

    @BeforeEach
    void setUp() {

        recuperacion = new RecuperacionSolicitudesExternas(
                solicitudRepository,
                registradoPublisher,
                completadaPublisher,
                true);
    }

    @Test
    void recuperaSolicitudConAmbasValidacionesAprobadas() {

        SolicitudVehiculoExterno solicitud = crearSolicitud(
                4L,
                EstadoIncorporacionExterna.PENDIENTE,
                ResultadoValidacionExterna.VALIDADO,
                ResultadoValidacionExterna.VALIDADO);

        when(solicitudRepository.findAll())
                .thenReturn(List.of(solicitud));

        recuperacion.recuperarPendientes();

        ArgumentCaptor<VehiculoExternoValidacionCompletadaEvent> captor =
                ArgumentCaptor.forClass(
                        VehiculoExternoValidacionCompletadaEvent.class);

        verify(completadaPublisher).publicar(captor.capture());

        assertEquals(
                4L,
                captor.getValue().vehiculoExternoId());

        assertEquals(
                generarCorrelationId(4L),
                captor.getValue().correlationId());

        verifyNoInteractions(registradoPublisher);
    }

    @Test
    void recuperaSolicitudConValidacionPendiente() {

        SolicitudVehiculoExterno solicitud = crearSolicitud(
                5L,
                EstadoIncorporacionExterna.PENDIENTE,
                null,
                ResultadoValidacionExterna.VALIDADO);

        when(solicitudRepository.findAll())
                .thenReturn(List.of(solicitud));

        recuperacion.recuperarPendientes();

        ArgumentCaptor<VehiculoExternoRegistradoEvent> captor =
                ArgumentCaptor.forClass(
                        VehiculoExternoRegistradoEvent.class);

        verify(registradoPublisher).publicar(captor.capture());

        assertEquals(
                5L,
                captor.getValue().vehiculoExternoId());

        assertEquals(
                2L,
                captor.getValue().clienteId());

        assertEquals(
                "EXT105",
                captor.getValue().patente());

        verifyNoInteractions(completadaPublisher);
    }

    @Test
    void noRecuperaSolicitudesFinalizadas() {

        SolicitudVehiculoExterno incorporada = crearSolicitud(
                6L,
                EstadoIncorporacionExterna.INCORPORADA,
                ResultadoValidacionExterna.VALIDADO,
                ResultadoValidacionExterna.VALIDADO);

        SolicitudVehiculoExterno rechazada = crearSolicitud(
                7L,
                EstadoIncorporacionExterna.RECHAZADA,
                ResultadoValidacionExterna.RECHAZADO,
                null);

        when(solicitudRepository.findAll())
                .thenReturn(List.of(incorporada, rechazada));

        recuperacion.recuperarPendientes();

        verifyNoInteractions(
                registradoPublisher,
                completadaPublisher);
    }

    @Test
    void noRecuperaSiEstaDeshabilitada() {

        RecuperacionSolicitudesExternas deshabilitada =
                new RecuperacionSolicitudesExternas(
                        solicitudRepository,
                        registradoPublisher,
                        completadaPublisher,
                        false);

        deshabilitada.recuperarPendientes();

        verifyNoInteractions(
                solicitudRepository,
                registradoPublisher,
                completadaPublisher);
    }

    private SolicitudVehiculoExterno crearSolicitud(
            Long id,
            EstadoIncorporacionExterna estado,
            ResultadoValidacionExterna resultadoPatente,
            ResultadoValidacionExterna resultadoTitularidad) {

        SolicitudVehiculoExterno solicitud =
                new SolicitudVehiculoExterno();

        solicitud.setId(id);
        solicitud.setClienteId(2L);
        solicitud.setPatente("EXT10" + id);
        solicitud.setEstado(estado);
        solicitud.setResultadoPatente(resultadoPatente);
        solicitud.setResultadoTitularidad(resultadoTitularidad);

        return solicitud;
    }

    private String generarCorrelationId(Long solicitudId) {

        return UUID.nameUUIDFromBytes(
                ("vehiculo-externo:" + solicitudId)
                        .getBytes(StandardCharsets.UTF_8))
                .toString();
    }
}