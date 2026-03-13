package com.hotel.services;

import com.hotel.dtos.DetalleCalendarioUnidadDTO;
import com.hotel.dtos.HabitacionDTO;
import com.hotel.dtos.UnidadDTO;
import com.hotel.dtos.dashboard.CategoriaMontoConteoDTO;
import com.hotel.dtos.dashboard.CategoriaMontoDTO;
import com.hotel.dtos.dashboard.DashboardDistribucionFinancieraDTO;
import com.hotel.dtos.dashboard.DashboardResumenDTO;
import com.hotel.dtos.dashboard.EstanciaMensualDTO;
import com.hotel.dtos.estancia.EstanciaDTO;
import com.hotel.dtos.reserva.ReservaDTO;
import com.hotel.dtos.DetalleCalendarioHabitacionDTO;
import com.hotel.mappers.EstanciaMapper;
import com.hotel.mappers.HabitacionMapper;
import com.hotel.mappers.ReservaMapper;
import com.hotel.mappers.UnidadMapper;
import com.hotel.models.Estancia;
import com.hotel.models.Habitacion;
import com.hotel.models.Reserva;
import com.hotel.models.Unidad;
import com.hotel.models.enums.CanalReserva;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.EstadoPago;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.models.enums.TipoPago;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.EstanciaRepository;
import com.hotel.repositories.GastoRepository;
import com.hotel.repositories.HabitacionRepository;
import com.hotel.repositories.PagoRepository;
import com.hotel.repositories.ReservaRepository;
import com.hotel.repositories.UnidadRepository;
import com.hotel.specifications.EstanciaSpecification;
import com.hotel.specifications.ReservaSpecification;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DetalleService {

    private static final List<EstadoEstancia> ESTADOS_ESTANCIA_ACTIVA = List.of(
            EstadoEstancia.ACTIVA,
            EstadoEstancia.EXCEDIDA);

    private static final List<EstadoEstancia> ESTADOS_ESTANCIA_ACTIVA_FINALIZADA = List.of(
            EstadoEstancia.ACTIVA,
            EstadoEstancia.FINALIZADA);

    private static final List<EstadoPago> ESTADOS_PAGO_INGRESO = List.of(
            EstadoPago.PENDIENTE,
            EstadoPago.COMPLETADO);

    Logger logger = Logger.getLogger(DetalleService.class.getName());

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
    public List<DetalleCalendarioUnidadDTO> obtenerCalendario(
            LocalDateTime desde,
            LocalDateTime hasta,
            TipoUnidad tipoUnidad,
            String codigoUnidad,
            List<EstadoReserva> estadosReserva,
            List<EstadoEstancia> estadosEstancia) {

        logger.info("[obtenerCalendario] verificando rango de fechas: desde=" + desde + ", hasta=" + hasta);
        validarRangoFechas(desde, hasta);

        if((estadosReserva == null || estadosReserva.isEmpty()) && (estadosEstancia == null || estadosEstancia.isEmpty())) {
            logger.info("[obtenerCalendario] no se especificaron estados de reserva ni estancia, se traera una lista vacia");
            return List.of();
        }

        List<EstadoReserva> estadosReservaFiltrados = normalizarEstadosReserva(estadosReserva);
        List<EstadoEstancia> estadosEstanciaFiltrados = normalizarEstadosEstancia(estadosEstancia);
        List<Habitacion> habitacionesConsultadas = obtenerHabitacionesAConsultar(tipoUnidad, codigoUnidad);
        logger.info("[obtenerCalendario] habitaciones consultadas: " + habitacionesConsultadas.size() + " para tipoUnidad=" + tipoUnidad + " y codigoUnidad=" + codigoUnidad);

        List<Reserva> reservas = reservaRepository.findAll(
                ReservaSpecification.byCalendario(
                        desde,
                        hasta,
                        habitacionesConsultadas.stream().map(Habitacion::getCodigo).toList(),
                        estadosReservaFiltrados));

        List<Estancia> estancias = estadosEstanciaFiltrados.isEmpty()
                ? List.of()
                : estanciaRepository.findAll(
                        EstanciaSpecification.byCalendario(
                                desde,
                                hasta,
                                habitacionesConsultadas.stream().map(Habitacion::getCodigo).toList(),
                                estadosEstanciaFiltrados));


        return construirCalendario(habitacionesConsultadas, reservas, estancias);
    }


    private List<DetalleCalendarioUnidadDTO> construirCalendario(List<Habitacion> habitaciones, List<Reserva> reservas, List<Estancia> estancias) {
        if (habitaciones == null || habitaciones.isEmpty()) {
            return List.of();
        }

        Map<Long, Unidad> unidadesPorId = new LinkedHashMap<>();
        for (Habitacion habitacion : habitaciones) {
            if (habitacion == null || habitacion.getUnidad() == null) {
                continue;
            }
            unidadesPorId.putIfAbsent(habitacion.getUnidad().getId(), habitacion.getUnidad());
        }

        List<DetalleCalendarioUnidadDTO> detalleCalendario = new ArrayList<>();
        for (Unidad unidad : unidadesPorId.values()) {
            DetalleCalendarioUnidadDTO dto = new DetalleCalendarioUnidadDTO();
            dto.setUnidad(UnidadMapper.entityToDto(unidad));
            dto.setHabitaciones(new ArrayList<>());

            Map<Long, ReservaDTO> reservasUnidad = new LinkedHashMap<>();
            Map<Long, EstanciaDTO> estanciasUnidad = new LinkedHashMap<>();

            List<Habitacion> habitacionesUnidad = unidad.getHabitaciones() == null
                    ? List.of()
                    : unidad.getHabitaciones();

            for (Habitacion habitacionUnidad : habitacionesUnidad) {
                DetalleCalendarioHabitacionDTO habitacionDTO = new DetalleCalendarioHabitacionDTO();
                habitacionDTO.setHabitacion(HabitacionMapper.entityToDto(habitacionUnidad));
                List<ReservaDTO> reservasHabitacion = reservas.stream()
                        .filter(reserva -> reserva.getHabitaciones().contains(habitacionUnidad))
                        .map(ReservaMapper::entityToDTO)
                        .toList();
                habitacionDTO.setReservas(reservasHabitacion);
                for (ReservaDTO reservaDTO : reservasHabitacion) {
                    reservasUnidad.putIfAbsent(reservaDTO.getId(), reservaDTO);
                }

                List<EstanciaDTO> estanciasHabitacion = estancias.stream()
                        .filter(estancia -> estancia.getHabitaciones().contains(habitacionUnidad))
                        .map(EstanciaMapper::entityToDTO)
                        .toList();
                habitacionDTO.setEstancias(estanciasHabitacion);
                for (EstanciaDTO estanciaDTO : estanciasHabitacion) {
                    estanciasUnidad.putIfAbsent(estanciaDTO.getId(), estanciaDTO);
                }

                dto.getHabitaciones().add(habitacionDTO);
            }

            dto.setReservas(new ArrayList<>(reservasUnidad.values()));
            dto.setEstancias(new ArrayList<>(estanciasUnidad.values()));

            if (!dto.getHabitaciones().isEmpty()) {
                detalleCalendario.add(dto);
            }
        }

        return detalleCalendario;
    }
    private List<EstadoReserva> normalizarEstadosReserva(List<EstadoReserva> estadosReserva) {
        if (estadosReserva == null || estadosReserva.isEmpty()) {
            return List.of();
        }
        return estadosReserva;
    }

    private List<EstadoEstancia> normalizarEstadosEstancia(List<EstadoEstancia> estadosEstancia) {
        if (estadosEstancia == null || estadosEstancia.isEmpty()) {
            return List.of();
        }

        return estadosEstancia;
    }

    private List<Habitacion> obtenerHabitacionesAConsultar(TipoUnidad tipoUnidad, String codigoUnidad) {
        String codigoUnidadNormalizado = codigoUnidad == null || codigoUnidad.isBlank() ? null : codigoUnidad.trim().toLowerCase();

        if(tipoUnidad == null && codigoUnidadNormalizado == null) {
            return habitacionRepository.findAll();
        }

        if(tipoUnidad == null) {
            return habitacionRepository.findByCodigoContainingIgnoreCaseOrderByCodigoAsc(codigoUnidadNormalizado);
        }

        if(tipoUnidad.equals(TipoUnidad.HABITACION) || tipoUnidad.equals(TipoUnidad.APARTAMENTO)) {
            if(codigoUnidadNormalizado == null) {
                return habitacionRepository.findHabitacionesByTipoUnidadApartamentoOApartaestudio(TipoUnidad.APARTAMENTO);
            }
            List<Habitacion> habitacionesConsultadas = habitacionRepository.findHabitacionesByTipoUnidadApartamentoOApartaestudio(TipoUnidad.APARTAMENTO);
            return habitacionesConsultadas.stream()
                    .filter(habitacion -> habitacion.getCodigo().toLowerCase().contains(codigoUnidadNormalizado))
                    .toList();
        }

        if (tipoUnidad.equals(TipoUnidad.APARTAESTUDIO)) {
            if(codigoUnidadNormalizado == null) {
                return habitacionRepository.findHabitacionesByTipoUnidadApartamentoOApartaestudio(TipoUnidad.APARTAESTUDIO);
            }
            List<Habitacion> habitacionesConsultadas = habitacionRepository.findHabitacionesByTipoUnidadApartamentoOApartaestudio(TipoUnidad.APARTAESTUDIO);
            return habitacionesConsultadas.stream()
                    .filter(habitacion -> habitacion.getCodigo().toLowerCase().contains(codigoUnidadNormalizado))
                    .toList();
        }

        throw new IllegalArgumentException("Tipo de unidad no soportado: " + tipoUnidad);
    }


    @Transactional(readOnly = true)
    public DashboardResumenDTO obtenerDashboardResumen() {

        long estanciasActivas = estanciaRepository.countByEstadoIn(ESTADOS_ESTANCIA_ACTIVA);
        long reservasConfirmadas = reservaRepository.countByEstadoIn(List.of(EstadoReserva.CONFIRMADA, EstadoReserva.EXPIRADA));
        Map<String, Long> reservasPorCanal = construirReservasPorCanal();

        long apartamentosTotales = unidadRepository.countByTipo(TipoUnidad.APARTAMENTO);
        long apartaestudioTotales = unidadRepository.countByTipo(TipoUnidad.APARTAESTUDIO);
        long apartamentosOcupados = unidadRepository.countUnidadesConTodasHabitacionesEnEstadoPorTipo(
                TipoUnidad.APARTAMENTO,
                EstadoOperativo.OCUPADO);
        long apartamentosReservados = unidadRepository.countUnidadesConTodasHabitacionesEnEstadoPorTipo(
                TipoUnidad.APARTAMENTO,
                EstadoOperativo.RESERVADO);
        long apartaestudioOcupados = unidadRepository.countUnidadesConHabitacionEnEstadoPorTipo(
                TipoUnidad.APARTAESTUDIO,
                EstadoOperativo.OCUPADO);
        long apartaestudioReservados = unidadRepository.countUnidadesConHabitacionEnEstadoPorTipo(
                TipoUnidad.APARTAESTUDIO,
                EstadoOperativo.RESERVADO);

        long habitacionesOcupadas = habitacionRepository.countByUnidadTipoAndEstadoOperativo(
                TipoUnidad.APARTAMENTO,
                EstadoOperativo.OCUPADO);
        long habitacionesReservadas = habitacionRepository.countByUnidadTipoAndEstadoOperativo(
                TipoUnidad.APARTAMENTO,
                EstadoOperativo.RESERVADO);
        long habitacionesTotales = habitacionRepository.countByUnidadTipo(TipoUnidad.APARTAMENTO);

        long habitacionesGlobalOcupadas = habitacionRepository.countByEstadoOperativo(EstadoOperativo.OCUPADO);
        long habitacionesGlobalReservadas = habitacionRepository.countByEstadoOperativo(EstadoOperativo.RESERVADO);
        long habitacionesGlobalTotales = habitacionRepository.count();
        List<EstanciaMensualDTO> estanciasUltimos12Meses = construirEstanciasUltimos12Meses();

        long reservasExpiradas = reservaRepository.countByEstado(EstadoReserva.EXPIRADA);
        long estanciasExcedidas = estanciaRepository.countByEstado(EstadoEstancia.EXCEDIDA);

        DashboardResumenDTO.OperativoResumen operativo = new DashboardResumenDTO.OperativoResumen();
        operativo.setEstanciasActivas(estanciasActivas);
        operativo.setReservasConfirmadas(reservasConfirmadas);
        operativo.setReservasPorCanal(reservasPorCanal);
        operativo.setApartamentosOcupados(apartamentosOcupados);
        operativo.setApartamentosReservados(apartamentosReservados);
        operativo.setApartaestudioOcupados(apartaestudioOcupados);
        operativo.setApartaestudioReservados(apartaestudioReservados);
        operativo.setApartamentosTotales(apartamentosTotales);
        operativo.setApartaestudioTotales(apartaestudioTotales);
        operativo.setHabitacionesOcupadas(habitacionesOcupadas);
        operativo.setHabitacionesReservadas(habitacionesReservadas);
        operativo.setHabitacionesTotales(habitacionesTotales);
        operativo.setOcupacionPorcentaje(calcularPorcentaje(habitacionesGlobalOcupadas, habitacionesGlobalTotales));
        operativo.setReservadasPorcentaje(calcularPorcentaje(habitacionesGlobalReservadas, habitacionesGlobalTotales));
        operativo.setEstanciasUltimos12Meses(estanciasUltimos12Meses);

        DashboardResumenDTO.AlertaResumen alertas = new DashboardResumenDTO.AlertaResumen();
        alertas.setReservasExpiradas(reservasExpiradas);
        alertas.setEstanciasExcedidas(estanciasExcedidas);
        alertas.setTotalAlertas(reservasExpiradas + estanciasExcedidas);

        DashboardResumenDTO resumen = new DashboardResumenDTO();
        resumen.setOperativo(operativo);
        resumen.setAlertas(alertas);
        return resumen;
    }

    @Transactional(readOnly = true)
    public DashboardDistribucionFinancieraDTO obtenerDistribucionFinanciera(
            LocalDateTime desde,
            LocalDateTime hasta) {
        validarRangoFechasOpcional(desde, hasta);

        BigDecimal ingresosPagos = pagoRepository.sumarMontoPorEstadoConRangoOpcional(EstadoPago.COMPLETADO, desde, hasta);
        long ingresosPendientesCantidad = pagoRepository.countByEstadoAndTipoPagoConRangoOpcional(
                EstadoPago.PENDIENTE,
                TipoPago.ESTANCIA_COMPLETADA,
                desde,
                hasta);
        BigDecimal ingresosPendientesMonto = pagoRepository.sumarMontoPorEstadoAndTipoPagoConRangoOpcional(
                EstadoPago.PENDIENTE,
                TipoPago.ESTANCIA_COMPLETADA,
                desde,
                hasta);
        BigDecimal ingresosTotales = ingresosPagos.add(ingresosPendientesMonto);
        BigDecimal gastos = gastoRepository.sumarMontoEnRango(desde, hasta);
        BigDecimal neto = ingresosPagos.subtract(gastos);

        DashboardDistribucionFinancieraDTO dto = new DashboardDistribucionFinancieraDTO();
        dto.setIngresosTotales(ingresosTotales);
        dto.setIngresosPagos(ingresosPagos);
        dto.setGastos(gastos);
        dto.setNeto(neto);
        dto.setIngresosPendientesCantidad(ingresosPendientesCantidad);
        dto.setIngresosPendientesMonto(ingresosPendientesMonto);
        dto.setIngresosPorTipoPago(mapearCategoriasMonto(pagoRepository.sumarIngresosPorTipoPago(
                ESTADOS_PAGO_INGRESO,
                TipoPago.ESTANCIA_COMPLETADA,
                desde,
                hasta)));
        dto.setIngresosPorMedioPago(mapearCategoriasMontoConteo(pagoRepository.sumarYContarIngresosPorMedioPago(
                ESTADOS_PAGO_INGRESO,
                TipoPago.ESTANCIA_COMPLETADA,
                desde,
                hasta)));
        return dto;
    }

    private void validarRangoFechas(LocalDateTime desde, LocalDateTime hasta) {
        if (desde == null || hasta == null) {
            throw new IllegalArgumentException("desde y hasta son obligatorios");
        }
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("desde no puede ser mayor que hasta");
        }
    }

    private void validarRangoFechasOpcional(LocalDateTime desde, LocalDateTime hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
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

    private List<CategoriaMontoDTO> mapearCategoriasMonto(List<Object[]> filas) {
        return filas.stream()
                .map(fila -> new CategoriaMontoDTO(String.valueOf(fila[0]), (BigDecimal) fila[1]))
                .toList();
    }

    private List<CategoriaMontoConteoDTO> mapearCategoriasMontoConteo(List<Object[]> filas) {
        return filas.stream()
                .map(fila -> new CategoriaMontoConteoDTO(
                        String.valueOf(fila[0]),
                        (BigDecimal) fila[1],
                        ((Number) fila[2]).longValue()))
                .toList();
    }

    private Map<String, Long> construirReservasPorCanal() {
        Map<String, Long> reservasPorCanal = new LinkedHashMap<>();
        for (CanalReserva canal : CanalReserva.values()) {
            reservasPorCanal.put(canal.name(), 0L);
        }
        for (Object[] fila : reservaRepository.contarReservasPorCanal()) {
            CanalReserva canal = (CanalReserva) fila[0];
            long cantidad = ((Number) fila[1]).longValue();
            reservasPorCanal.put(canal.name(), cantidad);
        }
        return reservasPorCanal;
    }

    private List<EstanciaMensualDTO> construirEstanciasUltimos12Meses() {
        YearMonth mesActual = YearMonth.from(LocalDate.now());
        YearMonth mesInicio = mesActual.minusMonths(11);

        LocalDateTime desde = mesInicio.atDay(1).atStartOfDay();
        LocalDateTime hasta = mesActual.atEndOfMonth().atTime(23, 59, 59);

        List<Object[]> filas = estanciaRepository.contarEstanciasPorMesSegunEntradaRealYEstados(
                ESTADOS_ESTANCIA_ACTIVA_FINALIZADA,
                desde,
                hasta);

        Map<YearMonth, Long> conteoPorMes = new HashMap<>();
        for (Object[] fila : filas) {
            int anio = ((Number) fila[0]).intValue();
            int mes = ((Number) fila[1]).intValue();
            long cantidad = ((Number) fila[2]).longValue();
            conteoPorMes.put(YearMonth.of(anio, mes), cantidad);
        }

        List<EstanciaMensualDTO> resultado = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            YearMonth periodo = mesInicio.plusMonths(i);
            resultado.add(new EstanciaMensualDTO(
                    periodo.toString(),
                    conteoPorMes.getOrDefault(periodo, 0L)));
        }
        return resultado;
    }

}
