package com.hotel.repositories;

import com.hotel.models.Gasto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GastoRepository extends JpaRepository<Gasto, Long> {

    @Query("""
           select coalesce(sum(g.monto), 0)
           from Gasto g
           where (:desde is null or g.fecha >= :desde)
             and (:hasta is null or g.fecha <= :hasta)
           """)
    BigDecimal sumarMontoEnRango(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
