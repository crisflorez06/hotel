package com.hotel.repositories;

import com.hotel.models.Gasto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GastoRepository extends JpaRepository<Gasto, Long> {

    @Query("""
           select coalesce(sum(g.monto), 0)
           from Gasto g
           where g.fecha between :desde and :hasta
           """)
    BigDecimal sumarMontoEnRango(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("""
           select function('date', g.fecha), coalesce(sum(g.monto), 0)
           from Gasto g
           where g.fecha between :desde and :hasta
           group by function('date', g.fecha)
           order by function('date', g.fecha)
           """)
    List<Object[]> sumarGastosDiarios(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("""
           select g.concepto, coalesce(sum(g.monto), 0)
           from Gasto g
           where g.fecha between :desde and :hasta
           group by g.concepto
           order by g.concepto
           """)
    List<Object[]> sumarGastosPorConcepto(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
