package com.rentar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.rentar.entity.Vehiculo; //analizar si conviene quitarlo junto con @repository.

/** Repositorio de acceso a datos para la entidad Vehiculo.*/

@Repository
public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    /** Verifica si ya existe un vehículo registrado con la patente indicada.
      @param patente patente a verificar
      @return true si la patente ya existe
     */
    boolean existsByPatente(String patente);
}