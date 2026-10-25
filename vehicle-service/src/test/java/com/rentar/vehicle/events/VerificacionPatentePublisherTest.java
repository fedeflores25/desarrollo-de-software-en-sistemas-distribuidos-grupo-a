package com.rentar.vehicle.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class VerificacionPatentePublisherTest {

    @Test
    void publishesPersistentVerificationRequestWithVehiclePayload() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        VerificacionPatentePublisher publisher = new VerificacionPatentePublisher(rabbitTemplate, true);
        ArgumentCaptor<Message> mensaje = ArgumentCaptor.forClass(Message.class);

        publisher.publicar(new VerificacionPatenteSolicitadaEvent(12L, "ABC123"));

        verify(rabbitTemplate).send(eq(EventosConfig.EXCHANGE), eq(EventosConfig.RK_VERIFICACION_SOLICITADA), mensaje.capture());
        Message publicado = mensaje.getValue();
        String contenido = new String(publicado.getBody(), StandardCharsets.UTF_8);
        assertThat(contenido).contains("\"eventType\":\"vehiculos.verificacion.solicitada\"");
        assertThat(contenido).contains("\"vehiculoId\":12");
        assertThat(contenido).contains("\"patente\":\"ABC123\"");
        assertThat(publicado.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
    }
}
