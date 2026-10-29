package com.rentar.vehicle.events;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.rentar.vehicle.entity.SolicitudVehiculoExterno;
import com.rentar.vehicle.entity.enums.EstadoIncorporacionExterna;
import com.rentar.vehicle.entity.enums.ResultadoValidacionExterna;
import com.rentar.vehicle.repository.SolicitudVehiculoExternoRepository;

/**
 * Recupera solicitudes externas que permanecen pendientes.
 *
 * La revision comienza cinco minutos despues de iniciar
 * el servicio y se repite cada quince minutos.
 */
@Configuration
@EnableScheduling
public class RecuperacionSolicitudesExternas {

    private static final Logger log =
            LoggerFactory.getLogger(
                    RecuperacionSolicitudesExternas.class);

    private final SolicitudVehiculoExternoRepository solicitudRepository;

    private final VehiculoExternoRegistradoPublisher registradoPublisher;

    private final VehiculoExternoValidacionCompletadaPublisher completadaPublisher;

    private final boolean habilitado;

    public RecuperacionSolicitudesExternas(
            SolicitudVehiculoExternoRepository solicitudRepository,
            VehiculoExternoRegistradoPublisher registradoPublisher,
            VehiculoExternoValidacionCompletadaPublisher completadaPublisher,
            @Value("${vehicle.eventos.habilitado:true}")
            boolean habilitado) {

        this.solicitudRepository = solicitudRepository;
        this.registradoPublisher = registradoPublisher;
        this.completadaPublisher = completadaPublisher;
        this.habilitado = habilitado;
    }

    @Scheduled(
            initialDelayString =
                    "${vehicle.externos.recuperacion.delay-inicial-ms:300000}",
            fixedDelayString =
                    "${vehicle.externos.recuperacion.intervalo-ms:900000}")
    public void recuperarPendientes() {

        if (!habilitado) {
            return;
        }

        try {
            List<SolicitudVehiculoExterno> solicitudes =
                    solicitudRepository.findAll();

            for (SolicitudVehiculoExterno solicitud : solicitudes) {

                if (solicitud.getEstado()
                        != EstadoIncorporacionExterna.PENDIENTE) {
                    continue;
                }

                try {
                    recuperarSolicitud(solicitud);

                } catch (RuntimeException exception) {

                    log.error(
                            "Error al recuperar solicitud externa {}",
                            solicitud.getId(),
                            exception);
                }
            }

        } catch (RuntimeException exception) {

            log.error(
                    "No se pudieron consultar solicitudes pendientes",
                    exception);
        }
    }

    private void recuperarSolicitud(
            SolicitudVehiculoExterno solicitud) {

        if (solicitud.getResultadoPatente()
                    == ResultadoValidacionExterna.RECHAZADO
                || solicitud.getResultadoTitularidad()
                    == ResultadoValidacionExterna.RECHAZADO) {

            return;
        }

        String correlationId =
                generarCorrelationId(solicitud.getId());

        boolean patenteValidada =
                solicitud.getResultadoPatente()
                        == ResultadoValidacionExterna.VALIDADO;

        boolean titularidadValidada =
                solicitud.getResultadoTitularidad()
                        == ResultadoValidacionExterna.VALIDADO;

        if (patenteValidada && titularidadValidada) {

            completadaPublisher.publicar(
                    new VehiculoExternoValidacionCompletadaEvent(
                            solicitud.getId(),
                            correlationId));

            log.info(
                    "Se republico validacion completada de solicitud {}",
                    solicitud.getId());

        } else {

            registradoPublisher.publicar(
                    new VehiculoExternoRegistradoEvent(
                            solicitud.getId(),
                            solicitud.getClienteId(),
                            solicitud.getPatente()));

            log.info(
                    "Se republico registro de solicitud externa {}",
                    solicitud.getId());
        }
    }

    private String generarCorrelationId(Long solicitudId) {

        return UUID.nameUUIDFromBytes(
                ("vehiculo-externo:" + solicitudId)
                        .getBytes(StandardCharsets.UTF_8))
                .toString();
    }
}