package com.hotel.services;

import com.hotel.dtos.EstanciaDTO;
import com.hotel.dtos.ReservaCalendarioDTO;
import com.hotel.dtos.ReservaNuevaRequestDTO;
import com.hotel.mappers.EstanciaMapper;
import com.hotel.mappers.ReservaMapper;
import com.hotel.models.*;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.models.enums.ModoOcupacion;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.ReservaRepository;
import com.hotel.resolvers.AlojamientoResolver;
import com.hotel.resolvers.UnidadHabitacionResolver;
import com.hotel.specifications.ReservaSpecification;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReservaService {

    private static final Logger logger = LoggerFactory.getLogger(ReservaService.class);

    private final ReservaRepository reservaRepository;
    private final OcupanteService ocupanteService;
    private final HabitacionService habitacionService;
    private final UnidadService unidadService;
    private final EstanciaService estanciaService;
    private final AlojamientoResolver alojamientoResolver;
    private final UnidadHabitacionResolver unidadHabitacionResolver;

    public ReservaService(ReservaRepository reservaRepository,
                          AlojamientoResolver alojamientoResolver,
                          OcupanteService ocupanteService,
                          HabitacionService habitacionService,
                          UnidadService unidadService,
                          EstanciaService estanciaService,
                          UnidadHabitacionResolver unidadHabitacionResolver) {
        this.unidadHabitacionResolver = unidadHabitacionResolver;
        this.estanciaService = estanciaService;
        this.unidadService = unidadService;
        this.ocupanteService = ocupanteService;
        this.reservaRepository = reservaRepository;
        this.habitacionService = habitacionService;
        this.alojamientoResolver = alojamientoResolver;
    }

    public Reserva buscarPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrado con id: " + id));
    }

    @Transactional
    public Reserva crearReserva(ReservaNuevaRequestDTO request) {
        logger.info("Verificando disponibilidad para la reserva solicitada");
        List<Reserva> reservasNoDisponibles = estaDisponibleEnRango(request.getCodigo(), request.getTipoUnidad(), request.getEntradaEstimada(), request.getSalidaEstimada());
        if (reservasNoDisponibles == null) {


            //cambiar esto porque devuelve un null en el error  entonces no se podra leer el mensaje
            String detalles = reservasNoDisponibles.stream()
                    .map(r -> "Entrada: " + r.getEntradaEstimada() + " - Salida: " + r.getSalidaEstimada())
                    .collect(Collectors.joining(" | ")); // separador entre reservas

            throw new IllegalStateException(
                    "Existen reservas en conflicto (" + detalles +
                            ") para el rango solicitado: Entrada=" + request.getEntradaEstimada() +
                            " / Salida=" + request.getSalidaEstimada()
            );
        }


        logger.info("Validando fechas de entrada y salida estimada");
        if (request.getSalidaEstimada().isBefore(request.getEntradaEstimada())) {
            throw new IllegalArgumentException("salida estimada debe ser posterior a entrada estimada");
        }

        logger.info("Creando reserva para el ocupante con ID: {}", request.getIdOcupante());
        Reserva reserva = ReservaMapper.requestNuevoToEntity(request);

        logger.info("Buscando ocupante con ID: {}", request.getIdOcupante());
        reserva.setOcupante(ocupanteService.buscarPorId(request.getIdOcupante()));

        List<Habitacion> habitaciones = unidadHabitacionResolver.buscarListaHabitaciones(request.getCodigo(), request.getTipoUnidad());
        reserva.setHabitaciones(habitaciones);
        EstadoOperativo nuevoEstado = EstadoOperativo.DISPONIBLE;
        alojamientoResolver.actualizarEstadoAlojamiento(habitaciones, nuevoEstado);
        return reservaRepository.save(reserva);
    }

    public List<ReservaCalendarioDTO> buscarReservasCalendario(String mes, TipoUnidad tipoUnidad, String codigoUnidad) {
        if (mes == null || mes.isBlank()) {
            throw new IllegalArgumentException("mes es obligatorio (formato YYYY-MM)");
        }

        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.parse(mes, DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("mes debe tener formato YYYY-MM", ex);
        }

        // Construye el rango completo del mes.
        LocalDateTime desde = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime hasta = yearMonth.atEndOfMonth().atTime(LocalTime.MAX);

        List<Reserva> reservas = reservaRepository.findAll(ReservaSpecification.byCalendario(desde, hasta, tipoUnidad, codigoUnidad));


        return llenarTipoYCodigoUnidad(ReservaMapper.entityListaToCalendarioDTOList(reservas));
    }

    private List<ReservaCalendarioDTO> llenarTipoYCodigoUnidad(List<ReservaCalendarioDTO> reservas) {
        for(ReservaCalendarioDTO reservaDto : reservas) {
            Reserva reserva = buscarPorId(reservaDto.getId());
            if(reserva.getModoOcupacion() == ModoOcupacion.INDIVIDUAL) {
                reservaDto.setCodigoUnidad(reserva.getHabitaciones().getFirst().getUnidad().getCodigo());
                reservaDto.setTipoUnidad(TipoUnidad.HABITACION);
            } else {
                reservaDto.setCodigoUnidad(reserva.getHabitaciones().getFirst().getUnidad().getCodigo());
                reservaDto.setTipoUnidad(reserva.getHabitaciones().getFirst().getUnidad().getTipo());
            }
        }
        return reservas;


    }


    public List<Reserva> estaDisponibleEnRango(String codigo, TipoUnidad tipoUnidad, LocalDateTime desde, LocalDateTime hasta) {
        logger.info("Verificando disponibilidad en rango para codigo: {} y tipoUnidad: {}", codigo, tipoUnidad);

        //verificamos si la habitacion o unidad estan disponibles, si tiene estancia ver si su salida estimada es antes de la fecha de entrada de la reserva
        Boolean estaDisponible = alojamientoResolver.verificarDisponiblidad(codigo, tipoUnidad);
        if (!estaDisponible) {
            EstanciaDTO estancia = estanciaService.obtenerEstancia(codigo, tipoUnidad);
            if(desde.isBefore(estancia.getSalidaEstimada())){
                return null;
            }
        }

        logger.info("Buscando reservas en rango para codigo: {} y tipoUnidad: {}", codigo, tipoUnidad);
        if(tipoUnidad.equals(TipoUnidad.HABITACION)) {

            Habitacion habitacion = habitacionService.buscarPorCodigo(codigo);
            List<Reserva> reservasEnRango = reservaRepository.findReservasByHabitacionAndRango(habitacion.getId(), desde, hasta);
            if (!reservasEnRango.isEmpty()) {
                return null;
            }
            return reservasEnRango;
        }
        logger.info("Buscando reservas en rango para la unidad con codigo: {}", codigo);

        Unidad unidad = unidadService.buscarPorCodigo(codigo);
        List<Reserva> reservasEnRango = new ArrayList<>();
        for(Habitacion h : unidad.getHabitaciones()) {
            List<Reserva> reservasHabitacion = reservaRepository.findReservasByHabitacionAndRango(h.getId(), desde, hasta);
            reservasEnRango.addAll(reservasHabitacion);
        }

        logger.info("Total de reservas encontradas en rango para la unidad con codigo {}: {}", codigo, reservasEnRango.size());

        if(!reservasEnRango.isEmpty()) {
            return null;
        }
        return reservasEnRango;
    }
}
