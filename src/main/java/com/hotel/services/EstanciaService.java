package com.hotel.services;

import com.hotel.dtos.EstanciaRequestDTO;
import com.hotel.mappers.EstanciaMapper;
import com.hotel.models.*;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.ModoOcupacion;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.EstanciaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.List;

@Service
public class EstanciaService {

    private static final Logger logger = LoggerFactory.getLogger(EstanciaService.class);

    private final EstanciaRepository estanciaRepository;
    private final ReservaService reservaService;
    private final ClienteService clienteService;
    private final HabitacionService habitacionService;
    private final UnidadService unidadService;
    private final AcompananteService acompananteService;


    public EstanciaService(EstanciaRepository estanciaRepository,
                           ReservaService reservaService,
                           ClienteService clienteService,
                           AcompananteService acompananteService,
                           HabitacionService habitacionService, UnidadService unidadService) {
        this.estanciaRepository = estanciaRepository;
        this.reservaService = reservaService;
        this.clienteService = clienteService;
        this.acompananteService = acompananteService;
        this.habitacionService = habitacionService;
        this.unidadService = unidadService;
    }

    @Transactional
    public Estancia crearEstancia(EstanciaRequestDTO request) {

        logger.info("Verificando disponibilidad para la estancia solicitada");
        if (!verificarDisponiblidad(request.getCodigo(), request.getTipoUnidad())) {
            throw new IllegalStateException("La unidad o habitacion no está disponible para la estancia.");
        }

        logger.info("Validando fechas de entrada y salida estimada");
        if (request.getSalidaEstimada().isBefore(request.getEntradaReal())) {
            throw new IllegalArgumentException("salida estimada debe ser posterior a entrada real");
        }

        logger.info("Creando estancia para el cliente con ID: {}", request.getIdCliente());
        Estancia estancia = EstanciaMapper.requestToEntity(request);

        if(request.getIdReserva() != null) {
            logger.info("Asociando reserva con ID: {} a la estancia", request.getIdReserva());
            estancia.setReserva(reservaService.buscarPorId(request.getIdReserva()));
        }

        logger.info("Buscando cliente con ID: {}", request.getIdCliente());
        estancia.setCliente(clienteService.buscarPorId(request.getIdCliente()));

        logger.info("Determinando tipo de unidad para la estancia: {}", request.getTipoUnidad());
        if(request.getTipoUnidad().equals(TipoUnidad.HABITACION)) {
            logger.info("Llenando una habitacion para la estancia en modo individual");
            estancia.setModoOcupacion(ModoOcupacion.INDIVIDUAL);
            estancia.setEstanciaHabitaciones(llenarUnaHabitacion(estancia, request.getCodigo()));

        } else {
            logger.info("Llenando todas las habitaciones para la estancia en modo completo");

            estancia.setModoOcupacion(ModoOcupacion.COMPLETO);
            estancia.setEstanciaHabitaciones(llenarTodasHabitaciones(estancia, request.getCodigo()));
        }

        cambiarEstadoHabitacion(estancia.getEstanciaHabitaciones(), EstadoOperativo.OCUPADO);
        logger.info("Llenando acompañantes para la estancia");

        estancia.setEstanciaAcompanantes(llenarAcompanantes(estancia, request.getIdAcompanantes()));

        logger.info("informacion completa de la estancia creada, idEstancia: {}, clienteId: {}, tipo de ocupacion: {},numeroHabitaciones: {}, numeroAcompanantes: {}",
                estancia.getId(),
                estancia.getCliente().getId(),
                estancia.getModoOcupacion(),
                estancia.getEstanciaHabitaciones().size(),
                estancia.getEstanciaAcompanantes().size()
        );

        return estanciaRepository.save(estancia);
    }

