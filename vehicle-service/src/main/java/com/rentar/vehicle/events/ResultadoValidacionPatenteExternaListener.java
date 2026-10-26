package com.rentar.vehicle.events;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;
import com.rentar.vehicle.entity.enums.ResultadoValidacionExterna;
import com.rentar.vehicle.service.SolicitudVehiculoExternoService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Procesa el resultado de patente de un vehiculo externo. */
@Component
public class ResultadoValidacionPatenteExternaListener {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ResultadoValidacionPatenteExternaListener.class);

    private static final JsonMapper JSON =
            JsonMapper.builder().build();

    private final SolicitudVehiculoExternoService solicitudService;

    public ResultadoValidacionPatenteExternaListener(
            SolicitudVehiculoExternoService solicitudService) {

        this.solicitudService = solicitudService;
    }

    @RabbitListener(
            queues = EventosConfig.COLA_EXTERNO_PATENTE,
            autoStartup = "${vehicle.eventos.habilitado:true}")
    public void consumir(Message mensaje, Channel channel)
            throws IOException {

        long deliveryTag =
                mensaje.getMessageProperties().getDeliveryTag();

        Long vehiculoExternoId;
        String patente;
        String correlationId;
        ResultadoValidacionExterna resultado;

        try {
            JsonNode sobre = JSON.readTree(
                    new String(
                            mensaje.getBody(),
                            StandardCharsets.UTF_8));

            JsonNode payload = sobre.path("payload");

            String eventId =
                    sobre.path("eventId").asString("");

            String eventType =
                    sobre.path("eventType").asString("");

            correlationId =
                    sobre.path("correlationId").asString("");

            vehiculoExternoId =
                    payload.path("vehiculoExternoId").asLong();

            patente =
                    payload.path("patente").asString("");

            resultado = ResultadoValidacionExterna.valueOf(
                    payload.path("resultado").asString(""));

            if (!EventosConfig.RK_EXTERNO_VALIDACION_PATENTE
                    .equals(eventType)
                    || eventId.isBlank()
                    || correlationId.isBlank()
                    || vehiculoExternoId <= 0
                    || patente.isBlank()) {

                throw new IllegalArgumentException(
                        "Evento de validacion de patente invalido");
            }

        } catch (Exception exception) {

            log.warn(
                    "Se descarta mensaje de patente externa invalido: {}",
                    exception.getMessage());

            channel.basicReject(deliveryTag, false);
            return;
        }

        try {
            solicitudService.procesarResultadoPatente(
                    vehiculoExternoId,
                    patente,
                    resultado,
                    correlationId);

            channel.basicAck(deliveryTag, false);

            log.info(
                    "Validacion de patente procesada para solicitud {}: {}",
                    vehiculoExternoId,
                    resultado);

        } catch (IllegalArgumentException exception) {

            log.warn(
                    "Se descarta resultado de patente externa: {}",
                    exception.getMessage());

            channel.basicReject(deliveryTag, false);

        } catch (RuntimeException exception) {

            log.error(
                    "Error al procesar patente externa {}",
                    vehiculoExternoId,
                    exception);

            channel.basicNack(deliveryTag, false, true);
        }
    }
}