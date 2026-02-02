package com.hotel.services;

import com.hotel.dtos.EstanciaDTO;
import com.hotel.dtos.EstanciaRequestDTO;
import com.hotel.dtos.PagoNuevoRequestDTO;
import com.hotel.mappers.EstanciaMapper;
import com.hotel.models.Estancia;
import com.hotel.models.Habitacion;
import com.hotel.models.Pago;
import com.hotel.models.Reserva;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.ModoOcupacion;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.EstanciaRepository;
import com.hotel.resolvers.AlojamientoResolver;
import com.hotel.resolvers.UnidadHabitacionResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.List;

@Service
public class EstanciaService {

    private static final Logger logger = LoggerFactory.getLogger(EstanciaService.class);

    private final EstanciaRepository estanciaRepository;
    private final ReservaService reservaService;
    private final OcupanteService ocupanteService;
    private final PagoService pagoService;
    private final AlojamientoResolver alojamientoResolver;
    private final UnidadHabitacionResolver unidadHabitacionResolver;
    private final DisponibilidadService disponibilidadService;


    public EstanciaService(EstanciaRepository estanciaRepository,
                           OcupanteService ocupanteService,
                           PagoService pagoService,
                           AlojamientoResolver alojamientoResolver,
                           UnidadHabitacionResolver unidadHabitacionResolver,
                           DisponibilidadService disponibilidadService,
                           ReservaService reservaService) {
        this.reservaService = reservaService;
        this.disponibilidadService = disponibilidadService;
        this.unidadHabitacionResolver = unidadHabitacionResolver;
        this.alojamientoResolver = alojamientoResolver;
        this.estanciaRepository = estanciaRepository;
        this.ocupanteService = ocupanteService;
        this.pagoService = pagoService;
    }

    @Transactional
    public Estancia crearEstancia(EstanciaRequestDTO request) {

        String codigo = request.getCodigo();
        TipoUnidad tipoUnidad = request.getTipoUnidad();

        logger.info("[crearEstancia] Verificando disponibilidad para la unidad o habitacion con codigo: {}", codigo);
        String existeDisponibilidad = disponibilidadService.verificarDisponibilidad(codigo, tipoUnidad, request.getEntradaReal(), request.getSalidaEstimada());

        if(!existeDisponibilidad.isEmpty()) {
            throw new IllegalStateException("No se puede crear la estancia: " + existeDisponibilidad);
        }

        //si se crea mas verificaciones del request creo un metodo aparte para validaciones
        logger.info("[crearEstancia] Validando fechas de entrada y salida de la estancia");
        if (request.getSalidaEstimada().isBefore(request.getEntradaReal())) {
            throw new IllegalArgumentException("salida estimada debe ser posterior a entrada real");
        }

        logger.info("[crearEstancia] Mapeando datos del request a la entidad Estancia");
        Estancia estancia = EstanciaMapper.requestToEntity(request);

        logger.info("[crearEstancia] Determinando ocupantes de la estancia");
        estancia.setOcupantes(ocupanteService.determinarOcupantesEstancia(request.getIdCliente(), request.getIdAcompanantes()));

        logger.info("[crearEstancia] Llenando habitaciones asociadas a la estancia");
        estancia.setHabitaciones(unidadHabitacionResolver.buscarListaHabitaciones(codigo, tipoUnidad));

        logger.info("[crearEstancia] Estableciendo modo de ocupacion para la estancia");
        estancia.setModoOcupacion(determinarModoOcupacion(tipoUnidad));

        logger.info("[crearEstancia] Determinando estado inicial de la estancia");
        estancia.setEstado(determinarEstadoEstancia(request.getEntradaReal(), request.getSalidaEstimada()));

        logger.info("[crearEstancia] Guardando estancia en la base de datos");
        Estancia estanciaGuardada = estanciaRepository.save(estancia);

        logger.info("[crearEstancia] Actualizando estado de las habitaciones asociadas a la estancia");
        alojamientoResolver.actualizarEstadoAlojamiento(estanciaGuardada.getHabitaciones(), EstadoOperativo.OCUPADO);

        if(request.getPago() != null) {
            logger.info("[crearEstancia] Creando pago asociado a la estancia");
            Pago pago = pagoService.crearPago(request.getPago(), estanciaGuardada.getId());
            estancia.setPago(pago);
        }

        return estanciaGuardada;
    }

