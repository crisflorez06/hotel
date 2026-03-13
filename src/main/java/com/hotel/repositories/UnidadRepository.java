package com.hotel.repositories;

import com.hotel.models.Unidad;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.TipoUnidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UnidadRepository extends JpaRepository<Unidad, Long>, JpaSpecificationExecutor<Unidad> {

    Optional<Unidad> findByCodigo(String codigo);

    long countByTipo(TipoUnidad tipo);

    @Query("""
           select count(u)
           from Unidad u
           where u.tipo = :tipoUnidad
             and exists (
                select 1
                from Habitacion h
                where h.unidad = u
             )
             and not exists (
                select 1
                from Habitacion h
                where h.unidad = u
                  and h.estadoOperativo <> :estadoOperativo
             )
           """)
    long countUnidadesConTodasHabitacionesEnEstadoPorTipo(
            @Param("tipoUnidad") TipoUnidad tipoUnidad,
            @Param("estadoOperativo") EstadoOperativo estadoOperativo);

    @Query("""
           select count(distinct u.id)
           from Unidad u
           join u.habitaciones h
           where u.tipo = :tipoUnidad
             and h.estadoOperativo = :estadoOperativo
           """)
    long countUnidadesConHabitacionEnEstadoPorTipo(
            @Param("tipoUnidad") TipoUnidad tipoUnidad,
            @Param("estadoOperativo") EstadoOperativo estadoOperativo);
}
