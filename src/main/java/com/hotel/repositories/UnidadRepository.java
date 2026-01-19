package com.hotel.repositories;

import com.hotel.models.Unidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UnidadRepository extends JpaRepository<Unidad, Long>, JpaSpecificationExecutor<Unidad> {

    Optional<Unidad> findByCodigo(String codigo);
}
