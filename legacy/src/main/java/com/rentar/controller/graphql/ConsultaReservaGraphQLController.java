package com.rentar.controller.graphql;

import com.rentar.dto.ReservaFiltro;
import com.rentar.dto.ConsultaReservaResponse;
import com.rentar.service.IConsultaReservaService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class ConsultaReservaGraphQLController {

    private final IConsultaReservaService consultaReservaService;

    public ConsultaReservaGraphQLController(IConsultaReservaService consultaReservaService) {
        this.consultaReservaService = consultaReservaService;
    }

    // El nombre debe coincidir con schema.graphqls
    @QueryMapping
    public List<ConsultaReservaResponse> consultarReservas(@Argument ReservaFiltro filtro) {
        return consultaReservaService.consultarReservas(filtro);
    }
}