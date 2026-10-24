package com.rentar.maintenance.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rentar.maintenance.service.IMantenimientoService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Consume reservas confirmadas y confirma RabbitMQ solo despues de procesarlas. */
@Component
public class ReservaConfirmadaListener {

    private final ObjectMapper objectMapper;
    private final IMantenimientoService mantenimientoService;
    private final MantenimientoResultadoPublisher resultadoPublisher;

    public ReservaConfirmadaListener(ObjectMapper objectMapper, IMantenimientoService mantenimientoService,
                                    MantenimientoResultadoPublisher resultadoPublisher) {
        this.objectMapper = objectMapper;
        this.mantenimientoService = mantenimientoService;
        this.resultadoPublisher = resultadoPublisher;
    }

    @RabbitListener(queues = EventosConfig.COLA_RESERVA_CONFIRMADA)
    public void consumir(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            JsonNode evento = objectMapper.readTree(new String(message.getBody(), StandardCharsets.UTF_8));
            validarEvento(evento);

            JsonNode payload = evento.path("payload");
            mantenimientoService.registrarYProcesar(
                    payload.path("reservaId").asLong(),
                    payload.path("vehiculoId").asLong(),
                    LocalDateTime.parse(payload.path("fechaInicio").asText()),
                    LocalDateTime.parse(payload.path("fechaFin").asText()),
                    evento.path("eventId").asText(),
                    evento.path("correlationId").asText())
                    .ifPresent(resultadoPublisher::publicar);

            channel.basicAck(deliveryTag, false);
        } catch (Exception exception) {
            channel.basicReject(deliveryTag, false);
        }
    }

    private void validarEvento(JsonNode evento) {
        JsonNode payload = evento.path("payload");
        if (!EventosConfig.RESERVA_CONFIRMADA.equals(evento.path("eventType").asText())
                || evento.path("eventId").asText().isBlank()
                || evento.path("correlationId").asText().isBlank()
                || payload.path("reservaId").asLong() <= 0
                || payload.path("vehiculoId").asLong() <= 0
                || payload.path("fechaInicio").asText().isBlank()
                || payload.path("fechaFin").asText().isBlank()) {
            throw new IllegalArgumentException("Evento reserva.confirmada invalido");
        }
    }
}
