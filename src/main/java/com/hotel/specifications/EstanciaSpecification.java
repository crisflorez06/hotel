package com.hotel.specifications;

import com.hotel.models.Estancia;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.models.enums.ModoOcupacion;
import com.hotel.models.enums.TipoOcupante;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.data.jpa.domain.Specification;

public class EstanciaSpecification {

    public static Specification<Estancia> byCalendario(
            LocalDateTime desde,
            LocalDateTime hasta,
            List<String> codigosHabitaciones,
            List<EstadoEstancia> estados) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query != null) {
                query.distinct(true);
            }

            Join<?, ?> reservaJoin = root.join("reserva", JoinType.LEFT);
            Expression<LocalDateTime> inicio = criteriaBuilder.coalesce(
                    root.get("entradaReal"),
                    reservaJoin.get("entradaEstimada"));

            Expression<LocalDateTime> fin = criteriaBuilder.coalesce(
                    root.get("salidaReal"),
                    root.get("salidaEstimada"));

            if (desde != null && hasta != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(inicio, hasta));
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(fin, desde));
            } else if (desde != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(fin, desde));
            } else if (hasta != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(inicio, hasta));
            }

            Join<?, ?> habitacionJoin = root.join("habitaciones", JoinType.INNER);
            if (codigosHabitaciones != null && !codigosHabitaciones.isEmpty()) {
                List<String> codigosNormalizados = codigosHabitaciones.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(codigo -> !codigo.isEmpty())
                        .map(String::toLowerCase)
                        .toList();

                if (!codigosNormalizados.isEmpty()) {
                    predicates.add(criteriaBuilder.lower(habitacionJoin.get("codigo")).in(codigosNormalizados));
                }
            }

            List<EstadoEstancia> estadosFiltrados = estados == null
                    ? List.of()
                    : estados.stream()
                    .filter(estado -> estado != EstadoEstancia.RESERVADA)
                    .toList();

            if (!estadosFiltrados.isEmpty()) {
                List<EstadoEstancia> estadosSinCancelada = estadosFiltrados.stream()
                        .filter(estado -> estado != EstadoEstancia.CANCELADA)
                        .toList();

                Predicate estadoPredicate = null;
                if (!estadosSinCancelada.isEmpty()) {
                    estadoPredicate = root.get("estado").in(estadosSinCancelada);
                }

                if (estadosFiltrados.contains(EstadoEstancia.CANCELADA)) {
                    Predicate canceladaValida = criteriaBuilder.and(
                            criteriaBuilder.equal(root.get("estado"), EstadoEstancia.CANCELADA),
                            criteriaBuilder.or(
                                    criteriaBuilder.isNull(root.get("reserva")),
                                    criteriaBuilder.and(
                                            criteriaBuilder.isNotNull(root.get("reserva")),
                                            criteriaBuilder.isNotNull(root.get("entradaReal")))));

                    estadoPredicate = estadoPredicate == null
                            ? canceladaValida
                            : criteriaBuilder.or(estadoPredicate, canceladaValida);
                }

                if (estadoPredicate != null) {
                    predicates.add(estadoPredicate);
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Estancia> byTablaFilters(
            Long idEstancia,
            List<EstadoEstancia> estados,
            TipoUnidad tipoUnidad,
            ModoOcupacion modoOcupacion,
            String codigoEstancia,
            String codigoUnidad,
            String nombreCliente,
            String numeroDocumentoCliente,
            Long idCliente,
            LocalDateTime entradaDesde,
            LocalDateTime entradaHasta,
            LocalDateTime salidaEstimadaDesde,
            LocalDateTime salidaEstimadaHasta,
            LocalDateTime salidaRealDesde,
            LocalDateTime salidaRealHasta,
            LocalDateTime rangoGeneralDesde,
            LocalDateTime rangoGeneralHasta,
            Boolean tieneReservaAsociada) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            query.distinct(true);

            if (idEstancia != null) {
                return criteriaBuilder.equal(root.get("id"), idEstancia);
            }

            if (estados != null && !estados.isEmpty()) {
                predicates.add(root.get("estado").in(estados));
            }
            if (modoOcupacion != null) {
                predicates.add(criteriaBuilder.equal(root.get("modoOcupacion"), modoOcupacion));
            }
            if (codigoEstancia != null && !codigoEstancia.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("codigoFolio")),
                        "%" + codigoEstancia.toLowerCase() + "%"));
            }

            if (entradaDesde != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("entradaReal"), entradaDesde));
            }
            if (entradaHasta != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("entradaReal"), entradaHasta));
            }
            if (salidaEstimadaDesde != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("salidaEstimada"), salidaEstimadaDesde));
            }
            if (salidaEstimadaHasta != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("salidaEstimada"), salidaEstimadaHasta));
            }
            if (salidaRealDesde != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("salidaReal"), salidaRealDesde));
            }
            if (salidaRealHasta != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("salidaReal"), salidaRealHasta));
            }
            if (rangoGeneralDesde != null || rangoGeneralHasta != null) {
                Predicate entradaEnRango;
                Predicate salidaEstimadaEnRango;
                Predicate salidaRealEnRango;

                if (rangoGeneralDesde != null && rangoGeneralHasta != null) {
                    entradaEnRango = criteriaBuilder.between(
                            root.get("entradaReal"),
                            rangoGeneralDesde,
                            rangoGeneralHasta);
                    salidaEstimadaEnRango = criteriaBuilder.between(
                            root.get("salidaEstimada"),
                            rangoGeneralDesde,
                            rangoGeneralHasta);
                    salidaRealEnRango = criteriaBuilder.between(
                            root.get("salidaReal"),
                            rangoGeneralDesde,
                            rangoGeneralHasta);
                } else if (rangoGeneralDesde != null) {
                    entradaEnRango = criteriaBuilder.greaterThanOrEqualTo(root.get("entradaReal"), rangoGeneralDesde);
                    salidaEstimadaEnRango = criteriaBuilder.greaterThanOrEqualTo(root.get("salidaEstimada"), rangoGeneralDesde);
                    salidaRealEnRango = criteriaBuilder.greaterThanOrEqualTo(root.get("salidaReal"), rangoGeneralDesde);
                } else {
                    entradaEnRango = criteriaBuilder.lessThanOrEqualTo(root.get("entradaReal"), rangoGeneralHasta);
                    salidaEstimadaEnRango = criteriaBuilder.lessThanOrEqualTo(root.get("salidaEstimada"), rangoGeneralHasta);
                    salidaRealEnRango = criteriaBuilder.lessThanOrEqualTo(root.get("salidaReal"), rangoGeneralHasta);
                }

                predicates.add(criteriaBuilder.or(entradaEnRango, salidaEstimadaEnRango, salidaRealEnRango));
            }

            if (tieneReservaAsociada != null) {
                predicates.add(tieneReservaAsociada
                        ? criteriaBuilder.isNotNull(root.get("reserva"))
                        : criteriaBuilder.isNull(root.get("reserva")));
            }

            Join<?, ?> habitacionJoin = root.join("habitaciones", JoinType.LEFT);
            Join<?, ?> unidadJoin = habitacionJoin.join("unidad", JoinType.LEFT);
            if (tipoUnidad != null) {
                if (tipoUnidad == TipoUnidad.HABITACION) {
                    predicates.add(criteriaBuilder.equal(root.get("modoOcupacion"), ModoOcupacion.INDIVIDUAL));
                } else {
                    predicates.add(criteriaBuilder.equal(unidadJoin.get("tipo"), tipoUnidad));
                    predicates.add(criteriaBuilder.equal(root.get("modoOcupacion"), ModoOcupacion.COMPLETO));
                }
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

            Join<?, ?> ocupanteJoin = root.join("ocupantes", JoinType.LEFT);
            Predicate clientePredicate = criteriaBuilder.equal(
                    ocupanteJoin.get("tipoOcupante"),
                    TipoOcupante.CLIENTE);
            predicates.add(clientePredicate);

            if (nombreCliente != null && !nombreCliente.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(criteriaBuilder.concat(
                                criteriaBuilder.concat(ocupanteJoin.get("nombres"), " "),
                                ocupanteJoin.get("apellidos"))),
                        "%" + nombreCliente.toLowerCase() + "%"));
            }
            if (numeroDocumentoCliente != null && !numeroDocumentoCliente.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(ocupanteJoin.get("numeroDocumento")),
                        "%" + numeroDocumentoCliente.toLowerCase() + "%"));
            }
            if (idCliente != null) {
                predicates.add(criteriaBuilder.equal(ocupanteJoin.get("id"), idCliente));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
