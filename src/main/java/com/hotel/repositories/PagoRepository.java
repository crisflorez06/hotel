package com.hotel.repositories;

import com.hotel.models.Estancia;
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

    List<Pago> findByEstanciaIdAndTipoPagoAndEstadoIn(
            Long idEstancia,
            TipoPago tipoPago,
            List<EstadoPago> estados
    );

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
           where p.estado = :estado
             and (:desde is null or p.fecha >= :desde)
             and (:hasta is null or p.fecha <= :hasta)
           """)
    BigDecimal sumarMontoPorEstadoConRangoOpcional(
            @Param("estado") EstadoPago estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("""
           select count(p)
           from Pago p
           where p.estado = :estado
             and p.tipoPago = :tipoPago
             and (:desde is null or p.fecha >= :desde)
             and (:hasta is null or p.fecha <= :hasta)
           """)
    long countByEstadoAndTipoPagoConRangoOpcional(
            @Param("estado") EstadoPago estado,
            @Param("tipoPago") TipoPago tipoPago,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("""
           select coalesce(sum(p.monto), 0)
           from Pago p
           where p.estado = :estado
             and p.tipoPago = :tipoPago
             and (:desde is null or p.fecha >= :desde)
             and (:hasta is null or p.fecha <= :hasta)
           """)
    BigDecimal sumarMontoPorEstadoAndTipoPagoConRangoOpcional(
            @Param("estado") EstadoPago estado,
            @Param("tipoPago") TipoPago tipoPago,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("""
           select p.tipoPago, coalesce(sum(p.monto), 0)
           from Pago p
           where p.estado in :estados
             and (p.estado <> com.hotel.models.enums.EstadoPago.PENDIENTE or p.tipoPago = :tipoPagoPendiente)
             and (:desde is null or p.fecha >= :desde)
             and (:hasta is null or p.fecha <= :hasta)
           group by p.tipoPago
           order by p.tipoPago
           """)
    List<Object[]> sumarIngresosPorTipoPago(
            @Param("estados") List<EstadoPago> estados,
            @Param("tipoPagoPendiente") TipoPago tipoPagoPendiente,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("""
           select p.medioPago, coalesce(sum(p.monto), 0), count(p)
           from Pago p
           where p.estado in :estados
              and (p.estado <> com.hotel.models.enums.EstadoPago.PENDIENTE or p.tipoPago = :tipoPagoPendiente)
               and (:desde is null or p.fecha >= :desde)
               and (:hasta is null or p.fecha <= :hasta)
           group by p.medioPago
           order by p.medioPago
           """)
    List<Object[]> sumarYContarIngresosPorMedioPago(
            @Param("estados") List<EstadoPago> estados,
            @Param("tipoPagoPendiente") TipoPago tipoPagoPendiente,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

}
