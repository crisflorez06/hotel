package com.hotel.repositories;

import com.hotel.models.Habitacion;
import com.hotel.models.enums.TipoUnidad;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface HabitacionRepository extends JpaRepository<Habitacion, Long>, JpaSpecificationExecutor<Habitacion> {

    Optional<Habitacion> findByCodigo(String codigo);
    List<Habitacion> findByCodigoContainingIgnoreCaseOrderByCodigoAsc(String codigo);

    @Query("""
            select h
            from Habitacion h
            where h.unidad.tipo = :tipoUnidad
              and h.unidad.tipo in (
                    com.hotel.models.enums.TipoUnidad.APARTAMENTO,
                    com.hotel.models.enums.TipoUnidad.APARTAESTUDIO
              )
            order by h.codigo asc
            """)
    List<Habitacion> findHabitacionesByTipoUnidadApartamentoOApartaestudio(@Param("tipoUnidad") TipoUnidad tipoUnidad);
}
