package com.hotel.repositories;

import com.hotel.models.Ocupante;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OcupanteRepository extends JpaRepository<Ocupante, Long>, JpaSpecificationExecutor<Ocupante> {

    List<Ocupante> findByNumeroDocumentoContaining(String numeroDocumento);
}
