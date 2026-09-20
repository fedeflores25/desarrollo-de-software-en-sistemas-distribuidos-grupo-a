package com.rentar.service.implementation;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.rentar.entity.Vehiculo;
import com.rentar.entity.enums.EstadoVehiculo;
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

    @Override
    public Vehiculo crearVehiculo(Vehiculo vehiculo) {
        if (vehiculoRepository.existsByPatente(vehiculo.getPatente())) {
            throw new IllegalArgumentException(
                    "Ya existe un vehiculo con la patente " + vehiculo.getPatente());
        }

        vehiculo.setId(null);
        vehiculo.setEstado(EstadoVehiculo.DISPONIBLE);
        vehiculo.setActivo(true);

        return vehiculoRepository.save(vehiculo);
    }

    @Override
    public List<Vehiculo> listarVehiculos() {
        return vehiculoRepository.findAll();
    }

    @Override
    public Vehiculo buscarVehiculoPorId(Long id) {
        return vehiculoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "No existe un vehiculo con id " + id));
    }

    @Override
    public Vehiculo modificarVehiculo(Long id, Vehiculo vehiculo) {
        Vehiculo vehiculoExistente = buscarVehiculoPorId(id);

        // La patente pertenece a la identidad del vehiculo y no se modifica.
        vehiculoExistente.setMarca(vehiculo.getMarca());
        vehiculoExistente.setModelo(vehiculo.getModelo());
        vehiculoExistente.setAnio(vehiculo.getAnio());
        vehiculoExistente.setColor(vehiculo.getColor());
        vehiculoExistente.setTipo(vehiculo.getTipo());
        vehiculoExistente.setPrecioDiario(vehiculo.getPrecioDiario());
        vehiculoExistente.setEstado(vehiculo.getEstado());

        return vehiculoRepository.save(vehiculoExistente);
    }

    @Override
    public void darDeBajaVehiculo(Long id) {
        Vehiculo vehiculo = buscarVehiculoPorId(id);
        vehiculo.setActivo(false);
        vehiculoRepository.save(vehiculo);
    }
}
