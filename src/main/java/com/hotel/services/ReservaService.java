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
    private final PagoService pagoService;
    private final OcupanteService ocupanteService;
    private final UnidadHabitacionResolver unidadHabitacionResolver;
    private final DisponibilidadService disponibilidadService;

    public ReservaService(ReservaRepository reservaRepository,
                          OcupanteService ocupanteService,
                          PagoService pagoService,
                          UnidadHabitacionResolver unidadHabitacionResolver,
                          DisponibilidadService disponibilidadService) {
        this.pagoService = pagoService;
        this.unidadHabitacionResolver = unidadHabitacionResolver;
        this.ocupanteService = ocupanteService;
        this.reservaRepository = reservaRepository;
        this.disponibilidadService = disponibilidadService;
    }

    public Reserva buscarPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrado con id: " + id));
    }

    @Transactional
    public Reserva crearReserva(ReservaNuevaRequestDTO request) {

        String codigo = request.getCodigo();
        TipoUnidad tipoUnidad = request.getTipoUnidad();
        LocalDateTime entradaEstimada = request.getEntradaEstimada();
        LocalDateTime salidaEstimada = request.getSalidaEstimada();


        logger.info("[crearReserva] Verificando disponibilidad para la unidad o habitacion con codigo: {}", request.getCodigo());
        String existeReserva = disponibilidadService.verificarDisponibilidad(codigo, tipoUnidad, entradaEstimada, salidaEstimada);
        if (!existeReserva.isEmpty()) {
            throw new IllegalArgumentException("No es posible crear la reserva: " + existeReserva);
        }

        logger.info("[crearReserva] Validando fechas de entrada y salida estimadas");
        String fechaConflicto = fechaTieneConflicto(salidaEstimada, entradaEstimada);
        if (!fechaConflicto.isEmpty()) {
            throw new IllegalArgumentException("No es posible crear la reserva: " + fechaConflicto);
        }

        logger.info("[crearReserva] Creando reserva para el ocupante con ID: {}", request.getIdOcupante());
        Reserva reserva = ReservaMapper.requestNuevoToEntity(request);

        logger.info("[crearReserva] Buscando ocupante para asignar a la reserva");
        reserva.setOcupante(ocupanteService.buscarPorId(request.getIdOcupante()));

        logger.info("[crearReserva] Asignando habitaciones a la reserva");
        List<Habitacion> habitaciones = unidadHabitacionResolver.buscarListaHabitaciones(request.getCodigo(), request.getTipoUnidad());
        reserva.setHabitaciones(habitaciones);

        Reserva reservaGuardada = reservaRepository.save(reserva);

        if(request.getPago() != null) {
            logger.info("[crearReserva] Creando pago asociado a la reserva con ID: {}", reservaGuardada.getId());
            Pago pago = pagoService.crearPago(request.getPago(), reservaGuardada.getId());
            reservaGuardada.setPago(pago);
        }

        return reservaGuardada;
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


    private String fechaTieneConflicto(LocalDateTime fechaSalidaEstimada, LocalDateTime fechaEntradaEstimada) {
        if (fechaSalidaEstimada.isBefore(fechaEntradaEstimada) || fechaSalidaEstimada.isEqual(fechaEntradaEstimada)) {
            return "fecha de salida estimada debe ser posterior a fecha de entrada estimada";
        }
        if(fechaEntradaEstimada.isBefore(LocalDateTime.now())) {
            return "fecha de entrada estimada no puede ser anterior a la fecha actual";
        }

        return "";
    }
}
