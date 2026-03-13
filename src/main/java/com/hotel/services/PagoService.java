package com.hotel.services;

import com.hotel.dtos.pago.CalcularPagoDTO;
import com.hotel.dtos.pago.PagoDTO;
import com.hotel.dtos.pago.PagoNuevoRequestDTO;
import com.hotel.mappers.PagoMapper;
import com.hotel.models.Estancia;
import com.hotel.models.Pago;
import com.hotel.models.Reserva;
import com.hotel.models.enums.*;
import com.hotel.repositories.PagoRepository;
import com.hotel.resolvers.EstanciaReservaResolver;
import com.hotel.resolvers.PagoResolver;
import com.hotel.specifications.PagoSpecification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.hotel.utils.EventoModificadoJsonBuilder;
import com.hotel.utils.EventoNuevoJsonBuilder;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.hotel.models.enums.TipoPago.ANTICIPO_ESTANCIA;
import static com.hotel.models.enums.TipoPago.ANTICIPO_RESERVA;

@Service
public class PagoService {

    private static final Logger logger = LoggerFactory.getLogger(PagoService.class);

    private final PagoRepository pagoRepository;
    private final PagoResolver pagoResolver;
    private final AuditoriaEventoService eventoService;
    private final EstanciaReservaResolver estanciaReservaResolver;

    public PagoService(PagoRepository pagoRepository,
                       PagoResolver pagoResolver,
                       AuditoriaEventoService eventoService,
                       EstanciaReservaResolver estanciaReservaResolver) {
        this.estanciaReservaResolver = estanciaReservaResolver;
        this.eventoService = eventoService;
        this.pagoResolver = pagoResolver;
        this.pagoRepository = pagoRepository;
    }

    @Transactional
    public PagoDTO crearPago(PagoNuevoRequestDTO request, Long idEstancia, boolean crearEvento) {
        if (request == null || idEstancia == null) {
            throw new IllegalArgumentException("No se proporcionó información de pago para crear un nuevo pago");
        }
        logger.info("[crearPago] Iniciando creación de pago con tipo: {}", request.getTipoPago());
        if(request.getTipoPago() == null) {
            throw new IllegalArgumentException("El tipo de pago es obligatorio para crear un pago");
        }

        Estancia estancia = estanciaReservaResolver.buscarEstanciaPorId(idEstancia);
        EstadoEstancia estadoEstancia = estancia.getEstado();

        Pago pago = PagoMapper.requestNuevoToEntity(request);

        verificarPago(request.getTipoPago(), estancia);

        if( request.getTipoPago() == TipoPago.ESTANCIA_COMPLETADA) {
            validarEstadoPagoEstanciaCompletada(request);
            pago.setEstado(request.getEstado());
        }


        pago.setEstancia(estancia);

        EventoNuevoJsonBuilder nuevoPagoJson = new EventoNuevoJsonBuilder()
                .agregarProp("codigoEstancia", estancia.getCodigoFolio())
                .agregarProp("tipoPago", request.getTipoPago())
                .agregarProp("monto", request.getMonto())
                .agregarProp("estado", pago.getEstado());

        Pago pagoCreado = pagoRepository.save(pago);

        String codigoEstancia;
        if(estadoEstancia.equals(EstadoEstancia.RESERVADA)){
            codigoEstancia = null;
        } else {
            codigoEstancia = estancia.getCodigoFolio();
        }

        if(!crearEvento) {
            return mapearPagoDTOConCodigo(pagoCreado);
        }

        eventoService.crearEvento(
                TipoEvento.CREACION_PAGO,
                TipoEntidad.PAGO,
                pagoCreado.getId(),
                nuevoPagoJson.build(),
                codigoEstancia,
                estancia.getReserva() != null ? estancia.getReserva().getCodigo() : null
        );


        return mapearPagoDTOConCodigo(pagoCreado);
    }

    public void validarPagoEstanciaCompletadaParaFinalizacion(PagoNuevoRequestDTO request, Estancia estancia) {
        if (request == null || estancia == null) {
            throw new IllegalArgumentException("No se proporcionó información de pago para finalizar la estancia");
        }
        if (request.getTipoPago() == null) {
            throw new IllegalArgumentException("El tipo de pago es obligatorio para crear un pago");
        }
        if (request.getTipoPago() != TipoPago.ESTANCIA_COMPLETADA) {
            throw new IllegalArgumentException("El tipo de pago para finalizar una estancia debe ser ESTANCIA_COMPLETADA");
        }

        validarEstadoPagoEstanciaCompletada(request);

        if (existePagoEstanciaCompleta(estancia.getId())) {
            throw new IllegalStateException("Ya existe un pago de tipo ESTANCIA_COMPLETADA para esta estancia, no se puede crear otro");
        }
    }

