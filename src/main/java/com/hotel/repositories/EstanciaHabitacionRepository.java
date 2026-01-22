package com.hotel.repositories;

import com.hotel.models.EstanciaHabitacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EstanciaHabitacionRepository extends JpaRepository<EstanciaHabitacion, Long> {

    List<EstanciaHabitacion> findByEstancia_Id(Long estanciaId);
}
