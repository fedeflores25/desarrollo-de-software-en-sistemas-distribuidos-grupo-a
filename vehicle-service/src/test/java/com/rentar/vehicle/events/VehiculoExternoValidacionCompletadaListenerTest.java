package com.rentar.vehicle.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import com.rabbitmq.client.Channel;
import com.rentar.vehicle.entity.SolicitudVehiculoExterno;
import com.rentar.vehicle.entity.enums.EstadoIncorporacionExterna;
import com.rentar.vehicle.repository.SolicitudVehiculoExternoRepository;
import com.rentar.vehicle.repository.VehicleRepository;
import com.rentar.vehicle.service.SolicitudVehiculoExternoService;

@ExtendWith(MockitoExtension.class)
class VehiculoExternoValidacionCompletadaListenerTest {

    @Mock
    private SolicitudVehiculoExternoService solicitudService;

    @Mock
    private SolicitudVehiculoExternoRepository solicitudRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private Channel channel;

    private VehiculoExternoValidacionCompletadaListener listener;

    @BeforeEach
    void setUp() {

        listener = new VehiculoExternoValidacionCompletadaListener(
                solicitudService,
                solicitudRepository,
                vehicleRepository,
                transactionManager);
    }

    @Test
    void incorporaYConfirmaMensajeCorrectamente() throws Exception {

        listener.consumir(mensajeValido(42L), channel);

        verify(solicitudService).incorporar(42L);
        verify(channel).basicAck(7L, false);

        verifyNoInteractions(
                solicitudRepository,
                vehicleRepository,
                transactionManager);
    }

    @Test
    void patenteDuplicadaRechazaSolicitudYConfirmaMensaje()
            throws Exception {

        SolicitudVehiculoExterno solicitud = solicitudPendiente();

        doThrow(new DataIntegrityViolationException(
                "Duplicate entry for patente"))
                .when(solicitudService)
                .incorporar(42L);

        TransactionStatus estadoTransaccion =
                mock(TransactionStatus.class);

        when(transactionManager.getTransaction(
                any(TransactionDefinition.class)))
                .thenReturn(estadoTransaccion);

        when(solicitudRepository.buscarParaActualizar(42L))
                .thenReturn(Optional.of(solicitud));

        when(vehicleRepository.existsByPatente("EXT142"))
                .thenReturn(true);

        listener.consumir(mensajeValido(42L), channel);

        assertEquals(
                EstadoIncorporacionExterna.RECHAZADA,
                solicitud.getEstado());

        verify(solicitudRepository).save(solicitud);

        verify(transactionManager).commit(estadoTransaccion);

        verify(channel).basicAck(7L, false);

        verify(channel, never()).basicNack(
                7L, false, true);
    }

    @Test
    void errorTemporalMantieneSolicitudPendiente()
            throws Exception {

        SolicitudVehiculoExterno solicitud = solicitudPendiente();

        doThrow(new RuntimeException("Error temporal"))
                .when(solicitudService)
                .incorporar(42L);

        TransactionStatus estadoTransaccion =
                mock(TransactionStatus.class);

        when(transactionManager.getTransaction(
                any(TransactionDefinition.class)))
                .thenReturn(estadoTransaccion);

        when(solicitudRepository.buscarParaActualizar(42L))
                .thenReturn(Optional.of(solicitud));

        when(vehicleRepository.existsByPatente("EXT142"))
                .thenReturn(false);

        listener.consumir(mensajeValido(42L), channel);

        assertEquals(
                EstadoIncorporacionExterna.PENDIENTE,
                solicitud.getEstado());

        verify(solicitudRepository, never()).save(
                any(SolicitudVehiculoExterno.class));

        verify(transactionManager).commit(estadoTransaccion);

        verify(channel).basicReject(7L, false);

        verify(channel, never()).basicNack(
                7L, false, true);
    }

    private SolicitudVehiculoExterno solicitudPendiente() {

        SolicitudVehiculoExterno solicitud =
                new SolicitudVehiculoExterno();

        solicitud.setId(42L);
        solicitud.setClienteId(2L);
        solicitud.setPatente("EXT142");

        solicitud.setEstado(
                EstadoIncorporacionExterna.PENDIENTE);

        return solicitud;
    }

    private Message mensajeValido(Long solicitudId) {

        String correlationId = UUID.nameUUIDFromBytes(
                ("vehiculo-externo:" + solicitudId)
                        .getBytes(StandardCharsets.UTF_8))
                .toString();

        String contenido = """
                {
                  "eventId": "evento-prueba-42",
                  "eventType": "%s",
                  "correlationId": "%s",
                  "payload": {
                    "vehiculoExternoId": %d
                  }
                }
                """.formatted(
                        EventosConfig.RK_EXTERNO_VALIDACION_COMPLETADA,
                        correlationId,
                        solicitudId);

        MessageProperties propiedades =
                new MessageProperties();

        propiedades.setDeliveryTag(7L);

        return new Message(
                contenido.getBytes(StandardCharsets.UTF_8),
                propiedades);
    }
}