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
import com.hotel.resolvers.PagoResolver;
import com.hotel.specifications.PagoSpecification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.hotel.utils.EventoModificadoJsonBuilder;
import com.hotel.utils.EventoNuevoJsonBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PagoService {

    private static final Logger logger = LoggerFactory.getLogger(PagoService.class);

    private final PagoRepository pagoRepository;
    private final PagoResolver pagoResolver;
    private final AuditoriaEventoService eventoService;


    public PagoService(PagoRepository pagoRepository,
                       PagoResolver pagoResolver,
                       AuditoriaEventoService eventoService) {
        this.eventoService = eventoService;
        this.pagoResolver = pagoResolver;
        this.pagoRepository = pagoRepository;
    }

    @Transactional
    public Pago crearPago(PagoNuevoRequestDTO request, Estancia estancia) {
        if (request == null) {
            logger.info("[crearPago] No se proporcionó información de pago. Se omite la creación de pago.");
            return null;
        }
        logger.info("[crearPago] Iniciando creación de pago con tipo: {}", request.getTipoPago());

        Pago pago = PagoMapper.requestNuevoToEntity(request);

        pago.setEstancia(estancia);

        EventoNuevoJsonBuilder nuevoPagoJson = new EventoNuevoJsonBuilder()
                .agregarProp("codigoEstancia", estancia.getCodigoFolio())
                .agregarProp("tipoPago", request.getTipoPago())
                .agregarProp("monto", request.getMonto())
                .agregarProp("estado", pago.getEstado());

        Pago pagoCreado = pagoRepository.save(pago);

        String codigoEstancia;
        if(estancia.getEstado().equals(EstadoEstancia.RESERVADA)){
            codigoEstancia = null;
        } else {
            codigoEstancia = estancia.getCodigoFolio();
        }

        eventoService.crearEvento(
                TipoEvento.CREACION_PAGO,
                TipoEntidad.PAGO,
                pagoCreado.getId(),
                nuevoPagoJson.build(),
                codigoEstancia,
                estancia.getReserva() != null ? estancia.getReserva().getCodigo() : null
        );


        return pagoCreado;
    }

    @Transactional
    public Pago reemplazarPago(PagoNuevoRequestDTO request, Pago pagoAnterior, Estancia estancia) {
        if (request == null) {
            logger.info("[reemplazarPago] No se proporcionó información de pago. Se omite el reemplazo.");
            return null;
        }

        if (pagoAnterior == null) {
            logger.info("[reemplazarPago] No existe pago anterior. Se crea un pago nuevo.");

            return crearPago(request, estancia);
        }

        logger.info("[reemplazarPago] Reemplazando pago con id: {}", pagoAnterior.getId());
        pagoAnterior.setEstado(EstadoPago.MODIFICADO);

        EventoModificadoJsonBuilder pagoModificadoJson = new EventoModificadoJsonBuilder()
                .agregarCambio("codigoEstancia", estancia.getCodigoFolio(), estancia.getCodigoFolio())
                .agregarCambio("tipoPago", pagoAnterior.getTipoPago(), request.getTipoPago())
                .agregarCambio("monto", pagoAnterior.getMonto(), request.getMonto());

        Pago pagoModificado = pagoRepository.save(pagoAnterior);

        Pago pagoNuevo = PagoMapper.requestNuevoToEntity(request);
        pagoNuevo.setEstancia(estancia);
        Pago pagoCreado = pagoRepository.save(pagoNuevo);

        String codigoEstancia;
        if(estancia.getEstado().equals(EstadoEstancia.RESERVADA)){
            codigoEstancia = null;
        } else {
            codigoEstancia = estancia.getCodigoFolio();
        }

        eventoService.crearEvento(
                TipoEvento.MODIFICACION_PAGO,
                TipoEntidad.PAGO,
                pagoModificado.getId(),
                pagoModificadoJson.build(),
                codigoEstancia,
                estancia.getReserva() != null ? estancia.getReserva().getCodigo() : null
        );


        return pagoCreado;
    }

    public Optional<Pago> buscarUltimoPagoPorEstanciaYTipo(Long idEstancia, TipoPago tipoPago) {
        if (idEstancia == null) {
            return Optional.empty();
        }
        return pagoRepository.findFirstByEstanciaIdAndTipoPagoOrderByFechaCreacionDesc(idEstancia, tipoPago);
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

    public Double obtenerEstimacionPago(CalcularPagoDTO request) {
        logger.info("[obtenerEstimacionPago] Calculando estimación de pago para el request: {}", request);
        return pagoResolver.calcularEstimacionPago(request);
    }

    public void eliminarPagos(Long idEstancia) {
        logger.info("[eliminarPagosPorEstancia] Eliminando pagos asociados a la estancia con id: {}", idEstancia);
        List<Pago> pagos = pagoRepository.findByEstanciaId(idEstancia);
        for(Pago pago : pagos) {
            logger.info("[eliminarPagosPorEstancia] Eliminando pago con id: {}", pago.getId());
            Estancia estancia = pago.getEstancia();
            pago.setEstado(EstadoPago.ELIMINADO);

            logger.info("[eliminarPagos] Registrando evento de eliminación de pagos para codigo de estancia: {}", estancia.getCodigoFolio());

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

        if (pago.getTipoPago() == TipoPago.ANTICIPO_RESERVA) {
            if (reserva != null) {
                dto.setCodigoReserva(reserva.getCodigo());
            }
            if (estanciaActivada) {
                dto.setCodigoEstancia(estancia.getCodigoFolio());
            }
            return dto;
        }

        if (pago.getTipoPago() == TipoPago.ANTICIPO_ESTANCIA
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
