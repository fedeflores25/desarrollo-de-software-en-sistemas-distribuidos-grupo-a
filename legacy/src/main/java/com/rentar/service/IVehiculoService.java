package com.rentar.service;

import java.util.List;

import com.rentar.entity.Vehiculo;

/**Define las operaciones disponibles para la gestión de vehículos. */
public interface IVehiculoService {

    Vehiculo crearVehiculo(Vehiculo vehiculo);

    List<Vehiculo> listarVehiculos();

    Vehiculo buscarVehiculoPorId(Long id);

    Vehiculo modificarVehiculo(Long id, Vehiculo vehiculo);

    void darDeBajaVehiculo(Long id);
}