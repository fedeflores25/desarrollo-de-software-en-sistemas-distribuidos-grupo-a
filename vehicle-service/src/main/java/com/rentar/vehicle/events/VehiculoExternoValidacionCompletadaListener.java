package com.rentar.vehicle.events;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.rabbitmq.client.Channel;
import com.rentar.vehicle.entity.SolicitudVehiculoExterno;
import com.rentar.vehicle.entity.enums.EstadoIncorporacionExterna;
import com.rentar.vehicle.exception.SolicitudVehiculoExternoNoEncontradaException;
import com.rentar.vehicle.repository.SolicitudVehiculoExternoRepository;
import com.rentar.vehicle.repository.VehicleRepository;
import com.rentar.vehicle.service.SolicitudVehiculoExternoService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Incorpora un vehiculo externo cuando ambas validaciones
 * fueron aprobadas.
 *
 * Ante un conflicto de patente, registra el rechazo
 * en una transaccion independiente.
 */
@Component
public class VehiculoExternoValidacionCompletadaListener {

    private static final Logger log =
            LoggerFactory.getLogger(
                    VehiculoExternoValidacionCompletadaListener.class);

    private static final JsonMapper JSON =
            JsonMapper.builder().build();

    private final SolicitudVehiculoExternoService solicitudService;

    private final SolicitudVehiculoExternoRepository solicitudRepository;

    private final VehicleRepository vehicleRepository;

    private final TransactionTemplate transaccionNueva;

    public VehiculoExternoValidacionCompletadaListener(
            SolicitudVehiculoExternoService solicitudService,
            SolicitudVehiculoExternoRepository solicitudRepository,
            VehicleRepository vehicleRepository,
            PlatformTransactionManager transactionManager) {

        this.solicitudService = solicitudService;
        this.solicitudRepository = solicitudRepository;
        this.vehicleRepository = vehicleRepository;

        this.transaccionNueva =
                new TransactionTemplate(transactionManager);

        this.transaccionNueva.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @RabbitListener(
            queues = EventosConfig.COLA_EXTERNO_COMPLETADA,
            autoStartup = "${vehicle.eventos.habilitado:true}")
    public void consumir(Message mensaje, Channel channel)
            throws IOException {

        long deliveryTag =
                mensaje.getMessageProperties().getDeliveryTag();

        Long vehiculoExternoId;

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

            String correlationId =
                    sobre.path("correlationId").asString("");

            vehiculoExternoId =
                    payload.path("vehiculoExternoId").asLong();

            if (!EventosConfig.RK_EXTERNO_VALIDACION_COMPLETADA
                    .equals(eventType)
                    || eventId.isBlank()
                    || correlationId.isBlank()
                    || vehiculoExternoId <= 0) {

                throw new IllegalArgumentException(
                        "Evento de incorporacion completada invalido");
            }

            String correlationIdEsperado =
                    UUID.nameUUIDFromBytes(
                            ("vehiculo-externo:" + vehiculoExternoId)
                                    .getBytes(StandardCharsets.UTF_8))
                            .toString();

            if (!correlationIdEsperado.equals(correlationId)) {

                throw new IllegalArgumentException(
                        "El correlationId no corresponde a la solicitud "
                                + vehiculoExternoId);
            }

        } catch (Exception exception) {

            log.warn(
                    "Se descarta mensaje de incorporacion invalido: {}",
                    exception.getMessage());

            channel.basicReject(deliveryTag, false);
            return;
        }

        try {
            solicitudService.incorporar(vehiculoExternoId);

            channel.basicAck(deliveryTag, false);

            log.info(
                    "Incorporacion externa procesada: {}",
                    vehiculoExternoId);

        } catch (SolicitudVehiculoExternoNoEncontradaException exception) {

            log.warn(
                    "Se descarta incorporacion de solicitud inexistente: {}",
                    exception.getMessage());

            channel.basicReject(deliveryTag, false);

        } catch (IllegalArgumentException exception) {

            log.warn(
                    "Se descarta incorporacion invalida: {}",
                    exception.getMessage());

            channel.basicReject(deliveryTag, false);

        } catch (RuntimeException exception) {

            log.error(
                    "Fallo al incorporar vehiculo externo {}",
                    vehiculoExternoId,
                    exception);

            boolean resuelto = false;

            try {
                resuelto =
                        resolverConflictoDePatente(vehiculoExternoId);

            } catch (RuntimeException errorRecuperacion) {

                log.error(
                        "No se pudo revisar el conflicto de patente "
                                + "de la solicitud {}",
                        vehiculoExternoId,
                        errorRecuperacion);
            }

            if (resuelto) {

                channel.basicAck(deliveryTag, false);

                log.info(
                        "Conflicto resuelto o solicitud finalizada: {}",
                        vehiculoExternoId);

            } else {

                log.warn(
                        "Se descarta entrega fallida de solicitud {}. "
                                + "La recuperacion programada revisara "
                                + "las solicitudes pendientes.",
                        vehiculoExternoId);

                channel.basicReject(deliveryTag, false);
            }
        }
    }

    /**
     * Se ejecuta despues de que la transaccion fallida
     * de incorporar() ya termino.
     *
     * Si otra solicitud incorporo la misma patente,
     * se rechaza esta solicitud en una nueva transaccion.
     */
    private boolean resolverConflictoDePatente(
            Long vehiculoExternoId) {

        Boolean resuelto = transaccionNueva.execute(status -> {

            SolicitudVehiculoExterno solicitud =
                    solicitudRepository
                            .buscarParaActualizar(vehiculoExternoId)
                            .orElse(null);

            if (solicitud == null) {
                return false;
            }

            if (solicitud.getEstado()
                    == EstadoIncorporacionExterna.INCORPORADA
                    || solicitud.getEstado()
                    == EstadoIncorporacionExterna.RECHAZADA) {

                return true;
            }

            if (solicitud.getEstado()
                    != EstadoIncorporacionExterna.PENDIENTE) {

                return false;
            }

            boolean patenteExistente =
                    vehicleRepository.existsByPatente(
                            solicitud.getPatente());

            if (!patenteExistente) {
                return false;
            }

            solicitud.setEstado(
                    EstadoIncorporacionExterna.RECHAZADA);

            solicitudRepository.save(solicitud);

            log.warn(
                    "Solicitud externa {} rechazada: "
                            + "la patente {} ya pertenece "
                            + "a otro vehiculo",
                    solicitud.getId(),
                    solicitud.getPatente());

            return true;
        });

        return Boolean.TRUE.equals(resuelto);
    }
}