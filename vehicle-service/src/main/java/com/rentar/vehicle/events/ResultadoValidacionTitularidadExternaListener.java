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

/** Procesa el resultado de titularidad de un vehiculo externo. */
@Component
public class ResultadoValidacionTitularidadExternaListener {

    private static final Logger log =
            LoggerFactory.getLogger(
                    ResultadoValidacionTitularidadExternaListener.class);

    private static final JsonMapper JSON =
            JsonMapper.builder().build();

    private final SolicitudVehiculoExternoService solicitudService;

    public ResultadoValidacionTitularidadExternaListener(
            SolicitudVehiculoExternoService solicitudService) {

        this.solicitudService = solicitudService;
    }

    @RabbitListener(
            queues = EventosConfig.COLA_EXTERNO_TITULARIDAD,
            autoStartup = "${vehicle.eventos.habilitado:true}")
    public void consumir(Message mensaje, Channel channel)
            throws IOException {

        long deliveryTag =
                mensaje.getMessageProperties().getDeliveryTag();

        Long vehiculoExternoId;
        Long clienteId;
        String documento;
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

            clienteId =
                    payload.path("clienteId").asLong();

            documento =
                    payload.path("documento").asString("");

            patente =
                    payload.path("patente").asString("");

            resultado = ResultadoValidacionExterna.valueOf(
                    payload.path("resultado").asString(""));

            if (!EventosConfig.RK_EXTERNO_VALIDACION_TITULARIDAD
                    .equals(eventType)
                    || eventId.isBlank()
                    || correlationId.isBlank()
                    || vehiculoExternoId <= 0
                    || clienteId <= 0
                    || documento.isBlank()
                    || patente.isBlank()) {

                throw new IllegalArgumentException(
                        "Evento de validacion de titularidad invalido");
            }

        } catch (Exception exception) {

            log.warn(
                    "Se descarta mensaje de titularidad invalido: {}",
                    exception.getMessage());

            channel.basicReject(deliveryTag, false);
            return;
        }

        try {
            solicitudService.procesarResultadoTitularidad(
                    vehiculoExternoId,
                    clienteId,
                    documento,
                    patente,
                    resultado,
                    correlationId);

            channel.basicAck(deliveryTag, false);

            log.info(
                    "Validacion de titularidad procesada para solicitud {}: {}",
                    vehiculoExternoId,
                    resultado);

        } catch (IllegalArgumentException exception) {

            log.warn(
                    "Se descarta resultado de titularidad: {}",
                    exception.getMessage());

            channel.basicReject(deliveryTag, false);

        } catch (RuntimeException exception) {

            log.error(
                    "Error al procesar titularidad externa {}",
                    vehiculoExternoId,
                    exception);

            channel.basicNack(deliveryTag, false, true);
        }
    }
}