    @Transactional
    public void editarEstancia(EstanciaRequestDTO request, Long idEstancia) {

        String codigo = request.getCodigo();
        TipoUnidad tipoUnidad = request.getTipoUnidad();
        LocalDateTime entradaReal = request.getEntradaReal();
        LocalDateTime salidaEstimada = request.getSalidaEstimada();

        logger.info("[editarEstancia] verificando entrada y salida de la estancia");
        String existeDisponibilidad = disponibilidadService.verificarDisponibilidad(codigo, tipoUnidad, entradaReal, salidaEstimada);
        if(!existeDisponibilidad.isEmpty()) {
            throw new IllegalStateException("No se puede crear la estancia: " + existeDisponibilidad);
        }

        logger.info("[editarEstancia] Editando estancia con id: {}", idEstancia);
        Estancia estancia = estanciaRepository.findById(idEstancia)
                .orElseThrow(() -> new IllegalArgumentException("Estancia no encontrada con id: " + idEstancia));


        estancia.setEstado(determinarEstadoEstancia(entradaReal, salidaEstimada));
        estancia.setEntradaReal(entradaReal);
        estancia.setSalidaEstimada(salidaEstimada);

        logger.info("[editarEstancia] Determinando ocupantes de la estancia");
        estancia.setOcupantes(ocupanteService.determinarOcupantesEstancia(request.getIdCliente(), request.getIdAcompanantes()));

        estancia.setNotas(estancia.getNotas() + " | Notas editadas: " + request.getNotas());

        estanciaRepository.save(estancia);

    }


    @Transactional
    public Void eliminarEstancia(Long idEstancia) {
        logger.info("[eliminarEstancia] Eliminando estancia con id: {}", idEstancia);
        Estancia estancia = estanciaRepository.findById(idEstancia)
                .orElseThrow(() -> new IllegalArgumentException("Estancia no encontrada con id: " + idEstancia));

        alojamientoResolver.actualizarEstadoAlojamiento(estancia.getHabitaciones(), EstadoOperativo.DISPONIBLE);

        estancia.setEstado(EstadoEstancia.CANCELADA);
        estanciaRepository.save(estancia);
        return null;
    }

    public EstanciaDTO obtenerEstancia(String codigo, TipoUnidad tipoUnidad) {
        logger.info("[obtenerEstanciaPorUnidad] Obteniendo estancia para la unidad con codigo: {}", codigo);
        List<Habitacion> habitaciones = unidadHabitacionResolver.buscarListaHabitaciones(codigo, tipoUnidad);
        Long habitacionId = habitaciones.getFirst().getId();

        Estancia estancia = estanciaRepository.findActivaOExcedidaPorHabitacionId(habitacionId).orElseThrow(
                () -> new IllegalArgumentException("No se encontró una estancia activa o excedida para la unidad con codigo: " + codigo)
        );
        return EstanciaMapper.entityToDTO(estancia);
    }

    //verificar reglas de negocio para finalizar estancia
    @Transactional
    public void finalizarEstancia(Long idEstancia, PagoNuevoRequestDTO pagoRequest) {
        logger.info("[finalizarEstancia] Finalizando estancia con id: {}", idEstancia);
        Estancia estancia = estanciaRepository.findById(idEstancia)
                .orElseThrow(() -> new IllegalArgumentException("Estancia no encontrada con id: " + idEstancia));

        estancia.setEstado(EstadoEstancia.FINALIZADA);
        estancia.setSalidaReal(LocalDateTime.now());

        if(pagoRequest != null) {
            logger.info("[finalizarEstancia] Creando pago asociado a la estancia finalizada");
            Pago pago = pagoService.crearPago(pagoRequest, estancia.getId());
            estancia.setPago(pago);
        }
        estanciaRepository.save(estancia);

        alojamientoResolver.actualizarEstadoAlojamiento(estancia.getHabitaciones(), EstadoOperativo.DISPONIBLE);
    }

    //verificar reglas de negocio para crear estancia con reserva
    @Transactional
    public EstanciaDTO crearEstanciaConReserva(Long idReserva, EstanciaRequestDTO request) {
        logger.info("[crearEstanciaConReserva] Obteniendo reserva con id: {}", idReserva);
        Reserva reserva = reservaService.buscarPorId(idReserva);
        Estancia estancia = crearEstancia(request);
        estancia.setReserva(reserva);
        estancia = estanciaRepository.save(estancia);
        return EstanciaMapper.entityToDTO(estancia);
    }



    private ModoOcupacion determinarModoOcupacion(TipoUnidad tipoUnidad) {

        return tipoUnidad == TipoUnidad.HABITACION ? ModoOcupacion.INDIVIDUAL : ModoOcupacion.COMPLETO;
    }



    private EstadoEstancia determinarEstadoEstancia(LocalDateTime entrada, LocalDateTime salida) {
        if(entrada.isBefore(LocalDateTime.now())  || entrada.isEqual(LocalDateTime.now()) && salida.isAfter(LocalDateTime.now())) {
            return EstadoEstancia.ACTIVA;
        }
        if(salida.isBefore(LocalDateTime.now())) {
            return EstadoEstancia.EXCEDIDA;
        }

        throw new IllegalStateException("No se puede tener una estancia despues de la fecha actual, debe crearse una reserva");
    }




}
