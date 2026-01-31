package com.hotel.repositories;

import com.hotel.models.Estancia;
import com.hotel.models.EstanciaHabitacion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstanciaHabitacionRepository extends JpaRepository<EstanciaHabitacion, Long> {

    List<EstanciaHabitacion> findByEstancia_Id(Long estanciaId);

    @Query("""
           select eh.estancia
           from EstanciaHabitacion eh
           where eh.habitacion.id = :habitacionId
             and eh.estadoOcupacion = true
             and eh.estancia.activo = true
           """)
    Optional<Estancia> findActiveEstanciaByHabitacionId(@Param("habitacionId") Long habitacionId);
}
