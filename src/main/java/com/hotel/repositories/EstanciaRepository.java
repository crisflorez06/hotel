package com.hotel.repositories;

import com.hotel.models.Estancia;
import com.hotel.models.enums.EstadoEstancia;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstanciaRepository extends JpaRepository<Estancia, Long>, JpaSpecificationExecutor<Estancia> {
    boolean existsByCodigoFolio(String codigoFolio);

    Optional<Estancia> findByReserva_Id(Long reservaId);

    @Query("""
           select distinct e
           from Estancia e
           join fetch e.ocupantes o
           where lower(o.numeroDocumento) in :documentos
             and e.estado <> :estadoExcluido
           """)
    List<Estancia> findByOcupantesNumeroDocumentoInAndEstadoNot(
            @Param("documentos") Set<String> documentos,
            @Param("estadoExcluido") EstadoEstancia estadoExcluido);

    @Query("""
           select e
           from Estancia e
           join e.habitaciones h
           where h.id = :habitacionId
             and (e.estado = com.hotel.models.enums.EstadoEstancia.ACTIVA
                  or e.estado = com.hotel.models.enums.EstadoEstancia.EXCEDIDA)
           """)
    Optional<Estancia> findActivaOExcedidaPorHabitacionId(
            @Param("habitacionId") Long habitacionId
    );
}
