package com.rentar.titularidad.event;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** Publica el resultado de la validacion de titularidad. */
@Component
public class TitularidadResultadoPublisher {

    private static final JsonMapper JSON =
            JsonMapper.builder().build();

    private final RabbitTemplate rabbitTemplate;
    private final boolean habilitado;

    public TitularidadResultadoPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${titularidad.eventos.habilitado:true}")
            boolean habilitado) {

        this.rabbitTemplate = rabbitTemplate;
        this.habilitado = habilitado;
    }

    public void publicar(
            Long vehiculoExternoId,
            Long clienteId,
            String documento,
            String patente,
            String resultado,
            String correlationId) {

        if (!habilitado) {
            return;
        }

        String eventId = UUID.randomUUID().toString();

        ObjectNode evento = JSON.createObjectNode();

        evento.put("eventId", eventId);
        evento.put(
                "eventType",
                EventosConfig.VALIDACION_TITULARIDAD);
        evento.put("correlationId", correlationId);
        evento.put(
                "occurredAt",
                LocalDateTime.now().toString());

        ObjectNode payload = evento.putObject("payload");

        payload.put("vehiculoExternoId", vehiculoExternoId);
        payload.put("clienteId", clienteId);
        payload.put("documento", documento);
        payload.put("patente", patente);
        payload.put("resultado", resultado);

        MessageProperties propiedades = new MessageProperties();

        propiedades.setContentType(
                MessageProperties.CONTENT_TYPE_JSON);
        propiedades.setContentEncoding(
                StandardCharsets.UTF_8.name());
        propiedades.setDeliveryMode(
                MessageDeliveryMode.PERSISTENT);
        propiedades.setMessageId(eventId);

        Message mensaje = new Message(
                JSON.writeValueAsString(evento)
                        .getBytes(StandardCharsets.UTF_8),
                propiedades);

        rabbitTemplate.send(
                EventosConfig.EXCHANGE,
                EventosConfig.VALIDACION_TITULARIDAD,
                mensaje);
    }
}