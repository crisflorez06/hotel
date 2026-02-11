package com.hotel.repositories;

import com.hotel.models.Estancia;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstanciaRepository extends JpaRepository<Estancia, Long>, JpaSpecificationExecutor<Estancia> {

    Optional<Estancia> findByReserva_Id(Long reservaId);

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
