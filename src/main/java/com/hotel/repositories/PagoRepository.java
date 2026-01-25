package com.hotel.repositories;

import com.hotel.models.Pago;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    boolean existsByEstanciaId(Long idEstancia);

    boolean existsByReservaId(Long idReserva);
}
