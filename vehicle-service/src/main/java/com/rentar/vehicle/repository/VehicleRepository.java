package com.rentar.vehicle.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rentar.vehicle.entity.Vehiculo;

public interface VehicleRepository extends JpaRepository<Vehiculo, Long> {

    boolean existsByPatente(String patente);

    Optional<Vehiculo> findByPatente(String patente);

    List<Vehiculo> findAllByActivoTrue();
}
