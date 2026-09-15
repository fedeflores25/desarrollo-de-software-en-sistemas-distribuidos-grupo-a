package com.rentar.controller.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rentar.dto.ClienteRequest;
import com.rentar.entity.Cliente;
import com.rentar.service.implementation.ClienteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;


@RestController
@RequestMapping("/api/clientes")
@Validated
@Tag(
    name = "Gestión de clientes",
    description = "Operaciones REST para el alta, modificación, baja y consulta de clientes"
)
public class ClienteController {

    private final ClienteService clienteService;


    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }


    // ALTA

    @Operation(
        summary = "Registrar un nuevo cliente",
        description = "Da de alta un cliente en el sistema con estado activo"
    )
    @PostMapping
    public ResponseEntity<Cliente> crear(
            @Valid @RequestBody ClienteRequest request) {

        Cliente cliente = clienteService.crear(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cliente);
    }


    // CONSULTA DE TODOS

    @Operation(
        summary = "Listar todos los clientes",
        description = "Obtiene el listado completo de clientes activos e inactivos"
    )
    @GetMapping
    public ResponseEntity<List<Cliente>> listar() {

        return ResponseEntity.ok(
                clienteService.listar()
        );
    }


    // CONSULTA POR ID

    @Operation(
        summary = "Buscar un cliente por ID",
        description = "Obtiene los datos de un cliente utilizando su identificador"
    )
    @GetMapping("/{id}")
    public ResponseEntity<Cliente> buscarPorId(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                clienteService.buscarPorId(id)
        );
    }


    // MODIFICACION

    @Operation(
        summary = "Modificar un cliente",
        description = "Modifica los datos de un cliente existente"
    )
    @PutMapping("/{id}")
    public ResponseEntity<Cliente> modificar(
            @PathVariable Long id,
            @Valid @RequestBody ClienteRequest request) {

        return ResponseEntity.ok(
                clienteService.modificar(id, request)
        );
    }


    // BAJA

    @Operation(
        summary = "Dar de baja lógica a un cliente",
        description = "Marca al cliente como inactivo sin eliminarlo de la base de datos"
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darDeBaja(
            @PathVariable Long id) {

        clienteService.darDeBaja(id);

        return ResponseEntity.noContent().build();
    }

}