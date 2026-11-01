package com.rentar.vehicle.service;

import java.util.Collection;
import java.util.List;

import com.rentar.vehicle.dto.VehicleCreateRequest;
import com.rentar.vehicle.dto.VehicleUpdateRequest;
import com.rentar.vehicle.entity.Vehiculo;
import com.rentar.vehicle.entity.enums.EstadoVehiculo;
import com.rentar.vehicle.entity.enums.ResultadoVerificacion;

public interface VehicleService {

    Vehiculo create(VehicleCreateRequest request);

    Vehiculo getById(Long id);

    Vehiculo getByPatente(String patente);

    List<Vehiculo> list();

    Vehiculo update(Long id, VehicleUpdateRequest request);

    Vehiculo deactivate(Long id);

    Vehiculo updateStatus(Long id, EstadoVehiculo estado);

    boolean procesarResultadoVerificacion(String eventId, Long vehiculoId, String patente,
                                          ResultadoVerificacion resultado);

    List<Vehiculo> listAvailable(Collection<Long> excludedVehicleIds);
}
