package com.hotel.repositories;

import com.hotel.models.Pago;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.hotel.models.enums.EstadoPago;
import com.hotel.models.enums.TipoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoRepository extends JpaRepository<Pago, Long>, JpaSpecificationExecutor<Pago> {

    Optional<Pago> findFirstByEstanciaIdAndTipoPagoOrderByFechaCreacionDesc(Long idEstancia, TipoPago tipoPago);

    List<Pago> findByEstanciaId(Long idEstancia);

    @Query("""
       SELECT COALESCE(SUM(p.monto), 0)
       FROM Pago p
       WHERE p.estancia.id = :idEstancia
         AND p.estado IN :estados
       """)
    BigDecimal sumarMontoPorEstanciaYEstados(@Param("idEstancia") Long idEstancia,
                                             @Param("estados") List<EstadoPago> estados);

    @Query("""
           select coalesce(sum(p.monto), 0)
           from Pago p
           where p.estado in :estados
             and p.fecha between :desde and :hasta
           """)
    BigDecimal sumarMontoPorEstadosYRango(
            @Param("estados") List<EstadoPago> estados,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    long countByEstadoAndFechaBetween(EstadoPago estado, LocalDateTime desde, LocalDateTime hasta);

    @Query("""
           select coalesce(sum(p.monto), 0)
           from Pago p
           where p.estado = :estado
             and p.fecha between :desde and :hasta
           """)
    BigDecimal sumarMontoPorEstadoYRango(
            @Param("estado") EstadoPago estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("""
           select function('date', p.fecha), coalesce(sum(p.monto), 0)
           from Pago p
           where p.estado in :estados
             and p.fecha between :desde and :hasta
           group by function('date', p.fecha)
           order by function('date', p.fecha)
           """)
    List<Object[]> sumarIngresosDiarios(
            @Param("estados") List<EstadoPago> estados,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("""
           select p.tipoPago, coalesce(sum(p.monto), 0)
           from Pago p
           where p.estado in :estados
             and p.fecha between :desde and :hasta
           group by p.tipoPago
           order by p.tipoPago
           """)
    List<Object[]> sumarIngresosPorTipoPago(
            @Param("estados") List<EstadoPago> estados,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("""
           select p.medioPago, coalesce(sum(p.monto), 0)
           from Pago p
           where p.estado in :estados
             and p.fecha between :desde and :hasta
           group by p.medioPago
           order by p.medioPago
           """)
    List<Object[]> sumarIngresosPorMedioPago(
            @Param("estados") List<EstadoPago> estados,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("""
           select p
           from Pago p
           left join fetch p.estancia e
           left join fetch e.reserva r
           where p.estado = :estado
             and p.fecha <= :fechaLimite
           order by p.fecha asc
           """)
    List<Pago> findPendientesHastaFecha(
            @Param("estado") EstadoPago estado,
            @Param("fechaLimite") LocalDateTime fechaLimite);

    @Query("""
           select count(p)
           from Pago p
           where p.estado = :estado
             and p.fecha <= :fechaLimite
           """)
    long countPendientesHastaFecha(
            @Param("estado") EstadoPago estado,
            @Param("fechaLimite") LocalDateTime fechaLimite);

}
