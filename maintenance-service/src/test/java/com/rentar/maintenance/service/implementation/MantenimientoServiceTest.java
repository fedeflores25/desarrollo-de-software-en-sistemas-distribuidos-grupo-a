package com.rentar.maintenance.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentar.maintenance.entity.SolicitudMantenimiento;
import com.rentar.maintenance.entity.enums.EstadoMantenimiento;
import com.rentar.maintenance.repository.SolicitudMantenimientoRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MantenimientoServiceTest {

    @Mock
    private SolicitudMantenimientoRepository solicitudRepository;

    @Test
    void procesaComoRealizadoCuandoElVehiculoNoEstaConfiguradoComoFallido() {
        MantenimientoService service = new MantenimientoService(solicitudRepository, "7");
        when(solicitudRepository.existsByEventId("evento-1")).thenReturn(false);
        when(solicitudRepository.findByReservaId(10L)).thenReturn(Optional.empty());
        when(solicitudRepository.save(any(SolicitudMantenimiento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Optional<SolicitudMantenimiento> resultado = service.registrarYProcesar(10L, 5L,
                LocalDateTime.parse("2026-10-20T10:00:00"),
                LocalDateTime.parse("2026-10-22T10:00:00"), "evento-1", "correlacion-1");

        assertThat(resultado).isPresent();
        assertThat(resultado.orElseThrow().getEstado()).isEqualTo(EstadoMantenimiento.REALIZADO);
        verify(solicitudRepository).save(any(SolicitudMantenimiento.class));
    }

    @Test
    void procesaComoNoRealizadoCuandoElVehiculoEstaConfiguradoComoFallido() {
        MantenimientoService service = new MantenimientoService(solicitudRepository, "7, 9");
        when(solicitudRepository.existsByEventId("evento-2")).thenReturn(false);
        when(solicitudRepository.findByReservaId(11L)).thenReturn(Optional.empty());
        when(solicitudRepository.save(any(SolicitudMantenimiento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Optional<SolicitudMantenimiento> resultado = service.registrarYProcesar(11L, 7L,
                LocalDateTime.parse("2026-10-20T10:00:00"),
                LocalDateTime.parse("2026-10-22T10:00:00"), "evento-2", "correlacion-2");

        assertThat(resultado.orElseThrow().getEstado()).isEqualTo(EstadoMantenimiento.NO_REALIZADO);
    }

    @Test
    void ignoraUnEventoYaProcesado() {
        MantenimientoService service = new MantenimientoService(solicitudRepository, "");
        when(solicitudRepository.existsByEventId("evento-duplicado")).thenReturn(true);

        Optional<SolicitudMantenimiento> resultado = service.registrarYProcesar(12L, 4L,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), "evento-duplicado", "correlacion-3");

        assertThat(resultado).isEmpty();
        verify(solicitudRepository, never()).save(any());
    }
}
