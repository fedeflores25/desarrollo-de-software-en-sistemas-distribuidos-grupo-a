package com.rentar.titularidad.event;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.rabbitmq.client.Channel;
import com.rentar.titularidad.service.RegistroTitularidadSimulado;

@ExtendWith(MockitoExtension.class)
class VehiculoExternoRegistradoListenerTest {

    private static final String CORRELATION_ID =
            "3ab5f5fe-7a7a-4815-9acb-4e26724f7564";

    @Mock
    private TitularidadResultadoPublisher publisher;

    @Mock
    private Channel channel;

    private VehiculoExternoRegistradoListener listener;

    @BeforeEach
    void setUp() {
        listener = new VehiculoExternoRegistradoListener(
                new RegistroTitularidadSimulado(), publisher);
    }

    @Test
    void publicaValidadoSiElClienteEsTitular() throws Exception {
        listener.consumir(mensaje(50L, 1L, "EXT101"), channel);

        verify(publisher).publicar(
                50L, 1L, "30111222", "EXT101", "VALIDADO", CORRELATION_ID);
        verify(channel).basicAck(7L, false);
    }

    @Test
    void publicaRechazadoSiElClienteNoEsTitular() throws Exception {
        listener.consumir(mensaje(51L, 2L, "EXT101"), channel);

        verify(publisher).publicar(
                51L, 2L, "30111222", "EXT101", "RECHAZADO", CORRELATION_ID);
        verify(channel).basicAck(7L, false);
    }

    @Test
    void publicaRechazadoSiLaPatenteNoExiste() throws Exception {
        listener.consumir(mensaje(52L, 1L, "DESCONOCIDA"), channel);

        verify(publisher).publicar(
                52L, 1L, "NO_REGISTRADO", "DESCONOCIDA", "RECHAZADO", CORRELATION_ID);
        verify(channel).basicAck(7L, false);
    }

    @Test
    void descartaEventosConTipoIncorrecto() throws Exception {
        String evento = mensajeJson(50L, 1L, "EXT101")
                .replace("vehiculos.externos.registrados", "evento.incorrecto");

        listener.consumir(crearMensaje(evento), channel);

        verify(channel).basicReject(7L, false);
        verifyNoInteractions(publisher);
    }

    @Test
    void descartaEventosSinCorrelationId() throws Exception {
        String evento = mensajeJson(50L, 1L, "EXT101")
                .replace(CORRELATION_ID, "");

        listener.consumir(crearMensaje(evento), channel);

        verify(channel).basicReject(7L, false);
        verifyNoInteractions(publisher);
    }

    @Test
    void descartaMensajesConJsonInvalido() throws Exception {
        listener.consumir(crearMensaje("{ json invalido"), channel);

        verify(channel).basicReject(7L, false);
        verifyNoInteractions(publisher);
    }

    @Test
    void reencolaSiFallaLaPublicacionDelResultado() throws Exception {
        doThrow(new IllegalStateException("Error de publicacion"))
                .when(publisher).publicar(
                        53L, 1L, "30111222", "EXT101", "VALIDADO", CORRELATION_ID);

        listener.consumir(mensaje(53L, 1L, "EXT101"), channel);

        verify(channel).basicNack(7L, false, true);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    private Message mensaje(long externoId, long clienteId, String patente) {
        return crearMensaje(mensajeJson(externoId, clienteId, patente));
    }

    private String mensajeJson(long externoId, long clienteId, String patente) {
        return """
                {
                  "eventId": "d23d995e-e8a4-4c95-b184-73a675d41d05",
                  "eventType": "vehiculos.externos.registrados",
                  "correlationId": "%s",
                  "occurredAt": "2026-10-28T12:00:00",
                  "payload": {
                    "vehiculoExternoId": %d,
                    "clienteId": %d,
                    "patente": "%s"
                  }
                }
                """.formatted(CORRELATION_ID, externoId, clienteId, patente);
    }

    private Message crearMensaje(String json) {
        MessageProperties propiedades = new MessageProperties();
        propiedades.setDeliveryTag(7L);
        return new Message(json.getBytes(StandardCharsets.UTF_8), propiedades);
    }
}