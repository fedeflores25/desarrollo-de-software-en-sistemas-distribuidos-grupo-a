package com.rentar.vehicle.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rentar.vehicle.dto.SolicitudVehiculoExternoRequest;
import com.rentar.vehicle.entity.SolicitudVehiculoExterno;
import com.rentar.vehicle.entity.Vehiculo;
import com.rentar.vehicle.entity.enums.EstadoIncorporacionExterna;
import com.rentar.vehicle.entity.enums.EstadoVehiculo;
import com.rentar.vehicle.entity.enums.ResultadoValidacionExterna;
import com.rentar.vehicle.events.VehiculoExternoRegistradoEvent;
import com.rentar.vehicle.events.VehiculoExternoValidacionCompletadaEvent;
import com.rentar.vehicle.exception.PatenteDuplicadaException;
import com.rentar.vehicle.exception.SolicitudVehiculoExternoNoEncontradaException;
import com.rentar.vehicle.repository.SolicitudVehiculoExternoRepository;
import com.rentar.vehicle.repository.VehicleRepository;

@Service
public class SolicitudVehiculoExternoServiceImpl
        implements SolicitudVehiculoExternoService {

    private static final BigDecimal PRECIO_MAXIMO =
            new BigDecimal("99999999.99");

    private final SolicitudVehiculoExternoRepository solicitudRepository;
    private final VehicleRepository vehicleRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SolicitudVehiculoExternoServiceImpl(
            SolicitudVehiculoExternoRepository solicitudRepository,
            VehicleRepository vehicleRepository,
            ApplicationEventPublisher eventPublisher) {

        this.solicitudRepository = solicitudRepository;
        this.vehicleRepository = vehicleRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public SolicitudVehiculoExterno create(
            SolicitudVehiculoExternoRequest request) {

        validarSolicitud(request);

        String patente = textoObligatorio(
                request.getPatente(), "La patente", 15)
                .toUpperCase(Locale.ROOT);

        if (vehicleRepository.existsByPatente(patente)) {
            throw new PatenteDuplicadaException(
                    "Ya existe un vehiculo con la patente " + patente);
        }

        SolicitudVehiculoExterno solicitud =
                new SolicitudVehiculoExterno();

        solicitud.setClienteId(request.getClienteId());

        solicitud.setDocumento(
                textoObligatorio(
                        request.getDocumento(), "El documento", 20));

        solicitud.setPatente(patente);

        solicitud.setMarca(
                textoObligatorio(
                        request.getMarca(), "La marca", 60));

        solicitud.setModelo(
                textoObligatorio(
                        request.getModelo(), "El modelo", 60));

        solicitud.setAnio(request.getAnio());

        solicitud.setColor(
                textoOpcional(
                        request.getColor(), "El color", 40));

        solicitud.setTipo(request.getTipo());

        solicitud.setPrecioDiario(
                request.getPrecioDiario()
                        .setScale(2, RoundingMode.UNNECESSARY));

        solicitud.setEstado(EstadoIncorporacionExterna.PENDIENTE);

        SolicitudVehiculoExterno guardada =
                solicitudRepository.save(solicitud);

        eventPublisher.publishEvent(
                new VehiculoExternoRegistradoEvent(
                        guardada.getId(),
                        guardada.getClienteId(),
                        guardada.getPatente()));

        return guardada;
    }

    @Override
    public SolicitudVehiculoExterno getById(Long id) {

        validarId(id);

        return solicitudRepository.findById(id)
                .orElseThrow(() ->
                        new SolicitudVehiculoExternoNoEncontradaException(
                                "No existe una solicitud externa con id " + id));
    }

    @Override
    public List<SolicitudVehiculoExterno> list() {
        return solicitudRepository.findAll();
    }

    @Override
    @Transactional
    public void procesarResultadoPatente(
            Long vehiculoExternoId,
            String patente,
            ResultadoValidacionExterna resultado,
            String correlationId) {

        validarDatos(vehiculoExternoId, resultado, correlationId);

        SolicitudVehiculoExterno solicitud =
                buscarParaActualizar(vehiculoExternoId);

        if (!Objects.equals(solicitud.getPatente(), patente)) {
            throw new IllegalArgumentException(
                    "La patente no coincide con la solicitud");
        }

        if (solicitud.getEstado()
                != EstadoIncorporacionExterna.PENDIENTE) {
            return;
        }

        if (solicitud.getResultadoPatente() != null) {
            if (solicitud.getResultadoPatente() != resultado) {
                throw new IllegalArgumentException(
                        "Resultado de patente contradictorio");
            }
            return;
        }

        solicitud.setResultadoPatente(resultado);

        if (resultado == ResultadoValidacionExterna.RECHAZADO) {
            solicitud.setEstado(EstadoIncorporacionExterna.RECHAZADA);
        }

        solicitudRepository.save(solicitud);

        publicarSiEstaValidada(solicitud, correlationId);
    }

    @Override
    @Transactional
    public void procesarResultadoTitularidad(
            Long vehiculoExternoId,
            Long clienteId,
            String documento,
            String patente,
            ResultadoValidacionExterna resultado,
            String correlationId) {

        validarDatos(vehiculoExternoId, resultado, correlationId);

        SolicitudVehiculoExterno solicitud =
                buscarParaActualizar(vehiculoExternoId);

        if (!Objects.equals(solicitud.getClienteId(), clienteId)
                || !Objects.equals(solicitud.getPatente(), patente)) {

            throw new IllegalArgumentException(
                    "Los datos no coinciden con la solicitud");
        }

        if (solicitud.getEstado()
                != EstadoIncorporacionExterna.PENDIENTE) {
            return;
        }

        boolean documentoCoincide =
                Objects.equals(solicitud.getDocumento(), documento);

        ResultadoValidacionExterna resultadoFinal =
                resultado == ResultadoValidacionExterna.VALIDADO
                        && documentoCoincide
                        ? ResultadoValidacionExterna.VALIDADO
                        : ResultadoValidacionExterna.RECHAZADO;

        if (solicitud.getResultadoTitularidad() != null) {
            if (solicitud.getResultadoTitularidad()
                    != resultadoFinal) {

                throw new IllegalArgumentException(
                        "Resultado de titularidad contradictorio");
            }
            return;
        }

        solicitud.setResultadoTitularidad(resultadoFinal);

        if (resultadoFinal
                == ResultadoValidacionExterna.RECHAZADO) {

            solicitud.setEstado(EstadoIncorporacionExterna.RECHAZADA);
        }

        solicitudRepository.save(solicitud);

        publicarSiEstaValidada(solicitud, correlationId);
    }

    @Override
    @Transactional
    public void incorporar(Long vehiculoExternoId) {

        validarId(vehiculoExternoId);

        SolicitudVehiculoExterno solicitud =
                buscarParaActualizar(vehiculoExternoId);

        if (solicitud.getEstado()
                == EstadoIncorporacionExterna.INCORPORADA) {
            return;
        }

        if (solicitud.getEstado()
                == EstadoIncorporacionExterna.RECHAZADA) {
            return;
        }

        if (solicitud.getResultadoPatente()
                    != ResultadoValidacionExterna.VALIDADO
                || solicitud.getResultadoTitularidad()
                    != ResultadoValidacionExterna.VALIDADO) {

            throw new IllegalArgumentException(
                    "La solicitud no tiene ambas validaciones aprobadas");
        }

        if (vehicleRepository.existsByPatente(solicitud.getPatente())) {
            solicitud.setEstado(EstadoIncorporacionExterna.RECHAZADA);
            solicitudRepository.save(solicitud);
            return;
        }

        Vehiculo vehiculo = new Vehiculo();

        vehiculo.setPatente(solicitud.getPatente());
        vehiculo.setMarca(solicitud.getMarca());
        vehiculo.setModelo(solicitud.getModelo());
        vehiculo.setAnio(solicitud.getAnio());
        vehiculo.setColor(solicitud.getColor());
        vehiculo.setTipo(solicitud.getTipo());
        vehiculo.setPrecioDiario(solicitud.getPrecioDiario());

        vehiculo.setEstado(EstadoVehiculo.DISPONIBLE);
        vehiculo.setActivo(true);

        Vehiculo guardado = vehicleRepository.save(vehiculo);

        solicitud.setVehiculoId(guardado.getId());
        solicitud.setEstado(EstadoIncorporacionExterna.INCORPORADA);

        solicitudRepository.save(solicitud);
    }

    private SolicitudVehiculoExterno buscarParaActualizar(Long id) {

        validarId(id);

        return solicitudRepository.buscarParaActualizar(id)
                .orElseThrow(() ->
                        new SolicitudVehiculoExternoNoEncontradaException(
                                "No existe una solicitud externa con id " + id));
    }

    private void validarSolicitud(
            SolicitudVehiculoExternoRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "La solicitud es obligatoria");
        }

        if (request.getClienteId() == null
                || request.getClienteId() <= 0) {

            throw new IllegalArgumentException(
                    "El cliente debe ser valido");
        }

        textoObligatorio(
                request.getDocumento(), "El documento", 20);

        textoObligatorio(
                request.getPatente(), "La patente", 15);

        textoObligatorio(
                request.getMarca(), "La marca", 60);

        textoObligatorio(
                request.getModelo(), "El modelo", 60);

        textoOpcional(
                request.getColor(), "El color", 40);

        if (request.getAnio() == null
                || request.getAnio() < 1900) {

            throw new IllegalArgumentException(
                    "El anio debe ser mayor o igual a 1900");
        }

        if (request.getTipo() == null) {
            throw new IllegalArgumentException(
                    "El tipo de vehiculo es obligatorio");
        }

        BigDecimal precio = request.getPrecioDiario();

        if (precio == null
                || precio.signum() <= 0
                || precio.compareTo(PRECIO_MAXIMO) > 0
                || precio.stripTrailingZeros().scale() > 2) {

            throw new IllegalArgumentException(
                    "El precio diario debe ser positivo, "
                    + "tener hasta dos decimales y no superar "
                    + PRECIO_MAXIMO);
        }
    }

    private String textoObligatorio(
            String valor,
            String nombre,
            int longitudMaxima) {

        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    nombre + " es obligatorio");
        }

        String limpio = valor.trim();

        if (limpio.length() > longitudMaxima) {
            throw new IllegalArgumentException(
                    nombre + " no puede superar "
                            + longitudMaxima + " caracteres");
        }

        return limpio;
    }

    private String textoOpcional(
            String valor,
            String nombre,
            int longitudMaxima) {

        if (valor == null || valor.isBlank()) {
            return null;
        }

        String limpio = valor.trim();

        if (limpio.length() > longitudMaxima) {
            throw new IllegalArgumentException(
                    nombre + " no puede superar "
                            + longitudMaxima + " caracteres");
        }

        return limpio;
    }

    private void validarId(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "El id de la solicitud debe ser mayor a cero");
        }
    }

    private void validarDatos(
            Long vehiculoExternoId,
            ResultadoValidacionExterna resultado,
            String correlationId) {

        validarId(vehiculoExternoId);

        if (resultado == null
                || correlationId == null
                || correlationId.isBlank()) {

            throw new IllegalArgumentException(
                    "Los datos de validacion son invalidos");
        }

        String correlationIdEsperado = UUID.nameUUIDFromBytes(
                ("vehiculo-externo:" + vehiculoExternoId)
                        .getBytes(StandardCharsets.UTF_8))
                .toString();

        if (!correlationIdEsperado.equals(correlationId)) {
            throw new IllegalArgumentException(
                    "El correlationId no corresponde a la solicitud "
                            + vehiculoExternoId);
        }
    }

    private void publicarSiEstaValidada(
            SolicitudVehiculoExterno solicitud,
            String correlationId) {

        if (solicitud.getEstado()
                != EstadoIncorporacionExterna.PENDIENTE) {
            return;
        }

        if (solicitud.getResultadoPatente()
                    == ResultadoValidacionExterna.VALIDADO
                && solicitud.getResultadoTitularidad()
                    == ResultadoValidacionExterna.VALIDADO) {

            eventPublisher.publishEvent(
                    new VehiculoExternoValidacionCompletadaEvent(
                            solicitud.getId(),
                            correlationId));
        }
    }
}