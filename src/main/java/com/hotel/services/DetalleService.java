package com.hotel.services;

import com.hotel.dtos.DetalleCalendarioDTO;
import com.hotel.dtos.DetalleDTO;
import com.hotel.dtos.dashboard.CategoriaMontoDTO;
import com.hotel.dtos.dashboard.DashboardAlertaItemDTO;
import com.hotel.dtos.dashboard.DashboardAlertasDTO;
import com.hotel.dtos.dashboard.DashboardConteoUnidadDTO;
import com.hotel.dtos.dashboard.DashboardDistribucionFinancieraDTO;
import com.hotel.dtos.dashboard.DashboardResumenDTO;
import com.hotel.dtos.dashboard.DashboardSerieFinancieraDTO;
import com.hotel.dtos.estancia.EstanciaCalendarioDTO;
import com.hotel.dtos.reserva.ReservaCalendarioDTO;
import com.hotel.mappers.EstanciaMapper;
import com.hotel.mappers.ReservaMapper;
import com.hotel.mappers.UnidadMapper;
import com.hotel.models.Estancia;
import com.hotel.models.Habitacion;
import com.hotel.models.Pago;
import com.hotel.models.Reserva;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.EstadoPago;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.models.enums.ModoOcupacion;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.EstanciaRepository;
import com.hotel.repositories.GastoRepository;
import com.hotel.repositories.HabitacionRepository;
import com.hotel.repositories.PagoRepository;
import com.hotel.repositories.ReservaRepository;
import com.hotel.repositories.UnidadRepository;
import com.hotel.specifications.EstanciaSpecification;
import com.hotel.specifications.ReservaSpecification;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DetalleService {

    private static final List<EstadoEstancia> ESTADOS_ESTANCIA_ACTIVA = List.of(
            EstadoEstancia.ACTIVA,
            EstadoEstancia.EXCEDIDA);

    private static final List<EstadoPago> ESTADOS_PAGO_INGRESO = List.of(
            EstadoPago.PENDIENTE,
            EstadoPago.COMPLETADO);

    private final EstanciaRepository estanciaRepository;
    private final ReservaRepository reservaRepository;
    private final UnidadRepository unidadRepository;
    private final HabitacionRepository habitacionRepository;
    private final PagoRepository pagoRepository;
    private final GastoRepository gastoRepository;

    public DetalleService(EstanciaRepository estanciaRepository,
                          ReservaRepository reservaRepository,
                          UnidadRepository unidadRepository,
                          HabitacionRepository habitacionRepository,
                          PagoRepository pagoRepository,
                          GastoRepository gastoRepository) {
        this.estanciaRepository = estanciaRepository;
        this.reservaRepository = reservaRepository;
        this.unidadRepository = unidadRepository;
        this.habitacionRepository = habitacionRepository;
        this.pagoRepository = pagoRepository;
        this.gastoRepository = gastoRepository;
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

    @Transactional(readOnly = true)
    public DashboardResumenDTO obtenerDashboardResumen(LocalDateTime desde, LocalDateTime hasta) {
        validarRangoFechas(desde, hasta);

        long estanciasActivas = estanciaRepository.countByEstadoIn(ESTADOS_ESTANCIA_ACTIVA);
        long reservasConfirmadas = reservaRepository
                .countByEstadoAndEntradaEstimadaLessThanEqualAndSalidaEstimadaGreaterThanEqual(
                        EstadoReserva.CONFIRMADA,
                        hasta,
                        desde);

        long habitacionesOcupadas = estanciaRepository.countHabitacionesOcupadasPorEstados(ESTADOS_ESTANCIA_ACTIVA);
        long habitacionesTotales = habitacionRepository.count();

        BigDecimal ingresos = pagoRepository.sumarMontoPorEstadosYRango(ESTADOS_PAGO_INGRESO, desde, hasta);
        BigDecimal gastos = gastoRepository.sumarMontoEnRango(desde, hasta);
        BigDecimal neto = ingresos.subtract(gastos);

        long pagosPendientesCantidad = pagoRepository.countByEstadoAndFechaBetween(EstadoPago.PENDIENTE, desde, hasta);
        BigDecimal pagosPendientesMonto = pagoRepository.sumarMontoPorEstadoYRango(EstadoPago.PENDIENTE, desde, hasta);

        long reservasExpiradas = reservaRepository.countReservasExpiradas(
                List.of(EstadoReserva.CONFIRMADA),
                LocalDateTime.now());
        long estanciasExcedidas = estanciaRepository.countByEstado(EstadoEstancia.EXCEDIDA);
        long pagosPendientesAntiguos = pagoRepository.countPendientesHastaFecha(
                EstadoPago.PENDIENTE,
                LocalDateTime.now().minusDays(7));

        DashboardResumenDTO.OperativoResumen operativo = new DashboardResumenDTO.OperativoResumen();
        operativo.setEstanciasActivas(estanciasActivas);
        operativo.setReservasConfirmadas(reservasConfirmadas);
        operativo.setHabitacionesOcupadas(habitacionesOcupadas);
        operativo.setHabitacionesTotales(habitacionesTotales);
        operativo.setOcupacionPorcentaje(calcularPorcentaje(habitacionesOcupadas, habitacionesTotales));

        DashboardResumenDTO.FinancieroResumen financiero = new DashboardResumenDTO.FinancieroResumen();
        financiero.setIngresos(ingresos);
        financiero.setGastos(gastos);
        financiero.setNeto(neto);
        financiero.setPagosPendientesCantidad(pagosPendientesCantidad);
        financiero.setPagosPendientesMonto(pagosPendientesMonto);

        DashboardResumenDTO.AlertaResumen alertas = new DashboardResumenDTO.AlertaResumen();
        alertas.setReservasExpiradas(reservasExpiradas);
        alertas.setEstanciasExcedidas(estanciasExcedidas);
        alertas.setPagosPendientesAntiguos(pagosPendientesAntiguos);
        alertas.setTotalAlertas(reservasExpiradas + estanciasExcedidas + pagosPendientesAntiguos);

        DashboardResumenDTO resumen = new DashboardResumenDTO();
        resumen.setOperativo(operativo);
        resumen.setFinanciero(financiero);
        resumen.setAlertas(alertas);
        return resumen;
    }

    @Transactional(readOnly = true)
    public List<DashboardConteoUnidadDTO> obtenerConteosUnidadPorEstado() {
        Map<TipoUnidad, DashboardConteoUnidadDTO> porTipo = new EnumMap<>(TipoUnidad.class);
        for (TipoUnidad tipo : TipoUnidad.values()) {
            DashboardConteoUnidadDTO dto = new DashboardConteoUnidadDTO();
            dto.setTipoUnidad(tipo);
            dto.setDisponible(0L);
            dto.setOcupado(0L);
            dto.setParcialmente(0L);
            dto.setTotal(0L);
            porTipo.put(tipo, dto);
        }

        List<Object[]> conteos = unidadRepository.contarPorTipoYEstadoOperativo();
        for (Object[] fila : conteos) {
            TipoUnidad tipo = (TipoUnidad) fila[0];
            EstadoOperativo estado = (EstadoOperativo) fila[1];
            long cantidad = ((Number) fila[2]).longValue();

            DashboardConteoUnidadDTO dto = porTipo.get(tipo);
            if (dto == null) {
                continue;
            }

            if (estado == EstadoOperativo.DISPONIBLE) {
                dto.setDisponible(cantidad);
            } else if (estado == EstadoOperativo.OCUPADO) {
                dto.setOcupado(cantidad);
            } else if (estado == EstadoOperativo.RESERVADO) {
                dto.setOcupado(dto.getOcupado() + cantidad);
            } else if (estado == EstadoOperativo.PARCIALMENTE) {
                dto.setParcialmente(cantidad);
            }
            dto.setTotal(dto.getDisponible() + dto.getOcupado() + dto.getParcialmente());
        }

        return List.of(
                porTipo.get(TipoUnidad.HABITACION),
                porTipo.get(TipoUnidad.APARTAESTUDIO),
                porTipo.get(TipoUnidad.APARTAMENTO));
    }

    @Transactional(readOnly = true)
    public List<DashboardSerieFinancieraDTO> obtenerSerieFinanciera(
            LocalDateTime desde,
            LocalDateTime hasta,
            String granularidad) {
        validarRangoFechas(desde, hasta);

        Map<LocalDate, BigDecimal> ingresosDiarios = convertirSerieAMapa(
                pagoRepository.sumarIngresosDiarios(ESTADOS_PAGO_INGRESO, desde, hasta));
        Map<LocalDate, BigDecimal> gastosDiarios = convertirSerieAMapa(
                gastoRepository.sumarGastosDiarios(desde, hasta));

        List<DashboardSerieFinancieraDTO> serieDiaria = new ArrayList<>();
        LocalDate actual = desde.toLocalDate();
        LocalDate fin = hasta.toLocalDate();

        while (!actual.isAfter(fin)) {
            BigDecimal ingreso = ingresosDiarios.getOrDefault(actual, BigDecimal.ZERO);
            BigDecimal gasto = gastosDiarios.getOrDefault(actual, BigDecimal.ZERO);

            DashboardSerieFinancieraDTO punto = new DashboardSerieFinancieraDTO();
            punto.setPeriodo(actual);
            punto.setIngresos(ingreso);
            punto.setGastos(gasto);
            punto.setNeto(ingreso.subtract(gasto));
            serieDiaria.add(punto);

            actual = actual.plusDays(1);
        }

        return agregarSeriePorGranularidad(serieDiaria, granularidad);
    }

    @Transactional(readOnly = true)
    public DashboardDistribucionFinancieraDTO obtenerDistribucionFinanciera(
            LocalDateTime desde,
            LocalDateTime hasta) {
        validarRangoFechas(desde, hasta);

        DashboardDistribucionFinancieraDTO dto = new DashboardDistribucionFinancieraDTO();
        dto.setIngresosPorTipoPago(mapearCategoriasMonto(pagoRepository.sumarIngresosPorTipoPago(ESTADOS_PAGO_INGRESO, desde, hasta)));
        dto.setIngresosPorMedioPago(mapearCategoriasMonto(pagoRepository.sumarIngresosPorMedioPago(ESTADOS_PAGO_INGRESO, desde, hasta)));
        dto.setGastosPorConcepto(mapearCategoriasMonto(gastoRepository.sumarGastosPorConcepto(desde, hasta)));
        return dto;
    }

    @Transactional(readOnly = true)
    public DashboardAlertasDTO obtenerAlertasDashboard(int dias, Pageable pageable) {
        if (dias <= 0) {
            throw new IllegalArgumentException("dias debe ser mayor que 0");
        }

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime fechaLimitePendientes = ahora.minusDays(dias);

        List<DashboardAlertaItemDTO> items = new ArrayList<>();
        items.addAll(construirAlertasReservasExpiradas(ahora));
        items.addAll(construirAlertasEstanciasExcedidas());
        items.addAll(construirAlertasPagosPendientes(fechaLimitePendientes));

        items.sort(Comparator.comparing(DashboardAlertaItemDTO::getFechaReferencia).reversed());

        long totalItems = items.size();
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        int fromIndex = Math.min(page * size, items.size());
        int toIndex = Math.min(fromIndex + size, items.size());
        List<DashboardAlertaItemDTO> paginaItems = items.subList(fromIndex, toIndex);

        DashboardAlertasDTO response = new DashboardAlertasDTO();
        response.setResumen(construirResumenAlertas(ahora, fechaLimitePendientes));
        response.setItems(paginaItems);
        response.setPage(page);
        response.setSize(size);
        response.setTotalItems(totalItems);
        response.setTotalPages(size == 0 ? 0 : (int) Math.ceil((double) totalItems / size));
        return response;
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

    private void validarRangoFechas(LocalDateTime desde, LocalDateTime hasta) {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("desde y hasta son obligatorios");
        }
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("desde no puede ser mayor que hasta");
        }
    }

    private BigDecimal calcularPorcentaje(long numerador, long denominador) {
        if (denominador <= 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(numerador)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominador), 2, RoundingMode.HALF_UP);
    }

    private Map<LocalDate, BigDecimal> convertirSerieAMapa(List<Object[]> filas) {
        Map<LocalDate, BigDecimal> resultado = new HashMap<>();
        for (Object[] fila : filas) {
            LocalDate fecha = extraerLocalDate(fila[0]);
            BigDecimal valor = (BigDecimal) fila[1];
            resultado.put(fecha, valor);
        }
        return resultado;
    }

    private LocalDate extraerLocalDate(Object value) {
        if (value instanceof LocalDate fecha) {
            return fecha;
        }
        if (value instanceof java.sql.Date fechaSql) {
            return fechaSql.toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }

    private List<DashboardSerieFinancieraDTO> agregarSeriePorGranularidad(
            List<DashboardSerieFinancieraDTO> serieDiaria,
            String granularidad) {
        String nivel = granularidad == null || granularidad.isBlank()
                ? "DAY"
                : granularidad.trim().toUpperCase();

        if ("DAY".equals(nivel)) {
            return serieDiaria;
        }

        Map<LocalDate, DashboardSerieFinancieraDTO> agrupado = new HashMap<>();
        for (DashboardSerieFinancieraDTO item : serieDiaria) {
            LocalDate periodo = item.getPeriodo();
            LocalDate llave;
            if ("WEEK".equals(nivel)) {
                llave = periodo.with(DayOfWeek.MONDAY);
            } else if ("MONTH".equals(nivel)) {
                llave = periodo.withDayOfMonth(1);
            } else {
                throw new IllegalArgumentException("granularidad no soportada. Usa DAY, WEEK o MONTH");
            }

            DashboardSerieFinancieraDTO acumulado = agrupado.get(llave);
            if (acumulado == null) {
                acumulado = new DashboardSerieFinancieraDTO();
                acumulado.setPeriodo(llave);
                acumulado.setIngresos(BigDecimal.ZERO);
                acumulado.setGastos(BigDecimal.ZERO);
                acumulado.setNeto(BigDecimal.ZERO);
                agrupado.put(llave, acumulado);
            }

            acumulado.setIngresos(acumulado.getIngresos().add(item.getIngresos()));
            acumulado.setGastos(acumulado.getGastos().add(item.getGastos()));
            acumulado.setNeto(acumulado.getNeto().add(item.getNeto()));
        }

        return agrupado.values().stream()
                .sorted(Comparator.comparing(DashboardSerieFinancieraDTO::getPeriodo))
                .toList();
    }

    private List<CategoriaMontoDTO> mapearCategoriasMonto(List<Object[]> filas) {
        return filas.stream()
                .map(fila -> new CategoriaMontoDTO(String.valueOf(fila[0]), (BigDecimal) fila[1]))
                .toList();
    }

    private List<DashboardAlertaItemDTO> construirAlertasReservasExpiradas(LocalDateTime ahora) {
        List<Reserva> reservasExpiradas = reservaRepository.findReservasExpiradas(
                List.of(EstadoReserva.CONFIRMADA),
                ahora);

        return reservasExpiradas.stream().map(reserva -> {
            DashboardAlertaItemDTO item = new DashboardAlertaItemDTO();
            item.setTipo("RESERVA_EXPIRADA");
            item.setSeveridad("ALTA");
            item.setCodigoReserva(reserva.getCodigo());
            item.setCodigoEstancia(null);
            item.setFechaReferencia(reserva.getSalidaEstimada());
            item.setMensaje("Reserva confirmada vencida sin activacion de estancia.");
            return item;
        }).toList();
    }

    private List<DashboardAlertaItemDTO> construirAlertasEstanciasExcedidas() {
        List<Estancia> estancias = estanciaRepository.findByEstadoConReserva(EstadoEstancia.EXCEDIDA);

        return estancias.stream().map(estancia -> {
            DashboardAlertaItemDTO item = new DashboardAlertaItemDTO();
            item.setTipo("ESTANCIA_EXCEDIDA");
            item.setSeveridad("MEDIA");
            item.setCodigoReserva(estancia.getReserva() != null ? estancia.getReserva().getCodigo() : null);
            item.setCodigoEstancia(estancia.getCodigoFolio());
            item.setFechaReferencia(estancia.getSalidaEstimada());
            item.setMensaje("Estancia en estado EXCEDIDA; requiere gestion de salida o extension.");
            return item;
        }).toList();
    }

    private List<DashboardAlertaItemDTO> construirAlertasPagosPendientes(LocalDateTime fechaLimitePendientes) {
        List<Pago> pagosPendientes = pagoRepository.findPendientesHastaFecha(EstadoPago.PENDIENTE, fechaLimitePendientes);

        return pagosPendientes.stream().map(pago -> {
            DashboardAlertaItemDTO item = new DashboardAlertaItemDTO();
            item.setTipo("PAGO_PENDIENTE_ANTIGUO");
            item.setSeveridad("MEDIA");
            item.setCodigoReserva(pago.getEstancia() != null && pago.getEstancia().getReserva() != null
                    ? pago.getEstancia().getReserva().getCodigo()
                    : null);
            item.setCodigoEstancia(pago.getEstancia() != null ? pago.getEstancia().getCodigoFolio() : null);
            item.setFechaReferencia(pago.getFecha());
            item.setMensaje("Pago pendiente con antiguedad superior al umbral configurado.");
            return item;
        }).toList();
    }

    private DashboardResumenDTO.AlertaResumen construirResumenAlertas(
            LocalDateTime ahora,
            LocalDateTime fechaLimitePendientes) {
        long reservasExpiradas = reservaRepository.countReservasExpiradas(
                List.of(EstadoReserva.CONFIRMADA),
                ahora);
        long estanciasExcedidas = estanciaRepository.countByEstado(EstadoEstancia.EXCEDIDA);
        long pagosPendientes = pagoRepository.countPendientesHastaFecha(EstadoPago.PENDIENTE, fechaLimitePendientes);

        DashboardResumenDTO.AlertaResumen resumen = new DashboardResumenDTO.AlertaResumen();
        resumen.setReservasExpiradas(reservasExpiradas);
        resumen.setEstanciasExcedidas(estanciasExcedidas);
        resumen.setPagosPendientesAntiguos(pagosPendientes);
        resumen.setTotalAlertas(reservasExpiradas + estanciasExcedidas + pagosPendientes);
        return resumen;
    }
}