    @Transactional
    public Pago crearPagoPorCambioUnidad(Estancia estancia, TipoUnidad tipoUnidadAnterior) {
        if (estancia == null || tipoUnidadAnterior == null) {
            logger.info("[crearPagoPorCambioUnidad] No se proporcionó información de estancia o tipo de unidad");
            throw new IllegalArgumentException("No se proporcionó información de estancia o tipo de unidad");
        }
        if(estancia.getEstado() != EstadoEstancia.ACTIVA && estancia.getEstado() != EstadoEstancia.EXCEDIDA) {
            logger.info("[crearPagoPorCambioUnidad] No se puede crear un pago por cambio de unidad para una estancia que no está ACTIVA o EXCEDIDA. El estado actual de la estancia es: {}", estancia.getEstado());
            throw new IllegalStateException("No se puede crear un pago por cambio de unidad para una estancia que no está ACTIVA o EXCEDIDA. El estado actual de la estancia es: " + estancia.getEstado());
        }

        logger.info("[crearPagoPorCambioUnidad] Creando pago por cambio de unidad para estancia con codigo: {} y nuevo tipo de unidad: {}", estancia.getCodigoFolio(), tipoUnidadAnterior);
        Integer totalOcupantes = estancia.getOcupantes().size();
        LocalDateTime fechaEntrada = estancia.getEntradaReal();
        LocalDateTime fechaFinalizacion = LocalDateTime.now();

        logger.info("[crearPagoPorCambioUnidad] Preparando datos para cálculo de pago por cambio de unidad. Total ocupantes: {}, Fecha entrada: {}, Fecha finalización: {}", totalOcupantes, fechaEntrada, fechaFinalizacion);
        CalcularPagoDTO calcularPagoDTO = PagoMapper.entityToCalcularPagoDTO(estancia.getId(), tipoUnidadAnterior, totalOcupantes, fechaEntrada, fechaFinalizacion);

        BigDecimal monto = obtenerEstimacionPagoSinPagosAnteriores(calcularPagoDTO);

        logger.info("[crearPagoPorCambioUnidad] Monto calculado para pago por cambio de unidad: {}", monto);
        Pago pago = PagoMapper.cambioUnidadToEntity(monto, fechaFinalizacion, estancia);

        EventoNuevoJsonBuilder nuevoPagoJson = new EventoNuevoJsonBuilder()
                .agregarProp("codigoEstancia", estancia.getCodigoFolio())
                .agregarProp("tipoPago", pago.getTipoPago())
                .agregarProp("monto", monto);


        Pago pagoCreado = pagoRepository.save(pago);

        eventoService.crearEvento(
                TipoEvento.CREACION_PAGO,
                TipoEntidad.PAGO,
                pagoCreado.getId(),
                nuevoPagoJson.build(),
                estancia.getCodigoFolio(),
                null
        );


        return pagoCreado;
    }

    @Transactional(readOnly = true)
    public Page<PagoDTO> buscarPagos(
            List<EstadoPago> estados,
            List<MedioPago> mediosPago,
            TipoPago tipoPago,
            String codigoEstancia,
            String codigoReserva,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            Pageable pageable) {
        logger.info("[buscarPagos] Buscando pagos con filtros aplicados");

        Page<Pago> pagos = pagoRepository.findAll(
                PagoSpecification.byFilters(estados, mediosPago, tipoPago, codigoEstancia, codigoReserva, fechaDesde, fechaHasta),
                pageable
        );

        return pagos.map(this::mapearPagoDTOConCodigo);
    }

    public BigDecimal obtenerEstimacionPago(CalcularPagoDTO request) {
        logger.info("[obtenerEstimacionPago] Calculando estimación de pago para el request: {}", request);
        return pagoResolver.calcularEstimacionPago(request);
    }

    public BigDecimal obtenerEstimacionPagoSinPagosAnteriores(CalcularPagoDTO request) {
        logger.info("[obtenerEstimacionPagoSinPagosAnteriores] Calculando estimación de pago sin considerar pagos anteriores para el request: {}", request);
        return pagoResolver.calcularEstimacionPagoSinPagosAnteriores(request);
    }

