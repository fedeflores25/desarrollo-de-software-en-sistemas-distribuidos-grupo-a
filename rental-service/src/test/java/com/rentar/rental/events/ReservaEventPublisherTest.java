package com.rentar.rental.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class ReservaEventPublisherTest {

    @Test
    void publicaUnMensajePersistenteConElPayloadDeLaReserva() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        ReservaEventPublisher publisher = new ReservaEventPublisher(rabbitTemplate, true);
        ReservaConfirmadaEvent evento = new ReservaConfirmadaEvent(40L, 6L,
                LocalDateTime.parse("2026-10-20T10:00:00"), LocalDateTime.parse("2026-10-22T10:00:00"));
        ArgumentCaptor<Message> mensaje = ArgumentCaptor.forClass(Message.class);

        publisher.publicar(evento);

        verify(rabbitTemplate).send(eq(EventosConfig.EXCHANGE), eq(EventosConfig.RK_RESERVA_CONFIRMADA), mensaje.capture());
        Message publicado = mensaje.getValue();
        String contenido = new String(publicado.getBody(), StandardCharsets.UTF_8);
        assertThat(contenido).contains("\"eventType\":\"reserva.confirmada\"");
        assertThat(contenido).contains("\"reservaId\":40");
        assertThat(contenido).contains("\"vehiculoId\":6");
        assertThat(contenido).contains("\"fechaInicio\":\"2026-10-20T10:00:00\"");
        assertThat(publicado.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
    }
}
