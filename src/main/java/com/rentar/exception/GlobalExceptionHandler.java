package com.rentar.exception;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // DOCUMENTO O EMAIL DUPLICADO
    @ExceptionHandler(ClienteDuplicadoException.class)
    public ResponseEntity<Map<String, String>> manejarClienteDuplicado(
            ClienteDuplicadoException ex) {

        Map<String, String> respuesta = new HashMap<>();
        respuesta.put("error", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(respuesta);
    }

    // CLIENTE NO ENCONTRADO
    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> manejarClienteNoEncontrado(
            ClienteNoEncontradoException ex) {

        Map<String, String> respuesta = new HashMap<>();
        respuesta.put("error", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(respuesta);
    }

    // ERRORES DE VALIDACION
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> manejarValidaciones(
            MethodArgumentNotValidException ex) {

        Map<String, String> errores = new HashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                    errores.put(
                            error.getField(),
                            error.getDefaultMessage()
                    )
                );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errores);
    }

    // RESERVA NO ENCONTRADA
    @ExceptionHandler(ReservaNoEncontradaException.class)
    public ResponseEntity<Map<String, String>> manejarReservaNoEncontrada(
            ReservaNoEncontradaException ex) {

        Map<String, String> respuesta = new HashMap<>();
        respuesta.put("error", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(respuesta);
    }

    // VEHICULO NO ENCONTRADO
    @ExceptionHandler(VehiculoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> manejarVehiculoNoEncontrado(
            VehiculoNoEncontradoException ex) {

        Map<String, String> respuesta = new HashMap<>();
        respuesta.put("error", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(respuesta);
    }

    // CLIENTE O VEHICULO INACTIVO
    @ExceptionHandler({ClienteInactivoException.class, VehiculoInactivoException.class})
    public ResponseEntity<Map<String, String>> manejarInactivo(
            RuntimeException ex) {

        Map<String, String> respuesta = new HashMap<>();
        respuesta.put("error", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(respuesta);
    }

    // VEHICULO NO DISPONIBLE EN EL PERIODO SOLICITADO
    @ExceptionHandler(VehiculoNoDisponibleException.class)
    public ResponseEntity<Map<String, String>> manejarVehiculoNoDisponible(
            VehiculoNoDisponibleException ex) {

        Map<String, String> respuesta = new HashMap<>();
        respuesta.put("error", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(respuesta);
    }

    // DATOS DE RESERVA INVALIDOS (regla de negocio, ej: fechas)
    @ExceptionHandler(ReservaInvalidaException.class)
    public ResponseEntity<Map<String, String>> manejarReservaInvalida(
            ReservaInvalidaException ex) {

        Map<String, String> respuesta = new HashMap<>();
        respuesta.put("error", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(respuesta);
    }

    // ESTADO DE RESERVA NO PERMITE LA OPERACION (ej: cancelar una ya comenzada)
    @ExceptionHandler(ReservaEstadoInvalidoException.class)
    public ResponseEntity<Map<String, String>> manejarReservaEstadoInvalido(
            ReservaEstadoInvalidoException ex) {

        Map<String, String> respuesta = new HashMap<>();
        respuesta.put("error", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(respuesta);
    }
}