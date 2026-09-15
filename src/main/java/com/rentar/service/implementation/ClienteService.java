package com.rentar.service.implementation;

import java.util.List;

import org.springframework.stereotype.Service;

import com.rentar.dto.ClienteRequest;
import com.rentar.entity.Cliente;
import com.rentar.exception.ClienteNoEncontradoException;
import com.rentar.repository.ClienteRepository;
import com.rentar.service.IClienteService;

@Service
public class ClienteService implements IClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    // ALTA
    public Cliente crear(ClienteRequest request) {

        if (clienteRepository.existsByDocumento(request.getDocumento())) {
            throw new IllegalArgumentException(
                    "Ya existe un cliente con ese documento");
        }

        if (clienteRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(
                    "Ya existe un cliente con ese email");
        }

        Cliente cliente = new Cliente();

        cliente.setDocumento(request.getDocumento());
        cliente.setNombre(request.getNombre());
        cliente.setApellido(request.getApellido());
        cliente.setEmail(request.getEmail());
        cliente.setTelefono(request.getTelefono());
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        cliente.setActivo(true);

        return clienteRepository.save(cliente);
    }

    // CONSULTA DE TODOS
    public List<Cliente> listar() {
        return clienteRepository.findAll();
    }

    // CONSULTA POR ID
    public Cliente buscarPorId(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() ->
                    new ClienteNoEncontradoException(
                            "No existe un cliente con id " + id));
    }

    // MODIFICACION
    public Cliente modificar(Long id, ClienteRequest request) {

        Cliente cliente = buscarPorId(id);

        if (!cliente.getDocumento().equals(request.getDocumento())
                && clienteRepository.existsByDocumento(request.getDocumento())) {

            throw new IllegalArgumentException(
                    "Ya existe un cliente con ese documento");
        }

        if (!cliente.getEmail().equals(request.getEmail())
                && clienteRepository.existsByEmail(request.getEmail())) {

            throw new IllegalArgumentException(
                    "Ya existe un cliente con ese email");
        }

        cliente.setDocumento(request.getDocumento());
        cliente.setNombre(request.getNombre());
        cliente.setApellido(request.getApellido());
        cliente.setEmail(request.getEmail());
        cliente.setTelefono(request.getTelefono());
        cliente.setFechaNacimiento(request.getFechaNacimiento());

        return clienteRepository.save(cliente);
    }

    // BAJA
    public void darDeBaja(Long id) {

        Cliente cliente = buscarPorId(id);

        cliente.setActivo(false);

        clienteRepository.save(cliente);
    }
}