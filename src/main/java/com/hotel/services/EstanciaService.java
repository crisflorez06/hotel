package com.hotel.services;

import com.hotel.dtos.estancia.ActivarEstanciaDTO;
import com.hotel.dtos.estancia.EstanciaDTO;
import com.hotel.dtos.estancia.EstanciaRequestDTO;
import com.hotel.dtos.estancia.SalidaEstanciaDTO;
import com.hotel.dtos.estancia.EstanciaTablaDTO;
import com.hotel.dtos.pago.PagoNuevoRequestDTO;
import com.hotel.mappers.EstanciaMapper;
import com.hotel.models.Estancia;
import com.hotel.models.Habitacion;
import com.hotel.models.Ocupante;
import com.hotel.models.Pago;
import com.hotel.models.Reserva;
import com.hotel.models.enums.*;
import com.hotel.repositories.EstanciaRepository;
import com.hotel.resolvers.AlojamientoResolver;
import com.hotel.resolvers.EstanciaReservaResolver;
import com.hotel.resolvers.UnidadHabitacionResolver;
import com.hotel.specifications.EstanciaSpecification;
import com.hotel.utils.EventoModificadoJsonBuilder;
import com.hotel.utils.EventoNuevoJsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EstanciaService {

    private static final Logger logger = LoggerFactory.getLogger(EstanciaService.class);

    private final EstanciaRepository estanciaRepository;
    private final OcupanteService ocupanteService;
    private final PagoService pagoService;
    private final AlojamientoResolver alojamientoResolver;
    private final UnidadHabitacionResolver unidadHabitacionResolver;
    private final DisponibilidadService disponibilidadService;
    private final EstanciaReservaResolver ERService;
    private final CodigoUnicoService codigoUnicoService;
    private final AuditoriaEventoService eventoService;


    public EstanciaService(EstanciaRepository estanciaRepository,
                           OcupanteService ocupanteService,
                           PagoService pagoService,
                           AlojamientoResolver alojamientoResolver,
                           UnidadHabitacionResolver unidadHabitacionResolver,
                           DisponibilidadService disponibilidadService,
                           EstanciaReservaResolver ERService,
                           CodigoUnicoService codigoUnicoService,
                           AuditoriaEventoService eventoService) {
        this.eventoService = eventoService;
        this.ERService = ERService;
        this.disponibilidadService = disponibilidadService;
        this.unidadHabitacionResolver = unidadHabitacionResolver;
        this.alojamientoResolver = alojamientoResolver;
        this.estanciaRepository = estanciaRepository;
        this.ocupanteService = ocupanteService;
        this.pagoService = pagoService;
        this.codigoUnicoService = codigoUnicoService;
    }

    @Transactional
    public Estancia crearEstanciaNueva(EstanciaRequestDTO request) {

        TipoUnidad tipoUnidad = request.getTipoUnidad();
        String codigo = request.getCodigo();


        logger.info("[crearEstanciaNueva] Validando creacion de nueva estancia");
        validarCreacionEstancia(request.getEntradaReal(), request.getSalidaEstimada(), codigo, tipoUnidad);

        logger.info("[crearEstanciaNueva] Mapeando datos del request a la entidad Estancia");
        Estancia estancia = EstanciaMapper.requestToEntity(request);
        estancia.setCodigoFolio(codigoUnicoService.generarCodigoEstancia());

        logger.info("[crearEstanciaNueva] Determinando ocupantes de la estancia");
        estancia.setOcupantes(ocupanteService.determinarOcupantes(request.getIdCliente(), request.getIdAcompanantes()));

        logger.info("[crearEstanciaNueva] Llenando habitaciones asociadas a la estancia");
        estancia.setHabitaciones(unidadHabitacionResolver.buscarListaHabitaciones(codigo, tipoUnidad));

        logger.info("[crearEstanciaNueva] Estableciendo modo de ocupacion para la estancia");
        estancia.setModoOcupacion(determinarModoOcupacion(request.getTipoUnidad()));

        logger.info("[crearEstanciaNueva] Determinando estado inicial de la estancia");
        estancia.setEstado(determinarEstadoEstancia(estancia.getEntradaReal(), estancia.getSalidaEstimada()));

        logger.info("[crearEstanciaNueva] Guardando estancia en la base de datos");
        Estancia estanciaGuardada = estanciaRepository.save(estancia);

        logger.info("[crearEstanciaNueva] Actualizando estado de las habitaciones asociadas a la estancia");
        alojamientoResolver.actualizarEstadoAlojamiento(estanciaGuardada.getHabitaciones(), EstadoOperativo.OCUPADO);

        logger.info("[crearEstanciaNueva] Creando pagos asociados a la estancia");
        pagoService.crearPago(request.getPago(), estanciaGuardada);

        logger.info("[crearEstanciaNueva] Registrando evento de creación de nueva estancia para codigo: {}", estanciaGuardada.getCodigoFolio());
        EventoNuevoJsonBuilder nuevaEstanciaJson = new EventoNuevoJsonBuilder()
                .agregarProp("codigoEstancia", estanciaGuardada.getCodigoFolio())
                .agregarProp("unidad", codigo)
                .agregarProp("fechaEntrada", estanciaGuardada.getEntradaReal())
                .agregarProp("fechaSalida", estanciaGuardada.getSalidaEstimada());

        eventoService.crearEvento(
                TipoEvento.CREACION_ESTANCIA,
                TipoEntidad.ESTANCIA,
                estanciaGuardada.getId(),
                nuevaEstanciaJson.build(),
                estanciaGuardada.getCodigoFolio(),
                null
        );

        return estanciaGuardada;
    }



    @Transactional
    public Estancia activarEstancia(ActivarEstanciaDTO request) {

        Reserva reserva = ERService.buscarReservaPorIdDesdeEstancia(request.getIdReserva());
        Estancia estancia = reserva.getEstancia();

        logger.info("[activarEstancia] Validando reglas de negocio para activar estancia desde reserva con id: {}", request.getIdReserva());
        validarActivacionEstancia(reserva, estancia);

        logger.info("[activarEstancia] validando fechas de entrada y salida de la estancia");
        validarFechasEstancia(request.getEntradaReal(), request.getSalidaEstimada());

        // 3) Aplicar cambios a la estancia
        logger.info("[activarEstancia] Actualizando estancia a ACTIVA");
        estancia.setEntradaReal(request.getEntradaReal());
        estancia.setSalidaEstimada(request.getSalidaEstimada());

        logger.info("[activarEstancia] Determinando ocupantes de la estancia");
        estancia.setOcupantes(ocupanteService.determinarOcupantes(request.getIdCliente(), request.getIdAcompanantes()));

        logger.info("[activarEstancia] Determinando estado inicial de la estancia");
        estancia.setEstado(determinarEstadoEstancia(estancia.getEntradaReal(), estancia.getSalidaEstimada()));

        logger.info("[activarEstancia] Guardando estancia en la base de datos");
        Estancia estanciaGuardada = estanciaRepository.save(estancia);

        logger.info("[activarEstancia] Actualizando estado de la reserva asociada a la estancia a COMPLETADA");
        ERService.actualizarEstadoReservaDesdeEstancia(reserva, EstadoReserva.COMPLETADA);

        logger.info("[activarEstancia] Actualizando estado de las habitaciones asociadas a la estancia");
        alojamientoResolver.actualizarEstadoAlojamiento(estanciaGuardada.getHabitaciones(), EstadoOperativo.OCUPADO);

        logger.info("[activarEstancia] Creando pagos asociados a la estancia");
        pagoService.crearPago(request.getPago(), estanciaGuardada);

        logger.info("[activarEstancia] Registrando evento de creación de nueva estancia para codigo: {}", estanciaGuardada.getCodigoFolio());
        EventoNuevoJsonBuilder nuevaEstancia = new EventoNuevoJsonBuilder()
                .agregarProp("codigoEstancia", estanciaGuardada.getCodigoFolio())
                .agregarProp("unidad", unidadHabitacionResolver.determinarCodigoUnidad(estancia.getHabitaciones()))
                .agregarProp("fechaEntrada", estanciaGuardada.getEntradaReal())
                .agregarProp("fechaSalida", estanciaGuardada.getSalidaEstimada());

        eventoService.crearEvento(
                TipoEvento.ACTIVACION_ESTANCIA,
                TipoEntidad.ESTANCIA,
                estanciaGuardada.getId(),
                nuevaEstancia.build(),
                estanciaGuardada.getCodigoFolio(),
                reserva.getCodigo()
        );

        return estanciaGuardada;

    }


    @Transactional
    public void editarEstancia(EstanciaRequestDTO request, Long idEstancia) {

        EventoModificadoJsonBuilder estanciaModificadaJson = new EventoModificadoJsonBuilder();

        String codigo = request.getCodigo();
        TipoUnidad tipoUnidad = request.getTipoUnidad();
        LocalDateTime entradaReal = request.getEntradaReal();
        LocalDateTime salidaEstimada = request.getSalidaEstimada();

        logger.info("[editarEstancia] Editando estancia con id: {}", idEstancia);
        Estancia estancia = estanciaRepository.findById(idEstancia)
                .orElseThrow(() -> new IllegalArgumentException("Estancia no encontrada con id: " + idEstancia));

        if (!(estancia.getEstado() == EstadoEstancia.ACTIVA || estancia.getEstado() == EstadoEstancia.EXCEDIDA)) {
            throw new IllegalStateException("Solo se pueden editar estancias en estado ACTIVA o EXCEDIDA. Estado actual: " + estancia.getEstado());
        }

        logger.info("[editarEstancia] validando fechas de entrada y salida de la estancia");
        validarFechasEstancia(request.getEntradaReal(), request.getSalidaEstimada());

        logger.info("[editarEstancia] Verificando que no cambio el codigo, ya que no se permite cambiar la unidad asignada a una estancia");
        validarCambioDeCodigo(request.getCodigo(), request.getTipoUnidad(), estancia);

        logger.info("[editarEstancia] verificando entrada y salida de la estancia");
        String existeDisponibilidad = disponibilidadService.verificarDisponibilidad(estancia, null, codigo, tipoUnidad, entradaReal, salidaEstimada);
        if(!existeDisponibilidad.isEmpty()) {
            throw new IllegalStateException("No se puede editar la estancia: " + existeDisponibilidad);
        }


        estancia.setEstado(determinarEstadoEstancia(entradaReal, salidaEstimada));

        if(!estancia.getEntradaReal().isEqual(entradaReal)) {
            estanciaModificadaJson.agregarCambio("entradaReal", estancia.getEntradaReal(), entradaReal);
            estancia.setEntradaReal(entradaReal);
        }

        if(!estancia.getSalidaEstimada().isEqual(salidaEstimada)) {
            estanciaModificadaJson.agregarCambio("salidaEstimada", estancia.getSalidaEstimada(), salidaEstimada);
            estancia.setSalidaEstimada(salidaEstimada);
        }

        logger.info("[editarEstancia] Determinando ocupantes de la estancia");
        Ocupante cliente = ocupanteService.determinarCliente(estancia.getOcupantes()).
                orElseThrow(() -> new IllegalStateException("La estancia debe tener un cliente ocupante"));

        if(!cliente.getId().equals(request.getIdCliente())) {
            estanciaModificadaJson.agregarCambio("cliente",
                    ocupanteService.obtenerNombre(cliente.getId()),
                    ocupanteService.obtenerNombre(request.getIdCliente()));
        }

        Integer ocupantesAntes = estancia.getOcupantes() != null ? estancia.getOcupantes().size() - 1 : 0;
        Integer ocupantesDespues = request.getIdAcompanantes() != null ? request.getIdAcompanantes().size() : 0;

        if(!ocupantesAntes.equals(ocupantesDespues)) {
            estanciaModificadaJson.agregarCambio("cantidadAcompanantes",
                    estancia.getOcupantes() != null ? estancia.getOcupantes().size() - 1 : 0,
                    request.getIdAcompanantes() != null ? request.getIdAcompanantes().size() : 0);
        }

        estancia.setOcupantes(ocupanteService.determinarOcupantes(request.getIdCliente(), request.getIdAcompanantes()));

        estancia.setNotas(estancia.getNotas() + " | Notas editadas: " + request.getNotas());

        Estancia estanciaGuardada = estanciaRepository.save(estancia);

        if(request.getPago() != null) {
            logger.info("[editarEstancia] Modificando o creando pago asociado a la estancia editada");
            modificarPagoEstancia(estancia, request.getPago());
        }

        if (estanciaModificadaJson.tieneCambios()) {
            eventoService.crearEvento(
                    TipoEvento.MODIFICACION_ESTANCIA,
                    TipoEntidad.ESTANCIA,
                    estanciaGuardada.getId(),
                    estanciaModificadaJson.build(),
                    estanciaGuardada.getCodigoFolio(),
                    null
            );
        }

    }


    @Transactional
    public Void eliminarEstancia(Long idEstancia) {
        logger.info("[eliminarEstancia] Eliminando estancia con id: {}", idEstancia);
        Estancia estancia = estanciaRepository.findById(idEstancia)
                .orElseThrow(() -> new IllegalArgumentException("Estancia no encontrada con id: " + idEstancia));

        alojamientoResolver.actualizarEstadoAlojamiento(estancia.getHabitaciones(), EstadoOperativo.DISPONIBLE);

        logger.info("[eliminarEstancia] Actualizando estado de la estancia a CANCELADA");
        estancia.setEstado(EstadoEstancia.CANCELADA);

        logger.info("[eliminarEstancia] Eliminando pagos asociados a la estancia");
        pagoService.eliminarPagos(estancia.getId());

        logger.info("[eliminarEstancia] Guardando cambios en la estancia eliminada");
        estanciaRepository.save(estancia);


        logger.info("[eliminarEstancia] Registrando evento de eliminación de estancia para codigo: {}", estancia.getCodigoFolio());

        EventoNuevoJsonBuilder estanciaEliminadaJson = new EventoNuevoJsonBuilder()
                .agregarProp("codigoEstancia", estancia.getCodigoFolio());

        eventoService.crearEvento(
                TipoEvento.ELIMINACION_ESTANCIA,
                TipoEntidad.ESTANCIA,
                estancia.getId(),
                estanciaEliminadaJson.build(),
                estancia.getCodigoFolio(),
                null
        );
        return null;
    }

    public EstanciaDTO obtenerEstancia(String codigo, TipoUnidad tipoUnidad) {
        logger.info("[obtenerEstanciaPorUnidad] Obteniendo estancia para la unidad con codigo: {}", codigo);
        List<Habitacion> habitaciones = unidadHabitacionResolver.buscarListaHabitaciones(codigo, tipoUnidad);
        Long habitacionId = habitaciones.getFirst().getId();

        Estancia estancia = estanciaRepository.findActivaOExcedidaPorHabitacionId(habitacionId).orElseThrow(
                () -> new IllegalArgumentException("No se encontró una estancia activa o excedida para la unidad con codigo: " + codigo)
        );


        logger.info("[obtenerEstanciaPorUnidad] Mapeando estancia a DTO");
        return EstanciaMapper.entityToDTO(estancia);

    }

    public Page<EstanciaTablaDTO> buscarEstanciasTabla(
            List<EstadoEstancia> estados,
            TipoUnidad tipoUnidad,
            ModoOcupacion modoOcupacion,
            String codigoEstancia,
            String codigoUnidad,
            String nombreCliente,
            String numeroDocumentoCliente,
            Long idCliente,
            LocalDateTime entradaDesde,
            LocalDateTime entradaHasta,
            LocalDateTime salidaEstimadaDesde,
            LocalDateTime salidaEstimadaHasta,
            LocalDateTime salidaRealDesde,
            LocalDateTime salidaRealHasta,
            LocalDateTime rangoGeneralDesde,
            LocalDateTime rangoGeneralHasta,
            Boolean tieneReservaAsociada,
            Pageable pageable) {
        Pageable pageableConOrden = pageable;
        if (pageable.getSort().isUnsorted()) {
            pageableConOrden = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "entradaReal"));
        }

        Page<Estancia> estancias = estanciaRepository.findAll(
                EstanciaSpecification.byTablaFilters(
                        estados,
                        tipoUnidad,
                        modoOcupacion,
                        codigoEstancia,
                        codigoUnidad,
                        nombreCliente,
                        numeroDocumentoCliente,
                        idCliente,
                        entradaDesde,
                        entradaHasta,
                        salidaEstimadaDesde,
                        salidaEstimadaHasta,
                        salidaRealDesde,
                        salidaRealHasta,
                        rangoGeneralDesde,
                        rangoGeneralHasta,
                        tieneReservaAsociada),
                pageableConOrden
        );

        return estancias.map(this::mapearEstanciaTablaDTO);
    }

    //verificar reglas de negocio para finalizar estancia
    @Transactional
    public void finalizarEstancia(SalidaEstanciaDTO request) {

        Long idEstancia = request.getIdEstancia();

        logger.info("[finalizarEstancia] Finalizando estancia con id: {}", idEstancia);
        Estancia estancia = estanciaRepository.findById(idEstancia)
                .orElseThrow(() -> new IllegalArgumentException("Estancia no encontrada con id: " + idEstancia));

        estancia.setEstado(EstadoEstancia.FINALIZADA);
        estancia.setSalidaReal(request.getFechaSalidaReal());

        logger.info("[finalizarEstancia] Creando pago asociado a la estancia finalizada");
        pagoService.crearPago(request.getPagoEstancia(), estancia);

        estancia.setNotas(estancia.getNotas() + " | Notas de salida: " + request.getNotasSalida());

        estancia.setPrecioTotal(pagoService.sumarTotalPagosPorEstancia(estancia.getId()));

        logger.info("[finalizarEstancia] Guardando cambios en la estancia finalizada");
        Estancia estanciaFinalizada = estanciaRepository.save(estancia);


        logger.info("[finalizarEstancia] Registrando evento de creación de nueva estancia para codigo: {}", estanciaFinalizada.getCodigoFolio());
        EventoNuevoJsonBuilder nuevaEstancia = new EventoNuevoJsonBuilder()
                .agregarProp("codigoEstancia", estanciaFinalizada.getCodigoFolio())
                .agregarProp("unidad", unidadHabitacionResolver.determinarCodigoUnidad(estancia.getHabitaciones()))
                .agregarProp("fechaEntrada", estanciaFinalizada.getEntradaReal())
                .agregarProp("fechaSalida", estanciaFinalizada.getSalidaReal());

        eventoService.crearEvento(
                TipoEvento.FINALIZACION_ESTANCIA,
                TipoEntidad.ESTANCIA,
                estanciaFinalizada.getId(),
                nuevaEstancia.build(),
                estanciaFinalizada.getCodigoFolio(),
                null
        );

        alojamientoResolver.actualizarEstadoAlojamiento(estancia.getHabitaciones(), EstadoOperativo.DISPONIBLE);
    }

    private void modificarPagoEstancia(Estancia estancia, PagoNuevoRequestDTO request) {
        logger.info("[modificarPagoEstancia] Creando nuevo o editando el pago asociado a la estancia editada");
        Pago pagoAnterior = pagoService.buscarUltimoPagoPorEstanciaYTipo(estancia.getId(), TipoPago.ANTICIPO_ESTANCIA).orElse(null);

        if (pagoAnterior == null) {
            pagoService.crearPago(request, estancia);
        } else {
            pagoService.reemplazarPago(request, pagoAnterior, estancia);
        }
    }


    private void validarCreacionEstancia(LocalDateTime entradaReal, LocalDateTime salidaEstimada, String codigo, TipoUnidad tipoUnidad) {
        logger.info("[validarCreacionEstancia] Validando fechas de entrada y salida");
        validarFechasEstancia(entradaReal, salidaEstimada);

        logger.info("[validarCreacionEstancia] Verificando disponibilidad para codigo: {}", codigo);
        String detalleNoDisponible = disponibilidadService.verificarDisponibilidad(
                null, null, codigo, tipoUnidad, entradaReal, salidaEstimada
        );

        if (!detalleNoDisponible.isEmpty()) {
            throw new IllegalStateException("No se puede crear la estancia: " + detalleNoDisponible);
        }
    }

    private void validarCambioDeCodigo(String codigo, TipoUnidad tipoUnidad, Estancia estancia) {
        logger.info("[validarCambioDeCodigo] Validando que no se cambie el codigo de la unidad asignada a la estancia");
        if (tipoUnidad.equals(TipoUnidad.HABITACION)) {
            if(!estancia.getHabitaciones().getFirst().getCodigo().equals(codigo)) {
                throw new IllegalStateException("No se puede cambiar el codigo de la unidad asignada a la estancia");
            }
        }
        if(tipoUnidad.equals(TipoUnidad.APARTAMENTO) || tipoUnidad.equals(TipoUnidad.APARTAESTUDIO)) {
            if(!estancia.getHabitaciones().getFirst().getUnidad().getCodigo().equals(codigo)) {
                throw new IllegalStateException("No se puede cambiar el codigo de la unidad asignada a la estancia");
            }
        }
    }

    private void validarActivacionEstancia(Reserva reserva, Estancia estancia) {

        logger.info("[validarActivacionEstancia] Validando estado de reserva");
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
            throw new IllegalStateException("La reserva debe estar CONFIRMADA para hacer check-in");
        }

        logger.info("[validarActivacionEstancia] Validando existencia de estancia asociada");
        if (estancia == null) {
            throw new IllegalStateException("La reserva no tiene estancia asociada");
        }

        logger.info("[validarActivacionEstancia] Validando estado de estancia");
        if (estancia.getEstado() != EstadoEstancia.RESERVADA) {
            throw new IllegalStateException("La estancia debe estar en estado RESERVADA para hacer check-in");
        }

    }

    private void validarFechasEstancia(LocalDateTime entrada, LocalDateTime salida) {
        if (salida.isBefore(entrada) || salida.isEqual(entrada)) {
            throw new IllegalArgumentException("La fecha de salida debe ser posterior a la fecha de entrada");
        }
    }


    private ModoOcupacion determinarModoOcupacion(TipoUnidad tipoUnidad) {

        return tipoUnidad == TipoUnidad.HABITACION ? ModoOcupacion.INDIVIDUAL : ModoOcupacion.COMPLETO;
    }



    private EstadoEstancia determinarEstadoEstancia(LocalDateTime entrada, LocalDateTime salida) {

        LocalDateTime now = LocalDateTime.now();

        if ((entrada.isBefore(now) || entrada.isEqual(now)) && salida.isAfter(now)) {
            return EstadoEstancia.ACTIVA;
        }

        if (salida.isBefore(now) || salida.isEqual(now)) {
            return EstadoEstancia.EXCEDIDA;
        }

        throw new IllegalStateException(
                "No se puede tener una estancia futura; debe crearse una reserva"
        );
    }

    private EstanciaTablaDTO mapearEstanciaTablaDTO(Estancia estancia) {
        EstanciaTablaDTO dto = new EstanciaTablaDTO();
        dto.setId(estancia.getId());
        dto.setCodigoEstancia(estancia.getCodigoFolio());
        dto.setEstadoEstancia(estancia.getEstado());
        dto.setModoOcupacion(estancia.getModoOcupacion());
        dto.setEntradaReal(estancia.getEntradaReal());
        dto.setSalidaEstimada(estancia.getSalidaEstimada());
        dto.setSalidaReal(estancia.getSalidaReal());
        dto.setTotalPersonas(estancia.getOcupantes() != null ? estancia.getOcupantes().size() : 0);
        dto.setTieneReservaAsociada(estancia.getReserva() != null);
        dto.setIdReservaAsociada(estancia.getReserva() != null ? estancia.getReserva().getId() : null);
        dto.setCodigoReservaAsociada(estancia.getReserva() != null ? estancia.getReserva().getCodigo() : null);
        dto.setTotalPagoEstancia(BigDecimal.ZERO);
        dto.setCantidadPagosModificadosOEliminados(0);

        if (estancia.getOcupantes() != null) {
            Ocupante cliente = estancia.getOcupantes().stream()
                    .filter(ocupante -> ocupante.getTipoOcupante() == TipoOcupante.CLIENTE)
                    .findFirst()
                    .orElse(null);
            if (cliente != null) {
                dto.setIdCliente(cliente.getId());
                dto.setTipoDocumentoCliente(cliente.getTipoDocumento());
                dto.setNumeroDocumentoCliente(cliente.getNumeroDocumento());
                dto.setNombreCliente(String.format("%s %s", cliente.getNombres(), cliente.getApellidos()).trim());
            }
        }

        if (estancia.getHabitaciones() != null && !estancia.getHabitaciones().isEmpty()) {
            if (estancia.getModoOcupacion() == ModoOcupacion.INDIVIDUAL) {
                dto.setCodigoUnidad(estancia.getHabitaciones().getFirst().getCodigo());
                dto.setTipoUnidad(TipoUnidad.HABITACION);
            } else if (estancia.getHabitaciones().getFirst().getUnidad() != null) {
                dto.setCodigoUnidad(estancia.getHabitaciones().getFirst().getUnidad().getCodigo());
                dto.setTipoUnidad(estancia.getHabitaciones().getFirst().getUnidad().getTipo());
            }
        }

        if (estancia.getPagos() != null) {
            dto.setTotalPagoEstancia(estancia.getPagos().stream()
                    .filter(pago -> pago.getEstado() == EstadoPago.PENDIENTE
                            || pago.getEstado() == EstadoPago.COMPLETADO)
                    .map(Pago::getMonto)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));

            int cantidadPagosModificadosOEliminados = (int) estancia.getPagos().stream()
                    .filter(pago -> pago.getEstado() == EstadoPago.MODIFICADO
                            || pago.getEstado() == EstadoPago.ELIMINADO)
                    .count();
            dto.setCantidadPagosModificadosOEliminados(cantidadPagosModificadosOEliminados);
        }

        return dto;
    }





}
