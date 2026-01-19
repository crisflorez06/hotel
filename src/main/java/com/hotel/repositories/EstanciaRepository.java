package com.hotel.repositories;

import com.hotel.models.Estancia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstanciaRepository extends JpaRepository<Estancia, Long> {
}
