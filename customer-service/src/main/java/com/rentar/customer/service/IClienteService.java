package com.rentar.customer.service;

import java.util.List;

import com.rentar.customer.dto.ClienteRequest;
import com.rentar.customer.entity.Cliente;

public interface IClienteService {

    // Registrar un nuevo cliente
    Cliente crear(ClienteRequest request);

    // Consultar todos los clientes
    List<Cliente> listar();

    // Consultar un cliente por ID
    Cliente buscarPorId(Long id);

    // Modificar los datos de un cliente
    Cliente modificar(Long id, ClienteRequest request);

    // Dar de baja logicamente a un cliente
    void darDeBaja(Long id);

}