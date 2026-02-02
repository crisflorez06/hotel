package com.hotel.services;

import com.hotel.models.Estancia;
import com.hotel.models.Habitacion;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.EstanciaRepository;
import com.hotel.repositories.ReservaRepository;
import com.hotel.resolvers.UnidadHabitacionResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DisponibilidadService {

    private static final Logger logger = LoggerFactory.getLogger(DisponibilidadService.class);

    private final UnidadHabitacionResolver unidadHabitacionResolver;
    private final EstanciaRepository estanciaRepository;
    private final ReservaRepository reservaRepository;

    public DisponibilidadService(UnidadHabitacionResolver unidadHabitacionResolver,
                                 EstanciaRepository estanciaRepository,
                                ReservaRepository reservaRepository)
    {
        this.unidadHabitacionResolver = unidadHabitacionResolver;
        this.estanciaRepository = estanciaRepository;
        this.reservaRepository = reservaRepository;
    }


    public String verificarDisponibilidad(String codigo, TipoUnidad tipoUnidad, LocalDateTime fechaIncioReserva, LocalDateTime fechaFinReserva) {

        logger.info("[DisponibilidadService.verificarDisponiblidad] Verificando disponibilidad para reserva con codigo: {} y tipoUnidad: {} en el rango de fechas: {} - {}", codigo, tipoUnidad, fechaIncioReserva, fechaFinReserva);
        List<Habitacion> habitaciones = unidadHabitacionResolver.buscarListaHabitaciones(codigo, tipoUnidad);
        List<Habitacion> habitacionesConReserva = verificarReservaPorHabitacion(habitaciones, fechaIncioReserva, fechaFinReserva);

        logger.info("[DisponibilidadService.verificarDisponiblidad] Habitaciones con reserva encontradas: {}", habitacionesConReserva.size());
        if(!habitacionesConReserva.isEmpty()) {
            return construirMensajeHabitacionesConReserva(habitacionesConReserva);
        }

        //si existe una estancia activa o excedida, verificar si se solapa con la reserva
        if (!tieneDisponiblidadEstancia(codigo, tipoUnidad)){

            Long habitacionId = habitaciones.getFirst().getId();
            Estancia estancia = estanciaRepository.findActivaOExcedidaPorHabitacionId(habitacionId).orElseThrow(
                    () -> new IllegalArgumentException("No se encontró una estancia activa o excedida para la unidad con codigo: " + codigo)
            );

            //debo solucionar para que en string me salga el codigo exacto de la habitacion o habitaciones que tienen la estancia
            if(fechaIncioReserva.isBefore(estancia.getSalidaEstimada()) || estancia.getEstado().equals(EstadoEstancia.EXCEDIDA)) {
                logger.info("[DisponibilidadService.verificarDisponiblidad] Existe una estancia activa o excedida que se solapa con la reserva para codigo: {} y tipoUnidad: {}", codigo, tipoUnidad);
                return "existe una estancia para la habitacion con codigo: " + codigo;
            }
        }

        return "";

    }

    private Boolean tieneDisponiblidadEstancia(String codigo, TipoUnidad tipoUnidad) {
        logger.info("[DisponibilidadService.verificarDisponiblidad] Verificando disponibilidad para codigo: {} y tipoUnidad: {}", codigo, tipoUnidad);

        List<Habitacion> habitaciones = unidadHabitacionResolver.buscarListaHabitaciones(codigo, tipoUnidad);

        for (Habitacion habitacion : habitaciones) {
            if (!habitacion.getEstadoOperativo().equals(EstadoOperativo.DISPONIBLE)) {
                logger.info("[DisponibilidadService.verificarDisponiblidad] Habitacion con codigo: {} no está disponible (estado: {})", habitacion.getCodigo(), habitacion.getEstadoOperativo());
                return false;
            }
        }

        logger.info("[DisponibilidadService.verificarDisponiblidad] Todas las habitaciones están disponibles para codigo: {} y tipoUnidad: {}", codigo, tipoUnidad);
        return true;

    }

    private List<Habitacion> verificarReservaPorHabitacion(List<Habitacion> habitaciones, LocalDateTime fechaIncioReserva, LocalDateTime fechaFinReserva) {
        logger.info("[DisponibilidadService.verificarReservaPorHabitacion] Verificando reservas para las habitaciones en el rango de fechas: {} - {}", fechaIncioReserva, fechaFinReserva);
        List<Habitacion> habitacionesConReserva = new ArrayList<>();
        for (Habitacion habitacion : habitaciones) {
            boolean existeReserva = reservaRepository.existsReservaByHabitacionAndRango(
                    habitacion.getId(),
                    fechaIncioReserva,
                    fechaFinReserva
            );
            if (existeReserva) {
                logger.info("[DisponibilidadService.verificarReservaPorHabitacion] Existe una reserva activa para la habitacion con codigo: {} en el rango de fechas: {} - {}", habitacion.getCodigo(), fechaIncioReserva, fechaFinReserva);
                habitacionesConReserva.add(habitacion);
            }
        }

        return habitacionesConReserva;
    }

    private String construirMensajeHabitacionesConReserva(List<Habitacion> habitaciones) {
        StringBuilder mensaje = new StringBuilder();
        for (Habitacion habitacion : habitaciones) {
            mensaje.append(habitacion.getCodigo()).append(", ");
        }

        mensaje.setLength(mensaje.length() - 2); // Eliminar la última coma y espacio

        logger.info("[DisponibilidadService.construirMensajeHabitacionesConReserva] Habitaciones con reservas: {}", mensaje.toString());
        return "existe una reserva para las habitaciones con codigo: " + mensaje.toString();
    }

}
