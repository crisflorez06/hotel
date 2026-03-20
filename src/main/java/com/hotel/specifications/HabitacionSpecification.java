package com.hotel.specifications;

import com.hotel.models.Habitacion;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.Piso;
import com.hotel.models.enums.TipoUnidad;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public class HabitacionSpecification {

    public static Specification<Habitacion> byFilters(
            List<EstadoOperativo> estados,
            List<Piso> pisos,
            String codigo) {
        String codigoNormalizado = normalizarCodigo(codigo);

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("unidad").get("tipo"), TipoUnidad.APARTAMENTO));

            if (estados != null && !estados.isEmpty()) {
                predicates.add(root.get("estadoOperativo").in(estados));
            }
            if (pisos != null && !pisos.isEmpty()) {
                predicates.add(root.get("unidad").get("piso").in(pisos));
            }
            if (codigoNormalizado != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("codigo")),
                        "%" + codigoNormalizado + "%"));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String normalizarCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }

        String codigoLimpio = codigo.trim();
        if (codigoLimpio.isEmpty()) {
            return null;
        }

        return codigoLimpio.toLowerCase(Locale.ROOT);
    }
}
