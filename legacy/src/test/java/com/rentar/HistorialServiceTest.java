
package com.rentar;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.rentar.dto.HistorialResponse;
import com.rentar.service.IHistorialService;

/**
 * Pruebas de Consulta de Historial de Alquileres (Hito 1).
 */
@SpringBootTest
@Transactional
class HistorialServiceTest {

    @Autowired
    private IHistorialService historialService;

    // CONSULTA: devuelve el historial correcto de todos los clientes (solo FINALIZADO o CANCELADA)
    @Test
    void obtenerHistorial_debeDevolverAlquileresFinalizadosYCancelados() {

        List<HistorialResponse> historial = historialService.obtenerHistorial();

        assertFalse(historial.isEmpty(), "El historial no debe estar vacío si hay datos de prueba cargados");

        // Verifica que la regla de negocio de los estados se cumpla estrictamente
        assertTrue(historial.stream()
                .allMatch(h -> h.getEstado().equals("FINALIZADA") || h.getEstado().equals("CANCELADA")),
                "El historial solo debe contener reservas en estado FINALIZADA o CANCELADA");
    }
}
















