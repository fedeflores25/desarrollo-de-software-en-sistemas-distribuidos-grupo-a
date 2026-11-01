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

/** Publica la solicitud policial sin bloquear el alta del vehículo. */
@Component
public class VerificacionPatentePublisher {

    private static final Logger log = LoggerFactory.getLogger(VerificacionPatentePublisher.class);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final RabbitTemplate rabbitTemplate;
    private final boolean habilitado;

    public VerificacionPatentePublisher(RabbitTemplate rabbitTemplate,
                                        @Value("${vehicle.eventos.habilitado:true}") boolean habilitado) {
        this.rabbitTemplate = rabbitTemplate;
        this.habilitado = habilitado;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void alSolicitarVerificacion(VerificacionPatenteSolicitadaEvent evento) {
        if (!habilitado) {
            return;
        }
        try {
            publicar(evento);
        } catch (Exception exception) {
            log.warn("No se pudo publicar la verificacion del vehiculo {}: {}",
                    evento.vehiculoId(), exception.getMessage());
        }
    }

    void publicar(VerificacionPatenteSolicitadaEvent evento) {
        String eventId = UUID.randomUUID().toString();
        MessageProperties propiedades = new MessageProperties();
        propiedades.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        propiedades.setContentEncoding(StandardCharsets.UTF_8.name());
        propiedades.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        propiedades.setMessageId(eventId);

        rabbitTemplate.send(EventosConfig.EXCHANGE, EventosConfig.RK_VERIFICACION_SOLICITADA,
                new Message(armarJson(evento, eventId).getBytes(StandardCharsets.UTF_8), propiedades));
    }

    static String armarJson(VerificacionPatenteSolicitadaEvent evento, String eventId) {
        ObjectNode sobre = JSON.createObjectNode();
        sobre.put("eventId", eventId);
        sobre.put("eventType", EventosConfig.RK_VERIFICACION_SOLICITADA);
        sobre.put("correlationId", UUID.randomUUID().toString());
        sobre.put("occurredAt", LocalDateTime.now().format(FORMATO_FECHA));
        ObjectNode payload = sobre.putObject("payload");
        payload.put("vehiculoId", evento.vehiculoId());
        payload.put("patente", evento.patente());
        return JSON.writeValueAsString(sobre);
    }
}
