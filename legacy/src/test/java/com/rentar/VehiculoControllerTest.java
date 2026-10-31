package com.rentar;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.rentar.entity.Vehiculo;
import com.rentar.entity.enums.TipoVehiculo;
import com.rentar.service.IVehiculoService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VehiculoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IVehiculoService vehiculoService;

    @Test
    void crear_debeResponder201ConVehiculoActivoYDisponible() throws Exception {
        mockMvc.perform(post("/api/vehiculos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "patente": "TST201",
                          "marca": "Honda",
                          "modelo": "Civic",
                          "anio": 2024,
                          "color": "Azul",
                          "tipo": "SEDAN",
                          "precioDiario": 55000
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patente").value("TST201"))
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.activo").value(true));
    }

    @Test
    void crear_debeResponder409SiLaPatenteEstaRepetida() throws Exception {
        String request = """
                {
                  "patente": "TST202",
                  "marca": "Honda",
                  "modelo": "Civic",
                  "anio": 2024,
                  "tipo": "SEDAN",
                  "precioDiario": 55000
                }
                """;

        mockMvc.perform(post("/api/vehiculos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/vehiculos")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void crear_debeResponder400SiFaltanDatosObligatorios() throws Exception {
        mockMvc.perform(post("/api/vehiculos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.patente").value("La patente es obligatoria"));
    }

    @Test
    void buscarPorId_debeResponder404SiElVehiculoNoExiste() throws Exception {
        mockMvc.perform(get("/api/vehiculos/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void darDeBaja_debeResponder204() throws Exception {
        Vehiculo creado = vehiculoService.crearVehiculo(nuevoVehiculo("TST203"));

        mockMvc.perform(delete("/api/vehiculos/{id}", creado.getId()))
                .andExpect(status().isNoContent());
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
