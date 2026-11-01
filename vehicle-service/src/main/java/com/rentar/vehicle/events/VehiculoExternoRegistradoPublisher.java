package com.rentar.vehicle.events;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** Publica el registro de un vehiculo externo sin bloquear la solicitud. */
@Component
public class VehiculoExternoRegistradoPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(
                    VehiculoExternoRegistradoPublisher.class);

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final JsonMapper JSON =
            JsonMapper.builder().build();

    private final RabbitTemplate rabbitTemplate;
    private final boolean habilitado;

    public VehiculoExternoRegistradoPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${vehicle.eventos.habilitado:true}")
            boolean habilitado) {

        this.rabbitTemplate = rabbitTemplate;
        this.habilitado = habilitado;
    }

    @Async
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT)
    public void alRegistrarExterno(
            VehiculoExternoRegistradoEvent evento) {

        if (!habilitado) {
            return;
        }

        try {
            publicar(evento);
        } catch (Exception exception) {
            log.warn(
                    "No se pudo publicar la solicitud externa {}: {}",
                    evento.vehiculoExternoId(),
                    exception.getMessage());
        }
    }

    void publicar(VehiculoExternoRegistradoEvent evento) {

        String eventId = UUID.randomUUID().toString();

        MessageProperties propiedades = new MessageProperties();
        propiedades.setContentType(
                MessageProperties.CONTENT_TYPE_JSON);
        propiedades.setContentEncoding(
                StandardCharsets.UTF_8.name());
        propiedades.setDeliveryMode(
                MessageDeliveryMode.PERSISTENT);
        propiedades.setMessageId(eventId);

        rabbitTemplate.send(
                EventosConfig.EXCHANGE,
                EventosConfig.RK_EXTERNO_REGISTRADO,
                new Message(
                        armarJson(evento, eventId)
                                .getBytes(StandardCharsets.UTF_8),
                        propiedades));
    }

    static String armarJson(
            VehiculoExternoRegistradoEvent evento,
            String eventId) {

        ObjectNode sobre = JSON.createObjectNode();

        String correlationId = UUID.nameUUIDFromBytes(
                ("vehiculo-externo:" + evento.vehiculoExternoId())
                        .getBytes(StandardCharsets.UTF_8))
                .toString();

        sobre.put("eventId", eventId);
        sobre.put(
                "eventType",
                EventosConfig.RK_EXTERNO_REGISTRADO);
        sobre.put("correlationId", correlationId);
        sobre.put(
                "occurredAt",
                LocalDateTime.now().format(FORMATO_FECHA));

        ObjectNode payload = sobre.putObject("payload");

        payload.put(
                "vehiculoExternoId",
                evento.vehiculoExternoId());
        payload.put("clienteId", evento.clienteId());
        payload.put("patente", evento.patente());

        return JSON.writeValueAsString(sobre);
    }
}