    private Boolean verificarDisponiblidad(String codigo, TipoUnidad tipoUnidad) {
        logger.info("Verificando disponibilidad para codigo: {} y tipoUnidad: {}", codigo, tipoUnidad);
        if (tipoUnidad.equals(TipoUnidad.HABITACION)) {
            Habitacion habitacion = habitacionService.buscarPorCodigo(codigo);
            return habitacion.getEstadoOperativo() == EstadoOperativo.DISPONIBLE;
        } else {
            Unidad unidad = unidadService.buscarPorCodigo(codigo);
            return unidad.getEstadoOperativo() == EstadoOperativo.DISPONIBLE;
        }

    }

    private List<EstanciaHabitacion> llenarTodasHabitaciones(Estancia estancia, String codigoUnidad) {

        logger.info("Utilizando metodo llenarTodasHabitaciones con codigoUnidad: {}", codigoUnidad);
        List<Habitacion> habitaciones = unidadService.buscarHabitacionesPorCodigoUnidad(codigoUnidad);

        return habitaciones.stream()
                .map(habitacion -> {
                    EstanciaHabitacion eh = new EstanciaHabitacion();
                    eh.setEstancia(estancia);
                    eh.setHabitacion(habitacion);
                    eh.setEstadoOcupacion(true);

                    logger.info("Estancia de habitacion creada en llenarTodasHabitaciones: {}", eh);
                    return eh;
                })
                .toList();
    }


    private List<EstanciaHabitacion> llenarUnaHabitacion(Estancia estancia, String codigoHabitacion) {
        logger.info("Utilizando metodo llenarUnaHabitacion con codigoHabitacion: {}", codigoHabitacion);
        Habitacion habitacion = habitacionService.buscarPorCodigo(codigoHabitacion);

        EstanciaHabitacion eh = new EstanciaHabitacion();
        eh.setEstancia(estancia);
        eh.setHabitacion(habitacion);
        eh.setEstadoOcupacion(true);

        logger.info("Estancia de habitacion creada en llenarUnaHabitacion: {}", eh);
        return List.of(eh);
    }

    private void cambiarEstadoHabitacion(List<EstanciaHabitacion> estanciaHabitaciones, EstadoOperativo estado) {
        logger.info("Cambiando estado de habitaciones asociadas a la estancia a: {}", estado);
        for (EstanciaHabitacion eh : estanciaHabitaciones) {
            Habitacion habitacion = eh.getHabitacion();
            habitacion.setEstadoOperativo(estado);
        }

        logger.info("cambiando estado de la unidad asociada a la habitacion");

        cambiarEstadoUnidad(estanciaHabitaciones);

    }

    private void cambiarEstadoUnidad(List<EstanciaHabitacion> estanciaHabitaciones) {

        List<Habitacion> habitaciones = estanciaHabitaciones.stream()
                .map(EstanciaHabitacion::getHabitacion)
                .toList();

        EstadoOperativo primerEstado = habitaciones.getFirst().getEstadoOperativo();

        boolean todosIguales = habitaciones.stream()
                .allMatch(h -> h.getEstadoOperativo() == primerEstado);

        EstadoOperativo estadoCalculado = todosIguales
                ? primerEstado
                : EstadoOperativo.PARCIALMENTE;

        String codigoUnidad = habitaciones.getFirst().getUnidad().getCodigo();
        logger.info("Unidad {} -> Estado calculado: {}", codigoUnidad, estadoCalculado);

        Unidad unidad = habitaciones.getFirst().getUnidad();
        unidad.setEstadoOperativo(estadoCalculado);
    }

    private List<EstanciaAcompanante> llenarAcompanantes(Estancia estancia, List<Long> idsAcompanantes) {
        List<EstanciaAcompanante> acompanantes = new ArrayList<>();

        for (Long id : idsAcompanantes) {
            Acompanante acompanante = acompananteService.buscarPorId(id);
            EstanciaAcompanante relacion = new EstanciaAcompanante();
            relacion.setEstancia(estancia);
            relacion.setAcompanante(acompanante);
            relacion.setEstadoOcupacion(true);
            acompanantes.add(relacion);
        }

        return acompanantes;
    }

}
