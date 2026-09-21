package com.rentar.controller.graphql;

import com.rentar.dto.HistorialResponse;
import com.rentar.service.IHistorialService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;

@Controller
public class HistorialGraphQLController {

    private final IHistorialService historialService;

    // Inyección de dependencias
    public HistorialGraphQLController(IHistorialService historialService) {
        this.historialService = historialService;
    }

    @QueryMapping
    public List<HistorialResponse> consultarHistorialAlquileres() {
        return historialService.obtenerHistorial();
    }

}