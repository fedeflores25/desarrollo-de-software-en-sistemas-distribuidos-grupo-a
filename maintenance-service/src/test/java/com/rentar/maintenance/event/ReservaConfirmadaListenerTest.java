package com.rentar.maintenance.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rabbitmq.client.Channel;
import com.rentar.maintenance.entity.SolicitudMantenimiento;
import com.rentar.maintenance.service.IMantenimientoService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

class ReservaConfirmadaListenerTest {

    @Test
    void confirmaElMensajeLuegoDeProcesarlo() throws Exception {
        IMantenimientoService service = mock(IMantenimientoService.class);
        MantenimientoResultadoPublisher publisher = mock(MantenimientoResultadoPublisher.class);
        Channel channel = mock(Channel.class);
        SolicitudMantenimiento solicitud = new SolicitudMantenimiento(15L, 4L,
                LocalDateTime.parse("2026-10-20T10:00:00"), LocalDateTime.parse("2026-10-21T10:00:00"),
                "evento-1", "correlacion-1");
        when(service.registrarYProcesar(eq(15L), eq(4L), any(), any(), eq("evento-1"), eq("correlacion-1")))
                .thenReturn(Optional.of(solicitud));

        ReservaConfirmadaListener listener = new ReservaConfirmadaListener(service, publisher);
        listener.consumir(mensaje(51L, """
                {"eventId":"evento-1","eventType":"reserva.confirmada","correlationId":"correlacion-1",
                 "payload":{"reservaId":15,"vehiculoId":4,"fechaInicio":"2026-10-20T10:00:00","fechaFin":"2026-10-21T10:00:00"}}
                """), channel);

        verify(publisher).publicar(solicitud);
        verify(channel).basicAck(51L, false);
    }

    @Test
    void rechazaSinReencolarUnMensajeInvalido() throws Exception {
        IMantenimientoService service = mock(IMantenimientoService.class);
        MantenimientoResultadoPublisher publisher = mock(MantenimientoResultadoPublisher.class);
        Channel channel = mock(Channel.class);

        new ReservaConfirmadaListener(service, publisher).consumir(mensaje(52L, "{}"), channel);

        verify(channel).basicReject(52L, false);
        verifyNoInteractions(service, publisher);
    }

    private Message mensaje(long deliveryTag, String contenido) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(deliveryTag);
        return new Message(contenido.getBytes(StandardCharsets.UTF_8), properties);
    }
}
