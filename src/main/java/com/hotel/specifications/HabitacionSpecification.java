package com.hotel.specifications;

import com.hotel.models.Habitacion;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.Piso;
import com.hotel.models.enums.TipoUnidad;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class HabitacionSpecification {

    public static Specification<Habitacion> byFilters(
            List<EstadoOperativo> estados,
            List<Piso> pisos,
            String codigo) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("unidad").get("tipo"), TipoUnidad.APARTAMENTO));

            if (estados != null && !estados.isEmpty()) {
                predicates.add(root.get("estadoOperativo").in(estados));
            }
            if (pisos != null && !pisos.isEmpty()) {
                predicates.add(root.get("unidad").get("piso").in(pisos));
            }
            if (codigo != null && !codigo.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("codigo")),
                        "%" + codigo.toLowerCase() + "%"));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
