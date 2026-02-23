package com.hotel.services;

import com.hotel.models.Estancia;
import com.hotel.models.Habitacion;
import com.hotel.models.Reserva;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.repositories.EstanciaRepository;
import com.hotel.repositories.ReservaRepository;
import com.hotel.resolvers.UnidadHabitacionResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

import static com.hotel.models.enums.EstadoEstancia.EXCEDIDA;

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
        List<Reserva> reservas = listaReservaPorHabitacion(habitaciones, fechaIncioReserva, fechaFinReserva);


        logger.info("[DisponibilidadService.verificarDisponiblidad] Habitaciones con reserva encontradas: {}", reservas.size());
        if(!reservas.isEmpty()) {
            List<Habitacion> habitacionesConReserva = reservas.stream()
                    .flatMap(reserva -> reserva.getHabitaciones().stream())
                    .toList();
            return construirMensajeHabitaciones(habitacionesConReserva, "reserva");
        }

        if (!tieneDisponiblidadHabitaciones(habitaciones)){
            logger.info("[DisponibilidadService.verificarDisponiblidad] No hay disponibilidad por estado operativo, verificando estancias activas o excedidas");
            List<Estancia> estancias = listaEstanciaActivaOExcedidaPorHabitacion(habitaciones, fechaIncioReserva);
            if(!estancias.isEmpty()) {
                List<Habitacion> habitacionesConEstancia = estancias.stream()
                        .flatMap(estancia -> estancia.getHabitaciones().stream())
                        .toList();
                return construirMensajeHabitaciones(habitacionesConEstancia, "estancia");
            }
        }

        return "";

    }

    public String verificarDisponibilidadEditar(Reserva reserva, Estancia estancia ,List<Habitacion> habitaciones, LocalDateTime fechaIncioReserva, LocalDateTime fechaFinReserva) {
        logger.info("[DisponibilidadService.verificarDisponiblidadEditar] Verificando disponibilidad para edición de reserva o estancia");
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

        if(!tieneDisponiblidadHabitaciones(habitaciones)) {
            logger.info("[DisponibilidadService.verificarDisponiblidadEditar] Verificando disponibilidad para estancia con id: {}", estanciaId);
            List<Estancia> estancias = listaEstanciaActivaOExcedidaPorHabitacion(habitaciones, fechaIncioReserva)
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
        }

        return "";
    }

    private Boolean tieneDisponiblidadHabitaciones(List<Habitacion> habitaciones) {

        logger.info("[DisponibilidadService.tieneDisponiblidadEstancia] Verificando estado operativo de las habitaciones para determinar disponibilidad de estancia");
        for (Habitacion habitacion : habitaciones) {
            if (!habitacion.getEstadoOperativo().equals(EstadoOperativo.DISPONIBLE)) {
                logger.info("[DisponibilidadService.tieneDisponiblidadEstancia] Habitacion con codigo: {} no está disponible (estado: {})", habitacion.getCodigo(), habitacion.getEstadoOperativo());
                return false;
            }
        }

        logger.info("[DisponibilidadService.tieneDisponiblidadEstancia] Todas las habitaciones están disponibles para estancia");
        return true;

    }

    private List<Reserva> listaReservaPorHabitacion(List<Habitacion> habitaciones, LocalDateTime fechaIncioReserva, LocalDateTime fechaFinReserva) {
        logger.info("[DisponibilidadService.listaReservaPorHabitacion] Verificando reservas para las habitaciones en el rango de fechas: {} - {}", fechaIncioReserva, fechaFinReserva);
        if (habitaciones == null || habitaciones.isEmpty()) {
            return List.of();
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

    private List<Estancia> listaEstanciaActivaOExcedidaPorHabitacion(
            List<Habitacion> habitaciones,
            LocalDateTime fechaInicio
    ) {
        logger.info("[DisponibilidadService.listaEstanciaActivaOExcedidaPorHabitacion] Verificando estancias activas o excedidas para las habitaciones");
        if (habitaciones == null || habitaciones.isEmpty()) {
            return List.of();
        }

        List<Long> habitacionIds = habitaciones.stream()
                .map(Habitacion::getId)
                .toList();

        return estanciaRepository.findActivasOExcedidasPorHabitaciones(
                        habitacionIds,
                        List.of(EstadoEstancia.ACTIVA, EstadoEstancia.EXCEDIDA)
                ).stream()
                .filter(estancia ->
                        estancia.getEstado() == EXCEDIDA
                                || (estancia.getSalidaEstimada() != null
                                && fechaInicio.isBefore(estancia.getSalidaEstimada()))
                )
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

}
