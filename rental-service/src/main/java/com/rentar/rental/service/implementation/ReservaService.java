package com.rentar.rental.service.implementation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rentar.rental.entity.Reserva;
import com.rentar.rental.entity.enums.EstadoReserva;
import com.rentar.rental.exception.ReservaEstadoInvalidoException;
import com.rentar.rental.exception.ReservaInvalidaException;
import com.rentar.rental.exception.ReservaNoEncontradaException;
import com.rentar.rental.events.ReservaConfirmadaEvent;
import com.rentar.rental.exception.VehiculoNoDisponibleException;
import com.rentar.rental.repository.ReservaRepository;
import com.rentar.rental.service.FiltroReserva;
import com.rentar.rental.service.IMantenimientoService;
import com.rentar.rental.service.IReservaService;

@Service
public class ReservaService implements IReservaService {

    private final ReservaRepository reservaRepository;
    private final IMantenimientoService mantenimientoService;
    private final ApplicationEventPublisher eventos;

    public ReservaService(
            ReservaRepository reservaRepository,
            IMantenimientoService mantenimientoService,
            ApplicationEventPublisher eventos) {

        this.reservaRepository = reservaRepository;
        this.mantenimientoService = mantenimientoService;
        this.eventos = eventos;
    }

    // ALTA
    @Override
    @Transactional
    public Reserva crear(Long clienteId, Long vehiculoId, LocalDateTime fechaInicio,
            LocalDateTime fechaFin, BigDecimal precioDiario) {

        if (clienteId == null || vehiculoId == null || fechaInicio == null
                || fechaFin == null || precioDiario == null) {
            throw new ReservaInvalidaException("Faltan datos obligatorios de la reserva");
        }

        if (precioDiario.signum() <= 0) {
            throw new ReservaInvalidaException("El precio diario debe ser mayor a cero");
        }

        LocalDateTime ahora = LocalDateTime.now();

        if (!fechaInicio.isAfter(ahora)) {
            throw new ReservaInvalidaException("La fecha de inicio debe ser futura");
        }

        if (!fechaFin.isAfter(fechaInicio)) {
            throw new ReservaInvalidaException(
                    "La fecha de fin debe ser posterior a la fecha de inicio");
        }

        boolean ocupado = reservaRepository.existsSolapamiento(
                vehiculoId, fechaInicio, fechaFin, EstadoReserva.CONFIRMADA);

        if (ocupado) {
            throw new VehiculoNoDisponibleException(
                    "El vehiculo no esta disponible en el periodo solicitado");
        }

        long dias = Math.max(1, ChronoUnit.DAYS.between(
                fechaInicio.toLocalDate(), fechaFin.toLocalDate()));

        BigDecimal precio = precioDiario.setScale(2, RoundingMode.HALF_UP);
        BigDecimal importeTotal = precio.multiply(BigDecimal.valueOf(dias));

        Reserva reserva = new Reserva();
        reserva.setClienteId(clienteId);
        reserva.setVehiculoId(vehiculoId);
        reserva.setFechaInicio(fechaInicio);
        reserva.setFechaFin(fechaFin);
        reserva.setPrecioDiario(precio);
        reserva.setCantidadDias((int) dias);
        reserva.setImporteTotal(importeTotal);
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reserva.setFechaAlta(ahora);

        Reserva guardada = reservaRepository.save(reserva);

        // El mantenimiento queda PENDIENTE en la misma transaccion. El evento se publica
        // recien cuando la transaccion se confirma y sin esperar el resultado.
        mantenimientoService.registrarPendiente(guardada.getId());

        eventos.publishEvent(new ReservaConfirmadaEvent(
                guardada.getId(), guardada.getVehiculoId(),
                guardada.getFechaInicio(), guardada.getFechaFin()));

        return guardada;
    }

    // CANCELACION
    @Override
    @Transactional
    public Reserva cancelar(Long id) {

        Reserva reserva = buscarPorId(id);

        if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new ReservaEstadoInvalidoException(
                    "Solo se pueden cancelar reservas en estado CONFIRMADA");
        }

        if (!reserva.getFechaInicio().isAfter(LocalDateTime.now())) {
            throw new ReservaEstadoInvalidoException(
                    "No se puede cancelar una reserva cuyo periodo ya comenzo");
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        reserva.setFechaCancelacion(LocalDateTime.now());

        return reservaRepository.save(reserva);
    }

    // CONSULTA POR ID
    @Override
    @Transactional(readOnly = true)
    public Reserva buscarPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new ReservaNoEncontradaException(
                        "No existe una reserva con id " + id));
    }

    // CONSULTA CON FILTROS
    @Override
    @Transactional
    public List<Reserva> listar(FiltroReserva filtro) {

        finalizarVencidas();

        FiltroReserva f = filtro != null
                ? filtro
                : new FiltroReserva(null, null, null, null, null);

        boolean filtrarVehiculos = f.vehiculoIds() != null && !f.vehiculoIds().isEmpty();
        // Una coleccion vacia no puede ir en un IN: se manda un valor que no coincide con nada.
        List<Long> vehiculoIds = filtrarVehiculos ? f.vehiculoIds() : List.of(-1L);

        return reservaRepository.buscarConFiltros(
                f.clienteId(), filtrarVehiculos, vehiculoIds, f.estado(), f.desde(), f.hasta());
    }

    // HISTORIAL
    @Override
    @Transactional
    public List<Reserva> historial(Long clienteId) {

        if (clienteId == null) {
            throw new ReservaInvalidaException("El cliente es obligatorio");
        }

        finalizarVencidas();

        return reservaRepository.findByClienteIdAndEstadoIn(
                clienteId, List.of(EstadoReserva.FINALIZADA, EstadoReserva.CANCELADA));
    }

    // VEHICULOS OCUPADOS EN UN PERIODO
    @Override
    @Transactional(readOnly = true)
    public List<Long> idsVehiculosOcupados(LocalDateTime fechaInicio, LocalDateTime fechaFin) {

        if (fechaInicio == null || fechaFin == null) {
            throw new ReservaInvalidaException("Las fechas del periodo son obligatorias");
        }

        if (!fechaFin.isAfter(fechaInicio)) {
            throw new ReservaInvalidaException(
                    "La fecha de fin debe ser posterior a la fecha de inicio");
        }

        return reservaRepository.findVehiculoIdsOcupados(
                fechaInicio, fechaFin, EstadoReserva.CONFIRMADA);
    }

    /**
     * Nada en el sistema marca una reserva como FINALIZADA cuando termina su periodo, asi que
     * antes de consultar se pasan a FINALIZADA las CONFIRMADAS que ya vencieron.
     */
    private void finalizarVencidas() {
        reservaRepository.actualizarEstadoVencidas(
                EstadoReserva.CONFIRMADA, EstadoReserva.FINALIZADA, LocalDateTime.now());
    }
}
