package com.rentar;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.rentar.dto.ReservaFiltro;
import com.rentar.dto.ConsultaReservaResponse;
import com.rentar.service.IConsultaReservaService;

/**
 * Pruebas de Consulta de Reservas con filtros dinámicos (Hito 1).
 */
@SpringBootTest
@Transactional
class ConsultaReservaServiceTest {

    @Autowired
    private IConsultaReservaService consultaReservaService;

    // PRUEBA 1: Verificar que soporte recibir un filtro nulo (comportamiento de administrador sin filtros)
    @Test
    void consultarReservas_sinFiltrosDebeDevolverTodosLosResultados() {
        // Ejecutamos pasando null como si el front no enviara variables
        List<ConsultaReservaResponse> resultados = consultaReservaService.consultarReservas(null);

        assertNotNull(resultados, "La respuesta no debe ser nula");
        assertFalse(resultados.isEmpty(), "Debe devolver datos de la base al no aplicar filtros restrictivos");
    }

    // PRUEBA 2: Verificar filtrado específico por cliente
    @Test
    void consultarReservas_conFiltroDeClienteDebeEjecutarseCorrectamente() {
        ReservaFiltro filtro = new ReservaFiltro();
        filtro.setClienteId(1L);

        List<ConsultaReservaResponse> resultados = consultaReservaService.consultarReservas(filtro);

        assertNotNull(resultados, "La lista filtrada no debe ser nula");
    }

    // PRUEBA 3: Verificar filtrado por estado de la reserva
    @Test
    void consultarReservas_conFiltroDeEstado() {
        ReservaFiltro filtro = new ReservaFiltro();
        filtro.setEstado("CONFIRMADA");

        List<ConsultaReservaResponse> resultados = consultaReservaService.consultarReservas(filtro);

        assertNotNull(resultados);
    }
}