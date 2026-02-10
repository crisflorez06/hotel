package com.hotel.repositories;

import com.hotel.models.Pago;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.hotel.models.enums.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoRepository extends JpaRepository<Pago, Long>, JpaSpecificationExecutor<Pago> {

    Optional<Pago> findFirstByEstanciaIdOrderByFechaCreacionDesc(Long idEstancia);

    List<Pago> findByEstanciaId(Long idEstancia);

    @Query("""
       SELECT COALESCE(SUM(p.monto), 0)
       FROM Pago p
       WHERE p.estancia.id = :idEstancia
         AND p.estado IN :estados
       """)
    BigDecimal sumarMontoPorEstanciaYEstados(@Param("idEstancia") Long idEstancia,
                                             @Param("estados") List<EstadoPago> estados);


}
