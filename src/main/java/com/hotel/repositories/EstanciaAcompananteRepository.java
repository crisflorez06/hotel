package com.hotel.repositories;

import com.hotel.models.Acompanante;
import com.hotel.models.EstanciaAcompanante;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstanciaAcompananteRepository extends JpaRepository<EstanciaAcompanante, Long> {

    @Query("select ea.acompanante from EstanciaAcompanante ea where ea.estancia.id = :estanciaId")
    List<Acompanante> findAcompanantesByEstanciaId(@Param("estanciaId") Long estanciaId);
}
