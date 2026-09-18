package com.rentar.service.implementation;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.rentar.dto.DisponibilidadFiltro;
import com.rentar.dto.VehiculoDisponibleResponse;
import com.rentar.entity.Vehiculo;
import com.rentar.exception.DisponibilidadInvalidaException;
import com.rentar.repository.VehiculoRepository;
import com.rentar.service.IDisponibilidadService;

@Service
public class DisponibilidadService implements IDisponibilidadService {

    private final VehiculoRepository vehiculoRepository;

    public DisponibilidadService(VehiculoRepository vehiculoRepository) {
        this.vehiculoRepository = vehiculoRepository;
    }

    @Override
    public List<VehiculoDisponibleResponse> buscarDisponibles(
            DisponibilidadFiltro filtro) {

        // VALIDACION DE FECHAS
        if (!filtro.getFechaInicio().isAfter(LocalDateTime.now())) {
            throw new DisponibilidadInvalidaException(
                    "La fecha de inicio debe ser posterior a la fecha actual");
        }

        if (!filtro.getFechaFin().isAfter(filtro.getFechaInicio())) {
            throw new DisponibilidadInvalidaException(
                    "La fecha de fin debe ser posterior a la fecha de inicio");
        }

        // VALIDACION DEL RANGO DE PRECIOS
        if (filtro.getPrecioMin() != null
                && filtro.getPrecioMax() != null
                && filtro.getPrecioMin().compareTo(filtro.getPrecioMax()) > 0) {

            throw new DisponibilidadInvalidaException(
                    "El precio mínimo no puede ser mayor al precio máximo");
        }

        return vehiculoRepository.findAll()
                .stream()

                // SOLO VEHICULOS ACTIVOS
                .filter(Vehiculo::isActivo)

                // FILTRO POR TIPO
                .filter(vehiculo ->
                        filtro.getTipo() == null
                        || vehiculo.getTipo() == filtro.getTipo())

                // FILTRO POR MARCA
                .filter(vehiculo ->
                        filtro.getMarca() == null
                        || vehiculo.getMarca().equalsIgnoreCase(
                                filtro.getMarca()))

                // FILTRO POR MODELO
                .filter(vehiculo ->
                        filtro.getModelo() == null
                        || vehiculo.getModelo().equalsIgnoreCase(
                                filtro.getModelo()))

                // FILTRO POR PRECIO MINIMO
                .filter(vehiculo ->
                        filtro.getPrecioMin() == null
                        || vehiculo.getPrecioDiario()
                                .compareTo(filtro.getPrecioMin()) >= 0)

                // FILTRO POR PRECIO MAXIMO
                .filter(vehiculo ->
                        filtro.getPrecioMax() == null
                        || vehiculo.getPrecioDiario()
                                .compareTo(filtro.getPrecioMax()) <= 0)

                // CONVERSION A DTO DE RESPUESTA
                .map(this::mapToResponse)

                .toList();
    }

    private VehiculoDisponibleResponse mapToResponse(Vehiculo vehiculo) {

        VehiculoDisponibleResponse response =
                new VehiculoDisponibleResponse();

        response.setPatente(vehiculo.getPatente());
        response.setMarca(vehiculo.getMarca());
        response.setModelo(vehiculo.getModelo());
        response.setAnio(vehiculo.getAnio());
        response.setColor(vehiculo.getColor());
        response.setTipo(vehiculo.getTipo());
        response.setPrecioDiario(vehiculo.getPrecioDiario());

        return response;
    }
}