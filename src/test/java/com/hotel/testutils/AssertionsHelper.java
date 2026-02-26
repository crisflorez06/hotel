package com.hotel.testutils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.models.*;
import com.hotel.models.enums.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public final class AssertionsHelper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private AssertionsHelper() {
    }

    public static void comprobarEstanciaDb(
            Estancia estancia,
            Reserva reserva,
            Integer totalOcupantes,
            LocalDateTime entradaReal,
            LocalDateTime salidaEstimada,
            LocalDateTime fechaSalidaReal,
            String notas,
            ModoOcupacion modoOcupacion,
            EstadoEstancia estadoEstancia,
            BigDecimal precioTotal,
            Integer totalHabitaciones,
            Integer totalPagos
    ) {

        // ---- Folio ----
        assertThat(estancia.getCodigoFolio()).isNotBlank();

        // ---- Reserva ----
        if (reserva == null) {
            assertThat(estancia.getReserva()).isNull();
        } else {
            assertThat(estancia.getReserva()).isNotNull();
            assertThat(estancia.getReserva().getId()).isEqualTo(reserva.getId());
        }

        // ---- Ocupantes ----
        assertThat(estancia.getOcupantes()).hasSize(totalOcupantes);

        // ---- Fechas ----
        if(entradaReal == null) {
            assertThat(estancia.getEntradaReal()).isNull();
        } else {
        assertThat(estancia.getEntradaReal()).isEqualToIgnoringNanos(entradaReal);
        }
        if(salidaEstimada == null) {
            assertThat(estancia.getSalidaEstimada()).isNull();
        } else {
        assertThat(estancia.getSalidaEstimada()).isEqualToIgnoringNanos(salidaEstimada);
        }
        if(fechaSalidaReal == null) {
            assertThat(estancia.getSalidaReal()).isNull();
        } else {
            assertThat(estancia.getSalidaReal()).isEqualToIgnoringNanos(fechaSalidaReal);
        }

        // ---- Estados ----
        assertThat(estancia.getModoOcupacion()).isEqualTo(modoOcupacion);
        assertThat(estancia.getEstado()).isEqualTo(estadoEstancia);

        // ---- Precio total ----
        if (precioTotal == null) {
            assertThat(estancia.getPrecioTotal()).isNull();
        } else {
            assertThat(estancia.getPrecioTotal())
                    .isNotNull()
                    .isEqualByComparingTo(precioTotal);
        }

        // ---- Notas ----
        if (notas == null) {
            assertThat(estancia.getNotas()).isNull();
        } else {
            assertThat(estancia.getNotas()).contains(notas);
        }

        // ---- Habitaciones ----
        if (totalHabitaciones != null) {
            assertThat(estancia.getHabitaciones()).hasSize(totalHabitaciones);
        }

        // ---- Pagos ----
        if (totalPagos != null) {
            assertThat(estancia.getPagos()).hasSize(totalPagos);
        }
    }

    public static void comprobarHabitacionesDb(List<Habitacion> habitaciones, Estancia estancia) {
        if (habitaciones != null) {
            List<Long> idsHabitacionesEsperadas = habitaciones.stream()
                    .map(Habitacion::getId)
                    .toList();

            List<Long> idsHabitacionesActuales = estancia.getHabitaciones().stream()
                    .map(Habitacion::getId)
                    .toList();

            assertThat(idsHabitacionesActuales).hasSameSizeAs(idsHabitacionesEsperadas);
            assertThat(idsHabitacionesActuales).containsExactlyInAnyOrderElementsOf(idsHabitacionesEsperadas);
        }
    }

    public static void comprobarUnidadYHabitacionesDb(
            Unidad unidad,
            EstadoOperativo estadoOperativo,
            Integer totalOcupadasEnUnidad
    ) {
        assertThat(unidad.getEstadoOperativo()).isEqualTo(estadoOperativo);


        Integer ocupadas = Math.toIntExact(unidad.getHabitaciones().stream()
                .filter(h -> h.getEstadoOperativo() == EstadoOperativo.OCUPADO)
                .count());

        assertThat(ocupadas).isEqualTo(totalOcupadasEnUnidad);
    }

    public static void comprobarReservaDb(
            Reserva reserva,
            Integer numeroPersonas,
            LocalDate entradaEstimada,
            LocalDate salidaEstimada,
            ModoOcupacion modoOcupacion,
            EstadoReserva estadoReserva,
            CanalReserva canalReserva,
            String notas,
            Integer totalHabitaciones
    ) {
        // ---- Código ----
        assertThat(reserva.getCodigo()).isNotBlank();

        // ---- Cliente ----
        assertThat(reserva.getCliente()).isNotNull();

        // ---- Número de personas ----
        assertThat(reserva.getNumeroPersonas()).isEqualTo(numeroPersonas);

        // ---- Fechas ----
        assertThat(reserva.getFechaCreacion()).isNotNull();
        assertThat(reserva.getEntradaEstimada().toLocalDate()).isEqualTo(entradaEstimada);
        assertThat(reserva.getSalidaEstimada().toLocalDate()).isEqualTo(salidaEstimada);

        // ---- Estados ----
        assertThat(reserva.getModoOcupacion()).isEqualTo(modoOcupacion);
        assertThat(reserva.getEstado()).isEqualTo(estadoReserva);

        // ---- Canal de reserva ----
        assertThat(reserva.getCanalReserva()).isEqualTo(canalReserva);

        // ---- Notas ----
        assertThat(reserva.getNotas()).contains(notas);

        // ---- Habitaciones ----
        assertThat(reserva.getHabitaciones()).hasSize(totalHabitaciones);

        // ---- Estancia asociada ----
        assertThat(reserva.getEstancia()).isNotNull();
    }

    public static void comprobarOcupantesDb(
            List<Ocupante> ocupantes,
            Ocupante clienteEsperado,
            List<Ocupante> acompanantesEsperados
    ) {
        Long idClienteEsperado = clienteEsperado.getId();
        if(acompanantesEsperados != null) {
            List<Long> idsAcompanantesEsperados = acompanantesEsperados.stream()
                    .map(Ocupante::getId)
                    .toList();

            List<Long> idsAcompanantesActuales = ocupantes.stream()
                    .filter(ocupante -> ocupante.getTipoOcupante() == TipoOcupante.ACOMPANANTE)
                    .map(Ocupante::getId)
                    .toList();
            assertThat(idsAcompanantesActuales).hasSameSizeAs(idsAcompanantesEsperados);
            assertThat(idsAcompanantesActuales).containsExactlyInAnyOrderElementsOf(idsAcompanantesEsperados);
        } else {
            List<Ocupante> acompanantesActuales = ocupantes.stream()
                    .filter(ocupante -> ocupante.getTipoOcupante() == TipoOcupante.ACOMPANANTE)
                    .toList();
            assertThat(acompanantesActuales).isEmpty();
        }


        assertThat(ocupantes).isNotNull();
        assertThat(ocupantes).allMatch(ocupante -> ocupante.getId() != null);

        List<Ocupante> clientes = ocupantes.stream()
                .filter(ocupante -> ocupante.getTipoOcupante() == TipoOcupante.CLIENTE)
                .toList();


        assertThat(clientes).hasSize(1);
        assertThat(clientes.getFirst().getId()).isEqualTo(idClienteEsperado);






    }

    public static void comprobarPagosDb(
            List<Pago> pagos,
            BigDecimal montoExitoso,
            BigDecimal montoNulo,
            EstadoPago estadoPago1,
            int totalEstadosPago1,
            EstadoPago estadoPago2,
            int totalEstadosPago2,
            int totalTipoPagoEstanciaFinalizada,
            int totalTipoPagoEstanciaAnticipada,
            int totalTipoPagoReserva

    ) {
        Long estado1 = pagos.stream()
                .filter(p -> p.getEstado() == estadoPago1)
                .count();

        Long estado2 = pagos.stream()
                .filter(p -> p.getEstado() == estadoPago2)
                .count();

        assertThat(estado1).isEqualTo(totalEstadosPago1);
        assertThat(estado2).isEqualTo(totalEstadosPago2);

        BigDecimal exitoso = pagos.stream()
                .filter(p -> p.getEstado() == EstadoPago.COMPLETADO)
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        assertThat(exitoso).isEqualByComparingTo(montoExitoso);

        BigDecimal nulo = pagos.stream()
                .filter(p -> p.getEstado() == EstadoPago.ELIMINADO || p.getEstado() == EstadoPago.MODIFICADO)
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(nulo).isEqualByComparingTo(montoNulo);

        Long tipoPagoEstanciaCompletada = pagos.stream()
                .filter(p -> p.getTipoPago() == TipoPago.ESTANCIA_COMPLETADA)
                .count();

        assertThat(tipoPagoEstanciaCompletada).isEqualTo(totalTipoPagoEstanciaFinalizada);

        Long tipoPagoEstanciaAnticipada = pagos.stream()
                .filter(p -> p.getTipoPago() == TipoPago.ANTICIPO_ESTANCIA)
                .count();

        assertThat(tipoPagoEstanciaAnticipada).isEqualTo(totalTipoPagoEstanciaAnticipada);

        Long tipoPagoReserva = pagos.stream()
                .filter(p -> p.getTipoPago() == TipoPago.ANTICIPO_RESERVA)
                .count();

        assertThat(tipoPagoReserva).isEqualTo(totalTipoPagoReserva);

    }

    public static void comprobarEventoDb(
            AuditoriaEvento evento,
            TipoEvento tipoEvento,
            String codigoEstancia,
            String codigoReserva,
            Integer cantidadPropiedadesJson
    ) {
        assertThat(evento.getTipoEvento()).isEqualTo(tipoEvento);

        switch (tipoEvento) {
            case FINALIZACION_ESTANCIA:
            case CREACION_ESTANCIA:
            case MODIFICACION_ESTANCIA:
            case ELIMINACION_ESTANCIA:
            case ACTIVACION_ESTANCIA:
                assertThat(evento.getEntidad()).isEqualTo(TipoEntidad.ESTANCIA);
                break;
            case CREACION_PAGO:
            case MODIFICACION_PAGO:
            case ELIMINACION_PAGO:
                assertThat(evento.getEntidad()).isEqualTo(TipoEntidad.PAGO);
                break;
            case CREACION_RESERVA:
            case MODIFICACION_RESERVA:
            case ELIMINACION_RESERVA:
                 assertThat(evento.getEntidad()).isEqualTo(TipoEntidad.RESERVA);
                break;
            default:
                throw new IllegalArgumentException("Tipo de evento no esperado en esta comprobación: " + tipoEvento);
        }

        comprobarJson(evento.getDetalle(), cantidadPropiedadesJson);



        if(codigoEstancia == null) {
            assertThat(evento.getCodigoEstancia()).isNull();
        } else {
            assertThat(evento.getCodigoEstancia()).isEqualTo(codigoEstancia);
        }

        if(codigoReserva == null) {
            assertThat(evento.getCodigoReserva()).isNull();
        } else {
            assertThat(evento.getCodigoReserva()).isEqualTo(codigoReserva);
        }
    }

    private static void comprobarJson(String json, int cantidadPropiedades) {
        assertThat(json).isNotBlank();
        assertThat(cantidadPropiedades).isGreaterThan(0);

        try {
            JsonNode root = OBJECT_MAPPER.readTree(json);
            assertThat(root.isObject()).isTrue();
            assertThat(root.size()).isEqualTo(cantidadPropiedades);

            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                assertThat(field.getKey()).isNotBlank();
                assertThat(valorJsonConContenido(field.getValue())).isTrue();
            }
        } catch (Exception e) {
            throw new AssertionError("JSON inválido o no cumple la estructura esperada: " + json, e);
        }
    }

    private static boolean valorJsonConContenido(JsonNode value) {
        if (value == null || value.isNull()) {
            return false;
        }
        if (value.isObject()) {
            if (value.isEmpty()) {
                return false;
            }
            Iterator<Map.Entry<String, JsonNode>> fields = value.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (field.getKey() == null || field.getKey().isBlank()) {
                    return false;
                }
                if (!valorJsonConContenido(field.getValue())) {
                    return false;
                }
            }
            return true;
        }
        return valorConContenido(value);
    }

    private static boolean valorConContenido(JsonNode value) {
        if (value == null || value.isNull()) {
            return false;
        }
        if (value.isTextual()) {
            return !value.asText().isBlank();
        }
        return true;
    }
}
