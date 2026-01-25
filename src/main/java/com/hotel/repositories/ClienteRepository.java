package com.hotel.repositories;

import com.hotel.models.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByEstancias_Id(Long idEstancia);
}
