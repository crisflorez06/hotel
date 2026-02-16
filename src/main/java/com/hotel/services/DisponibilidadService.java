package com.hotel.services;

import com.hotel.models.Estancia;
import com.hotel.models.Habitacion;
import com.hotel.models.Reserva;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.EstadoReserva;
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
import java.util.Optional;

import static com.hotel.models.enums.EstadoEstancia.EXCEDIDA;

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


    public String verificarDisponibilidad(Estancia estancia, Reserva reserva, String codigo, TipoUnidad tipoUnidad, LocalDateTime fechaIncioReserva, LocalDateTime fechaFinReserva) {

        logger.info("[DisponibilidadService.verificarDisponiblidad] Verificando disponibilidad para reserva con codigo: {} y tipoUnidad: {} en el rango de fechas: {} - {}", codigo, tipoUnidad, fechaIncioReserva, fechaFinReserva);
        List<Habitacion> habitaciones = unidadHabitacionResolver.buscarListaHabitaciones(codigo, tipoUnidad);
        List<Habitacion> habitacionesConReserva = new ArrayList<>();
        if(reserva == null) {
            logger.info("[DisponibilidadService.verificarReservaPorHabitacion] Si el codigo de reserva no es nulo, se omite la verificación de reservas para las habitaciones");

        } else {
            logger.info("[DisponibilidadService.verificarDisponiblidad] Se encontró una reserva asociada para codigo: {} y tipoUnidad: {}, se procede a verificar reservas activas en el rango de fechas", codigo, tipoUnidad);
            habitacionesConReserva = verificarReservaPorHabitacion(reserva.getCodigo(), habitaciones, fechaIncioReserva, fechaFinReserva);
        }

        logger.info("[DisponibilidadService.verificarDisponiblidad] Habitaciones con reserva encontradas: {}", habitacionesConReserva.size());
        if(!habitacionesConReserva.isEmpty()) {
            return construirMensajeHabitaciones(habitacionesConReserva, "reserva");
        }

        if(estancia != null) {
            logger.info("[DisponibilidadService.verificarDisponiblidad] si la estancia no es nula, se omite la verificación de disponibilidad de habitaciones para estancia con codigo: {} y tipoUnidad: {}", codigo, tipoUnidad);
            return "";
        }

        if (!tieneDisponiblidadHabitaciones(habitaciones)){
            logger.info("[DisponibilidadService.verificarDisponiblidad] No hay disponibilidad para estancia con codigo: {} y tipoUnidad: {}", codigo, tipoUnidad);
            List<Habitacion> habitacionesConEstancia = existeEstanciaActivaOExcedidaPorHabitacion(habitaciones, fechaIncioReserva);
            if(!habitacionesConEstancia.isEmpty()) {
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

    private List<Habitacion> verificarReservaPorHabitacion(String codigoReserva ,List<Habitacion> habitaciones, LocalDateTime fechaIncioReserva, LocalDateTime fechaFinReserva) {
        logger.info("[DisponibilidadService.verificarReservaPorHabitacion] Verificando reservas para las habitaciones en el rango de fechas: {} - {}", fechaIncioReserva, fechaFinReserva);
        List<Habitacion> habitacionesConReserva = new ArrayList<>();
        if(reservaRepository.existsByCodigo(codigoReserva)){
            logger.info("[DisponibilidadService.verificarReservaPorHabitacion] se encontró una reserva con el codigo: {}, se omite la verificación de reservas para las habitaciones", codigoReserva);
            return habitacionesConReserva;
        }


        for (Habitacion habitacion : habitaciones) {
            boolean existeReserva = reservaRepository.existsReservaByHabitacionAndRango(
                    habitacion.getId(),
                    fechaIncioReserva,
                    fechaFinReserva,
                    List.of(EstadoReserva.CONFIRMADA)
            );
            if (existeReserva) {
                logger.info("[DisponibilidadService.verificarReservaPorHabitacion] Existe una reserva activa para la habitacion con codigo: {} en el rango de fechas: {} - {}", habitacion.getCodigo(), fechaIncioReserva, fechaFinReserva);
                habitacionesConReserva.add(habitacion);
            }
        }

        return habitacionesConReserva;
    }

    private List<Habitacion> existeEstanciaActivaOExcedidaPorHabitacion(
            List<Habitacion> habitaciones,
            LocalDateTime fechaInicioReserva
    ) {
        logger.info("[DisponibilidadService.existeEstanciaActivaOExcedidaPorHabitacion] Verificando estancias activas o excedidas para las habitaciones");

        List<Habitacion> habitacionesConEstancia = new ArrayList<>();

        for (Habitacion habitacion : habitaciones) {
            estanciaRepository
                    .findActivaOExcedidaPorHabitacionId(habitacion.getId())
                    .ifPresent(estancia -> {

                        if (fechaInicioReserva.isBefore(estancia.getSalidaEstimada()) || estancia.getEstado() == EXCEDIDA) {
                            logger.info("[DisponibilidadService.verificarDisponibilidad] Existe una estancia activa/excedida que se solapa para codigo: {}", habitacion.getCodigo());
                            habitacionesConEstancia.add(habitacion);
                        }

                    });
        }

        return habitacionesConEstancia;
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
