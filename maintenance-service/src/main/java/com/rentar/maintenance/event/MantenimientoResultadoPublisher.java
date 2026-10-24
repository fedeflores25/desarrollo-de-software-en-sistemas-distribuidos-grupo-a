package com.rentar.maintenance.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rentar.maintenance.entity.SolicitudMantenimiento;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Publica el resultado para que Rental actualice la reserva sin acoplarse a esta base. */
@Component
public class MantenimientoResultadoPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final boolean eventosHabilitados;

    public MantenimientoResultadoPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper,
                                           @Value("${maintenance.eventos.habilitado:true}") boolean eventosHabilitados) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.eventosHabilitados = eventosHabilitados;
    }

    public void publicar(SolicitudMantenimiento solicitud) {
        if (!eventosHabilitados) {
            return;
        }

        ObjectNode evento = objectMapper.createObjectNode();
        evento.put("eventId", UUID.randomUUID().toString());
        evento.put("eventType", EventosConfig.MANTENIMIENTO_RESULTADO);
        evento.put("correlationId", solicitud.getCorrelationId());
        evento.put("occurredAt", LocalDateTime.now().toString());

        ObjectNode payload = evento.putObject("payload");
        payload.put("reservaId", solicitud.getReservaId());
        payload.put("vehiculoId", solicitud.getVehiculoId());
        payload.put("resultado", solicitud.getEstado().name());

        rabbitTemplate.convertAndSend(EventosConfig.EXCHANGE, EventosConfig.MANTENIMIENTO_RESULTADO,
                evento.toString(), message -> {
                    message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return message;
                });
    }
}
