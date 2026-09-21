package com.rentar.service.implementation;
import com.rentar.service.IHistorialService;
import com.rentar.repository.ReservaRepository;
import com.rentar.entity.Reserva;
import com.rentar.dto.HistorialResponse;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.rentar.entity.enums.EstadoReserva;

@Service
public class HistorialService implements IHistorialService {

    private final ReservaRepository reservaRepository;

    public HistorialService(ReservaRepository reservaRepository) {
        this.reservaRepository = reservaRepository;
    }

    @Override
    public List<HistorialResponse> obtenerHistorial() {
        List<EstadoReserva> estadosValidos = Arrays.asList(EstadoReserva.FINALIZADA, EstadoReserva.CANCELADA);
        
        List<Reserva> registros = reservaRepository.findByEstadoIn(estadosValidos);

        return registros.stream().map(reserva -> {
            HistorialResponse dto = new HistorialResponse();
            dto.setVehiculo(reserva.getVehiculo().getMarca() + " " + reserva.getVehiculo().getModelo());
            dto.setPatente(reserva.getVehiculo().getPatente());
            dto.setFechaInicio(reserva.getFechaInicio().toString());
            dto.setFechaFinalizacion(reserva.getFechaFin().toString());
            dto.setEstado(reserva.getEstado().name());
            dto.setImporteTotal(reserva.getImporteTotal());
            dto.setCantidadDias(reserva.getCantidadDias()); 
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<HistorialResponse> obtenerHistorial(Long idCliente) {
        // 1. Filtrar solo los terminados (Finalizado o Cancelado)
        List<EstadoReserva> estadosValidos = Arrays.asList(EstadoReserva.FINALIZADA, EstadoReserva.CANCELADA);
        // 2. Buscar en la BD
        List<Reserva> registros = reservaRepository.findByClienteIdAndEstadoIn(idCliente, estadosValidos);

        // 3. Mapear Entity a DTO 
        return registros.stream().map(reserva -> {
            HistorialResponse dto = new HistorialResponse();
            dto.setVehiculo(reserva.getVehiculo().getMarca() + " " + reserva.getVehiculo().getModelo());
            dto.setPatente(reserva.getVehiculo().getPatente());
            dto.setFechaInicio(reserva.getFechaInicio().toString());
            dto.setFechaFinalizacion(reserva.getFechaFin().toString());
            dto.setEstado(reserva.getEstado().name());
            dto.setImporteTotal(reserva.getImporteTotal());
            
            dto.setCantidadDias(reserva.getCantidadDias()); 

            return dto;
        }).collect(Collectors.toList());
    }
}