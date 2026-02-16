package com.hotel.specifications;

import com.hotel.models.AuditoriaEvento;
import com.hotel.models.enums.TipoEntidad;
import com.hotel.models.enums.TipoEvento;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class AuditoriaEventoSpecification {

    private AuditoriaEventoSpecification() {
    }

    public static Specification<AuditoriaEvento> byFilters(
            List<TipoEvento> tiposEvento,
            List<TipoEntidad> entidades,
            Long idEntidad,
            String codigoEstancia,
            String codigoReserva,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            String detalle) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (tiposEvento != null && !tiposEvento.isEmpty()) {
                predicates.add(root.get("tipoEvento").in(tiposEvento));
            }
            if (entidades != null && !entidades.isEmpty()) {
                predicates.add(root.get("entidad").in(entidades));
            }
            if (idEntidad != null) {
                predicates.add(criteriaBuilder.equal(root.get("idEntidad"), idEntidad));
            }
            if (codigoEstancia != null && !codigoEstancia.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("codigoEstancia")),
                        "%" + codigoEstancia.toLowerCase() + "%"));
            }
            if (codigoReserva != null && !codigoReserva.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("codigoReserva")),
                        "%" + codigoReserva.toLowerCase() + "%"));
            }
            if (fechaDesde != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fecha"), fechaDesde));
            }
            if (fechaHasta != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fecha"), fechaHasta));
            }
            if (detalle != null && !detalle.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("detalle")),
                        "%" + detalle.toLowerCase() + "%"));
            }

            query.orderBy(criteriaBuilder.desc(root.get("fecha")));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
