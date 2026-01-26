package com.hotel.services;

import com.hotel.dtos.EstanciaDTO;
import com.hotel.dtos.EstanciaEditarRequestDTO;
import com.hotel.dtos.EstanciaNuevoRequestDTO;
import com.hotel.mappers.EstanciaMapper;
import com.hotel.models.*;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.ModoOcupacion;
import com.hotel.models.enums.TipoOcupante;
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
    private final OcupanteService ocupanteService;
    private final HabitacionService habitacionService;
    private final EstanciaHabitacionService estanciaHabitacionService;
    private final PagoService pagoService;
    private final UnidadService unidadService;


    public EstanciaService(EstanciaRepository estanciaRepository,
                           OcupanteService ocupanteService,
                           HabitacionService habitacionService,
                           EstanciaHabitacionService estanciaHabitacionService,
                           PagoService pagoService,
                           UnidadService unidadService) {
        this.estanciaRepository = estanciaRepository;
        this.ocupanteService = ocupanteService;
        this.habitacionService = habitacionService;
        this.estanciaHabitacionService = estanciaHabitacionService;
        this.pagoService = pagoService;
        this.unidadService = unidadService;
    }

    @Transactional
    public Estancia crearEstancia(EstanciaNuevoRequestDTO request) {

        logger.info("Verificando disponibilidad para la estancia solicitada");
        if (!habitacionService.verificarDisponiblidad(request.getCodigo(), request.getTipoUnidad())) {
            throw new IllegalStateException("La unidad o habitacion no está disponible para la estancia.");
        }

        logger.info("Validando fechas de entrada y salida estimada");
        if (request.getSalidaEstimada().isBefore(request.getEntradaReal())) {
            throw new IllegalArgumentException("salida estimada debe ser posterior a entrada real");
        }

        logger.info("Creando estancia para el ocupante titular con ID: {}", request.getIdCliente());
        Estancia estancia = EstanciaMapper.requestNuevoToEntity(request);

        logger.info("Buscando y asignando ocupantes para la estancia");
        List<Ocupante> ocupantes = new ArrayList<>();

        Ocupante cliente = ocupanteService.buscarPorId(request.getIdCliente());
        TipoOcupante tipoOcupanteCliente = cliente.getTipoOcupante();

        if (tipoOcupanteCliente != TipoOcupante.CLIENTE) {
            ocupantes.add(ocupanteService.crearOcupanteConDiferenteTipo(cliente, TipoOcupante.CLIENTE));
        }  else {
            ocupantes.add(cliente);
        }

        ocupantes.addAll(cargarOcupantesAdicionales(request.getIdAcompanantes()));

        estancia.setOcupantes(ocupantes);


        logger.info("llenando habitaciones para la estancia");
        estancia.setEstanciaHabitaciones(estanciaHabitacionService.llenarHabitaciones(estancia, request.getCodigo(), request.getTipoUnidad()));

        logger.info("Determinando modo de ocupacion para la estancia");
        estancia.setModoOcupacion(determinarModoOcupacion(request.getTipoUnidad()));

        logger.info("Cambiando estado de las habitaciones asociadas a la estancia a OCUPADO");
        habitacionService.cambiarEstadoHabitaciones(request.getCodigo(), EstadoOperativo.OCUPADO, request.getTipoUnidad());


        Estancia estanciaGuardada = estanciaRepository.save(estancia);

        if(request.getPago() != null) {
            logger.info("Registrando pago asociado a la estancia");
            Pago pago = pagoService.crearPago(request.getPago(), estanciaGuardada.getId());
            estancia.setPago(pago);
        }

        return estanciaGuardada;
    }

    public Estancia editarEstancia(EstanciaEditarRequestDTO request, Long idEstancia) {
        logger.info("Editando estancia con id: {}", idEstancia);
        Estancia estancia = estanciaRepository.findById(idEstancia)
                .orElseThrow(() -> new IllegalArgumentException("Estancia no encontrada con id: " + idEstancia));

        if (request.getIdCliente() != null && request.getIdAcompanantes() != null) {
            logger.info("Actualizando ocupantes de la estancia");
            estancia.setOcupantes(reemplazarTitularYAdicionalesEnOcupantes(request.getIdCliente(), request.getIdAcompanantes()));
        } else if (request.getIdCliente() != null) {
            logger.info("Actualizando ocupante titular de la estancia");
            estancia.setOcupantes(reemplazarTitularEnOcupantes(estancia, request.getIdCliente()));
        } else if (request.getIdAcompanantes() != null) {
            logger.info("Actualizando ocupantes adicionales de la estancia");
            estancia.setOcupantes(reemplazarAdicionalesEnOcupantes(estancia, request.getIdAcompanantes()));
        }

        if (request.getEntradaReal() != null) {
            logger.info("Actualizando entrada real de la estancia");
            estancia.setEntradaReal(request.getEntradaReal());
        }

        if (request.getSalidaEstimada() != null) {
            logger.info("Actualizando salida estimada de la estancia");
            estancia.setSalidaEstimada(request.getSalidaEstimada());
        }

        if (request.getNotas() != null) {
            logger.info("Actualizando notas de la estancia");
            estancia.setNotas(estancia.getNotas() + " | Notas editadas: " + request.getNotas());
        }

        return estanciaRepository.save(estancia);
    }

    public Void eliminarEstancia(Long idEstancia) {
        logger.info("Eliminando estancia con id: {}", idEstancia);
        Estancia estancia = estanciaRepository.findById(idEstancia)
                .orElseThrow(() -> new IllegalArgumentException("Estancia no encontrada con id: " + idEstancia));

        String codigoUnidad = estancia.getEstanciaHabitaciones().getFirst().getHabitacion().getUnidad().getCodigo();
        TipoUnidad tipoUnidad = estancia.getEstanciaHabitaciones().getFirst().getHabitacion().getUnidad().getTipo();

        habitacionService.cambiarEstadoHabitaciones(codigoUnidad, EstadoOperativo.DISPONIBLE , tipoUnidad);
        estanciaHabitacionService.vaciarHabitaciones(estancia);

        estancia.setActivo(false);
        estanciaRepository.save(estancia);
        return null;
    }

    public EstanciaDTO obtenerEstancia(String codigo, TipoUnidad tipoUnidad) {
        logger.info("[obtenerEstanciaPorUnidad] Obteniendo estancia para la unidad con codigo: {}", codigo);

        Estancia estancia;
        if (tipoUnidad == TipoUnidad.HABITACION) {
            logger.info("[obtenerEstanciaPorUnidad] Buscando estancia por habitacion");
            Habitacion habitacion = habitacionService.buscarPorCodigo(codigo);
            estancia = obtenerEstanciaPorHabitacion(habitacion.getId());

        } else {
            Unidad unidad = unidadService.buscarPorCodigo(codigo);
            String codigoHabitacion = unidad.getHabitaciones().getFirst().getCodigo();
            logger.info("[obtenerEstanciaPorUnidad] Buscando estancia por habitacion asociada a la unidad, codigoHabitacion: {}", codigoHabitacion);
            Habitacion habitacion = habitacionService.buscarPorCodigo(codigoHabitacion);
            estancia = obtenerEstanciaPorHabitacion(habitacion.getId());
        }

        return EstanciaMapper.entityToDTO(estancia);
    }

    private Estancia obtenerEstanciaPorHabitacion(Long idHabitacion) {
        logger.info("[obtenerEstanciaPorHabitacion] Obteniendo estancia para la habitacion con id: {}", idHabitacion);
        Estancia estancia = estanciaHabitacionService.obtenerEstanciaActivaPorHabitacionId(idHabitacion);

        return estancia;
    }

    private ModoOcupacion determinarModoOcupacion(TipoUnidad tipoUnidad) {

        return tipoUnidad == TipoUnidad.HABITACION ? ModoOcupacion.INDIVIDUAL : ModoOcupacion.COMPLETO;
    }


    private List<Ocupante> cargarOcupantesAdicionales(List<Long> idsAcompanantes) {
        List<Ocupante> adicionales = new ArrayList<>();

        if (idsAcompanantes == null) {
            return adicionales;
        }

        for (Long id : idsAcompanantes) {
            Ocupante ocupante = ocupanteService.buscarPorId(id);
            TipoOcupante tipoOcupante = ocupante.getTipoOcupante();

            if (tipoOcupante != TipoOcupante.ACOMPANANTE) {
                adicionales.add(ocupanteService.crearOcupanteConDiferenteTipo(ocupante, TipoOcupante.ACOMPANANTE));
            } else {
                adicionales.add(ocupante);
            }
        }

        return adicionales;
    }

    private List<Ocupante> reemplazarTitularEnOcupantes(Estancia estancia, Long idCliente) {
        List<Ocupante> ocupantes = new ArrayList<>();

        for (Ocupante ocupante : estancia.getOcupantes()) {
            if (ocupante.getTipoOcupante() == TipoOcupante.CLIENTE) {
                Ocupante titular = ocupanteService.buscarPorId(idCliente);
                ocupantes.add(titular);
            } else {
                ocupantes.add(ocupante);
            }
        }

        return ocupantes;
    }

    private List<Ocupante> reemplazarAdicionalesEnOcupantes(Estancia estancia, List<Long> idsAcompanantes) {
        List<Ocupante> ocupantes = new ArrayList<>();

        for (Ocupante ocupante : estancia.getOcupantes()) {
            if (ocupante.getTipoOcupante() == TipoOcupante.ACOMPANANTE) {
                for (Long id : idsAcompanantes) {
                    Ocupante adicional = ocupanteService.buscarPorId(id);
                    ocupantes.add(adicional);
                }
            } else {
                ocupantes.add(ocupante);
            }
        }

        return ocupantes;
    }

    private List<Ocupante> reemplazarTitularYAdicionalesEnOcupantes(Long idCliente, List<Long> idsAcompanantes) {
        List<Ocupante> ocupantes = new ArrayList<>();

        Ocupante titular = ocupanteService.buscarPorId(idCliente);
        ocupantes.add(titular);

        for (Long id : idsAcompanantes) {
            Ocupante adicional = ocupanteService.buscarPorId(id);
            ocupantes.add(adicional);
        }

        return ocupantes;
    }

}
