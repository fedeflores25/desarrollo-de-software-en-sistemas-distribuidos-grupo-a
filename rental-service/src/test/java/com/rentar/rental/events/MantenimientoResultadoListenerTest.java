package com.rentar.rental.events;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rabbitmq.client.Channel;
import com.rentar.rental.entity.enums.EstadoMantenimiento;
import com.rentar.rental.service.IMantenimientoService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

class MantenimientoResultadoListenerTest {

    @Test
    void confirmaElMensajeLuegoDeGuardarElResultado() throws Exception {
        IMantenimientoService service = mock(IMantenimientoService.class);
        Channel channel = mock(Channel.class);
        when(service.registrarResultado("evento-1", 30L, EstadoMantenimiento.REALIZADO)).thenReturn(true);

        new MantenimientoResultadoListener(service).alRecibirResultado(mensaje(61L, """
                {"eventId":"evento-1","payload":{"reservaId":30,"resultado":"REALIZADO"}}
                """), channel);

        verify(service).registrarResultado("evento-1", 30L, EstadoMantenimiento.REALIZADO);
        verify(channel).basicAck(61L, false);
    }

    @Test
    void rechazaUnResultadoPendientePorqueNoEsUnResultadoFinal() throws Exception {
        IMantenimientoService service = mock(IMantenimientoService.class);
        Channel channel = mock(Channel.class);

        new MantenimientoResultadoListener(service).alRecibirResultado(mensaje(62L, """
                {"eventId":"evento-2","payload":{"reservaId":31,"resultado":"PENDIENTE"}}
                """), channel);

        verify(channel).basicReject(62L, false);
        verifyNoInteractions(service);
    }

    private Message mensaje(long deliveryTag, String contenido) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(deliveryTag);
        return new Message(contenido.getBytes(StandardCharsets.UTF_8), properties);
    }
}
