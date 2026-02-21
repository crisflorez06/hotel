package com.hotel.repositories;

import com.hotel.models.AuditoriaEvento;
import com.hotel.models.enums.TipoEntidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface AuditoriaEventoRepository extends JpaRepository<AuditoriaEvento, Long>, JpaSpecificationExecutor<AuditoriaEvento> {
    List<AuditoriaEvento> findByEntidadAndIdEntidadOrderByFechaDesc(TipoEntidad entidad, Long idEntidad);
    Optional<AuditoriaEvento> findFirstByEntidadAndIdEntidadOrderByFechaDesc(TipoEntidad entidad, Long idEntidad);
}
