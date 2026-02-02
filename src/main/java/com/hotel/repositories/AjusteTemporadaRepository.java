package com.hotel.repositories;

import com.hotel.models.AjusteTemporada;
import com.hotel.models.enums.Temporada;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AjusteTemporadaRepository extends JpaRepository<AjusteTemporada, Long> {

    Optional<AjusteTemporada> findByTemporada(Temporada temporada);

    Optional<AjusteTemporada> findByActivoTrue();
}
