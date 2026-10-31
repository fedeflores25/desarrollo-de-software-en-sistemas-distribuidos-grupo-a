package com.rentar.controller.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rentar.dto.ReservaRequest;
import com.rentar.dto.ReservaResponse;
import com.rentar.entity.Reserva;
import com.rentar.service.IReservaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reservas")
@Validated
@Tag(
    name = "Gestión de reservas",
    description = "Operaciones REST para el alta y la cancelación de reservas"
)
public class ReservaController {

    private final IReservaService reservaService;

    public ReservaController(IReservaService reservaService) {
        this.reservaService = reservaService;
    }

    // ALTA

    @Operation(
        summary = "Registrar una nueva reserva",
        description = "Reserva un vehiculo para un cliente en el periodo indicado, validando disponibilidad "
                + "y calculando el importe total. La reserva queda en estado CONFIRMADA."
    )
    @PostMapping
    public ResponseEntity<ReservaResponse> crear(
            @Valid @RequestBody ReservaRequest request) {

        Reserva reserva = reservaService.crear(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mapToResponse(reserva));
    }

    // CANCELACION

    @Operation(
        summary = "Cancelar una reserva",
        description = "Cancela una reserva CONFIRMADA cuyo periodo todavia no haya comenzado. "
                + "La reserva no se elimina: su estado pasa a CANCELADA."
    )
    @PostMapping("/{id}/cancelacion")
    public ResponseEntity<ReservaResponse> cancelar(
            @PathVariable Long id) {

        Reserva reserva = reservaService.cancelar(id);

        return ResponseEntity.ok(mapToResponse(reserva));
    }

    // CONVERSION DE ENTIDAD A DTO DE RESPUESTA

    private ReservaResponse mapToResponse(Reserva reserva) {

        return new ReservaResponse(
                reserva.getId(),
                reserva.getCliente().getId(),
                reserva.getVehiculo().getId(),
                reserva.getVehiculo().getPatente(),
                reserva.getFechaInicio(),
                reserva.getFechaFin(),
                reserva.getPrecioDiario(),
                reserva.getCantidadDias(),
                reserva.getImporteTotal(),
                reserva.getEstado().name(),
                reserva.getFechaAlta(),
                reserva.getFechaCancelacion()
        );
    }
}
