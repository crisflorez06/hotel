package com.hotel.testutils;

import com.hotel.models.*;
import com.hotel.models.enums.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public final class AssertionsHelper {

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
        assertThat(estancia.getSalidaEstimada()).isEqualToIgnoringNanos(salidaEstimada);
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
            LocalDateTime entradaEstimada,
            LocalDateTime salidaEstimada,
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
        assertThat(reserva.getEntradaEstimada()).isEqualToIgnoringNanos(entradaEstimada);
        assertThat(reserva.getSalidaEstimada()).isEqualToIgnoringNanos(salidaEstimada);

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

    public static void comprobarPagosDb(
            List<Pago> pagos,
            double montoExitoso,
            double montoNulo,
            EstadoPago estadoPago1,
            int totalEstadosPago1,
            EstadoPago estadoPago2,
            int totalEstadosPago2,
            int totalTipoPagoEstancia

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


        assertThat(exitoso).isEqualByComparingTo(BigDecimal.valueOf(montoExitoso));

        BigDecimal nulo = pagos.stream()
                .filter(p -> p.getEstado() == EstadoPago.ELIMINADO || p.getEstado() == EstadoPago.MODIFICADO)
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(nulo).isEqualByComparingTo(BigDecimal.valueOf(montoNulo));

        Long tipoPagoEstancia = pagos.stream()
                .filter(p -> p.getTipoPago() == TipoPago.ESTANCIA)
                .count();

        assertThat(tipoPagoEstancia).isEqualTo(totalTipoPagoEstancia);

    }
}
