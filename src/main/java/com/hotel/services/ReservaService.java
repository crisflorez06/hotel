package com.hotel.services;

import com.hotel.dtos.reserva.ReservaCalendarioDTO;
import com.hotel.dtos.reserva.ReservaNuevaRequestDTO;
import com.hotel.dtos.reserva.ReservaTablaDTO;
import com.hotel.dtos.pago.PagoNuevoRequestDTO;
import com.hotel.mappers.ReservaMapper;
import com.hotel.models.*;
import com.hotel.models.enums.*;
import com.hotel.repositories.ReservaRepository;
import com.hotel.resolvers.EstanciaReservaResolver;
import com.hotel.resolvers.UnidadHabitacionResolver;
import com.hotel.specifications.ReservaSpecification;
import com.hotel.utils.EventoModificadoJsonBuilder;
import com.hotel.utils.EventoNuevoJsonBuilder;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final CodigoUnicoService codigoUnicoService;
    private final AuditoriaEventoService eventoService;

    public ReservaService(ReservaRepository reservaRepository,
                          OcupanteService ocupanteService,
                          PagoService pagoService,
                          UnidadHabitacionResolver unidadHabitacionResolver,
                          DisponibilidadService disponibilidadService,
                          EstanciaReservaResolver estanciaReservaResolver,
                          CodigoUnicoService codigoUnicoService,
                          AuditoriaEventoService eventoService) {
        this.eventoService = eventoService;
        this.estanciaReservaResolver = estanciaReservaResolver;
        this.pagoService = pagoService;
        this.unidadHabitacionResolver = unidadHabitacionResolver;
        this.ocupanteService = ocupanteService;
        this.reservaRepository = reservaRepository;
        this.disponibilidadService = disponibilidadService;
        this.codigoUnicoService = codigoUnicoService;
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
        String existeDisponibilidad = disponibilidadService.verificarDisponibilidad(null, null, codigo, tipoUnidad, entradaEstimada, salidaEstimada);
        if (!existeDisponibilidad.isEmpty()) {
            throw new IllegalArgumentException("No es posible crear la reserva: " + existeDisponibilidad);
        }

        logger.info("[crearReserva] Validando fechas de entrada y salida estimadas");
        String fechaConflicto = fechaTieneConflicto(salidaEstimada, entradaEstimada);
        if (!fechaConflicto.isEmpty()) {
            throw new IllegalArgumentException("No es posible crear la reserva: " + fechaConflicto);
        }

        logger.info("[crearReserva] Creando reserva para el ocupante con ID: {}", request.getIdOcupante());
        Reserva reserva = ReservaMapper.requestNuevoToEntity(request);
        reserva.setCodigo(codigoUnicoService.generarCodigoReserva());

        logger.info("[crearReserva] Buscando ocupante para asignar a la reserva");
        reserva.setCliente(determinarCliente(request.getIdOcupante()));

        logger.info("[crearReserva] Asignando habitaciones a la reserva");
        List<Habitacion> habitaciones = unidadHabitacionResolver.buscarListaHabitaciones(request.getCodigo(), request.getTipoUnidad());
        reserva.setHabitaciones(habitaciones);

        logger.info("[crearReserva] Creando estancia asociada a la reserva");
        Estancia estancia = estanciaReservaResolver.crearEstanciaDesdeReserva(reserva);
        reserva.setEstancia(estancia);

        logger.info("[crearReserva] Creando pago inicial asociado a la estancia de la reserva");
        pagoService.crearPago(request.getPago(), estancia);

        Reserva reservaGuardada = reservaRepository.save(reserva);

        logger.info("[crearReserva] Registrando evento de creación de estancia para codigo de estancia: {} y unidad: {}", estancia.getCodigoFolio(), codigo);
        EventoNuevoJsonBuilder nuevaReservaJson = new EventoNuevoJsonBuilder()
                .agregarProp("codigoReserva", reservaGuardada.getCodigo())
                .agregarProp("unidad", codigo)
                .agregarProp("fechaEntrada", reservaGuardada.getEntradaEstimada())
                .agregarProp("fechaSalida", reservaGuardada.getSalidaEstimada());

        eventoService.crearEvento(
                TipoEvento.CREACION_RESERVA,
                TipoEntidad.RESERVA,
                reservaGuardada.getId(),
                nuevaReservaJson.build(),
                null,
                reservaGuardada.getCodigo()
        );

        return reservaGuardada;
    }

    @Transactional
    public void editarReserva(ReservaNuevaRequestDTO request, Long idReserva) {

        EventoModificadoJsonBuilder reservaModificaJson = new EventoModificadoJsonBuilder();


        String codigo = request.getCodigo();
        TipoUnidad tipoUnidad = request.getTipoUnidad();
        LocalDateTime entradaEstimada = request.getEntradaEstimada();
        LocalDateTime salidaEstimada = request.getSalidaEstimada();

        logger.info("[editarReserva] Editando reserva con id: {}", idReserva);
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada con id: " + idReserva));

        if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new IllegalStateException("Solo se puede editar una reserva en estado CONFIRMADA");
        }

        logger.info("[editarReserva] Validando fechas de la reserva");
        String fechaConflicto = fechaTieneConflicto(salidaEstimada, entradaEstimada);
        if (!fechaConflicto.isEmpty()) {
            throw new IllegalArgumentException("No es posible editar la reserva: " + fechaConflicto);
        }

        String existeDisponibilidad = disponibilidadService.verificarDisponibilidad(null, reserva, codigo, tipoUnidad, entradaEstimada, salidaEstimada);
        if (!existeDisponibilidad.isEmpty()) {
            throw new IllegalArgumentException("No es posible editar la reserva: " + existeDisponibilidad);
        }

        logger.info("[editarReserva] Verificando disponibilidad excluyendo la misma reserva");
        String conflictoDisponibilidad = verificarDisponibilidadEdicion(reserva.getId(), codigo, tipoUnidad, entradaEstimada, salidaEstimada);
        if (!conflictoDisponibilidad.isEmpty()) {
            throw new IllegalStateException("No se puede editar la reserva: " + conflictoDisponibilidad);
        }

        if(!reserva.getEntradaEstimada().isEqual(entradaEstimada)){
            reservaModificaJson.agregarCambio("fechaEntrada", reserva.getEntradaEstimada(), entradaEstimada);
            reserva.setEntradaEstimada(entradaEstimada);

        }

        if(!reserva.getSalidaEstimada().isEqual(salidaEstimada)){
            reservaModificaJson.agregarCambio("fechaSalida", reserva.getSalidaEstimada(), salidaEstimada);
            reserva.setSalidaEstimada(salidaEstimada);
        }

        if(!reserva.getNumeroPersonas().equals(request.getNumeroPersonas())){
            reservaModificaJson.agregarCambio("numeroPersonas", reserva.getNumeroPersonas(), request.getNumeroPersonas());
            reserva.setNumeroPersonas(request.getNumeroPersonas());
        }

        if(!reserva.getCanalReserva().equals(request.getCanalReserva())){
            reservaModificaJson.agregarCambio("canalReserva", reserva.getCanalReserva(), request.getCanalReserva());
            reserva.setCanalReserva(request.getCanalReserva());
        }

        if(!reserva.getCliente().getId().equals(request.getIdOcupante())){
            reservaModificaJson.agregarCambio("cliente",
                    ocupanteService.obtenerNombre(reserva.getCliente().getId()),
                    ocupanteService.obtenerNombre(request.getIdOcupante()));
        }
        reserva.setCliente(determinarCliente(request.getIdOcupante()));

        reserva.setNotas(reserva.getNotas() + " | Reserva editadas: " + request.getNotas());

        Estancia estancia = reserva.getEstancia();
        if (estancia != null) {
            estancia.setSalidaEstimada(salidaEstimada);
            estancia.setNotas(estancia.getNotas() + " | Reserva editada: " + request.getNotas());
        }

        Reserva reservaGuardada = reservaRepository.save(reserva);

        if (request.getPago() != null && estancia != null) {
            logger.info("[editarReserva] Modificando o creando pago asociado a la reserva");
            modificarPagoReserva(estancia, request.getPago());
        }

        if (reservaModificaJson.tieneCambios()) {
            eventoService.crearEvento(
                    TipoEvento.MODIFICACION_RESERVA,
                    TipoEntidad.RESERVA,
                    reservaGuardada.getId(),
                    reservaModificaJson.build(),
                    null,
                    reservaGuardada.getCodigo()
            );
        }
    }

    @Transactional
    public Void eliminarReserva(Long idReserva) {
        logger.info("[eliminarReserva] Eliminando reserva con id: {}", idReserva);
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada con id: " + idReserva));

        reserva.setEstado(EstadoReserva.CANCELADA);

        Estancia estancia = reserva.getEstancia();
        if (estancia != null) {
            estancia.setEstado(EstadoEstancia.CANCELADA);
            pagoService.eliminarPagos(estancia.getId());
        }

        reservaRepository.save(reserva);



        logger.info("[eliminarReserva] Registrando evento de eliminación de reserva para codigo: {}", reserva.getCodigo());

        EventoNuevoJsonBuilder reservaEliminadaJson = new EventoNuevoJsonBuilder()
                .agregarProp("codigoReserva", reserva.getCodigo());

        eventoService.crearEvento(
                TipoEvento.ELIMINACION_RESERVA,
                TipoEntidad.RESERVA,
                reserva.getId(),
                reservaEliminadaJson.build(),
                null,
                reserva.getCodigo()
        );
        return null;
    }

    public List<ReservaCalendarioDTO> buscarReservasPorNumeroDocumento(String numeroDocumento) {
        if (numeroDocumento == null || numeroDocumento.isBlank()) {
            throw new IllegalArgumentException("numeroDocumento es obligatorio");
        }

        List<Reserva> reservas = reservaRepository.findByCliente_NumeroDocumentoContainingIgnoreCaseAndEstadoIn(
                numeroDocumento,
                List.of(EstadoReserva.CONFIRMADA));


        return llenarTipoYCodigoUnidad(ReservaMapper.entityListaToCalendarioDTOList(reservas));
    }

    public Reserva cambiarEstadoReserva(Long reservaId, EstadoReserva nuevoEstado) {
        Reserva reserva = buscarPorId(reservaId);
        reserva.setEstado(nuevoEstado);
        return reservaRepository.save(reserva);
    }

    public Page<ReservaTablaDTO> buscarReservasTabla(
            List<EstadoReserva> estados,
            List<CanalReserva> canales,
            ModoOcupacion modoOcupacion,
            TipoUnidad tipoUnidad,
            String codigoReserva,
            String codigoUnidad,
            String nombreCliente,
            String numeroDocumentoCliente,
            Long idCliente,
            LocalDateTime fechaCreacionDesde,
            LocalDateTime fechaCreacionHasta,
            LocalDateTime entradaDesde,
            LocalDateTime entradaHasta,
            LocalDateTime salidaDesde,
            LocalDateTime salidaHasta,
            LocalDateTime rangoGeneralDesde,
            LocalDateTime rangoGeneralHasta,
            Boolean tieneEstanciaAsociada,
            Pageable pageable) {
        Pageable pageableConOrden = pageable;
        if (pageable.getSort().isUnsorted()) {
            pageableConOrden = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "fechaCreacion"));
        }

        Page<Reserva> reservas = reservaRepository.findAll(
                ReservaSpecification.byTablaFilters(
                        estados,
                        canales,
                        modoOcupacion,
                        tipoUnidad,
                        codigoReserva,
                        codigoUnidad,
                        nombreCliente,
                        numeroDocumentoCliente,
                        idCliente,
                        fechaCreacionDesde,
                        fechaCreacionHasta,
                        entradaDesde,
                        entradaHasta,
                        salidaDesde,
                        salidaHasta,
                        rangoGeneralDesde,
                        rangoGeneralHasta,
                        tieneEstanciaAsociada),
                pageableConOrden
        );

        return reservas.map(this::mapearReservaTablaDTO);
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

    private void modificarPagoReserva(Estancia estancia, PagoNuevoRequestDTO request) {
        logger.info("[modificarPagoReserva] Creando nuevo o editando pago de la reserva");
        Pago pagoAnterior = pagoService.buscarUltimoPagoPorEstanciaYTipo(estancia.getId(), TipoPago.ANTICIPO_RESERVA).orElse(null);

        if (pagoAnterior == null) {
            pagoService.crearPago(request, estancia);
        } else {
            pagoService.reemplazarPago(request, pagoAnterior, estancia);
        }
    }

    private void validarCambioDeCodigo(String codigo, TipoUnidad tipoUnidad, Reserva reserva) {
        if (tipoUnidad.equals(TipoUnidad.HABITACION)) {
            if (!reserva.getHabitaciones().getFirst().getCodigo().equals(codigo)) {
                throw new IllegalStateException("No se puede cambiar el codigo de la unidad asignada a la reserva");
            }
        }
        if (tipoUnidad.equals(TipoUnidad.APARTAMENTO) || tipoUnidad.equals(TipoUnidad.APARTAESTUDIO)) {
            if (!reserva.getHabitaciones().getFirst().getUnidad().getCodigo().equals(codigo)) {
                throw new IllegalStateException("No se puede cambiar el codigo de la unidad asignada a la reserva");
            }
        }
    }

    private Ocupante determinarCliente(long idCliente){
        logger.info("[determinarCliente] Determinando cliente para la reserva");
        List<Ocupante> ocupantes = ocupanteService.determinarOcupantes(idCliente, null);

        return  ocupantes.stream()
                .filter(ocupante -> ocupante.getTipoOcupante() == TipoOcupante.CLIENTE)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No se encontró un cliente válido con id: " + idCliente));

    }

    private String verificarDisponibilidadEdicion(
            Long idReserva,
            String codigo,
            TipoUnidad tipoUnidad,
            LocalDateTime fechaInicioReserva,
            LocalDateTime fechaFinReserva) {
        List<Habitacion> habitaciones = unidadHabitacionResolver.buscarListaHabitaciones(codigo, tipoUnidad);
        List<Habitacion> habitacionesConReserva = new ArrayList<>();

        for (Habitacion habitacion : habitaciones) {
            boolean existeReserva = reservaRepository.existsReservaByHabitacionAndRangoAndIdNot(
                    habitacion.getId(),
                    idReserva,
                    fechaInicioReserva,
                    fechaFinReserva,
                    List.of(EstadoReserva.CONFIRMADA)
            );
            if (existeReserva) {
                habitacionesConReserva.add(habitacion);
            }
        }

        if (habitacionesConReserva.isEmpty()) {
            return "";
        }

        StringBuilder mensaje = new StringBuilder();
        for (Habitacion habitacion : habitacionesConReserva) {
            mensaje.append(habitacion.getCodigo()).append(", ");
        }
        mensaje.setLength(mensaje.length() - 2);
        return "existe una reserva para las habitaciones con codigo: " + mensaje;
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

    private ReservaTablaDTO mapearReservaTablaDTO(Reserva reserva) {
        ReservaTablaDTO dto = new ReservaTablaDTO();
        Estancia estancia = reserva.getEstancia();
        boolean estanciaCanceladaSinActivar = estancia != null
                && estancia.getEstado() == EstadoEstancia.CANCELADA
                && estancia.getEntradaReal() == null;
        boolean estanciaReservada = estancia != null
                && estancia.getEstado() == EstadoEstancia.RESERVADA;
        boolean estanciaAsociada = estancia != null
                && !estanciaCanceladaSinActivar
                && !estanciaReservada;
        dto.setId(reserva.getId());
        dto.setCodigoReserva(reserva.getCodigo());
        dto.setCodigoEstancia(estanciaAsociada ? estancia.getCodigoFolio() : null);
        dto.setFechaCreacion(reserva.getFechaCreacion());
        dto.setEntradaEstimada(reserva.getEntradaEstimada());
        dto.setSalidaEstimada(reserva.getSalidaEstimada());
        dto.setNumeroPersonas(reserva.getNumeroPersonas());
        dto.setCanalReserva(reserva.getCanalReserva());
        dto.setModoOcupacion(reserva.getModoOcupacion());
        dto.setEstadoReserva(reserva.getEstado());
        dto.setTieneEstanciaAsociada(estanciaAsociada);
        dto.setTotalPagoReserva(BigDecimal.ZERO);
        dto.setCantidadPagosModificadosOEliminados(0);

        if (reserva.getCliente() != null) {
            dto.setIdCliente(reserva.getCliente().getId());
            dto.setTipoDocumentoCliente(reserva.getCliente().getTipoDocumento());
            dto.setNumeroDocumentoCliente(reserva.getCliente().getNumeroDocumento());
            dto.setNombreCliente(
                    String.format("%s %s",
                            reserva.getCliente().getNombres(),
                            reserva.getCliente().getApellidos()).trim());
        }

        if (reserva.getHabitaciones() != null && !reserva.getHabitaciones().isEmpty()) {
            if (reserva.getModoOcupacion() == ModoOcupacion.INDIVIDUAL) {
                dto.setCodigoUnidad(reserva.getHabitaciones().getFirst().getCodigo());
                dto.setTipoUnidad(TipoUnidad.HABITACION);
            } else if (reserva.getHabitaciones().getFirst().getUnidad() != null) {
                dto.setCodigoUnidad(reserva.getHabitaciones().getFirst().getUnidad().getCodigo());
                dto.setTipoUnidad(reserva.getHabitaciones().getFirst().getUnidad().getTipo());
            }
        }

        if (reserva.getEstancia() != null && reserva.getEstancia().getPagos() != null) {
            dto.setTotalPagoReserva(reserva.getEstancia().getPagos().stream()
                    .filter(pago -> pago.getTipoPago() == TipoPago.ANTICIPO_RESERVA)
                    .filter(pago -> pago.getEstado() == EstadoPago.COMPLETADO)
                    .map(Pago::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            int cantidadPagosModificadosOEliminados = (int) reserva.getEstancia().getPagos().stream()
                    .filter(pago -> pago.getEstado() == EstadoPago.MODIFICADO || pago.getEstado() == EstadoPago.ELIMINADO)
                    .count();
            dto.setCantidadPagosModificadosOEliminados(cantidadPagosModificadosOEliminados);
        }

        return dto;
    }


}
