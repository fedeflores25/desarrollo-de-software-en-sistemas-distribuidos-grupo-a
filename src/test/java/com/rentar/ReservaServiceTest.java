package com.rentar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.rentar.dto.ReservaRequest;
import com.rentar.entity.Cliente;
import com.rentar.entity.Reserva;
import com.rentar.entity.Vehiculo;
import com.rentar.entity.enums.EstadoReserva;
import com.rentar.exception.ReservaEstadoInvalidoException;
import com.rentar.exception.VehiculoNoDisponibleException;
import com.rentar.repository.ClienteRepository;
import com.rentar.repository.ReservaRepository;
import com.rentar.repository.VehiculoRepository;
import com.rentar.service.IReservaService;

/**
 * Pruebas de Alta y Cancelacion de reserva (Hito 1) sobre los datos cargados desde data.sql.
 * Requiere una base MySQL local levantada (ver application.properties).
 */
@SpringBootTest
@Transactional
class ReservaServiceTest {

    @Autowired
    private IReservaService reservaService;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private VehiculoRepository vehiculoRepository;

    // ALTA: caso feliz

    @Test
    void crear_debeCalcularImporteYQuedarConfirmada() {

        ReservaRequest request = new ReservaRequest();
        request.setClienteId(1L); // Ana Gomez (activa, data.sql)
        request.setVehiculoId(4L); // Ford EcoSport, DISPONIBLE, sin reservas en data.sql
        request.setFechaInicio(LocalDateTime.now().plusDays(10));
        request.setFechaFin(LocalDateTime.now().plusDays(13));

        Reserva reserva = reservaService.crear(request);

        assertNotNull(reserva.getId());
        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstado());
        assertEquals(3, reserva.getCantidadDias());
        assertEquals(
                0,
                reserva.getImporteTotal().compareTo(new BigDecimal("156000.00")));
    }

    // ALTA: vehiculo ocupado (solapa con las reservas CONFIRMADA del vehiculo 1 en data.sql)

    @Test
    void crear_debeFallarSiVehiculoNoDisponibleEnElPeriodo() {

        ReservaRequest request = new ReservaRequest();
        request.setClienteId(1L);
        request.setVehiculoId(1L); // Toyota Corolla, con reservas CONFIRMADA del 16 al 20/09 en data.sql
        request.setFechaInicio(LocalDateTime.now().plusDays(1));
        request.setFechaFin(LocalDateTime.now().plusDays(4));

        assertThrows(
                VehiculoNoDisponibleException.class,
                () -> reservaService.crear(request));
    }

    // CANCELACION: caso feliz (reserva propia, todavia no comenzada)

    @Test
    void cancelar_debeCambiarEstadoYLiberarElPeriodo() {

        ReservaRequest request = new ReservaRequest();
        request.setClienteId(1L);
        request.setVehiculoId(5L); // Toyota Hilux, DISPONIBLE, sin reservas en data.sql
        request.setFechaInicio(LocalDateTime.now().plusDays(5));
        request.setFechaFin(LocalDateTime.now().plusDays(6));

        Reserva creada = reservaService.crear(request);

        Reserva cancelada = reservaService.cancelar(creada.getId());

        assertEquals(EstadoReserva.CANCELADA, cancelada.getEstado());
        assertNotNull(cancelada.getFechaCancelacion());

        // El periodo debe quedar libre de nuevo: una reserva nueva superpuesta debe poder crearse.
        Reserva otra = reservaService.crear(request);
        assertNotNull(otra.getId());
    }

    // CANCELACION: no se puede cancelar una reserva cuyo periodo ya comenzo

    @Test
    void cancelar_debeFallarSiElPeriodoYaComenzo() {

        Cliente cliente = clienteRepository.findById(1L).orElseThrow();
        Vehiculo vehiculo = vehiculoRepository.findById(6L).orElseThrow(); // Ford Ranger, DISPONIBLE

        // Se crea directamente por repositorio para simular una reserva cuyo inicio ya paso
        // (el service no permite altas con fecha de inicio pasada).
        Reserva reservaYaComenzada = new Reserva();
        reservaYaComenzada.setCliente(cliente);
        reservaYaComenzada.setVehiculo(vehiculo);
        reservaYaComenzada.setFechaInicio(LocalDateTime.now().minusHours(2));
        reservaYaComenzada.setFechaFin(LocalDateTime.now().plusDays(1));
        reservaYaComenzada.setPrecioDiario(vehiculo.getPrecioDiario());
        reservaYaComenzada.setCantidadDias(1);
        reservaYaComenzada.setImporteTotal(vehiculo.getPrecioDiario());
        reservaYaComenzada.setEstado(EstadoReserva.CONFIRMADA);
        reservaYaComenzada.setFechaAlta(LocalDateTime.now().minusDays(1));
        reservaRepository.save(reservaYaComenzada);

        assertThrows(
                ReservaEstadoInvalidoException.class,
                () -> reservaService.cancelar(reservaYaComenzada.getId()));

        assertTrue(vehiculoRepository.existsById(6L));
    }
}
