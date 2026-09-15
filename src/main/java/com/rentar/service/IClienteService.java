package com.rentar.service;

import java.util.List;

import com.rentar.dto.ClienteRequest;
import com.rentar.entity.Cliente;

public interface IClienteService {

    Cliente crear(ClienteRequest request);

    List<Cliente> listar();

    Cliente buscarPorId(Long id);

    Cliente modificar(Long id, ClienteRequest request);

    void darDeBaja(Long id);
}