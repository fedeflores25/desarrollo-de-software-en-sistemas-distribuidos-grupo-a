package com.rentar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.rentar.entity.Vehiculo;
import com.rentar.entity.enums.EstadoVehiculo;
import com.rentar.entity.enums.TipoVehiculo;
import com.rentar.exception.PatenteDuplicadaException;
import com.rentar.exception.VehiculoNoEncontradoException;
import com.rentar.repository.VehiculoRepository;
import com.rentar.service.IVehiculoService;

@SpringBootTest
@Transactional
class VehiculoServiceTest {

    @Autowired
    private IVehiculoService vehiculoService;

    @Autowired
    private VehiculoRepository vehiculoRepository;

    @Test
    void crear_debeGuardarVehiculoActivoYDisponible() {
        Vehiculo creado = vehiculoService.crearVehiculo(nuevoVehiculo("TST101"));

        assertTrue(creado.isActivo());
        assertEquals(EstadoVehiculo.DISPONIBLE, creado.getEstado());
        assertEquals("TST101", creado.getPatente());
    }

    @Test
    void crear_debeFallarSiLaPatenteYaExiste() {
        vehiculoService.crearVehiculo(nuevoVehiculo("TST102"));

        assertThrows(PatenteDuplicadaException.class,
                () -> vehiculoService.crearVehiculo(nuevoVehiculo("TST102")));
    }

    @Test
    void buscarPorId_debeFallarSiElVehiculoNoExiste() {
        assertThrows(VehiculoNoEncontradoException.class,
                () -> vehiculoService.buscarVehiculoPorId(Long.MAX_VALUE));
    }

    @Test
    void modificar_debeConservarLaPatenteOriginal() {
        Vehiculo creado = vehiculoService.crearVehiculo(nuevoVehiculo("TST103"));
        Vehiculo actualizacion = nuevoVehiculo("OTR999");
        actualizacion.setMarca("Marca actualizada");
        actualizacion.setEstado(EstadoVehiculo.RESERVADO);

        Vehiculo modificado = vehiculoService.modificarVehiculo(creado.getId(), actualizacion);

        assertEquals("TST103", modificado.getPatente());
        assertEquals("Marca actualizada", modificado.getMarca());
        assertEquals(EstadoVehiculo.RESERVADO, modificado.getEstado());
    }

    @Test
    void darDeBaja_debeDejarElVehiculoInactivo() {
        Vehiculo creado = vehiculoService.crearVehiculo(nuevoVehiculo("TST104"));

        vehiculoService.darDeBajaVehiculo(creado.getId());

        Vehiculo vehiculo = vehiculoRepository.findById(creado.getId()).orElseThrow();
        assertFalse(vehiculo.isActivo());
    }

    private Vehiculo nuevoVehiculo(String patente) {
        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setPatente(patente);
        vehiculo.setMarca("Toyota");
        vehiculo.setModelo("Corolla");
        vehiculo.setAnio(2024);
        vehiculo.setColor("Blanco");
        vehiculo.setTipo(TipoVehiculo.SEDAN);
        vehiculo.setPrecioDiario(new BigDecimal("50000.00"));
        return vehiculo;
    }
}
