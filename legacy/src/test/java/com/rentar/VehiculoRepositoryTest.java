package com.rentar;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.rentar.repository.VehiculoRepository;

@SpringBootTest
@Transactional
class VehiculoRepositoryTest {

    @Autowired
    private VehiculoRepository vehiculoRepository;

    /** Verifica que se puedan consultar vehiculos cargados desde data.sql. */
    @Test
    void debeConsultarVehiculosCargadosDesdeDataSql() {
        long cantidad = vehiculoRepository.count();

        assertTrue(cantidad >= 10);
        assertTrue(vehiculoRepository.existsByPatente("ABC123"));
        assertTrue(vehiculoRepository.existsByPatente("DEF456"));

        System.out.println("Cantidad de vehiculos en la base de datos: " + vehiculoRepository.count());
    }
}
