package com.rentar.titularidad.service;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

/** Registro externo simulado de titulares de vehiculos. */
@Service
public class RegistroTitularidadSimulado {

    private final Map<String, Titular> titulares = new HashMap<>();

    public RegistroTitularidadSimulado() {

        // Datos ficticios para las pruebas del Hito 3.
        // Los clientes existen en customer-service.
        // Las patentes no existen inicialmente en vehicle-service.

        titulares.put("EXT101", new Titular(1L, "30111222"));
        titulares.put("EXT102", new Titular(2L, "30222333"));
        titulares.put("EXT103", new Titular(3L, "30333444"));
    }

    public Titular buscarTitular(String patente) {

        if (patente == null || patente.isBlank()) {
            return null;
        }

        String patenteNormalizada =
                patente.trim().toUpperCase(Locale.ROOT);

        return titulares.get(patenteNormalizada);
    }

    public boolean validarTitularidad(
            Long clienteId, String patente) {

        Titular titular = buscarTitular(patente);

        if (titular == null || clienteId == null) {
            return false;
        }

        return titular.getClienteId().equals(clienteId);
    }

    public static class Titular {

        private final Long clienteId;
        private final String documento;

        public Titular(Long clienteId, String documento) {
            this.clienteId = clienteId;
            this.documento = documento;
        }

        public Long getClienteId() {
            return clienteId;
        }

        public String getDocumento() {
            return documento;
        }
    }
}