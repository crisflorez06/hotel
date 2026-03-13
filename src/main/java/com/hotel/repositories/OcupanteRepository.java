package com.hotel.repositories;

import com.hotel.models.Ocupante;
import com.hotel.models.enums.TipoDocumento;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;

public interface OcupanteRepository extends JpaRepository<Ocupante, Long>, JpaSpecificationExecutor<Ocupante> {

    List<Ocupante> findByNumeroDocumentoContaining(String numeroDocumento);

    @Query("SELECT o FROM Ocupante o WHERE LOWER(TRIM(o.numeroDocumento)) = LOWER(TRIM(:numeroDocumento))")
    List<Ocupante> findByNumeroDocumentoNormalizado(@Param("numeroDocumento") String numeroDocumento);

    @Query("""
            SELECT o FROM Ocupante o
            WHERE o.tipoDocumento = :tipoDocumento
              AND LOWER(TRIM(o.numeroDocumento)) = LOWER(TRIM(:numeroDocumento))
            """)
    List<Ocupante> findByTipoDocumentoAndNumeroDocumentoNormalizado(
            @Param("tipoDocumento") TipoDocumento tipoDocumento,
            @Param("numeroDocumento") String numeroDocumento);
}
