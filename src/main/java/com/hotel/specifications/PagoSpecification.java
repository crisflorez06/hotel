package com.hotel.specifications;

import com.hotel.models.Pago;
import com.hotel.models.enums.EstadoPago;
import com.hotel.models.enums.MedioPago;
import com.hotel.models.enums.TipoPago;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class PagoSpecification {

    public static Specification<Pago> byFilters(
            Long idPago,
            List<EstadoPago> estados,
            List<MedioPago> mediosPago,
            TipoPago tipoPago,
            String codigoEstancia,
            String codigoReserva,
            LocalDateTime desde,
            LocalDateTime hasta) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (idPago != null) {
                return criteriaBuilder.equal(root.get("id"), idPago);
            }

            // Los pagos por cambio de unidad son internos del sistema y no deben aparecer
            // en la tabla de pagos del frontend, sin importar los filtros aplicados.
            predicates.add(criteriaBuilder.notEqual(root.get("tipoPago"), TipoPago.CAMBIO_UNIDAD));

            if (estados != null && !estados.isEmpty()) {
                predicates.add(root.get("estado").in(estados));
            }
            if (mediosPago != null && !mediosPago.isEmpty()) {
                predicates.add(root.get("medioPago").in(mediosPago));
            }
            if (tipoPago != null) {
                predicates.add(criteriaBuilder.equal(root.get("tipoPago"), tipoPago));
            }
            if (codigoEstancia != null && !codigoEstancia.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.join("estancia", JoinType.LEFT).get("codigoFolio")),
                        "%" + codigoEstancia.toLowerCase() + "%"));
            }
            if (codigoReserva != null && !codigoReserva.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("tipoPago"), TipoPago.ANTICIPO_RESERVA));
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(
                                root.join("estancia", JoinType.LEFT)
                                        .join("reserva", JoinType.LEFT)
                                        .get("codigo")),
                        "%" + codigoReserva.toLowerCase() + "%"));
            }
            if (desde != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fecha"), desde));
            }
            if (hasta != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fecha"), hasta));
            }

            query.orderBy(criteriaBuilder.desc(root.get("fecha")));
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
