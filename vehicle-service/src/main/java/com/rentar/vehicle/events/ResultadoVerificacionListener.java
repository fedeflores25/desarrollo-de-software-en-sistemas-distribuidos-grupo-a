package com.rentar.vehicle.events;

import com.rabbitmq.client.Channel;
import com.rentar.vehicle.entity.enums.ResultadoVerificacion;
import com.rentar.vehicle.service.VehicleService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Aplica la respuesta policial y confirma RabbitMQ solo despues de persistirla. */
@Component
public class ResultadoVerificacionListener {

    private static final Logger log = LoggerFactory.getLogger(ResultadoVerificacionListener.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final VehicleService vehicleService;

    public ResultadoVerificacionListener(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @RabbitListener(queues = EventosConfig.COLA_RESULTADO, autoStartup = "${vehicle.eventos.habilitado:true}")
    public void consumir(Message mensaje, Channel channel) throws IOException {
        long deliveryTag = mensaje.getMessageProperties().getDeliveryTag();
        try {
            JsonNode sobre = JSON.readTree(new String(mensaje.getBody(), StandardCharsets.UTF_8));
            JsonNode payload = sobre.path("payload");
            String eventId = sobre.path("eventId").asString("");
            long vehiculoId = payload.path("vehiculoId").asLong();
            String patente = payload.path("patente").asString("");
            ResultadoVerificacion resultado = ResultadoVerificacion.valueOf(payload.path("resultado").asString(""));

            if (!EventosConfig.RK_VERIFICACION_RESULTADO.equals(sobre.path("eventType").asString())
                    || eventId.isBlank() || vehiculoId <= 0 || patente.isBlank()) {
                throw new IllegalArgumentException("Resultado de verificacion invalido");
            }

            try {
                vehicleService.procesarResultadoVerificacion(eventId, vehiculoId, patente, resultado);
                channel.basicAck(deliveryTag, false);
            } catch (IllegalArgumentException exception) {
                log.warn("Se descarta resultado de verificacion invalido: {}", exception.getMessage());
                channel.basicReject(deliveryTag, false);
            } catch (RuntimeException exception) {
                log.error("No se pudo procesar resultado de verificacion; se reintentara", exception);
                channel.basicNack(deliveryTag, false, true);
            }
        } catch (Exception exception) {
            log.warn("Se descarta mensaje de verificacion mal formado: {}", exception.getMessage());
            channel.basicReject(deliveryTag, false);
        }
    }
}
