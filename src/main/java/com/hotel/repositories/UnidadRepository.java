package com.hotel.repositories;

import com.hotel.models.Unidad;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UnidadRepository extends JpaRepository<Unidad, Long>, JpaSpecificationExecutor<Unidad> {

    Optional<Unidad> findByCodigo(String codigo);

    @Query("""
           select u.tipo, u.estadoOperativo, count(u)
           from Unidad u
           group by u.tipo, u.estadoOperativo
           """)
    List<Object[]> contarPorTipoYEstadoOperativo();
}
