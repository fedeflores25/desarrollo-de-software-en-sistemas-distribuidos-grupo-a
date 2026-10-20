package com.rentar.rental.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.rentar.rental.entity.Reserva;
import com.rentar.rental.entity.enums.EstadoReserva;
import com.rentar.rental.events.ReservaConfirmadaEvent;
import com.rentar.rental.exception.ReservaEstadoInvalidoException;
import com.rentar.rental.exception.ReservaInvalidaException;
import com.rentar.rental.exception.ReservaNoEncontradaException;
import com.rentar.rental.exception.VehiculoNoDisponibleException;
import com.rentar.rental.repository.ReservaRepository;
import com.rentar.rental.service.implementation.ReservaService;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private IMantenimientoService mantenimientoService;

    @Mock
    private ApplicationEventPublisher eventos;

    @InjectMocks
    private ReservaService reservaService;

    private static final BigDecimal PRECIO = new BigDecimal("100.50");

    private static LocalDateTime enDias(int dias) {
        return LocalDateTime.now().plusDays(dias).withNano(0);
    }

    private static Reserva reserva(EstadoReserva estado, LocalDateTime inicio, LocalDateTime fin) {
        Reserva r = new Reserva();
        r.setId(1L);
        r.setClienteId(10L);
        r.setVehiculoId(20L);
        r.setFechaInicio(inicio);
        r.setFechaFin(fin);
        r.setPrecioDiario(PRECIO);
        r.setCantidadDias(1);
        r.setImporteTotal(PRECIO);
        r.setEstado(estado);
        r.setFechaAlta(LocalDateTime.now());
        return r;
    }

    // ==========================================
    // ALTA
    // ==========================================

    @Test
    void crearCalculaDiasEImporteYQuedaConfirmada() {

        when(reservaRepository.existsSolapamiento(
                eq(20L), any(), any(), eq(EstadoReserva.CONFIRMADA))).thenReturn(false);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva creada = reservaService.crear(10L, 20L, enDias(10), enDias(13), PRECIO);

        assertEquals(EstadoReserva.CONFIRMADA, creada.getEstado());
        assertEquals(3, creada.getCantidadDias());
        assertEquals(0, new BigDecimal("301.50").compareTo(creada.getImporteTotal()));
        assertNotNull(creada.getFechaAlta());

        // Deja el mantenimiento PENDIENTE y avisa que la reserva se confirmo
        verify(mantenimientoService).registrarPendiente(any());
        verify(eventos).publishEvent(any(ReservaConfirmadaEvent.class));
    }

    @Test
    void crearEnElMismoDiaCobraUnDiaMinimo() {

        LocalDateTime inicio = enDias(10).withHour(9).withMinute(0).withSecond(0);

        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva creada = reservaService.crear(10L, 20L, inicio, inicio.plusHours(6), PRECIO);

        assertEquals(1, creada.getCantidadDias());
        assertEquals(0, PRECIO.compareTo(creada.getImporteTotal()));
    }

    @Test
    void crearConVehiculoOcupadoLanzaVehiculoNoDisponible() {

        when(reservaRepository.existsSolapamiento(
                eq(20L), any(), any(), eq(EstadoReserva.CONFIRMADA))).thenReturn(true);

        assertThrows(VehiculoNoDisponibleException.class,
                () -> reservaService.crear(10L, 20L, enDias(10), enDias(12), PRECIO));

        verify(reservaRepository, never()).save(any());
        verify(eventos, never()).publishEvent(any(Object.class));
    }

    @Test
    void crearConFechaDeInicioPasadaEsInvalido() {

        assertThrows(ReservaInvalidaException.class,
                () -> reservaService.crear(10L, 20L, enDias(-1), enDias(2), PRECIO));

        verify(reservaRepository, never()).save(any());
    }

    @Test
    void crearConFinNoPosteriorAlInicioEsInvalido() {

        assertThrows(ReservaInvalidaException.class,
                () -> reservaService.crear(10L, 20L, enDias(10), enDias(10), PRECIO));
        assertThrows(ReservaInvalidaException.class,
                () -> reservaService.crear(10L, 20L, enDias(10), enDias(8), PRECIO));
    }

    @Test
    void crearConPrecioNoPositivoOFaltanteEsInvalido() {

        assertThrows(ReservaInvalidaException.class,
                () -> reservaService.crear(10L, 20L, enDias(10), enDias(12), BigDecimal.ZERO));
        assertThrows(ReservaInvalidaException.class,
                () -> reservaService.crear(10L, 20L, enDias(10), enDias(12), null));
        assertThrows(ReservaInvalidaException.class,
                () -> reservaService.crear(null, 20L, enDias(10), enDias(12), PRECIO));
    }

    // ==========================================
    // CANCELACION
    // ==========================================

    @Test
    void cancelarReservaConfirmadaFuturaLaPasaACanceladaYGuardaLaFecha() {

        Reserva r = reserva(EstadoReserva.CONFIRMADA, enDias(5), enDias(7));

        when(reservaRepository.findById(1L)).thenReturn(Optional.of(r));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva cancelada = reservaService.cancelar(1L);

        assertEquals(EstadoReserva.CANCELADA, cancelada.getEstado());
        assertNotNull(cancelada.getFechaCancelacion());
    }

    @Test
    void cancelarReservaYaCanceladaOFinalizadaEsInvalido() {

        when(reservaRepository.findById(1L)).thenReturn(
                Optional.of(reserva(EstadoReserva.CANCELADA, enDias(5), enDias(7))));

        assertThrows(ReservaEstadoInvalidoException.class, () -> reservaService.cancelar(1L));

        verify(reservaRepository, never()).save(any());
    }

    @Test
    void cancelarReservaCuyoPeriodoYaComenzoEsInvalido() {

        when(reservaRepository.findById(1L)).thenReturn(
                Optional.of(reserva(EstadoReserva.CONFIRMADA, enDias(-1), enDias(2))));

        assertThrows(ReservaEstadoInvalidoException.class, () -> reservaService.cancelar(1L));

        verify(reservaRepository, never()).save(any());
    }

    @Test
    void cancelarReservaInexistenteLanzaNoEncontrada() {

        when(reservaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ReservaNoEncontradaException.class, () -> reservaService.cancelar(99L));
    }

    // ==========================================
    // CONSULTAS
    // ==========================================

    @Test
    void historialFinalizaVencidasYTraeFinalizadasYCanceladas() {

        when(reservaRepository.findByClienteIdAndEstadoIn(eq(10L), any())).thenReturn(List.of());

        reservaService.historial(10L);

        verify(reservaRepository).actualizarEstadoVencidas(
                eq(EstadoReserva.CONFIRMADA), eq(EstadoReserva.FINALIZADA), any(LocalDateTime.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<EstadoReserva>> estados =
                ArgumentCaptor.forClass(java.util.Collection.class);
        verify(reservaRepository).findByClienteIdAndEstadoIn(eq(10L), estados.capture());

        assertEquals(2, estados.getValue().size());
        assertEquals(true, estados.getValue().contains(EstadoReserva.FINALIZADA));
        assertEquals(true, estados.getValue().contains(EstadoReserva.CANCELADA));
    }

    @Test
    void historialSinClienteEsInvalido() {

        assertThrows(ReservaInvalidaException.class, () -> reservaService.historial(null));
    }

    @Test
    void listarSinVehiculosNoFiltraPorVehiculo() {

        when(reservaRepository.buscarConFiltros(any(), anyBoolean(), any(), any(), any(), any()))
                .thenReturn(List.of());

        reservaService.listar(new FiltroReserva(10L, List.of(), null, null, null));

        ArgumentCaptor<Boolean> filtrar = ArgumentCaptor.forClass(Boolean.class);
        verify(reservaRepository).buscarConFiltros(
                eq(10L), filtrar.capture(), any(), any(), any(), any());

        assertEquals(false, filtrar.getValue());
    }

    @Test
    void vehiculosOcupadosConPeriodoInvalidoEsInvalido() {

        assertThrows(ReservaInvalidaException.class,
                () -> reservaService.idsVehiculosOcupados(enDias(5), enDias(5)));
        assertThrows(ReservaInvalidaException.class,
                () -> reservaService.idsVehiculosOcupados(null, enDias(5)));
    }
}
