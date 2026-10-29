package com.rentar.titularidad.event;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class TitularidadResultadoPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    void publicaValidadoConTodosLosCamposExigidos() {
        TitularidadResultadoPublisher publisher =
                new TitularidadResultadoPublisher(rabbitTemplate, true);
        String correlationId = "58268ba1-e486-4cbc-8339-8b52de152def";

        publisher.publicar(
                50L, 1L, "30111222", "EXT101", "VALIDADO", correlationId);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(
                eq("rentar.eventos"),
                eq("vehiculos.externos.validacion.titularidad"),
                captor.capture());

        Message mensaje = captor.getValue();
        JsonNode json = JsonMapper.builder().build().readTree(
                new String(mensaje.getBody(), StandardCharsets.UTF_8));

        assertDoesNotThrow(() -> UUID.fromString(json.path("eventId").asString()));
        assertEquals("vehiculos.externos.validacion.titularidad",
                json.path("eventType").asString());
        assertEquals(correlationId, json.path("correlationId").asString());
        assertDoesNotThrow(() ->
                LocalDateTime.parse(json.path("occurredAt").asString()));

        JsonNode payload = json.path("payload");
        assertEquals(50L, payload.path("vehiculoExternoId").asLong());
        assertEquals(1L, payload.path("clienteId").asLong());
        assertEquals("30111222", payload.path("documento").asString());
        assertEquals("EXT101", payload.path("patente").asString());
        assertEquals("VALIDADO", payload.path("resultado").asString());

        assertEquals(MessageDeliveryMode.PERSISTENT,
                mensaje.getMessageProperties().getDeliveryMode());
        assertEquals("application/json",
                mensaje.getMessageProperties().getContentType());
        assertEquals("UTF-8",
                mensaje.getMessageProperties().getContentEncoding());
        assertNotNull(mensaje.getMessageProperties().getMessageId());
        assertEquals(json.path("eventId").asString(),
                mensaje.getMessageProperties().getMessageId());
    }

    @Test
    void publicaRechazadoConElResultadoCorrecto() {
        TitularidadResultadoPublisher publisher =
                new TitularidadResultadoPublisher(rabbitTemplate, true);

        publisher.publicar(
                51L, 2L, "30111222", "EXT101", "RECHAZADO", "corr-51");

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(
                eq("rentar.eventos"),
                eq("vehiculos.externos.validacion.titularidad"),
                captor.capture());

        JsonNode json = JsonMapper.builder().build().readTree(
                new String(captor.getValue().getBody(), StandardCharsets.UTF_8));
        assertEquals("RECHAZADO", json.path("payload").path("resultado").asString());
        assertEquals("corr-51", json.path("correlationId").asString());
    }

    @Test
    void noPublicaSiLosEventosEstanDeshabilitados() {
        TitularidadResultadoPublisher publisher =
                new TitularidadResultadoPublisher(rabbitTemplate, false);

        publisher.publicar(
                50L, 1L, "30111222", "EXT101", "VALIDADO", "corr-50");

        verifyNoInteractions(rabbitTemplate);
    }
}