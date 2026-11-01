package com.rentar.vehicle.service;

import java.util.List;

import com.rentar.vehicle.dto.SolicitudVehiculoExternoRequest;
import com.rentar.vehicle.entity.SolicitudVehiculoExterno;
import com.rentar.vehicle.entity.enums.ResultadoValidacionExterna;

public interface SolicitudVehiculoExternoService {

    SolicitudVehiculoExterno create(
            SolicitudVehiculoExternoRequest request);

    SolicitudVehiculoExterno getById(Long id);

    List<SolicitudVehiculoExterno> list();

    void procesarResultadoPatente(
            Long vehiculoExternoId,
            String patente,
            ResultadoValidacionExterna resultado,
            String correlationId);

    void procesarResultadoTitularidad(
            Long vehiculoExternoId,
            Long clienteId,
            String documento,
            String patente,
            ResultadoValidacionExterna resultado,
            String correlationId);

    void incorporar(Long vehiculoExternoId);
}