    public void eliminarTodoLosPagos(Long idEstancia) {
        logger.info("[eliminarPagosPorEstancia] Eliminando pagos asociados a la estancia con id: {}", idEstancia);
        List<Pago> pagos = pagoRepository.findByEstanciaId(idEstancia);
        for(Pago pago : pagos) {
            logger.info("[eliminarPagosPorEstancia] Eliminando pago con id: {}", pago.getId());
            Estancia estancia = pago.getEstancia();
            pago.setEstado(EstadoPago.ELIMINADO);

            logger.info("[eliminarTodoLosPagos] Registrando evento de eliminación de pagos para codigo de estancia: {}", estancia.getCodigoFolio());

            EventoNuevoJsonBuilder estanciaEliminada = new EventoNuevoJsonBuilder()
                    .agregarProp("codigoEstancia", estancia.getCodigoFolio());


            String codigoEstancia;
            if(estancia.getEstado().equals(EstadoEstancia.RESERVADA)){
                codigoEstancia = null;
            } else {
                codigoEstancia = estancia.getCodigoFolio();
            }

            eventoService.crearEvento(
                    TipoEvento.ELIMINACION_PAGO,
                    TipoEntidad.PAGO,
                    pago.getId(),
                    estanciaEliminada.build(),
                    codigoEstancia,
                    estancia.getReserva() != null ? estancia.getReserva().getCodigo() : null
            );

        }

        pagoRepository.saveAll(pagos);
    }

    @Transactional
    public void eliminarPago(Long idPago) {
        logger.info("[eliminarPago] Eliminando pago con id: {}", idPago);
        Pago pago = pagoRepository.findById(idPago)
                .orElseThrow(() -> new EntityNotFoundException("Pago no encontrado con id: " + idPago));

        if (pago.getEstado() != EstadoPago.COMPLETADO && pago.getEstado() != EstadoPago.PENDIENTE) {
            throw new IllegalStateException("Solo se puede eliminar un pago en estado COMPLETADO o PENDIENTE. Estado actual: " + pago.getEstado());
        }

        if(pago.getTipoPago() == TipoPago.CAMBIO_UNIDAD) {
            throw new IllegalStateException("No se puede eliminar un pago de tipo CAMBIO_UNIDAD, este tipo de pago se maneja automáticamente por el sistema y no debe ser eliminado manualmente");
        }

        if(pago.getTipoPago() == TipoPago.ESTANCIA_COMPLETADA) {
            throw new IllegalStateException("No se puede eliminar un pago de tipo ESTANCIA_COMPLETADA, este tipo de pago se maneja automáticamente por el sistema y no debe ser eliminado manualmente");
        }

        Estancia estancia = pago.getEstancia();
        pago.setEstado(EstadoPago.ELIMINADO);
        pagoRepository.save(pago);

        logger.info("[eliminarPago] Registrando evento de eliminación de pago para codigo de estancia: {}", estancia.getCodigoFolio());

        EventoNuevoJsonBuilder pagoEliminadoJson = new EventoNuevoJsonBuilder()
                .agregarProp("codigoEstancia", estancia.getCodigoFolio());

        String codigoEstancia;
        if (estancia.getEstado().equals(EstadoEstancia.RESERVADA)) {
            codigoEstancia = null;
        } else {
            codigoEstancia = estancia.getCodigoFolio();
        }

        eventoService.crearEvento(
                TipoEvento.ELIMINACION_PAGO,
                TipoEntidad.PAGO,
                pago.getId(),
                pagoEliminadoJson.build(),
                codigoEstancia,
                estancia.getReserva() != null ? estancia.getReserva().getCodigo() : null
        );
    }

    @Transactional
    public void pagarPagoPendiente(Long idPago) {
        logger.info("[pagarPagoPendiente] Marcando pago con id: {} como COMPLETADO", idPago);
        Pago pago = pagoRepository.findById(idPago)
                .orElseThrow(() -> new EntityNotFoundException("Pago no encontrado con id: " + idPago));

        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            throw new IllegalStateException("Solo se puede pagar un pago en estado PENDIENTE. Estado actual: " + pago.getEstado());
        }

