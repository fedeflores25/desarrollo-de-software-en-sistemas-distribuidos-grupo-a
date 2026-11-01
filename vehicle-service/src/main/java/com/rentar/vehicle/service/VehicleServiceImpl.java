package com.rentar.vehicle.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import com.rentar.vehicle.entity.EventoVerificacionProcesado;
import com.rentar.vehicle.dto.VehicleCreateRequest;
import com.rentar.vehicle.dto.VehicleUpdateRequest;
import com.rentar.vehicle.entity.Vehiculo;
import com.rentar.vehicle.entity.enums.EstadoVehiculo;
import com.rentar.vehicle.entity.enums.ResultadoVerificacion;
import com.rentar.vehicle.events.VerificacionPatenteSolicitadaEvent;
import com.rentar.vehicle.exception.PatenteDuplicadaException;
import com.rentar.vehicle.exception.VehiculoInactivoException;
import com.rentar.vehicle.exception.VehiculoNoEncontradoException;
import com.rentar.vehicle.repository.VehicleRepository;
import com.rentar.vehicle.repository.EventoVerificacionProcesadoRepository;

@Service
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final EventoVerificacionProcesadoRepository eventoRepository;
    private final ApplicationEventPublisher eventPublisher;

    public VehicleServiceImpl(VehicleRepository vehicleRepository,
                              EventoVerificacionProcesadoRepository eventoRepository,
                              ApplicationEventPublisher eventPublisher) {
        this.vehicleRepository = vehicleRepository;
        this.eventoRepository = eventoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Vehiculo create(VehicleCreateRequest request) {
        if (vehicleRepository.existsByPatente(request.getPatente())) {
            throw new PatenteDuplicadaException(
                    "Ya existe un vehiculo con la patente " + request.getPatente());
        }

        Vehiculo vehiculo = new Vehiculo();
        vehiculo.setPatente(request.getPatente());
        copyCreateData(request, vehiculo);
        vehiculo.setActivo(true);
        vehiculo.setEstado(EstadoVehiculo.DISPONIBLE);

        Vehiculo guardado = vehicleRepository.save(vehiculo);
        eventPublisher.publishEvent(new VerificacionPatenteSolicitadaEvent(
                guardado.getId(), guardado.getPatente()));
        return guardado;
    }

    @Override
    public Vehiculo getById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new VehiculoNoEncontradoException(
                        "No existe un vehiculo con id " + id));
    }

    @Override
    public Vehiculo getByPatente(String patente) {
        return vehicleRepository.findByPatente(patente)
                .orElseThrow(() -> new VehiculoNoEncontradoException(
                        "No existe un vehiculo con patente " + patente));
    }

    @Override
    public List<Vehiculo> list() {
        return vehicleRepository.findAll();
    }

    @Override
    public Vehiculo update(Long id, VehicleUpdateRequest request) {
        Vehiculo vehiculo = getById(id);
        vehiculo.setMarca(request.getMarca());
        vehiculo.setModelo(request.getModelo());
        vehiculo.setAnio(request.getAnio());
        vehiculo.setColor(request.getColor());
        vehiculo.setTipo(request.getTipo());
        vehiculo.setPrecioDiario(request.getPrecioDiario());

        return vehicleRepository.save(vehiculo);
    }

    @Override
    public Vehiculo deactivate(Long id) {
        Vehiculo vehiculo = getById(id);
        vehiculo.setActivo(false);

        return vehicleRepository.save(vehiculo);
    }

    @Override
    public Vehiculo updateStatus(Long id, EstadoVehiculo estado) {
        Vehiculo vehiculo = getById(id);
        if (!vehiculo.isActivo()) {
            throw new VehiculoInactivoException(
                    "No se puede actualizar el estado de un vehiculo inactivo");
        }

        vehiculo.setEstado(estado);
        return vehicleRepository.save(vehiculo);
    }

    @Override
    @Transactional
    public boolean procesarResultadoVerificacion(String eventId, Long vehiculoId, String patente,
                                                 ResultadoVerificacion resultado) {
        if (eventoRepository.existsById(eventId)) {
            return false;
        }

        Vehiculo vehiculo = getById(vehiculoId);
        if (!vehiculo.getPatente().equals(patente)) {
            throw new IllegalArgumentException("La patente del resultado no coincide con el vehiculo");
        }

        if (resultado == ResultadoVerificacion.ROBADO && vehiculo.isActivo()) {
            vehiculo.setActivo(false);
            vehicleRepository.save(vehiculo);
        }

        eventoRepository.save(new EventoVerificacionProcesado(eventId, vehiculoId, resultado));
        return true;
    }

    @Override
    public List<Vehiculo> listAvailable(Collection<Long> excludedVehicleIds) {
        Set<Long> excludedIds = excludedVehicleIds == null
                ? Set.of()
                : Set.copyOf(excludedVehicleIds);

        return vehicleRepository.findAllByActivoTrue().stream()
                .filter(vehiculo -> vehiculo.getEstado() == EstadoVehiculo.DISPONIBLE)
                .filter(vehiculo -> !excludedIds.contains(vehiculo.getId()))
                .toList();
    }

    private void copyCreateData(VehicleCreateRequest request, Vehiculo vehiculo) {
        vehiculo.setMarca(request.getMarca());
        vehiculo.setModelo(request.getModelo());
        vehiculo.setAnio(request.getAnio());
        vehiculo.setColor(request.getColor());
        vehiculo.setTipo(request.getTipo());
        vehiculo.setPrecioDiario(request.getPrecioDiario());
    }
}
