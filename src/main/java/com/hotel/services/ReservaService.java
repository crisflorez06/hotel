package com.hotel.services;

import com.hotel.dtos.reserva.ReservaCalendarioDTO;
import com.hotel.dtos.reserva.ReservaNuevaRequestDTO;
import com.hotel.mappers.ReservaMapper;
import com.hotel.models.*;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.models.enums.ModoOcupacion;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.ReservaRepository;
import com.hotel.resolvers.EstanciaReservaResolver;
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

import java.util.List;

@Service
public class ReservaService {

    private static final Logger logger = LoggerFactory.getLogger(ReservaService.class);

    private final ReservaRepository reservaRepository;
    private final PagoService pagoService;
    private final OcupanteService ocupanteService;
    private final UnidadHabitacionResolver unidadHabitacionResolver;
    private final DisponibilidadService disponibilidadService;
    private final EstanciaReservaResolver estanciaReservaResolver;

    public ReservaService(ReservaRepository reservaRepository,
                          OcupanteService ocupanteService,
                          PagoService pagoService,
                          UnidadHabitacionResolver unidadHabitacionResolver,
                          DisponibilidadService disponibilidadService,
                          EstanciaReservaResolver estanciaReservaResolver) {
        this.estanciaReservaResolver = estanciaReservaResolver;
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
        reserva.setCliente(ocupanteService.buscarPorId(request.getIdOcupante()));

        logger.info("[crearReserva] Asignando habitaciones a la reserva");
        List<Habitacion> habitaciones = unidadHabitacionResolver.buscarListaHabitaciones(request.getCodigo(), request.getTipoUnidad());
        reserva.setHabitaciones(habitaciones);

        logger.info("[crearReserva] Creando estancia asociada a la reserva");
        Estancia estancia = estanciaReservaResolver.crearEstanciaDesdeReserva(reserva);
        reserva.setEstancia(estancia);

        logger.info("[crearReserva] Creando pago inicial asociado a la estancia de la reserva");
        pagoService.crearPago(request.getPago(), estancia);

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

    public List<ReservaCalendarioDTO> buscarReservasPorNumeroDocumento(String numeroDocumento) {
        if (numeroDocumento == null || numeroDocumento.isBlank()) {
            throw new IllegalArgumentException("numeroDocumento es obligatorio");
        }

        List<Reserva> reservas = reservaRepository.findByCliente_NumeroDocumentoContainingIgnoreCaseAndEstadoIn(
                numeroDocumento,
                List.of(EstadoReserva.PENDIENTE, EstadoReserva.CONFIRMADA));


        return llenarTipoYCodigoUnidad(ReservaMapper.entityListaToCalendarioDTOList(reservas));
    }

    public Reserva cambiarEstadoReserva(Long reservaId, EstadoReserva nuevoEstado) {
        Reserva reserva = buscarPorId(reservaId);
        reserva.setEstado(nuevoEstado);
        return reservaRepository.save(reserva);
    }

    private List<ReservaCalendarioDTO> llenarTipoYCodigoUnidad(List<ReservaCalendarioDTO> reservas) {
        for(ReservaCalendarioDTO reservaDto : reservas) {
            Reserva reserva = buscarPorId(reservaDto.getId());
            if(reserva.getModoOcupacion() == ModoOcupacion.INDIVIDUAL) {
                reservaDto.setCodigoUnidad(reserva.getHabitaciones().getFirst().getCodigo());
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
