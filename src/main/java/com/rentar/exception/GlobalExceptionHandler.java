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

    // PATENTE DUPLICADA
    @ExceptionHandler(PatenteDuplicadaException.class)
    public ResponseEntity<Map<String, String>> manejarPatenteDuplicada(
            PatenteDuplicadaException ex) {

        Map<String, String> respuesta = new HashMap<>();
        respuesta.put("error", ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
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
}
