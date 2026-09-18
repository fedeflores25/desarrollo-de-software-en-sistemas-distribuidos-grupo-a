package com.rentar;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.rentar.dto.DisponibilidadFiltro;
import com.rentar.dto.VehiculoDisponibleResponse;
import com.rentar.entity.enums.TipoVehiculo;
import com.rentar.exception.DisponibilidadInvalidaException;
import com.rentar.service.IDisponibilidadService;

/**
 * Pruebas de Consulta de Disponibilidad (Hito 1).
 */
@SpringBootTest
@Transactional
class DisponibilidadServiceTest {

    @Autowired
    private IDisponibilidadService disponibilidadService;

    // CONSULTA: devuelve vehiculos disponibles en el periodo

    @Test
    void buscarDisponibles_debeDevolverVehiculosDisponibles() {

        DisponibilidadFiltro filtro = new DisponibilidadFiltro();

        filtro.setFechaInicio(
                LocalDateTime.of(2026, 9, 21, 13, 0));

        filtro.setFechaFin(
                LocalDateTime.of(2026, 9, 22, 13, 0));

        List<VehiculoDisponibleResponse> disponibles =
                disponibilidadService.buscarDisponibles(filtro);

        assertTrue(disponibles.stream()
                .anyMatch(v -> v.getPatente().equals("JKL012")));
    }

    // CONSULTA: no devuelve vehiculos con reservas confirmadas que se solapen

    @Test
    void buscarDisponibles_noDebeDevolverVehiculoConReservaConfirmadaSolapada() {

        DisponibilidadFiltro filtro = new DisponibilidadFiltro();

        filtro.setFechaInicio(
                LocalDateTime.of(2026, 9, 21, 13, 0));

        filtro.setFechaFin(
                LocalDateTime.of(2026, 9, 22, 13, 0));

        List<VehiculoDisponibleResponse> disponibles =
                disponibilidadService.buscarDisponibles(filtro);

        assertTrue(disponibles.stream()
                .noneMatch(v -> v.getPatente().equals("GHI789")));
    }

    // CONSULTA: vehiculo disponible aunque tenga una reserva cancelada

    @Test
    void buscarDisponibles_debePermitirVehiculoConReservaCancelada() {

        DisponibilidadFiltro filtro = new DisponibilidadFiltro();

        filtro.setFechaInicio(
                LocalDateTime.of(2026, 9, 21, 13, 0));

        filtro.setFechaFin(
                LocalDateTime.of(2026, 9, 22, 13, 0));

        filtro.setTipo(TipoVehiculo.PICKUP);

        List<VehiculoDisponibleResponse> disponibles =
                disponibilidadService.buscarDisponibles(filtro);

        assertTrue(disponibles.stream()
                .anyMatch(v -> v.getPatente().equals("MNO345")));
    }

    // CONSULTA: filtro por tipo

    @Test
    void buscarDisponibles_debeFiltrarPorTipo() {

        DisponibilidadFiltro filtro = new DisponibilidadFiltro();

        filtro.setFechaInicio(
                LocalDateTime.of(2026, 9, 21, 13, 0));

        filtro.setFechaFin(
                LocalDateTime.of(2026, 9, 22, 13, 0));

        filtro.setTipo(TipoVehiculo.SUV);

        List<VehiculoDisponibleResponse> disponibles =
                disponibilidadService.buscarDisponibles(filtro);

        assertTrue(disponibles.stream()
                .allMatch(v -> v.getTipo() == TipoVehiculo.SUV));
    }

    // CONSULTA: filtro por rango de precio

    @Test
    void buscarDisponibles_debeFiltrarPorRangoDePrecio() {

        DisponibilidadFiltro filtro = new DisponibilidadFiltro();

        filtro.setFechaInicio(
                LocalDateTime.of(2026, 9, 21, 13, 0));

        filtro.setFechaFin(
                LocalDateTime.of(2026, 9, 22, 13, 0));

        filtro.setPrecioMin(new BigDecimal("45000"));
        filtro.setPrecioMax(new BigDecimal("60000"));

        List<VehiculoDisponibleResponse> disponibles =
                disponibilidadService.buscarDisponibles(filtro);

        assertTrue(disponibles.stream()
                .allMatch(v ->
                        v.getPrecioDiario()
                                .compareTo(new BigDecimal("45000")) >= 0
                        &&
                        v.getPrecioDiario()
                                .compareTo(new BigDecimal("60000")) <= 0));
    }

    // VALIDACION: la fecha de fin debe ser posterior a la fecha de inicio

    @Test
    void buscarDisponibles_debeFallarSiFechaFinNoEsPosterior() {

        DisponibilidadFiltro filtro = new DisponibilidadFiltro();

        filtro.setFechaInicio(
                LocalDateTime.now().plusDays(2));

        filtro.setFechaFin(
                LocalDateTime.now().plusDays(1));

        assertThrows(
                DisponibilidadInvalidaException.class,
                () -> disponibilidadService.buscarDisponibles(filtro));
    }
}