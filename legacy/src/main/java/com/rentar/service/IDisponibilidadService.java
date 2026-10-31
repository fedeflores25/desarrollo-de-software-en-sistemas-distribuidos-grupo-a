package com.rentar.service;

import java.util.List;

import com.rentar.dto.DisponibilidadFiltro;
import com.rentar.dto.VehiculoDisponibleResponse;

public interface IDisponibilidadService {

    List<VehiculoDisponibleResponse> buscarDisponibles(DisponibilidadFiltro filtro);
}