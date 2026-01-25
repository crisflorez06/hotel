package com.hotel.services;

import com.hotel.dtos.EstanciaDTO;
import com.hotel.dtos.EstanciaEditarRequestDTO;
import com.hotel.dtos.EstanciaNuevoRequestDTO;
import com.hotel.dtos.OcupanteDTO;
import com.hotel.mappers.EstanciaMapper;
import com.hotel.mappers.OcupanteMapper;
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
    private final ClienteService clienteService;
    private final HabitacionService habitacionService;
    private final AcompananteService acompananteService;
    private final EstanciaHabitacionService estanciaHabitacionService;
    private final PagoService pagoService;
    private final UnidadService unidadService;


    public EstanciaService(EstanciaRepository estanciaRepository,
                           ClienteService clienteService,
                           AcompananteService acompananteService,
                           HabitacionService habitacionService,
                           EstanciaHabitacionService estanciaHabitacionService,
                           PagoService pagoService,
                           UnidadService unidadService) {
        this.estanciaRepository = estanciaRepository;
        this.clienteService = clienteService;
        this.acompananteService = acompananteService;
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

        logger.info("Creando estancia para el cliente con ID: {}", request.getIdCliente());
        Estancia estancia = EstanciaMapper.requestNuevoToEntity(request);

        logger.info("Buscando cliente con ID: {}", request.getIdCliente());
        estancia.setCliente(clienteService.buscarPorId(request.getIdCliente()));

        logger.info("llenando habitaciones para la estancia");
        estancia.setEstanciaHabitaciones(estanciaHabitacionService.llenarHabitaciones(estancia, request.getCodigo(), request.getTipoUnidad()));

        logger.info("Determinando modo de ocupacion para la estancia");
        estancia.setModoOcupacion(determinarModoOcupacion(request.getTipoUnidad()));

        logger.info("Cambiando estado de las habitaciones asociadas a la estancia a OCUPADO");
        habitacionService.cambiarEstadoHabitaciones(request.getCodigo(), EstadoOperativo.OCUPADO, request.getTipoUnidad());

        logger.info("Llenando acompañantes para la estancia");
        estancia.setEstanciaAcompanantes(llenarAcompanantes(estancia, request.getIdAcompanantes()));

        logger.info("informacion completa de la estancia creada, idEstancia: {}, clienteId: {}, tipo de ocupacion: {},numeroHabitaciones: {}, numeroAcompanantes: {}",
                estancia.getId(),
                estancia.getCliente().getId(),
                estancia.getModoOcupacion(),
                estancia.getEstanciaHabitaciones().size(),
                estancia.getEstanciaAcompanantes().size()
        );

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

        if (request.getIdCliente() != null) {
            logger.info("Actualizando cliente de la estancia");
            estancia.setCliente(clienteService.buscarPorId(request.getIdCliente()));
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

        EstanciaDTO estanciaDTO = EstanciaMapper.entityToDTO(estancia);
        estanciaDTO.setOcupantes(obtenerOcupantesDeEstancia(estancia.getId()));


        return estanciaDTO;
    }

    private Estancia obtenerEstanciaPorHabitacion(Long idHabitacion) {
        logger.info("[obtenerEstanciaPorHabitacion] Obteniendo estancia para la habitacion con id: {}", idHabitacion);
        Estancia estancia = estanciaHabitacionService.obtenerEstanciaActivaPorHabitacionId(idHabitacion);

        logger.info("Estancia obtenida: idEstancia: {}, clienteId: {}", estancia.getId(), estancia.getCliente().getId());
        return estancia;
    }

    private List<OcupanteDTO> obtenerOcupantesDeEstancia(Long idEstancia) {
        logger.info("[obtenerOcupantesDeEstancia] Obteniendo ocupantes para la estancia con id: {}", idEstancia);
        Estancia estancia = estanciaRepository.findById(idEstancia)
                .orElseThrow(() -> new IllegalArgumentException("Estancia no encontrada con id: " + idEstancia));

        List<Acompanante> acompanantes = acompananteService.buscarPorEstanciaId(idEstancia);
        Cliente cliente = clienteService.buscarClientePorEstancia(idEstancia);

        List<OcupanteDTO> ocupantes = OcupanteMapper.listaAcompananteToOcupanteDto(acompanantes);
        ocupantes.add(OcupanteMapper.clienteToOcupanteDto(cliente));

        return ocupantes;
    }




    private ModoOcupacion determinarModoOcupacion(TipoUnidad tipoUnidad) {

        return tipoUnidad == TipoUnidad.HABITACION ? ModoOcupacion.INDIVIDUAL : ModoOcupacion.COMPLETO;
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
