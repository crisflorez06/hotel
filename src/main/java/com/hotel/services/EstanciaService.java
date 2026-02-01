package com.hotel.services;

import com.hotel.dtos.EstanciaDTO;
import com.hotel.dtos.EstanciaEditarRequestDTO;
import com.hotel.dtos.EstanciaNuevoRequestDTO;
import com.hotel.mappers.EstanciaMapper;
import com.hotel.models.*;
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
    private final OcupanteService ocupanteService;
    private final PagoService pagoService;
    private final AlojamientoResolver alojamientoResolver;
    private final UnidadHabitacionResolver unidadHabitacionResolver;


    public EstanciaService(EstanciaRepository estanciaRepository,
                           OcupanteService ocupanteService,
                           PagoService pagoService,
                           AlojamientoResolver alojamientoResolver,
                           UnidadHabitacionResolver unidadHabitacionResolver) {
        this.unidadHabitacionResolver = unidadHabitacionResolver;
        this.alojamientoResolver = alojamientoResolver;
        this.estanciaRepository = estanciaRepository;
        this.ocupanteService = ocupanteService;
        this.pagoService = pagoService;
    }

    @Transactional
    public Estancia crearEstancia(EstanciaNuevoRequestDTO request) {

        String codigo = request.getCodigo();
        TipoUnidad tipoUnidad = request.getTipoUnidad();

        logger.info("[crearEstancia] Verificando disponibilidad para la unidad o habitacion con codigo: {}", codigo);
        if(!alojamientoResolver.verificarDisponiblidad(codigo, tipoUnidad)) {
            throw new IllegalStateException("La unidad o habitacion no está disponible para la estancia.");
        }

        //si se crea mas verificaciones del request creo un metodo aparte para validaciones
        logger.info("[crearEstancia] Validando fechas de entrada y salida de la estancia");
        if (request.getSalidaEstimada().isBefore(request.getEntradaReal())) {
            throw new IllegalArgumentException("salida estimada debe ser posterior a entrada real");
        }

        logger.info("[crearEstancia] Mapeando datos del request a la entidad Estancia");
        Estancia estancia = EstanciaMapper.requestNuevoToEntity(request);

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
            logger.info("Registrando pago asociado a la estancia");
            Pago pago = pagoService.crearPago(request.getPago(), estanciaGuardada.getId());
            estancia.setPago(pago);
        }

        return estanciaGuardada;
    }

    public Estancia editarEstancia(EstanciaEditarRequestDTO request, Long idEstancia) {

        logger.info("[editarEstancia] Editando estancia con id: {}", idEstancia);
        Estancia estancia = estanciaRepository.findById(idEstancia)
                .orElseThrow(() -> new IllegalArgumentException("Estancia no encontrada con id: " + idEstancia));

        logger.info("[editarEstancia] verificando entrada y salida de la estancia");
        estancia.setEstado(determinarEstadoEstancia(request.getEntradaReal(), request.getSalidaEstimada()));
        estancia.setEntradaReal(request.getEntradaReal());
        estancia.setSalidaEstimada(request.getSalidaEstimada());

        logger.info("[editarEstancia] Determinando ocupantes de la estancia");
        estancia.setOcupantes(ocupanteService.determinarOcupantesEstancia(request.getIdCliente(), request.getIdAcompanantes()));

        estancia.setNotas(estancia.getNotas() + " | Notas editadas: " + request.getNotas());

        return estanciaRepository.save(estancia);

    }


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
