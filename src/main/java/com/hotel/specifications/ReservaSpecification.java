package com.hotel.specifications;

import com.hotel.models.Reserva;
import com.hotel.models.enums.CanalReserva;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.models.enums.ModoOcupacion;
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
            String codigoUnidad,
            List<EstadoReserva> estados) {
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

            if (estados != null && !estados.isEmpty()) {
                predicates.add(root.get("estado").in(estados));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Reserva> byTablaFilters(
            List<EstadoReserva> estados,
            List<CanalReserva> canales,
            ModoOcupacion modoOcupacion,
            TipoUnidad tipoUnidad,
            String codigoReserva,
            String codigoUnidad,
            String nombreCliente,
            String numeroDocumentoCliente,
            Long idCliente,
            LocalDateTime fechaCreacionDesde,
            LocalDateTime fechaCreacionHasta,
            LocalDateTime entradaDesde,
            LocalDateTime entradaHasta,
            LocalDateTime salidaDesde,
            LocalDateTime salidaHasta,
            Boolean tieneEstanciaAsociada) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            query.distinct(true);

            if (estados != null && !estados.isEmpty()) {
                predicates.add(root.get("estado").in(estados));
            }
            if (canales != null && !canales.isEmpty()) {
                predicates.add(root.get("canalReserva").in(canales));
            }
            if (modoOcupacion != null) {
                predicates.add(criteriaBuilder.equal(root.get("modoOcupacion"), modoOcupacion));
            }
            if (codigoReserva != null && !codigoReserva.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("codigo")),
                        "%" + codigoReserva.toLowerCase() + "%"));
            }
            if (nombreCliente != null && !nombreCliente.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(criteriaBuilder.concat(
                                criteriaBuilder.concat(root.get("cliente").get("nombres"), " "),
                                root.get("cliente").get("apellidos"))),
                        "%" + nombreCliente.toLowerCase() + "%"));
            }
            if (numeroDocumentoCliente != null && !numeroDocumentoCliente.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("cliente").get("numeroDocumento")),
                        "%" + numeroDocumentoCliente.toLowerCase() + "%"));
            }
            if (idCliente != null) {
                predicates.add(criteriaBuilder.equal(root.get("cliente").get("id"), idCliente));
            }

            if (fechaCreacionDesde != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fechaCreacion"), fechaCreacionDesde));
            }
            if (fechaCreacionHasta != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fechaCreacion"), fechaCreacionHasta));
            }
            if (entradaDesde != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("entradaEstimada"), entradaDesde));
            }
            if (entradaHasta != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("entradaEstimada"), entradaHasta));
            }
            if (salidaDesde != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("salidaEstimada"), salidaDesde));
            }
            if (salidaHasta != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("salidaEstimada"), salidaHasta));
            }

            if (tieneEstanciaAsociada != null) {
                predicates.add(tieneEstanciaAsociada
                        ? criteriaBuilder.isNotNull(root.get("estancia"))
                        : criteriaBuilder.isNull(root.get("estancia")));
            }

            Join<?, ?> habitacionJoin = root.join("habitaciones", JoinType.LEFT);
            Join<?, ?> unidadJoin = habitacionJoin.join("unidad", JoinType.LEFT);

            if (tipoUnidad != null) {
                predicates.add(criteriaBuilder.equal(unidadJoin.get("tipo"), tipoUnidad));
            }
            if (codigoUnidad != null && !codigoUnidad.isBlank()) {
                Predicate codigoHabitacion = criteriaBuilder.like(
                        criteriaBuilder.lower(habitacionJoin.get("codigo")),
                        "%" + codigoUnidad.toLowerCase() + "%");
                Predicate codigoUnidadPred = criteriaBuilder.like(
                        criteriaBuilder.lower(unidadJoin.get("codigo")),
                        "%" + codigoUnidad.toLowerCase() + "%");
                predicates.add(criteriaBuilder.or(codigoHabitacion, codigoUnidadPred));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
