package com.rentar.service.implementation;

import com.rentar.dto.ReservaFiltro;
import com.rentar.dto.ConsultaReservaResponse;
import com.rentar.entity.Reserva;
import com.rentar.entity.enums.EstadoReserva;
import com.rentar.entity.enums.TipoVehiculo;
import com.rentar.repository.ReservaRepository;
import com.rentar.service.IConsultaReservaService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ConsultaReservaService implements IConsultaReservaService {

    private final ReservaRepository reservaRepository;

    public ConsultaReservaService(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    @Override
    public List<ConsultaReservaResponse> consultarReservas(ReservaFiltro filtro) {
        if (filtro == null) {
            filtro = new ReservaFiltro();
        }

        // =========================================================================
        // TODO (FUTURA AUTENTICACIÓN - VALIDACIÓN DE ROLES): 
        // Cuando se implemente JWT, interceptamos al usuario.
        // String rol = SecurityContextHolder.getContext().getAuthentication().getAuthorities()...;
        // Long idLogueado = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        //
        // if (rol.equals("CLIENTE")) {
        //     // Ignoramos lo que mande el frontend y lo forzamos a ver solo lo suyo
        //     filtro.setClienteId(idLogueado);
        // }
        // Si el rol es "ADMIN", dejamos pasar el filtro de clienteId tal cual vino.
        // =========================================================================

        // Convertimos los Strings que vienen de GraphQL a los tipos reales de Java
        TipoVehiculo tipo = filtro.getTipoVehiculo() != null ? TipoVehiculo.valueOf(filtro.getTipoVehiculo()) : null;
        EstadoReserva estado = filtro.getEstado() != null ? EstadoReserva.valueOf(filtro.getEstado()) : null;
        LocalDateTime fechaIn = filtro.getFechaInicio() != null ? LocalDateTime.parse(filtro.getFechaInicio()) : null;
        LocalDateTime fechaFi = filtro.getFechaFin() != null ? LocalDateTime.parse(filtro.getFechaFin()) : null;

        List<Reserva> registros = reservaRepository.buscarConFiltrosDinamicos(
                filtro.getClienteId(),
                filtro.getVehiculoId(),
                tipo,
                estado,
                fechaIn,
                fechaFi
        );

        return registros.stream().map(reserva -> {
            ConsultaReservaResponse dto = new ConsultaReservaResponse();
            dto.setCliente(reserva.getCliente().getNombre() + " " + reserva.getCliente().getApellido());
            dto.setVehiculo(reserva.getVehiculo().getMarca() + " " + reserva.getVehiculo().getModelo());
            dto.setPatente(reserva.getVehiculo().getPatente());
            dto.setFechaInicio(reserva.getFechaInicio().toString());
            dto.setFechaFinalizacion(reserva.getFechaFin().toString());
            dto.setPrecioDiario(reserva.getPrecioDiario());
            dto.setImporteTotal(reserva.getImporteTotal());
            dto.setEstado(reserva.getEstado().name());
            return dto;
        }).collect(Collectors.toList());
    }
}