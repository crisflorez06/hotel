package com.hotel.specifications;

import com.hotel.models.Ocupante;
import com.hotel.models.enums.TipoDocumento;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class OcupanteSpecification {

    public static Specification<Ocupante> byTablaFilters(
            String nombre,
            String apellido,
            TipoDocumento tipoDocumento,
            String numeroDocumento,
            String telefono,
            String email) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (nombre != null && !nombre.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("nombres")),
                        "%" + nombre.toLowerCase() + "%"));
            }
            if (apellido != null && !apellido.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("apellidos")),
                        "%" + apellido.toLowerCase() + "%"));
            }
            if (tipoDocumento != null) {
                predicates.add(criteriaBuilder.equal(root.get("tipoDocumento"), tipoDocumento));
            }
            if (numeroDocumento != null && !numeroDocumento.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("numeroDocumento")),
                        "%" + numeroDocumento.toLowerCase() + "%"));
            }
            if (telefono != null && !telefono.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("telefono")),
                        "%" + telefono.toLowerCase() + "%"));
            }
            if (email != null && !email.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("email")),
                        "%" + email.toLowerCase() + "%"));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
