package com.rentar.rental.events;

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

/**
 * Publica reserva.confirmada en RabbitMQ.
 *
 * Se ejecuta despues de que la reserva se guardo (AFTER_COMMIT) y en otro hilo, asi la
 * confirmacion de la reserva no espera al broker ni falla si RabbitMQ no esta disponible.
 */
@Component
public class ReservaEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ReservaEventPublisher.class);

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final RabbitTemplate rabbitTemplate;
    private final boolean habilitado;

    public ReservaEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${rental.eventos.habilitado:true}") boolean habilitado) {

        this.rabbitTemplate = rabbitTemplate;
        this.habilitado = habilitado;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alConfirmarseReserva(ReservaConfirmadaEvent evento) {

        if (!habilitado) {
            return;
        }

        try {
            publicar(evento);

        } catch (Exception e) {
            // La reserva ya esta confirmada: un problema con el broker no la afecta.
            log.warn("No se pudo publicar reserva.confirmada de la reserva {}: {}",
                    evento.reservaId(), e.getMessage());
        }
    }

    void publicar(ReservaConfirmadaEvent evento) {

        String eventId = UUID.randomUUID().toString();

        MessageProperties propiedades = new MessageProperties();
        propiedades.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        propiedades.setContentEncoding(StandardCharsets.UTF_8.name());
        propiedades.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        propiedades.setMessageId(eventId);

        Message mensaje = new Message(armarJson(evento, eventId).getBytes(StandardCharsets.UTF_8), propiedades);

        rabbitTemplate.send(EventosConfig.EXCHANGE, EventosConfig.RK_RESERVA_CONFIRMADA, mensaje);

        log.info("Publicado reserva.confirmada de la reserva {} (evento {})", evento.reservaId(), eventId);
    }

    /** Arma el mensaje con el sobre comun y el payload exacto de la consigna. */
    static String armarJson(ReservaConfirmadaEvent evento, String eventId) {

        ObjectNode sobre = JSON.createObjectNode();
        sobre.put("eventId", eventId);
        sobre.put("eventType", EventosConfig.RK_RESERVA_CONFIRMADA);
        sobre.put("correlationId", UUID.randomUUID().toString());
        sobre.put("occurredAt", LocalDateTime.now().format(FORMATO_FECHA));

        ObjectNode payload = sobre.putObject("payload");
        payload.put("reservaId", evento.reservaId());
        payload.put("vehiculoId", evento.vehiculoId());
        payload.put("fechaInicio", evento.fechaInicio().format(FORMATO_FECHA));
        payload.put("fechaFin", evento.fechaFin().format(FORMATO_FECHA));

        return JSON.writeValueAsString(sobre);
    }
}
