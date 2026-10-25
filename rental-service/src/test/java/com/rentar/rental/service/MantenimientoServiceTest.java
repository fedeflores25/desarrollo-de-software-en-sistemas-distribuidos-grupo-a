package com.rentar.rental.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rentar.rental.entity.ReservaMantenimiento;
import com.rentar.rental.entity.enums.EstadoMantenimiento;
import com.rentar.rental.repository.ReservaMantenimientoRepository;
import com.rentar.rental.service.implementation.MantenimientoService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MantenimientoServiceTest {

    @Mock
    private ReservaMantenimientoRepository repository;

    @Test
    void guardaElPrimerResultadoDeUnaReservaPendiente() {
        ReservaMantenimiento mantenimiento = pendiente(20L);
        when(repository.findById(20L)).thenReturn(Optional.of(mantenimiento));

        boolean guardado = new MantenimientoService(repository)
                .registrarResultado("evento-1", 20L, EstadoMantenimiento.REALIZADO);

        assertThat(guardado).isTrue();
        assertThat(mantenimiento.getEstado()).isEqualTo(EstadoMantenimiento.REALIZADO);
        assertThat(mantenimiento.getUltimoEventId()).isEqualTo("evento-1");
        verify(repository).save(mantenimiento);
    }

    @Test
    void ignoraUnResultadoDuplicado() {
        ReservaMantenimiento mantenimiento = pendiente(21L);
        mantenimiento.setEstado(EstadoMantenimiento.NO_REALIZADO);
        when(repository.findById(21L)).thenReturn(Optional.of(mantenimiento));

        boolean guardado = new MantenimientoService(repository)
                .registrarResultado("evento-duplicado", 21L, EstadoMantenimiento.REALIZADO);

        assertThat(guardado).isFalse();
        verify(repository, never()).save(any());
    }

    private ReservaMantenimiento pendiente(Long reservaId) {
        ReservaMantenimiento mantenimiento = new ReservaMantenimiento();
        mantenimiento.setReservaId(reservaId);
        mantenimiento.setEstado(EstadoMantenimiento.PENDIENTE);
        return mantenimiento;
    }
}
