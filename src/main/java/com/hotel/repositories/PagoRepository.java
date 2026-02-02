package com.hotel.repositories;

import com.hotel.models.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PagoRepository extends JpaRepository<Pago, Long>, JpaSpecificationExecutor<Pago> {

    boolean existsByEstanciaId(Long idEstancia);

    boolean existsByReservaId(Long idReserva);
}
