package com.rentar.titularidad.event;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;
import com.rentar.titularidad.service.RegistroTitularidadSimulado;
import com.rentar.titularidad.service.RegistroTitularidadSimulado.Titular;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Consume solicitudes externas y verifica la titularidad. */
@Component
public class VehiculoExternoRegistradoListener {

    private static final Logger log =
            LoggerFactory.getLogger(
                    VehiculoExternoRegistradoListener.class);

    private static final JsonMapper JSON =
            JsonMapper.builder().build();

    private final RegistroTitularidadSimulado registro;
    private final TitularidadResultadoPublisher resultadoPublisher;

    public VehiculoExternoRegistradoListener(
            RegistroTitularidadSimulado registro,
            TitularidadResultadoPublisher resultadoPublisher) {

        this.registro = registro;
        this.resultadoPublisher = resultadoPublisher;
    }

    @RabbitListener(
            queues = EventosConfig.COLA_EXTERNO_REGISTRADO,
            autoStartup = "${titularidad.eventos.habilitado:true}")
    public void consumir(Message mensaje, Channel channel)
            throws IOException {

        long deliveryTag =
                mensaje.getMessageProperties().getDeliveryTag();

        Long vehiculoExternoId;
        Long clienteId;
        String patente;
        String correlationId;

        try {
            JsonNode evento = JSON.readTree(
                    new String(
                            mensaje.getBody(),
                            StandardCharsets.UTF_8));

            JsonNode payload = evento.path("payload");

            String eventId =
                    evento.path("eventId").asString("");

            String eventType =
                    evento.path("eventType").asString("");

            correlationId =
                    evento.path("correlationId").asString("");

            vehiculoExternoId =
                    payload.path("vehiculoExternoId").asLong();

            clienteId =
                    payload.path("clienteId").asLong();

            patente =
                    payload.path("patente").asString("");

            if (!EventosConfig.EXTERNO_REGISTRADO.equals(eventType)
                    || eventId.isBlank()
                    || correlationId.isBlank()
                    || vehiculoExternoId <= 0
                    || clienteId <= 0
                    || patente.isBlank()) {

                throw new IllegalArgumentException(
                        "Evento de vehiculo externo invalido");
            }

        } catch (Exception exception) {

            log.warn(
                    "Se descarta solicitud externa invalida: {}",
                    exception.getMessage());

            channel.basicReject(deliveryTag, false);
            return;
        }

        try {
            Titular titular = registro.buscarTitular(patente);

            boolean validado =
                    titular != null
                    && titular.getClienteId().equals(clienteId);

            String resultado =
                    validado ? "VALIDADO" : "RECHAZADO";

            String documento =
                    titular != null
                            ? titular.getDocumento()
                            : "NO_REGISTRADO";

            resultadoPublisher.publicar(
                    vehiculoExternoId,
                    clienteId,
                    documento,
                    patente,
                    resultado,
                    correlationId);

            channel.basicAck(deliveryTag, false);

            log.info(
                    "Titularidad procesada para solicitud {}: {}",
                    vehiculoExternoId,
                    resultado);

        } catch (RuntimeException exception) {

            log.error(
                    "Error al procesar titularidad de solicitud {}",
                    vehiculoExternoId,
                    exception);

            channel.basicNack(deliveryTag, false, true);
        }
    }
}