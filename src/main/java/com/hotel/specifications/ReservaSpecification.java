package com.hotel.specifications;

import com.hotel.models.Reserva;
import com.hotel.models.enums.TipoUnidad;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class ReservaSpecification {

    public static Specification<Reserva> byCalendario(
            LocalDateTime desde,
            LocalDateTime hasta,
            TipoUnidad tipoUnidad,
            String codigoUnidad) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            query.distinct(true);

            if (desde != null && hasta != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("entradaEstimada"), hasta));
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("salidaEstimada"), desde));
            } else if (desde != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("salidaEstimada"), desde));
            } else if (hasta != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("entradaEstimada"), hasta));
            }

            Join<?, ?> habitacionJoin = root.join("habitaciones", JoinType.INNER);
            Join<?, ?> unidadJoin = habitacionJoin.join("unidad", JoinType.INNER);

            if (tipoUnidad != null) {
                predicates.add(criteriaBuilder.equal(unidadJoin.get("tipo"), tipoUnidad));
            }
            if (codigoUnidad != null && !codigoUnidad.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(unidadJoin.get("codigo")),
                        "%" + codigoUnidad.toLowerCase() + "%"));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
