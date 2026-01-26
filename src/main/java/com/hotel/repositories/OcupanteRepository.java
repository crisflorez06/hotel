package com.hotel.repositories;

import com.hotel.models.Ocupante;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OcupanteRepository extends JpaRepository<Ocupante, Long> {

    List<Ocupante> findByNumeroDocumentoContaining(String numeroDocumento);
}
