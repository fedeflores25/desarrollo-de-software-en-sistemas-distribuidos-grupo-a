package com.rentar.service.implementation;

import java.util.List;

import org.springframework.stereotype.Service;

import com.rentar.dto.DisponibilidadFiltro;
import com.rentar.dto.VehiculoDisponibleResponse;
import com.rentar.service.IDisponibilidadService;

@Service
public class DisponibilidadService implements IDisponibilidadService {

    @Override
    public List<VehiculoDisponibleResponse> buscarDisponibles(DisponibilidadFiltro filtro) {

        // IMPLEMENTACION PENDIENTE DE INTEGRAR CON VEHICULOS Y RESERVAS
        throw new UnsupportedOperationException("Consulta de disponibilidad pendiente de implementacion");
    }
}