package com.hotel.services;

import com.hotel.models.Estancia;
import com.hotel.models.Habitacion;
import com.hotel.models.Reserva;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.repositories.EstanciaRepository;
import com.hotel.repositories.ReservaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DisponibilidadService {

    private static final Logger logger = LoggerFactory.getLogger(DisponibilidadService.class);

    private final EstanciaRepository estanciaRepository;
    private final ReservaRepository reservaRepository;

    public DisponibilidadService(EstanciaRepository estanciaRepository,
                                ReservaRepository reservaRepository)
    {
        this.estanciaRepository = estanciaRepository;
        this.reservaRepository = reservaRepository;
    }


    public String verificarDisponibilidadNuevo(List<Habitacion> habitaciones, LocalDateTime fechaIncioReserva, LocalDateTime fechaFinReserva) {

        logger.info("[DisponibilidadService.verificarDisponiblidad] Verificando reserva para las habitaciones en el rango de fechas: {} - {}", fechaIncioReserva, fechaFinReserva);
        validarRangoFechas(fechaIncioReserva, fechaFinReserva);
        List<Reserva> reservas = listaReservaPorHabitacion(habitaciones, fechaIncioReserva, fechaFinReserva);


        logger.info("[DisponibilidadService.verificarDisponiblidad] Habitaciones con reserva encontradas: {}", reservas.size());
        if(!reservas.isEmpty()) {
            List<Habitacion> habitacionesConReserva = reservas.stream()
                    .flatMap(reserva -> reserva.getHabitaciones().stream())
                    .toList();
            return construirMensajeHabitaciones(habitacionesConReserva, "reserva");
        }

        List<Estancia> estanciasAsociadas = listarEstanciasActivasOExcedidasPorHabitacion(habitaciones);
        if (hayHabitacionesOcupadas(habitaciones) && estanciasAsociadas.isEmpty()) {
            throw new IllegalStateException("Estado inconsistente: hay habitaciones marcadas como no disponibles sin una estancia activa o excedida asociada");
        }

        List<Estancia> estanciasBloqueantes = filtrarEstanciasQueBloqueanRango(estanciasAsociadas, fechaIncioReserva, fechaFinReserva);
        if(!estanciasBloqueantes.isEmpty()) {
            List<Habitacion> habitacionesConEstancia = estanciasBloqueantes.stream()
                    .flatMap(estancia -> estancia.getHabitaciones().stream())
                    .toList();
            return construirMensajeHabitaciones(habitacionesConEstancia, "estancia");
        }

        return "";

    }

    public String verificarDisponibilidadEditar(Reserva reserva, Estancia estancia ,List<Habitacion> habitaciones, LocalDateTime fechaIncioReserva, LocalDateTime fechaFinReserva) {
        logger.info("[DisponibilidadService.verificarDisponiblidadEditar] Verificando disponibilidad para edición de reserva o estancia");
        validarRangoFechas(fechaIncioReserva, fechaFinReserva);
        Long reservaId = reserva != null ? reserva.getId() : null;
        Long estanciaId = estancia != null ? estancia.getId() : null;

        logger.info("[DisponibilidadService.verificarDisponiblidadEditar] Verificando disponibilidad para reserva con id: {}", reservaId);
        List<Reserva> reservas = listaReservaPorHabitacion(habitaciones, fechaIncioReserva, fechaFinReserva)
                .stream()
                .filter(r -> !r.getId().equals(reservaId))
                .toList();

        logger.info("[DisponibilidadService.verificarDisponiblidadEditar] Habitaciones con reserva encontradas: {}", reservas.size());
        if(!reservas.isEmpty()) {
            List<Habitacion> habitacionesConReserva = reservas.stream()
                    .flatMap(r -> r.getHabitaciones().stream())
                    .toList();
            return construirMensajeHabitaciones(habitacionesConReserva, "reserva");
        }

        List<Estancia> estanciasAsociadas = listarEstanciasActivasOExcedidasPorHabitacion(habitaciones);
        if (hayHabitacionesOcupadas(habitaciones) && estanciasAsociadas.isEmpty()) {
            throw new IllegalStateException("Estado inconsistente: hay habitaciones marcadas como no disponibles sin una estancia activa o excedida asociada");
        }

        List<Estancia> estancias = filtrarEstanciasQueBloqueanRango(estanciasAsociadas, fechaIncioReserva, fechaFinReserva)
                .stream()
                .filter(e -> !e.getId().equals(estanciaId))
                .toList();

        logger.info("[DisponibilidadService.verificarDisponiblidadEditar] Habitaciones con estancia encontradas: {}", estancias.size());
        if(!estancias.isEmpty()) {
            List<Habitacion> habitacionesConEstancia = estancias.stream()
                    .flatMap(e -> e.getHabitaciones().stream())
                    .toList();
            return construirMensajeHabitaciones(habitacionesConEstancia, "estancia");
        }

        return "";
    }

    private boolean hayHabitacionesOcupadas(List<Habitacion> habitaciones) {
        logger.info("[DisponibilidadService.hayHabitacionesOcupadas] Verificando si hay habitaciones en estado OCUPADO");
        for (Habitacion habitacion : habitaciones) {
            if (habitacion.getEstadoOperativo().equals(EstadoOperativo.OCUPADO)) {
                logger.info("[DisponibilidadService.hayHabitacionesOcupadas] Habitacion con codigo: {} en estado OCUPADO", habitacion.getCodigo());
                return true;
            }
        }

        logger.info("[DisponibilidadService.hayHabitacionesOcupadas] No hay habitaciones en estado OCUPADO");
        return false;
    }

    private List<Reserva> listaReservaPorHabitacion(List<Habitacion> habitaciones, LocalDateTime fechaIncioReserva, LocalDateTime fechaFinReserva) {
        logger.info("[DisponibilidadService.listaReservaPorHabitacion] Verificando reservas para las habitaciones en el rango de fechas: {} - {}", fechaIncioReserva, fechaFinReserva);
        if (habitaciones == null || habitaciones.isEmpty()) {
            throw new IllegalArgumentException("La lista de habitaciones no puede ser nula o vacía");
        }

        List<Long> habitacionIds = habitaciones.stream()
                .map(Habitacion::getId)
                .toList();

        return reservaRepository.findReservasSolapadasPorHabitacionesYFechas(
                habitacionIds,
                fechaIncioReserva,
                fechaFinReserva,
                List.of(EstadoReserva.CONFIRMADA)
        );
    }

    private List<Estancia> listarEstanciasActivasOExcedidasPorHabitacion(List<Habitacion> habitaciones) {
        if (habitaciones == null || habitaciones.isEmpty()) {
            throw new IllegalArgumentException("La lista de habitaciones no puede ser nula o vacía");
        }

        List<Long> habitacionIds = habitaciones.stream()
                .map(Habitacion::getId)
                .toList();

        return estanciaRepository.findActivasOExcedidasPorHabitaciones(
                        habitacionIds,
                        List.of(EstadoEstancia.ACTIVA, EstadoEstancia.EXCEDIDA)
                );
    }

    private List<Estancia> filtrarEstanciasQueBloqueanRango(
            List<Estancia> estancias,
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin
    ) {
        return estancias.stream()
                .filter(e -> {
                    if (e.getEstado() == EstadoEstancia.EXCEDIDA) {
                        return true;
                    }

                    if (e.getSalidaEstimada() == null) {
                        return true;
                    }

                    LocalDateTime entrada = e.getEntradaReal() != null ? e.getEntradaReal() : LocalDateTime.MIN;
                    return entrada.isBefore(fechaFin) && e.getSalidaEstimada().isAfter(fechaInicio);
                })
                .toList();
    }


    private String construirMensajeHabitaciones(List<Habitacion> habitaciones, String tipo) {
        StringBuilder mensaje = new StringBuilder();
        for (Habitacion habitacion : habitaciones) {
            mensaje.append(habitacion.getCodigo()).append(", ");
        }

        mensaje.setLength(mensaje.length() - 2); // Eliminar la última coma y espacio

        logger.info("[DisponibilidadService.construirMensajeHabitaciones] Habitaciones con reservas: {}", mensaje.toString());
        return "existe una " + tipo + " para las habitaciones con codigo: " + mensaje.toString();
    }

    private void validarRangoFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        if (fechaInicio == null || fechaFin == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias");
        }

        if (!fechaInicio.isBefore(fechaFin)) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin");
        }
    }

}
