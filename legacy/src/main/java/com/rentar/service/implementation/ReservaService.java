package com.rentar.service.implementation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rentar.dto.ReservaRequest;
import com.rentar.entity.Cliente;
import com.rentar.entity.Reserva;
import com.rentar.entity.Vehiculo;
import com.rentar.entity.enums.EstadoReserva;
import com.rentar.exception.ClienteInactivoException;
import com.rentar.exception.ClienteNoEncontradoException;
import com.rentar.exception.ReservaEstadoInvalidoException;
import com.rentar.exception.ReservaInvalidaException;
import com.rentar.exception.ReservaNoEncontradaException;
import com.rentar.exception.VehiculoInactivoException;
import com.rentar.exception.VehiculoNoDisponibleException;
import com.rentar.exception.VehiculoNoEncontradoException;
import com.rentar.repository.ClienteRepository;
import com.rentar.repository.ReservaRepository;
import com.rentar.repository.VehiculoRepository;
import com.rentar.service.IReservaService;

@Service
public class ReservaService implements IReservaService {

    private final ReservaRepository reservaRepository;
    private final ClienteRepository clienteRepository;
    private final VehiculoRepository vehiculoRepository;

    public ReservaService(
            ReservaRepository reservaRepository,
            ClienteRepository clienteRepository,
            VehiculoRepository vehiculoRepository) {

        this.reservaRepository = reservaRepository;
        this.clienteRepository = clienteRepository;
        this.vehiculoRepository = vehiculoRepository;
    }

    // ALTA
    @Override
    @Transactional
    public Reserva crear(ReservaRequest request) {

        // La fecha de inicio futura ya se valida a nivel DTO con @Future.
        if (!request.getFechaFin().isAfter(request.getFechaInicio())) {
            throw new ReservaInvalidaException(
                    "La fecha de fin debe ser posterior a la fecha de inicio");
        }

        Cliente cliente = clienteRepository.findById(request.getClienteId())
                .orElseThrow(() -> new ClienteNoEncontradoException(
                        "No existe un cliente con id " + request.getClienteId()));

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ClienteInactivoException(
                    "El cliente " + cliente.getId() + " se encuentra inactivo");
        }

        Vehiculo vehiculo = vehiculoRepository.findById(request.getVehiculoId())
                .orElseThrow(() -> new VehiculoNoEncontradoException(
                        "No existe un vehiculo con id " + request.getVehiculoId()));

        if (!vehiculo.isActivo()) {
            throw new VehiculoInactivoException(
                    "El vehiculo " + vehiculo.getId() + " se encuentra inactivo");
        }

        boolean ocupado = reservaRepository.existsSolapamiento(
                vehiculo.getId(),
                request.getFechaInicio(),
                request.getFechaFin(),
                EstadoReserva.CONFIRMADA);

        if (ocupado) {
            throw new VehiculoNoDisponibleException(
                    "El vehiculo no esta disponible en el periodo solicitado");
        }

        long dias = Math.max(1, ChronoUnit.DAYS.between(
                request.getFechaInicio().toLocalDate(),
                request.getFechaFin().toLocalDate()));

        BigDecimal importeTotal = vehiculo.getPrecioDiario()
                .multiply(BigDecimal.valueOf(dias));

        Reserva reserva = new Reserva();
        reserva.setCliente(cliente);
        reserva.setVehiculo(vehiculo);
        reserva.setFechaInicio(request.getFechaInicio());
        reserva.setFechaFin(request.getFechaFin());
        reserva.setPrecioDiario(vehiculo.getPrecioDiario());
        reserva.setCantidadDias((int) dias);
        reserva.setImporteTotal(importeTotal);
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reserva.setFechaAlta(LocalDateTime.now());

        return reservaRepository.save(reserva);
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
    public Reserva buscarPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new ReservaNoEncontradaException(
                        "No existe una reserva con id " + id));
    }
}
