package com.rentar.controller.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rentar.dto.ActualizarVehiculoRequest;
import com.rentar.dto.CrearVehiculoRequest;
import com.rentar.dto.VehiculoResponse;
import com.rentar.entity.Vehiculo;
import com.rentar.service.IVehiculoService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/vehiculos")
@Tag(name = "Gestion de vehiculos", description = "Operaciones REST para administrar la flota")
public class VehiculoController {

    private final IVehiculoService vehiculoService;

    public VehiculoController(IVehiculoService vehiculoService) {
        this.vehiculoService = vehiculoService;
    }

    @Operation(summary = "Registrar un vehiculo")
    @PostMapping
    public ResponseEntity<VehiculoResponse> crear(
            @Valid @RequestBody CrearVehiculoRequest request) {

        Vehiculo vehiculo = vehiculoService.crearVehiculo(mapCrearRequest(request));

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mapToResponse(vehiculo));
    }

    @Operation(summary = "Listar vehiculos")
    @GetMapping
    public ResponseEntity<List<VehiculoResponse>> listar() {
        List<VehiculoResponse> vehiculos = vehiculoService.listarVehiculos()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return ResponseEntity.ok(vehiculos);
    }

    @Operation(summary = "Buscar un vehiculo por ID")
    @GetMapping("/{id}")
    public ResponseEntity<VehiculoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(mapToResponse(vehiculoService.buscarVehiculoPorId(id)));
    }

    @Operation(summary = "Modificar un vehiculo sin alterar su patente")
    @PutMapping("/{id}")
    public ResponseEntity<VehiculoResponse> modificar(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarVehiculoRequest request) {

        Vehiculo vehiculo = vehiculoService.modificarVehiculo(id, mapActualizarRequest(request));

        return ResponseEntity.ok(mapToResponse(vehiculo));
    }

    @Operation(summary = "Dar de baja logica a un vehiculo")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darDeBaja(@PathVariable Long id) {
        vehiculoService.darDeBajaVehiculo(id);
        return ResponseEntity.noContent().build();
    }

    private Vehiculo mapCrearRequest(CrearVehiculoRequest request) {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setPatente(request.getPatente());
        vehiculo.setMarca(request.getMarca());
        vehiculo.setModelo(request.getModelo());
        vehiculo.setAnio(request.getAnio());
        vehiculo.setColor(request.getColor());
        vehiculo.setTipo(request.getTipo());
        vehiculo.setPrecioDiario(request.getPrecioDiario());
        return vehiculo;
    }

    private Vehiculo mapActualizarRequest(ActualizarVehiculoRequest request) {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setMarca(request.getMarca());
        vehiculo.setModelo(request.getModelo());
        vehiculo.setAnio(request.getAnio());
        vehiculo.setColor(request.getColor());
        vehiculo.setTipo(request.getTipo());
        vehiculo.setPrecioDiario(request.getPrecioDiario());
        vehiculo.setEstado(request.getEstado());
        return vehiculo;
    }

    private VehiculoResponse mapToResponse(Vehiculo vehiculo) {
        return new VehiculoResponse(
                vehiculo.getId(),
                vehiculo.getPatente(),
                vehiculo.getMarca(),
                vehiculo.getModelo(),
                vehiculo.getAnio(),
                vehiculo.getColor(),
                vehiculo.getTipo(),
                vehiculo.getPrecioDiario(),
                vehiculo.getEstado(),
                vehiculo.isActivo());
    }
}
