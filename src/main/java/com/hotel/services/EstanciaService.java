package com.hotel.services;

import com.hotel.dtos.estancia.ActivarEstanciaDTO;
import com.hotel.dtos.estancia.EstanciaDTO;
import com.hotel.dtos.estancia.EstanciaRequestDTO;
import com.hotel.dtos.estancia.SalidaEstanciaDTO;
import com.hotel.dtos.estancia.EstanciaTablaDTO;
import com.hotel.mappers.EstanciaMapper;
import com.hotel.models.Estancia;
import com.hotel.models.Habitacion;
import com.hotel.models.Ocupante;
import com.hotel.models.Reserva;
import com.hotel.models.enums.*;
import com.hotel.repositories.EstanciaRepository;
import com.hotel.resolvers.AlojamientoResolver;
import com.hotel.resolvers.EstanciaReservaResolver;
import com.hotel.resolvers.UnidadHabitacionResolver;
import com.hotel.specifications.EstanciaSpecification;
import com.hotel.utils.EventoModificadoJsonBuilder;
import com.hotel.utils.EventoNuevoJsonBuilder;
import jakarta.annotation.Nonnull;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    public EstanciaDTO crearEstanciaNueva(EstanciaRequestDTO request) {

        TipoUnidad tipoUnidad = request.getTipoUnidad();
        String codigo = request.getCodigo();
        List<Habitacion> habitaciones = unidadHabitacionResolver.buscarListaHabitaciones(codigo, tipoUnidad);

        logger.info("[crearEstanciaNueva] Validando creacion de nueva estancia");
        validarCreacionEstancia(habitaciones, request.getEntradaReal(), request.getSalidaEstimada());

        logger.info("[crearEstanciaNueva] Mapeando datos del request a la entidad Estancia");
        Estancia estancia = EstanciaMapper.requestToEntity(request);
        estancia.setCodigoFolio(codigoUnicoService.generarCodigoEstancia());

        logger.info("[crearEstanciaNueva] Determinando ocupantes de la estancia");
        estancia.setOcupantes(ocupanteService.determinarOcupantes(request.getIdCliente(), request.getIdAcompanantes()));

        logger.info("[crearEstanciaNueva] Llenando habitaciones asociadas a la estancia");
        estancia.setHabitaciones(habitaciones);

        logger.info("[crearEstanciaNueva] Estableciendo modo de ocupacion para la estancia");
        estancia.setModoOcupacion(alojamientoResolver.determinarModoOcupacion(tipoUnidad));

        logger.info("[crearEstanciaNueva] Determinando estado inicial de la estancia");
        estancia.setEstado(determinarEstadoEstancia(estancia.getEntradaReal(), estancia.getSalidaEstimada()));

        logger.info("[crearEstanciaNueva] Guardando estancia en la base de datos");
        Estancia estanciaGuardada = estanciaRepository.save(estancia);

        logger.info("[crearEstanciaNueva] Actualizando estado de las habitaciones asociadas a la estancia");
        alojamientoResolver.actualizarEstadoAlojamiento(estanciaGuardada.getHabitaciones(), EstadoOperativo.OCUPADO);

        logger.info("[crearEstanciaNueva] Registrando evento de creación de nueva estancia para codigo: {}", estanciaGuardada.getCodigoFolio());
        EventoNuevoJsonBuilder nuevaEstanciaJson = new EventoNuevoJsonBuilder()
                .agregarProp("codigoEstancia", estanciaGuardada.getCodigoFolio())
                .agregarProp("unidad", codigo)
                .agregarProp("fechaEntrada", estanciaGuardada.getEntradaReal())
                .agregarProp("fechaSalida", estanciaGuardada.getSalidaEstimada());

        logger.info("[crearEstanciaNueva] propiedades del evento de nueva estancia: {}", nuevaEstanciaJson.build());
        eventoService.crearEvento(
                TipoEvento.CREACION_ESTANCIA,
                TipoEntidad.ESTANCIA,
                estanciaGuardada.getId(),
                nuevaEstanciaJson.build(),
                estanciaGuardada.getCodigoFolio(),
                null
        );

        return EstanciaMapper.entityToDTO(estanciaGuardada);
    }


    @Transactional
    public EstanciaDTO activarEstancia(@Nonnull ActivarEstanciaDTO request) {

        if(request.getIdReserva() == null) {
            throw new IllegalArgumentException("El id de reserva no puede ser nulo para activar una estancia");
        }
        Reserva reserva = ERService.buscarReservaPorIdDesdeEstancia(request.getIdReserva());
        Estancia estancia = reserva.getEstancia();
        LocalDateTime entradaReal = request.getEntradaReal();
        LocalDateTime salidaEstimada = request.getSalidaEstimada();

        logger.info("[activarEstancia] Validando reglas de negocio para activar estancia desde reserva con id: {}", request.getIdReserva());
        validarActivacionEstancia(reserva, estancia, entradaReal, salidaEstimada);

        // 3) Aplicar cambios a la estancia
        logger.info("[activarEstancia] Actualizando estancia a ACTIVA");
        estancia.setEntradaReal(entradaReal);
        estancia.setSalidaEstimada(salidaEstimada);

        logger.info("[activarEstancia] Determinando ocupantes de la estancia");
        estancia.setOcupantes(ocupanteService.determinarOcupantes(request.getIdCliente(), request.getIdAcompanantes()));

        logger.info("[activarEstancia] Determinando estado inicial de la estancia");
        estancia.setEstado(determinarEstadoEstancia(estancia.getEntradaReal(), estancia.getSalidaEstimada()));

        logger.info("[activarEstancia] Agregando notas de activación a la estancia");
        estancia.setNotas(estancia.getNotas() + " | Notas de activación: " + request.getNotas());

        logger.info("[activarEstancia] Guardando estancia en la base de datos");
        Estancia estanciaGuardada = estanciaRepository.save(estancia);

        logger.info("[activarEstancia] Actualizando estado de la reserva asociada a la estancia a COMPLETADA");
        ERService.actualizarEstadoReservaDesdeEstancia(reserva, EstadoReserva.COMPLETADA);

        logger.info("[activarEstancia] Actualizando estado de las habitaciones asociadas a la estancia");
        alojamientoResolver.actualizarEstadoAlojamiento(estanciaGuardada.getHabitaciones(), EstadoOperativo.OCUPADO);

        logger.info("[activarEstancia] Registrando evento de creación de nueva estancia para codigo: {}", estanciaGuardada.getCodigoFolio());
        EventoNuevoJsonBuilder nuevaEstancia = new EventoNuevoJsonBuilder()
                .agregarProp("codigoEstancia", estanciaGuardada.getCodigoFolio())
                .agregarProp("unidad", unidadHabitacionResolver.determinarCodigoUnidad(estanciaGuardada.getHabitaciones()))
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

        return EstanciaMapper.entityToDTO(estanciaGuardada);

    }

    @Transactional
    public void editarEstancia(@Nonnull EstanciaRequestDTO request, Long idEstancia) {

        EventoModificadoJsonBuilder estanciaModificadaJson = new EventoModificadoJsonBuilder();

        String codigo = request.getCodigo();
        TipoUnidad tipoUnidad = request.getTipoUnidad();
        List<Habitacion> habitaciones = unidadHabitacionResolver.buscarListaHabitaciones(codigo, tipoUnidad);
        LocalDateTime entradaReal = request.getEntradaReal();
        LocalDateTime salidaEstimada = request.getSalidaEstimada();

        logger.info("[editarEstancia] Editando estancia con id: {}", idEstancia);
        Estancia estancia = estanciaRepository.findById(idEstancia)
                .orElseThrow(() -> new EntityNotFoundException("Estancia no encontrada con id: " + idEstancia));

        if (!(estancia.getEstado() == EstadoEstancia.ACTIVA || estancia.getEstado() == EstadoEstancia.EXCEDIDA)) {
            throw new IllegalStateException("Solo se pueden editar estancias en estado ACTIVA o EXCEDIDA. Estado actual: " + estancia.getEstado());
        }

        logger.info("[editarEstancia] Verificando si se ha cambiado el codigo o tipo de unidad asignada a la estancia");
        boolean cambioCodigo = unidadHabitacionResolver.codigoHaCambiado(habitaciones, estancia, null);
        boolean cambioFechas = cambioFechas(entradaReal, salidaEstimada, estancia);

        if(cambioCodigo || cambioFechas) {
            logger.info("[editarEstancia] Verificando disponibilidad para el cambio de codigo o fechas");
            validarEdicionEstancia(estancia, habitaciones, request.getEntradaReal(), request.getSalidaEstimada());
        }


        logger.info("[editarEstancia] Verificando si hubo cambio de codigo o tipo de unidad asignada a la estancia");

        if(cambioCodigo){
            estanciaModificadaJson.agregarCambio("unidad",
                    unidadHabitacionResolver.determinarCodigoUnidad(estancia.getHabitaciones()),
                    codigo);

            TipoUnidad tipoUnidadAnterior = unidadHabitacionResolver.determinarTipoUnidad(estancia.getHabitaciones());

            alojamientoResolver.actualizarEstadoAlojamiento(estancia.getHabitaciones(), EstadoOperativo.DISPONIBLE);

            estancia.setHabitaciones(new ArrayList<>(habitaciones));
            alojamientoResolver.actualizarEstadoAlojamiento(estancia.getHabitaciones(), EstadoOperativo.OCUPADO);

            logger.info("[editarEstancia] Estableciendo modo de ocupacion para la estancia");
            estancia.setModoOcupacion(alojamientoResolver.determinarModoOcupacion(tipoUnidad));

            logger.info("[editarEstancia] Creando pago asociado al cambio de unidad de la estancia");



            if(tipoUnidadAnterior != tipoUnidad) {
                pagoService.crearPagoPorCambioUnidad(estancia, tipoUnidadAnterior);

            }

        }

        logger.info("[editarEstancia] Verificando si hubo cambio de fechas");
        if(cambioFechas) {
            logger.info("[editarEstancia] Cambio de fechas detectado, actualizando fechas de entrada y salida de la estancia");
            estanciaModificadaJson.agregarCambio("entradaReal", estancia.getEntradaReal(), entradaReal);
            estancia.setEntradaReal(entradaReal);

            estanciaModificadaJson.agregarCambio("salidaEstimada", estancia.getSalidaEstimada(), salidaEstimada);
            estancia.setSalidaEstimada(salidaEstimada);

            estancia.setEstado(determinarEstadoEstancia(entradaReal, salidaEstimada));
        }

        logger.info("[editarEstancia] Determinando ocupantes de la estancia");
        Ocupante cliente = ocupanteService.determinarCliente(estancia.getOcupantes()).
                orElseThrow(() -> new IllegalStateException("La estancia debe tener un cliente ocupante"));

        if(!cliente.getId().equals(request.getIdCliente())) {
            estanciaModificadaJson.agregarCambio("cliente",
                    ocupanteService.obtenerNombre(cliente.getId()),
                    ocupanteService.obtenerNombre(request.getIdCliente()));
        }

        Set<Long> idsAntes = estancia.getOcupantes()
                .stream()
                .map(Ocupante::getId)
                .filter(id -> !Objects.equals(id, cliente.getId()))
                .collect(Collectors.toSet());

        if(request.getIdAcompanantes() == null) {
            request.setIdAcompanantes(new ArrayList<>());
                if(!idsAntes.isEmpty()) {
                    estanciaModificadaJson.agregarCambio("acompanantes",
                            representarAcompanantesParaAuditoria(idsAntes),
                            "Sin acompanantes");
                }
        } else {
            Set<Long> idsDespues = new HashSet<>(request.getIdAcompanantes());

            if(!idsAntes.equals(idsDespues)) {

                estanciaModificadaJson.agregarCambio("acompanantes",
                        representarAcompanantesParaAuditoria(idsAntes),
                        representarAcompanantesParaAuditoria(idsDespues));
            }
        }


        estancia.setOcupantes(ocupanteService.determinarOcupantes(request.getIdCliente(), request.getIdAcompanantes()));

        estancia.setNotas(estancia.getNotas() + " | Notas editadas: " + request.getNotas());

        Estancia estanciaGuardada = estanciaRepository.save(estancia);

        logger.info("[editarEstancia] propiedades del evento de nueva estancia: {}", estanciaModificadaJson.build());

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
                .orElseThrow(() -> new EntityNotFoundException("Estancia no encontrada con id: " + idEstancia));

        if(estancia.getEstado() != EstadoEstancia.ACTIVA && estancia.getEstado() != EstadoEstancia.EXCEDIDA) {
            throw new IllegalStateException("Solo se pueden eliminar estancias en estado ACTIVA o EXCEDIDA. Estado actual: " + estancia.getEstado());
        }

        alojamientoResolver.actualizarEstadoAlojamiento(estancia.getHabitaciones(), EstadoOperativo.DISPONIBLE);

        logger.info("[eliminarEstancia] Actualizando estado de la estancia a CANCELADA");
        estancia.setEstado(EstadoEstancia.CANCELADA);

        logger.info("[eliminarEstancia] Eliminando pagos asociados a la estancia");
        pagoService.eliminarTodoLosPagos(estancia.getId());

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

    public EstanciaDTO obtenerEstancia(Long idEstancia) {

        logger.info("[obtenerEstancia] Obteniendo estancia con id: {}", idEstancia);
        Estancia estancia = estanciaRepository.findById(idEstancia)
                .orElseThrow(() -> new EntityNotFoundException("Estancia no encontrada con id: " + idEstancia));

        return EstanciaMapper.entityToDTO(estancia);

    }

    public Page<EstanciaTablaDTO> buscarEstanciasTabla(
            Long idEstancia,
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
                        idEstancia,
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

        return estancias.map(EstanciaMapper::entityToTablaDTO);
    }

    //verificar reglas de negocio para finalizar estancia
    @Transactional
    public Void finalizarEstancia(SalidaEstanciaDTO request) {

        Long idEstancia = request.getIdEstancia();

        logger.info("[finalizarEstancia] Finalizando estancia con id: {}", idEstancia);
        Estancia estancia = estanciaRepository.findById(idEstancia)
                .orElseThrow(() -> new EntityNotFoundException("Estancia no encontrada con id: " + idEstancia));

        if(estancia.getEstado() != EstadoEstancia.ACTIVA && estancia.getEstado() != EstadoEstancia.EXCEDIDA) {
            throw new IllegalStateException("Solo se pueden finalizar estancias en estado ACTIVA o EXCEDIDA. Estado actual: " + estancia.getEstado());
        }

        pagoService.validarPagoEstanciaCompletadaParaFinalizacion(request.getPagoEstancia(), estancia);

        estancia.setEstado(EstadoEstancia.FINALIZADA);
        estancia.setSalidaReal(request.getFechaSalidaReal());

        logger.info("[finalizarEstancia] Creando pago asociado a la estancia finalizada");
        pagoService.crearPago(request.getPagoEstancia(), estancia.getId(), true);

        estancia.setNotas(estancia.getNotas() + " | Notas de salida: " + request.getNotasSalida());

        estancia.setPrecioTotal(pagoService.sumarTotalPagosPorEstancia(estancia.getId()));

        logger.info("[finalizarEstancia] Guardando cambios en la estancia finalizada");
        Estancia estanciaFinalizada = estanciaRepository.save(estancia);


        logger.info("[finalizarEstancia] Registrando evento de creación de nueva estancia para codigo: {}", estanciaFinalizada.getCodigoFolio());
        EventoNuevoJsonBuilder nuevaEstancia = new EventoNuevoJsonBuilder()
                .agregarProp("codigoEstancia", estanciaFinalizada.getCodigoFolio())
                .agregarProp("unidad", unidadHabitacionResolver.determinarCodigoUnidad(estancia.getHabitaciones()))
                .agregarProp("fechaEntrada", estanciaFinalizada.getEntradaReal())
                .agregarProp("fechaSalida", estanciaFinalizada.getSalidaReal())
                .agregarProp("precioTotal", estanciaFinalizada.getPrecioTotal());


        eventoService.crearEvento(
                TipoEvento.FINALIZACION_ESTANCIA,
                TipoEntidad.ESTANCIA,
                estanciaFinalizada.getId(),
                nuevaEstancia.build(),
                estanciaFinalizada.getCodigoFolio(),
                null
        );

        alojamientoResolver.actualizarEstadoAlojamiento(estancia.getHabitaciones(), EstadoOperativo.DISPONIBLE);

        return null;
    }


    private void validarCreacionEstancia(List<Habitacion> habitaciones, LocalDateTime entradaReal, LocalDateTime salidaEstimada) {
        logger.info("[validarCreacionEstancia] Validando fechas de entrada y salida");
        validarFechasEstancia(entradaReal, salidaEstimada);

        logger.info("[validarCreacionEstancia] Verificando disponibilidad");
        String detalleNoDisponible = disponibilidadService.verificarDisponibilidadNuevo(habitaciones, entradaReal, salidaEstimada);

        if (!detalleNoDisponible.isEmpty()) {
            throw new IllegalStateException("No se puede crear la estancia: " + detalleNoDisponible);
        }
    }

    private boolean cambioFechas(LocalDateTime entradaReal, LocalDateTime salidaEstimada, Estancia estancia) {
        logger.info("[cambioFechas] Validando si se han cambiado las fechas de entrada o salida de la estancia");
        return !estancia.getEntradaReal().isEqual(entradaReal)
                || !estancia.getSalidaEstimada().isEqual(salidaEstimada);
    }

    private String representarAcompanantesParaAuditoria(Set<Long> idsAcompanantes) {
        if (idsAcompanantes == null || idsAcompanantes.isEmpty()) {
            return "Sin acompanantes";
        }
        return ocupanteService.obtenerNombresAcompanantes(idsAcompanantes);
    }

    private void validarActivacionEstancia(Reserva reserva, Estancia estancia, LocalDateTime entradaReal, LocalDateTime salidaEstimada) {

        logger.info("[validarActivacionEstancia] Validando estado de reserva");
        if (reserva.getEstado() != EstadoReserva.CONFIRMADA
                && reserva.getEstado() != EstadoReserva.EXPIRADA) {
            throw new IllegalStateException(
                    "La reserva debe estar CONFIRMADA o EXPIRADA para activar la estancia. Estado actual: " + reserva.getEstado());
        }

        logger.info("[validarActivacionEstancia] Validando existencia de estancia asociada");
        if (estancia == null) {
            throw new IllegalStateException("La reserva no tiene estancia asociada");
        }

        logger.info("[validarActivacionEstancia] Validando estado de estancia");
        if (estancia.getEstado() != EstadoEstancia.RESERVADA) {
            throw new IllegalStateException("La estancia debe estar en estado RESERVADA para activarla. Estado actual: " + estancia.getEstado());
        }

        logger.info("[validarActivacionEstancia] Validando fechas de entrada y salida de la estancia");
        validarFechasEstancia(entradaReal, salidaEstimada);

        List<Habitacion> habitacionesActivacion = estancia.getHabitaciones();
        if (habitacionesActivacion == null || habitacionesActivacion.isEmpty()) {
            throw new IllegalStateException("La estancia no tiene habitaciones asociadas para activar");
        }

        logger.info("[validandoActivacionEstancia] Verificando disponibilidad");
        String detalleNoDisponible = disponibilidadService.verificarDisponibilidadEditar(
                reserva,
                estancia,
                habitacionesActivacion,
                entradaReal,
                salidaEstimada
        );

        if (!detalleNoDisponible.isEmpty()) {
            throw new IllegalStateException("No se puede activar la estancia: " + detalleNoDisponible);
        }

    }

    private void validarFechasEstancia(LocalDateTime entrada, LocalDateTime salida) {
        if (salida.isBefore(entrada) || salida.isEqual(entrada)) {
            throw new IllegalArgumentException("La fecha de salida debe ser posterior a la fecha de entrada");
        }
        if(entrada.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha de entrada no puede ser superior a la fecha actual, crear reserva si se desea una estancia futura");
        }
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
                "No se puede determinar el estado de la estancia con las fechas proporcionadas. Entrada: " + entrada + ", Salida: " + salida
        );
    }

    private void validarEdicionEstancia(Estancia estancia, List<Habitacion> habitaciones, LocalDateTime entradaReal, LocalDateTime salidaEstimada) {
        logger.info("[validarEdicionEstancia] Validando fechas de entrada y salida");
        validarFechasEstancia(entradaReal, salidaEstimada);

        logger.info("[validarEdicionEstancia] Verificando disponibilidad");
        String detalleNoDisponible = disponibilidadService.verificarDisponibilidadEditar(null, estancia, habitaciones, entradaReal, salidaEstimada);

        if (!detalleNoDisponible.isEmpty()) {
            throw new IllegalStateException("No se puede editar la estancia: " + detalleNoDisponible);
        }
    }

    private EstanciaDTO crearEstanciaConModoOcupacion() {
        EstanciaDTO dto = new EstanciaDTO();
        dto.setModoOcupacion(ModoOcupacion.INDIVIDUAL);
        return dto;
    }
}
