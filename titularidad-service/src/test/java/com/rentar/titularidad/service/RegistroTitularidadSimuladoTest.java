package com.rentar.titularidad.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegistroTitularidadSimuladoTest {

    private RegistroTitularidadSimulado registro;

    @BeforeEach
    void setUp() {
        registro = new RegistroTitularidadSimulado();
    }

    @Test
    void validaLosTresTitularesRegistrados() {
        assertTrue(registro.validarTitularidad(1L, "EXT101"));
        assertTrue(registro.validarTitularidad(2L, "EXT102"));
        assertTrue(registro.validarTitularidad(3L, "EXT103"));
    }

    @Test
    void rechazaUnClienteQueNoEsTitular() {
        assertFalse(registro.validarTitularidad(2L, "EXT101"));
    }

    @Test
    void rechazaUnaPatenteDesconocida() {
        assertFalse(registro.validarTitularidad(1L, "PAT999"));
        assertNull(registro.buscarTitular("PAT999"));
    }

    @Test
    void rechazaDatosNulosOVacios() {
        assertFalse(registro.validarTitularidad(null, "EXT101"));
        assertFalse(registro.validarTitularidad(1L, null));
        assertFalse(registro.validarTitularidad(1L, "  "));
        assertNull(registro.buscarTitular(null));
        assertNull(registro.buscarTitular("  "));
    }

    @Test
    void aceptaPatentesConMinusculasYEspacios() {
        assertTrue(registro.validarTitularidad(1L, " ext101 "));
        assertTrue(registro.validarTitularidad(2L, "ext102"));
    }

    @Test
    void devuelveElDocumentoDelTitularRegistrado() {
        RegistroTitularidadSimulado.Titular titular =
                registro.buscarTitular("EXT103");

        assertNotNull(titular);
        assertEquals(3L, titular.getClienteId());
        assertEquals("30333444", titular.getDocumento());
    }
}