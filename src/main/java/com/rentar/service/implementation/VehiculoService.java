package com.rentar.service.implementation;

import org.springframework.stereotype.Service;

import com.rentar.repository.VehiculoRepository;
import com.rentar.service.IVehiculoService;

/**Implementa las operaciones relacionadas con los vehículos del sistema.
 Utiliza VehiculoRepository para acceder a los datos de los vehículos. */
@Service
public class VehiculoService implements IVehiculoService {

    private final VehiculoRepository vehiculoRepository;

    /**Recibe el repositorio que utilizará el servicio para acceder a los vehículos. */
    public VehiculoService(VehiculoRepository vehiculoRepository) {
        this.vehiculoRepository = vehiculoRepository;
    }
}