        if(pago.getTipoPago() == TipoPago.CAMBIO_UNIDAD) {
            throw new IllegalStateException("No se puede pagar un pago de tipo CAMBIO_UNIDAD, este tipo de pago se maneja automáticamente por el sistema y no debe ser pagado manualmente");
        }

        if(pago.getTipoPago() != TipoPago.ESTANCIA_COMPLETADA) {
            throw new IllegalStateException("Solo se pueden pagar manualmente los pagos de tipo ESTANCIA_COMPLETADA. El tipo de pago actual es: " + pago.getTipoPago());
        }

        Estancia estancia = pago.getEstancia();
        pago.setEstado(EstadoPago.COMPLETADO);
        pagoRepository.save(pago);

        logger.info("[pagarPagoPendiente] Registrando evento de pago completado para codigo de estancia: {}", estancia.getCodigoFolio());

        String pagoModificado = new EventoModificadoJsonBuilder()
                .agregarCambio("Monto", pago.getMonto(), pago.getMonto())
                .agregarCambio("Estado", EstadoPago.PENDIENTE, EstadoPago.COMPLETADO)
                .build();

        String codigoEstancia;
        if (estancia.getEstado().equals(EstadoEstancia.RESERVADA)) {
            codigoEstancia = null;
        } else {
            codigoEstancia = estancia.getCodigoFolio();
        }

