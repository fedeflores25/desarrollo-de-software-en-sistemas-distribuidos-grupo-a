package com.rentar.vehicle.events;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rabbitmq.client.Channel;
import com.rentar.vehicle.entity.enums.ResultadoVerificacion;
import com.rentar.vehicle.service.VehicleService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

class ResultadoVerificacionListenerTest {

    @Test
    void confirmsAValidStolenVehicleResult() throws Exception {
        VehicleService service = mock(VehicleService.class);
        Channel channel = mock(Channel.class);
        when(service.procesarResultadoVerificacion("evento-1", 18L, "ABC123", ResultadoVerificacion.ROBADO))
                .thenReturn(true);

        new ResultadoVerificacionListener(service).consumir(mensaje(71L, """
                {"eventId":"evento-1","eventType":"vehiculos.verificacion.resultado",
                 "payload":{"vehiculoId":18,"patente":"ABC123","resultado":"ROBADO"}}
                """), channel);

        verify(service).procesarResultadoVerificacion("evento-1", 18L, "ABC123", ResultadoVerificacion.ROBADO);
        verify(channel).basicAck(71L, false);
    }

    @Test
    void rejectsAMalformedResultWithoutRequeueingIt() throws Exception {
        VehicleService service = mock(VehicleService.class);
        Channel channel = mock(Channel.class);

        new ResultadoVerificacionListener(service).consumir(mensaje(72L, "{}"), channel);

        verify(channel).basicReject(72L, false);
        verifyNoInteractions(service);
    }

    private Message mensaje(long deliveryTag, String contenido) {
        MessageProperties properties = new MessageProperties();
        properties.setDeliveryTag(deliveryTag);
        return new Message(contenido.getBytes(StandardCharsets.UTF_8), properties);
    }
}
