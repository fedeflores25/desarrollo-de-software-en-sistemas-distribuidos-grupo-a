package com.rentar.controller.graphql;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.rentar.dto.DisponibilidadFiltro;
import com.rentar.dto.VehiculoDisponibleResponse;
import com.rentar.service.IDisponibilidadService;

@Controller
public class DisponibilidadController {

    private final IDisponibilidadService disponibilidadService;

    public DisponibilidadController(
            IDisponibilidadService disponibilidadService) {
        this.disponibilidadService = disponibilidadService;
    }

    @QueryMapping
    public List<VehiculoDisponibleResponse> vehiculosDisponibles(
            @Argument DisponibilidadFiltro filtro) {

        return disponibilidadService.buscarDisponibles(filtro);
    }
}