        eventoService.crearEvento(
                TipoEvento.MODIFICACION_PAGO,
                TipoEntidad.PAGO,
                pago.getId(),
                pagoModificado,
                codigoEstancia,
                estancia.getReserva() != null ? estancia.getReserva().getCodigo() : null
        );
    }

    @Transactional
    public void eliminarPagoEstanciaCompletada(PagoNuevoRequestDTO requestDTO, long idPago){
        logger.info("[eliminarPagoEstanciaCompletada] Eliminando pago de tipo ESTANCIA_COMPLETADA con id: {}", idPago);
        Pago pago = pagoRepository.findById(idPago)
                .orElseThrow(() -> new EntityNotFoundException("Pago no encontrado con id: " + idPago));

        if(pago.getTipoPago() != TipoPago.ESTANCIA_COMPLETADA) {
            throw new IllegalStateException("El pago con id: " + idPago + " no es de tipo ESTANCIA_COMPLETADA, por lo tanto no se puede eliminar con este método");
        }

        if (pago.getEstado() != EstadoPago.COMPLETADO && pago.getEstado() != EstadoPago.PENDIENTE) {
            throw new IllegalStateException("Solo se puede eliminar un pago en estado COMPLETADO o PENDIENTE. Estado actual: " + pago.getEstado());
        }

        if(requestDTO != null && requestDTO.getTipoPago() != TipoPago.ESTANCIA_COMPLETADA) {
            throw new IllegalArgumentException("El tipo de pago en el request no es ESTANCIA_COMPLETADA, no se puede eliminar el pago con id: " + idPago);
        }


        Estancia estancia = pago.getEstancia();
        pago.setEstado(EstadoPago.ELIMINADO);
        pagoRepository.save(pago);


        logger.info("[eliminarPagoEstanciaCompletada] Registrando evento de eliminación de pago de tipo ESTANCIA_COMPLETADA para codigo de estancia: {}", estancia.getCodigoFolio());

        String pagoModificado = new EventoModificadoJsonBuilder()
                .agregarCambio("Monto", pago.getMonto(), requestDTO.getMonto())
                .agregarCambio("Estado", pago.getEstado(), requestDTO.getEstado())
                .build();

        String codigoEstancia;
        if (estancia.getEstado().equals(EstadoEstancia.RESERVADA)) {
            codigoEstancia = null;
        } else {
            codigoEstancia = estancia.getCodigoFolio();
        }

        eventoService.crearEvento(
                TipoEvento.MODIFICACION_PAGO,
                TipoEntidad.PAGO,
                pago.getId(),
                pagoModificado,
                codigoEstancia,
                estancia.getReserva() != null ? estancia.getReserva().getCodigo() : null
        );

        crearPago(requestDTO, estancia.getId(), false);

    }

    private void verificarPago(TipoPago tipoPago, Estancia estancia) {
         EstadoEstancia estadoEstancia = estancia.getEstado();

        switch (tipoPago) {
            case ANTICIPO_RESERVA:
                if(estancia.getReserva() == null) {
                    throw new IllegalStateException("No se puede crear un pago de tipo ANTICIPO_RESERVA para una estancia que no tiene una reserva asociada");
                }
                Reserva reserva = estancia.getReserva();
                EstadoReserva estadoReserva = reserva.getEstado();
                if(!(estadoReserva == EstadoReserva.CONFIRMADA || estadoReserva == EstadoReserva.EXPIRADA) || estadoEstancia != EstadoEstancia.RESERVADA) {
                    throw new IllegalStateException("No se puede crear un pago de tipo ANTICIPO_RESERVA para una reserva que no está CONFIRMADA. El estado actual de la reserva es: " + reserva.getEstado());
                }
                break;

            case ANTICIPO_ESTANCIA:

                if(!(estadoEstancia == EstadoEstancia.ACTIVA || estadoEstancia == EstadoEstancia.EXCEDIDA)) {
                    throw new IllegalStateException("No se puede crear un pago de tipo ANTICIPO_ESTANCIA para una estancia que no está activada. El estado actual de la estancia es: " + estancia.getEstado());
                }
                break;

            case ESTANCIA_COMPLETADA:


                if(estadoEstancia != EstadoEstancia.FINALIZADA) {
                    throw new IllegalStateException("No se puede crear un pago de tipo ESTANCIA_COMPLETADA para una estancia que no está FINALIZADA. El estado actual de la estancia es: " + estancia.getEstado());
                }
                if(existePagoEstanciaCompleta(estancia.getId())) {

                    throw new IllegalStateException("Ya existe un pago de tipo ESTANCIA_COMPLETADA para esta estancia, no se puede crear otro");

                }

                break;
            default:
                throw new IllegalStateException("El tipo de pago " + tipoPago + " no es válido para crear un pago");
        }
    }


    private boolean existePagoEstanciaCompleta(Long idEstancia) {
        List<Pago> pagoCompletado = pagoRepository.findByEstanciaIdAndTipoPagoAndEstadoIn(
                idEstancia,
                TipoPago.ESTANCIA_COMPLETADA,
                List.of(EstadoPago .COMPLETADO, EstadoPago.PENDIENTE)
        );

        return pagoCompletado != null && !pagoCompletado.isEmpty();
    }

    private void validarEstadoPagoEstanciaCompletada(PagoNuevoRequestDTO request) {
        if(request.getEstado() == null ) {
            throw new IllegalArgumentException("El estado del pago es obligatorio para crear un pago de tipo ESTANCIA_COMPLETADA");
        }
        if(request.getEstado() != EstadoPago.COMPLETADO && request.getEstado() != EstadoPago.PENDIENTE) {
            throw new IllegalArgumentException("El estado del pago de tipo ESTANCIA_COMPLETADA debe ser COMPLETADO o PENDIENTE");
        }
    }

    public BigDecimal sumarTotalPagosPorEstancia(Long idEstancia) {
        logger.info("[sumarTotalPagosPorEstancia] Sumando total de pagos para la estancia con id: {}", idEstancia);

        return pagoRepository.sumarMontoPorEstanciaYEstados(
                idEstancia,
                List.of(EstadoPago.PENDIENTE, EstadoPago.COMPLETADO)
        );
    }

    private PagoDTO mapearPagoDTOConCodigo(Pago pago) {
        PagoDTO dto = PagoMapper.entityToDTO(pago);
        Estancia estancia = pago.getEstancia();
        if (estancia == null) {
            return dto;
        }

        Reserva reserva = estancia.getReserva();
        boolean estanciaActivada = estaEstanciaActivada(estancia);

        if (pago.getTipoPago() == ANTICIPO_RESERVA) {
            if (reserva != null) {
                dto.setCodigoReserva(reserva.getCodigo());
            }
            if (estanciaActivada) {
                dto.setCodigoEstancia(estancia.getCodigoFolio());
            }
            return dto;
        }

        if (pago.getTipoPago() == ANTICIPO_ESTANCIA
                || pago.getTipoPago() == TipoPago.ESTANCIA_COMPLETADA) {
            dto.setCodigoEstancia(estancia.getCodigoFolio());
            return dto;
        }

        dto.setCodigoEstancia(estancia.getCodigoFolio());
        return dto;
    }

    private boolean estaEstanciaActivada(Estancia estancia) {
        return estancia.getEstado() != null && estancia.getEstado() != EstadoEstancia.RESERVADA;
    }


}
