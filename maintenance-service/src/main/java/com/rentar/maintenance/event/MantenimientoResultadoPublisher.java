package com.rentar.maintenance.event;

import com.rentar.maintenance.entity.SolicitudMantenimiento;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** Publica el resultado para que Rental actualice la reserva sin acoplarse a esta base. */
@Component
public class MantenimientoResultadoPublisher {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    // Formato ISO-8601 local sin zona horaria y con segundos.
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final RabbitTemplate rabbitTemplate;
    private final boolean eventosHabilitados;

    public MantenimientoResultadoPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${maintenance.eventos.habilitado:true}")
            boolean eventosHabilitados) {

        this.rabbitTemplate = rabbitTemplate;
        this.eventosHabilitados = eventosHabilitados;
    }

    public void publicar(SolicitudMantenimiento solicitud) {
        if (!eventosHabilitados) {
            return;
        }

        ObjectNode evento = JSON.createObjectNode();

        evento.put("eventId", UUID.randomUUID().toString());
        evento.put("eventType", EventosConfig.MANTENIMIENTO_RESULTADO);
        evento.put("correlationId", solicitud.getCorrelationId());
        evento.put("occurredAt", LocalDateTime.now().format(FORMATO_FECHA));

        ObjectNode payload = evento.putObject("payload");

        payload.put("reservaId", solicitud.getReservaId());
        payload.put("vehiculoId", solicitud.getVehiculoId());
        payload.put("resultado", solicitud.getEstado().name());

        rabbitTemplate.convertAndSend(
                EventosConfig.EXCHANGE,
                EventosConfig.MANTENIMIENTO_RESULTADO,
                evento.toString(),
                message -> {
                    message.getMessageProperties()
                            .setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    return message;
                });
    }
}