package com.hotel.repositories;

import com.hotel.models.TarifaBase;
import com.hotel.models.enums.TipoUnidad;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TarifaBaseRepository extends JpaRepository<TarifaBase, Long> {

    Optional<TarifaBase> findByTipoUnidad(TipoUnidad tipoUnidad);
}
