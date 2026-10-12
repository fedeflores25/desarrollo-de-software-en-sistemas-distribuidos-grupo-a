package com.rentar.customer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.rentar.customer.entity.Cliente;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    boolean existsByDocumento(String documento);

    boolean existsByEmail(String email);

}