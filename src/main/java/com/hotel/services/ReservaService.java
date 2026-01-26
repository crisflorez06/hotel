package com.hotel.services;

import com.hotel.dtos.ReservaCalendarioDTO;
import com.hotel.dtos.ReservaNuevaRequestDTO;
import com.hotel.mappers.EstanciaMapper;
import com.hotel.mappers.ReservaMapper;
import com.hotel.models.*;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.ModoOcupacion;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.ReservaRepository;
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

@Service
public class ReservaService {

    private static final Logger logger = LoggerFactory.getLogger(ReservaService.class);

    private final ReservaRepository reservaRepository;
    private final OcupanteService ocupanteService;
    private final HabitacionService habitacionService;

    public ReservaService(ReservaRepository reservaRepository, OcupanteService ocupanteService, HabitacionService habitacionService) {
        this.ocupanteService = ocupanteService;
        this.reservaRepository = reservaRepository;
        this.habitacionService = habitacionService;
    }

    public Reserva buscarPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrado con id: " + id));
    }

    @Transactional
    public Reserva crearReserva(ReservaNuevaRequestDTO request) {
        logger.info("Verificando disponibilidad para la reserva solicitada");
        if (!habitacionService.verificarDisponiblidad(request.getCodigo(), request.getTipoUnidad())) {
            throw new IllegalStateException("La unidad o habitacion no está disponible para la reserva.");
        }

        logger.info("Validando fechas de entrada y salida estimada");
        if (request.getSalidaEstimada().isBefore(request.getEntradaEstimada())) {
            throw new IllegalArgumentException("salida estimada debe ser posterior a entrada estimada");
        }

        logger.info("Creando reserva para el ocupante con ID: {}", request.getIdOcupante());
        Reserva reserva = ReservaMapper.requestNuevoToEntity(request);

        logger.info("Buscando ocupante con ID: {}", request.getIdOcupante());
        reserva.setOcupante(ocupanteService.buscarPorId(request.getIdOcupante()));

        List<Habitacion> habitaciones = new ArrayList<>( habitacionService.clasificarHabitacionesPorTipoUnidad(request.getCodigo(), request.getTipoUnidad()));
        reserva.setHabitaciones(habitaciones);
        habitacionService.cambiarEstadoHabitaciones(request.getCodigo(), EstadoOperativo.RESERVADO, request.getTipoUnidad());
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

}
