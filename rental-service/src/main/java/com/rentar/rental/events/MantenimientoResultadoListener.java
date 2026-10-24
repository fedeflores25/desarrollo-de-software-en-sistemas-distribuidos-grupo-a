package com.rentar.rental.events;

import java.nio.charset.StandardCharsets;
import java.io.IOException;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.rentar.rental.entity.enums.EstadoMantenimiento;
import com.rentar.rental.service.IMantenimientoService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Consume mantenimiento.resultado y guarda el resultado asociado a la reserva.
 * Si el mensaje esta mal formado se descarta (no se reintenta), porque reintentarlo
 * nunca lo va a arreglar.
 */
@Component
public class MantenimientoResultadoListener {

    private static final Logger log = LoggerFactory.getLogger(MantenimientoResultadoListener.class);

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final IMantenimientoService mantenimientoService;

    public MantenimientoResultadoListener(IMantenimientoService mantenimientoService) {
        this.mantenimientoService = mantenimientoService;
    }

    @RabbitListener(
            queues = EventosConfig.COLA_RESULTADO,
            autoStartup = "${rental.eventos.habilitado:true}")
    public void alRecibirResultado(Message mensaje, Channel channel) throws IOException {

        String eventId;
        long reservaId;
        EstadoMantenimiento resultado;
        long deliveryTag = mensaje.getMessageProperties().getDeliveryTag();

        try {
            JsonNode sobre = JSON.readTree(new String(mensaje.getBody(), StandardCharsets.UTF_8));
            JsonNode payload = sobre.path("payload");

            eventId = sobre.path("eventId").asString("");
            reservaId = payload.path("reservaId").asLong();
            resultado = EstadoMantenimiento.valueOf(payload.path("resultado").asString(""));

            if (reservaId <= 0 || resultado == EstadoMantenimiento.PENDIENTE) {
                throw new IllegalArgumentException("reservaId o resultado invalidos");
            }

        } catch (RuntimeException e) {
            log.error("Se descarta un mantenimiento.resultado mal formado: {}", e.getMessage());
            channel.basicReject(deliveryTag, false);
            return;
        }

        try {
            boolean guardado = mantenimientoService.registrarResultado(eventId, reservaId, resultado);

            if (guardado) {
                log.info("Mantenimiento de la reserva {}: {} (evento {})", reservaId, resultado, eventId);
            }
            channel.basicAck(deliveryTag, false);
        } catch (RuntimeException e) {
            log.error("No se pudo guardar mantenimiento.resultado; se reintentara: {}", e.getMessage());
            channel.basicNack(deliveryTag, false, true);
        }
    }
}
