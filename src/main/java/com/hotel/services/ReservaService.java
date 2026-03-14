package com.hotel.services;

import com.hotel.dtos.reserva.ReservaDTO;
import com.hotel.dtos.reserva.ReservaRequestDTO;
import com.hotel.dtos.reserva.ReservaTablaDTO;
import com.hotel.mappers.ReservaMapper;
import com.hotel.models.*;
import com.hotel.models.enums.*;
import com.hotel.repositories.ReservaRepository;
import com.hotel.resolvers.AlojamientoResolver;
import com.hotel.resolvers.EstanciaReservaResolver;
import com.hotel.resolvers.UnidadHabitacionResolver;
import com.hotel.specifications.ReservaSpecification;
import com.hotel.utils.EventoModificadoJsonBuilder;
import com.hotel.utils.EventoNuevoJsonBuilder;
import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
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
        private final AlojamientoResolver alojamientoResolver;

    public ReservaService(ReservaRepository reservaRepository,
                          OcupanteService ocupanteService,
                          PagoService pagoService,
                          UnidadHabitacionResolver unidadHabitacionResolver,
                          DisponibilidadService disponibilidadService,
                          EstanciaReservaResolver estanciaReservaResolver,
                          CodigoUnicoService codigoUnicoService,
                          AuditoriaEventoService eventoService,
                          AlojamientoResolver alojamientoResolver) {
        this.alojamientoResolver = alojamientoResolver;
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

    public ReservaDTO obtenerReservaPorId(Long id) {
        Reserva reserva = buscarPorId(id);
        return ReservaMapper.entityToDTO(reserva);
    }

    @Transactional
    public ReservaDTO crearReserva(ReservaRequestDTO request) {

        String codigo = request.getCodigo();
        TipoUnidad tipoUnidad = request.getTipoUnidad();
        List<Habitacion> habitaciones = unidadHabitacionResolver.buscarListaHabitaciones(codigo, tipoUnidad);
        LocalDate entradaEstimada = request.getEntradaEstimada();
        LocalDate salidaEstimada = request.getSalidaEstimada();


        logger.info("[crearReserva] validar creacion de reserva para codigo de unidad: {} y tipo de unidad: {}", codigo, tipoUnidad);
        validarCreacionReserva(habitaciones, entradaEstimada, salidaEstimada);

        logger.info("[crearReserva] Creando reserva para el ocupante con ID: {}", request.getIdOcupante());
        Reserva reserva = ReservaMapper.requestNuevoToEntity(request);
        reserva.setCodigo(codigoUnicoService.generarCodigoReserva());

        logger.info("[crearReserva] Buscando ocupante para asignar a la reserva");
        reserva.setCliente(ocupanteService.buscarCliente(request.getIdOcupante()));

        logger.info("[crearReserva] Asignando habitaciones a la reserva");
        reserva.setHabitaciones(habitaciones);

        logger.info("[crearReserva] Determinando modo de ocupación para la reserva");
        reserva.setModoOcupacion(alojamientoResolver.determinarModoOcupacion(tipoUnidad));

        logger.info("[crearReserva] Creando estancia asociada a la reserva");
        Estancia estancia = estanciaReservaResolver.crearEstanciaDesdeReserva(reserva);
        reserva.setEstancia(estancia);

        Reserva reservaGuardada = reservaRepository.save(reserva);

        logger.info("[crearReserva] Actualizando estado de las habitaciones asociadas a la estancia");
        actualizarAlojamiento(entradaEstimada, habitaciones);

        logger.info("[crearReserva] Registrando evento de creación de estancia para codigo de estancia: {} y unidad: {}", estancia.getCodigoFolio(), codigo);
        EventoNuevoJsonBuilder nuevaReservaJson = new EventoNuevoJsonBuilder()
                .agregarProp("codigoReserva", reservaGuardada.getCodigo())
                .agregarProp("unidad", codigo)
                .agregarProp("fechaEntrada", reservaGuardada.getEntradaEstimada().toLocalDate())
                .agregarProp("fechaSalida", reservaGuardada.getSalidaEstimada().toLocalDate());

        eventoService.crearEvento(
                TipoEvento.CREACION_RESERVA,
                TipoEntidad.RESERVA,
                reservaGuardada.getId(),
                nuevaReservaJson.build(),
                null,
                reservaGuardada.getCodigo()
        );

        return ReservaMapper.entityToDTO(reservaGuardada);
    }


    @Transactional
    public void editarReserva(@Nonnull ReservaRequestDTO request, Long idReserva) {

        EventoModificadoJsonBuilder reservaModificaJson = new EventoModificadoJsonBuilder();


        String codigo = request.getCodigo();
        TipoUnidad tipoUnidad = request.getTipoUnidad();
        List<Habitacion> habitaciones = unidadHabitacionResolver.buscarListaHabitaciones(codigo, tipoUnidad);
        LocalDate entradaEstimada = request.getEntradaEstimada();
        LocalDate salidaEstimada = request.getSalidaEstimada();

        logger.info("[editarReserva] Editando reserva con id: {}", idReserva);
        Reserva reserva = reservaRepository.findById(idReserva)
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada con id: " + idReserva));

        if (reserva.getEstado() != EstadoReserva.CONFIRMADA
                && reserva.getEstado() != EstadoReserva.EXPIRADA) {
            throw new IllegalStateException("Solo se puede editar una reserva en estado CONFIRMADA o EXPIRADA");
        }

        logger.info("[editarReserva] Validando edición de reserva para codigo de unidad: {} y tipo de unidad: {}", codigo, tipoUnidad);
        boolean cambioCodigo = unidadHabitacionResolver.codigoHaCambiado(habitaciones, null, reserva);
        boolean cambioFechas = cambioFechas(entradaEstimada, salidaEstimada, reserva);

        if(cambioCodigo || cambioFechas) {
            logger.info("[editarReserva] Se detectó un cambio en las fechas o en el código de unidad, validando disponibilidad para la edición de la reserva");
            validarEdicionReserva(reserva, habitaciones, entradaEstimada, salidaEstimada);
        }

        logger.info("[editarReserva] Verificando si cambio el codigo de unidad o las fechas para registrar cambios en el evento de modificación de reserva");
        if(cambioCodigo) {
            reservaModificaJson.agregarCambio("unidad",
                    unidadHabitacionResolver.determinarCodigoUnidad(reserva.getHabitaciones()),
                    codigo);

            logger.info("[editarReserva] Actualizando estado de alojamiento por cambio de código de unidad");
            actualizarAlojamientoPorEdicion(entradaEstimada, reserva.getHabitaciones(), habitaciones);

            logger.info("[editarReserva] Actualizando habitaciones de la reserva y estancia asociada por cambio de código de unidad");
            reserva.setHabitaciones(new ArrayList<>(habitaciones));
            Estancia estancia = reserva.getEstancia();

            if (estancia != null) {
                estancia.setHabitaciones(new ArrayList<>(habitaciones));
                estancia.setModoOcupacion(alojamientoResolver.determinarModoOcupacion(tipoUnidad));
            }
            reserva.setEstancia(estancia);

            logger.info("[editarReserva] Actualizando modo de ocupación de la reserva por cambio de código de unidad");
            reserva.setModoOcupacion(alojamientoResolver.determinarModoOcupacion(tipoUnidad));

        }

        if(cambioFechas) {
            logger.info("[editarReserva] Actualizando fechas de entrada o salida de la reserva y estancia asociada por cambio de fechas");
            reservaModificaJson.agregarCambio("fechaEntrada", reserva.getEntradaEstimada().toLocalDate(), entradaEstimada);
            reserva.setEntradaEstimada(entradaEstimada.atStartOfDay());


            reservaModificaJson.agregarCambio("fechaSalida", reserva.getSalidaEstimada().toLocalDate(), salidaEstimada);
             reserva.setSalidaEstimada(salidaEstimada.atStartOfDay());
        }

        if (!reserva.getNumeroPersonas().equals(request.getNumeroPersonas())) {
            reservaModificaJson.agregarCambio("numeroPersonas", reserva.getNumeroPersonas(), request.getNumeroPersonas());
            reserva.setNumeroPersonas(request.getNumeroPersonas());
        }

        if (!reserva.getCanalReserva().equals(request.getCanalReserva())) {
            reservaModificaJson.agregarCambio("canalReserva", reserva.getCanalReserva(), request.getCanalReserva());
            reserva.setCanalReserva(request.getCanalReserva());
        }

        if (!reserva.getCliente().getId().equals(request.getIdOcupante())) {
            reservaModificaJson.agregarCambio("cliente",
                    ocupanteService.obtenerNombre(reserva.getCliente().getId()),
                    ocupanteService.obtenerNombre(request.getIdOcupante()));
        }
        reserva.setCliente(ocupanteService.buscarCliente(request.getIdOcupante()));

        reserva.setNotas(reserva.getNotas() + " | Reserva editadas: " + request.getNotas());


        Reserva reservaGuardada = reservaRepository.save(reserva);


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
                .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada con id: " + idReserva));

        if (reserva.getEstado() != EstadoReserva.CONFIRMADA
                && reserva.getEstado() != EstadoReserva.EXPIRADA) {
            throw new IllegalStateException("Solo se puede eliminar una reserva en estado CONFIRMADA o EXPIRADA");
        }

        reserva.setEstado(EstadoReserva.CANCELADA);

        Estancia estancia = reserva.getEstancia();
        if (estancia != null) {
            actualizarAlojamientoPorEliminacion(reserva.getHabitaciones());
            estancia.setEstado(EstadoEstancia.CANCELADA);
            pagoService.eliminarTodoLosPagos(estancia.getId());
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

    public List<ReservaDTO> buscarReservasPorNumeroDocumento(String numeroDocumento) {
        if (numeroDocumento == null || numeroDocumento.isBlank()) {
            throw new IllegalArgumentException("numeroDocumento es obligatorio");
        }

        List<Reserva> reservas = reservaRepository.findByCliente_NumeroDocumentoContainingIgnoreCaseAndEstadoIn(
                numeroDocumento,
                List.of(EstadoReserva.CONFIRMADA, EstadoReserva.EXPIRADA));


        return reservas.stream()
                .map(ReservaMapper::entityToDTO)
                .toList();
    }

    public Page<ReservaTablaDTO> buscarReservasTabla(
            Long idReserva,
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
                        idReserva,
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

        return reservas.map(ReservaMapper::entityToTablaDTO);
    }


    private void validarCreacionReserva(List<Habitacion> habitaciones, LocalDate entradaReal, LocalDate salidaEstimada) {
        logger.info("[validarCreacionReserva] Validando fechas de entrada y salida");
        validarFechasReserva(entradaReal, salidaEstimada);

        logger.info("[validarCreacionReserva] Verificando disponibilidad");
        String detalleNoDisponible = disponibilidadService.verificarDisponibilidadNuevo(habitaciones, entradaReal.atStartOfDay(), salidaEstimada.atStartOfDay());

        if (!detalleNoDisponible.isEmpty()) {
            throw new IllegalStateException("No se puede crear la reserva: " + detalleNoDisponible);
        }
    }

    private void validarEdicionReserva(Reserva reserva, List<Habitacion> habitaciones, LocalDate entradaReal, LocalDate salidaEstimada) {
        logger.info("[validarEdicionReserva] Validando fechas de entrada y salida");
        validarFechasReserva(entradaReal, salidaEstimada);

        logger.info("[validarEdicionReserva] Verificando disponibilidad");
        String detalleNoDisponible = disponibilidadService.verificarDisponibilidadEditar(reserva, null, habitaciones, entradaReal.atStartOfDay(), salidaEstimada.atStartOfDay());

        if (!detalleNoDisponible.isEmpty()) {
            throw new IllegalStateException("No se puede editar la reserva: " + detalleNoDisponible);
        }
    }

    private boolean cambioFechas(LocalDate entradaEstimada, LocalDate salidaEstimada, Reserva reserva) {
        logger.info("[cambioFechas] Validando si se han cambiado las fechas de entrada o salida de la reserva");
        LocalDate entradaEstimadaAntigua = reserva.getEntradaEstimada().toLocalDate();
        LocalDate salidaEstimadaAntigua = reserva.getSalidaEstimada().toLocalDate();

        return !entradaEstimadaAntigua.isEqual(entradaEstimada)
                || !salidaEstimadaAntigua.isEqual(salidaEstimada);
    }

    private void actualizarAlojamiento(LocalDate entrada, List<Habitacion> habitaciones) {
        if(LocalDate.now().isEqual(entrada)) {
            alojamientoResolver.actualizarEstadoAlojamiento(habitaciones, EstadoOperativo.RESERVADO);
        }
    }

    private void actualizarAlojamientoPorEdicion(LocalDate entrada, List<Habitacion> habitacionesAntiguas, List<Habitacion> habitacionesNuevas) {
        List<Habitacion> habitacionesReservadas = unidadHabitacionResolver.obtenerHabitacionesPorEstado(EstadoOperativo.RESERVADO, habitacionesAntiguas);
        if(!habitacionesReservadas.isEmpty()) {
            alojamientoResolver.actualizarEstadoAlojamiento(habitacionesReservadas, EstadoOperativo.DISPONIBLE);
            actualizarAlojamiento(entrada, habitacionesNuevas);
        }
    }

    private void actualizarAlojamientoPorEliminacion(List<Habitacion> habitaciones) {
        List<Habitacion> habitacionesReservadas = unidadHabitacionResolver.obtenerHabitacionesPorEstado(EstadoOperativo.RESERVADO, habitaciones);
        if (!habitacionesReservadas.isEmpty()) {
            alojamientoResolver.actualizarEstadoAlojamiento(habitacionesReservadas, EstadoOperativo.DISPONIBLE);
        }
    }


    private void validarFechasReserva(LocalDate entradaReal, LocalDate salidaEstimada) {
        if (entradaReal.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de entrada debe ser posterior a la fecha actual");
        }
        if (salidaEstimada.isBefore(entradaReal)) {
            throw new IllegalArgumentException("La fecha de salida estimada no puede ser anterior a la fecha de entrada");
        }
    }

}
