package com.hotel.services;

import com.hotel.dtos.DetalleCalendarioDTO;
import com.hotel.dtos.DetalleDTO;
import com.hotel.dtos.estancia.EstanciaCalendarioDTO;
import com.hotel.dtos.reserva.ReservaCalendarioDTO;
import com.hotel.mappers.EstanciaMapper;
import com.hotel.mappers.ReservaMapper;
import com.hotel.mappers.UnidadMapper;
import com.hotel.models.Estancia;
import com.hotel.models.Habitacion;
import com.hotel.models.Reserva;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.models.enums.ModoOcupacion;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.EstanciaRepository;
import com.hotel.repositories.ReservaRepository;
import com.hotel.specifications.EstanciaSpecification;
import com.hotel.specifications.ReservaSpecification;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DetalleService {

    private final EstanciaRepository estanciaRepository;
    private final ReservaRepository reservaRepository;

    public DetalleService(EstanciaRepository estanciaRepository, ReservaRepository reservaRepository) {
        this.estanciaRepository = estanciaRepository;
        this.reservaRepository = reservaRepository;
    }

    @Transactional(readOnly = true)
    public DetalleDTO obtenerDetallePorEstanciaId(Long idEstancia) {
        Estancia estancia = estanciaRepository.findById(idEstancia)
                .orElseThrow(() -> new IllegalArgumentException("Estancia no encontrada con id: " + idEstancia));

        DetalleDTO detalle = new DetalleDTO();
        detalle.setEstancia(EstanciaMapper.entityToDTO(estancia));

        List<Habitacion> habitaciones = estancia.getHabitaciones();
        if (habitaciones != null && !habitaciones.isEmpty() && habitaciones.getFirst().getUnidad() != null) {
            detalle.setUnidad(UnidadMapper.entityToDto(habitaciones.getFirst().getUnidad()));
        }

        Reserva reserva = estancia.getReserva();
        if (reserva != null) {
            detalle.setReserva(ReservaMapper.entityToCalendarioDTO(reserva));
        }

        return detalle;
    }

    @Transactional(readOnly = true)
    public DetalleCalendarioDTO obtenerCalendario(
            String mes,
            TipoUnidad tipoUnidad,
            String codigoUnidad,
            List<EstadoReserva> estadosReserva,
            List<EstadoEstancia> estadosEstancia) {
        if (mes == null || mes.isBlank()) {
            throw new IllegalArgumentException("mes es obligatorio (formato YYYY-MM)");
        }

        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.parse(mes, DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("mes debe tener formato YYYY-MM", ex);
        }

        LocalDateTime desde = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime hasta = yearMonth.atEndOfMonth().atTime(LocalTime.MAX);

        boolean filtrosReservaSeleccionados = estadosReserva != null && !estadosReserva.isEmpty();
        boolean filtrosEstanciaSeleccionados = estadosEstancia != null && !estadosEstancia.isEmpty();
        boolean consultarSoloReservas = filtrosReservaSeleccionados && !filtrosEstanciaSeleccionados;
        boolean consultarSoloEstancias = filtrosEstanciaSeleccionados && !filtrosReservaSeleccionados;

        List<Reserva> reservas = List.of();
        if (!consultarSoloEstancias) {
            List<EstadoReserva> estadosReservaFiltrados = normalizarEstadosReserva(estadosReserva);
            reservas = reservaRepository.findAll(
                    ReservaSpecification.byCalendario(
                            desde,
                            hasta,
                            tipoUnidad,
                            codigoUnidad,
                            estadosReservaFiltrados));
        }

        List<Estancia> estancias = List.of();
        if (!consultarSoloReservas) {
            List<EstadoEstancia> estadosEstanciaFiltrados = normalizarEstadosEstancia(
                    estadosEstancia,
                    filtrosEstanciaSeleccionados);
            if (!estadosEstanciaFiltrados.isEmpty()) {
                estancias = estanciaRepository.findAll(
                        EstanciaSpecification.byCalendario(
                                desde,
                                hasta,
                                tipoUnidad,
                                codigoUnidad,
                                estadosEstanciaFiltrados));
            }
        }

        DetalleCalendarioDTO detalleCalendario = new DetalleCalendarioDTO();
        detalleCalendario.setReservas(llenarTipoYCodigoUnidadReserva(
                ReservaMapper.entityListaToCalendarioDTOList(reservas),
                reservas));
        detalleCalendario.setEstancias(llenarTipoYCodigoUnidadEstancia(
                estancias.stream().map(EstanciaMapper::entityToCalendarioDTO).toList(),
                estancias));

        return detalleCalendario;
    }

    private List<EstadoReserva> normalizarEstadosReserva(List<EstadoReserva> estadosReserva) {
        if (estadosReserva == null || estadosReserva.isEmpty()) {
            return List.of(EstadoReserva.CONFIRMADA);
        }
        return estadosReserva;
    }

    private List<EstadoEstancia> normalizarEstadosEstancia(
            List<EstadoEstancia> estadosEstancia,
            boolean filtroSeleccionado) {
        if (estadosEstancia == null || estadosEstancia.isEmpty()) {
            return List.of(EstadoEstancia.ACTIVA, EstadoEstancia.EXCEDIDA);
        }

        List<EstadoEstancia> filtrados = estadosEstancia.stream()
                .filter(estado -> estado != EstadoEstancia.RESERVADA)
                .toList();

        if (filtrados.isEmpty()) {
            return filtroSeleccionado ? List.of() : List.of(EstadoEstancia.ACTIVA, EstadoEstancia.EXCEDIDA);
        }

        return filtrados;
    }

    private List<ReservaCalendarioDTO> llenarTipoYCodigoUnidadReserva(
            List<ReservaCalendarioDTO> reservas,
            List<Reserva> reservaEntities) {
        Map<Long, Reserva> reservasPorId = reservaEntities.stream()
                .collect(Collectors.toMap(Reserva::getId, Function.identity()));

        for (ReservaCalendarioDTO reservaDto : reservas) {
            Reserva reserva = reservasPorId.get(reservaDto.getId());
            if (reserva == null) {
                continue;
            }
            if (reserva.getModoOcupacion() == ModoOcupacion.INDIVIDUAL) {
                reservaDto.setCodigoUnidad(reserva.getHabitaciones().getFirst().getCodigo());
                reservaDto.setTipoUnidad(TipoUnidad.HABITACION);
            } else {
                reservaDto.setCodigoUnidad(reserva.getHabitaciones().getFirst().getUnidad().getCodigo());
                reservaDto.setTipoUnidad(reserva.getHabitaciones().getFirst().getUnidad().getTipo());
            }
        }
        return reservas;
    }

    private List<EstanciaCalendarioDTO> llenarTipoYCodigoUnidadEstancia(
            List<EstanciaCalendarioDTO> estancias,
            List<Estancia> estanciaEntities) {
        Map<Long, Estancia> estanciasPorId = estanciaEntities.stream()
                .collect(Collectors.toMap(Estancia::getId, Function.identity()));

        for (EstanciaCalendarioDTO estanciaDto : estancias) {
            Estancia estancia = estanciasPorId.get(estanciaDto.getId());
            if (estancia == null) {
                continue;
            }
            if (estancia.getModoOcupacion() == ModoOcupacion.INDIVIDUAL) {
                estanciaDto.setCodigoUnidad(estancia.getHabitaciones().getFirst().getCodigo());
                estanciaDto.setTipoUnidad(TipoUnidad.HABITACION.name());
            } else {
                estanciaDto.setCodigoUnidad(estancia.getHabitaciones().getFirst().getUnidad().getCodigo());
                estanciaDto.setTipoUnidad(estancia.getHabitaciones().getFirst().getUnidad().getTipo().name());
            }
        }
        return estancias;
    }
}
