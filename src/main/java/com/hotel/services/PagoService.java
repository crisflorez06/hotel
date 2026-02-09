package com.hotel.services;

import com.hotel.dtos.pago.CalcularPagoDTO;
import com.hotel.dtos.pago.PagoDTO;
import com.hotel.dtos.pago.PagoNuevoRequestDTO;
import com.hotel.mappers.PagoMapper;
import com.hotel.models.Estancia;
import com.hotel.models.Pago;
import com.hotel.models.enums.EstadoPago;
import com.hotel.models.enums.MedioPago;
import com.hotel.models.enums.TipoPago;
import com.hotel.repositories.EstanciaRepository;
import com.hotel.repositories.PagoRepository;
import com.hotel.resolvers.PagoResolver;
import com.hotel.specifications.PagoSpecification;
import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
    private final EstanciaRepository estanciaRepository;
    private final PagoResolver pagoResolver;


    public PagoService(PagoRepository pagoRepository,
                       EstanciaRepository estanciaRepository,
                       PagoResolver pagoResolver) {
        this.pagoResolver = pagoResolver;
        this.pagoRepository = pagoRepository;
        this.estanciaRepository = estanciaRepository;
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

        return pagoRepository.save(pago);
    }

    @Transactional
    public Pago reemplazarPago(PagoNuevoRequestDTO request, Pago pagoAnterior) {
        if (request == null) {
            logger.info("[reemplazarPago] No se proporcionó información de pago. Se omite el reemplazo.");
            return null;
        }

        if (pagoAnterior == null) {
            logger.info("[reemplazarPago] No existe pago anterior. Se crea un pago nuevo.");
            Pago pagoNuevo = PagoMapper.requestNuevoToEntity(request);
            return pagoRepository.save(pagoNuevo);
        }

        logger.info("[reemplazarPago] Reemplazando pago con id: {}", pagoAnterior.getId());
        pagoAnterior.setEstado(EstadoPago.REEMBOLSADO);
        pagoRepository.save(pagoAnterior);

        Pago pagoNuevo = PagoMapper.requestNuevoToEntity(request);
        pagoNuevo.setEstancia(pagoAnterior.getEstancia());

        return pagoRepository.save(pagoNuevo);
    }

    public Optional<Pago> buscarUltimoPagoPorEstancia(Long idEstancia) {
        if (idEstancia == null) {
            return Optional.empty();
        }
        return pagoRepository.findFirstByEstanciaIdOrderByFechaCreacionDesc(idEstancia);
    }

    public Page<PagoDTO> buscarPagos(
            List<EstadoPago> estados,
            List<MedioPago> mediosPago,
            TipoPago tipoPago,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            Pageable pageable) {
        logger.info("[buscarPagos] Buscando pagos con filtros aplicados");

        Page<Pago> pagos = pagoRepository.findAll(
                PagoSpecification.byFilters(estados, mediosPago, tipoPago, fechaDesde, fechaHasta),
                pageable
        );

        return pagos.map(PagoMapper::entityToDTO);
    }

    public Double obtenerEstimacionPago(CalcularPagoDTO request) {
        logger.info("[obtenerEstimacionPago] Calculando estimación de pago para el request: {}", request);
        return pagoResolver.calcularEstimacionPago(request);
    }


    public PagoDTO mapearPagoAEstancia(Pago pagoEstancia) {
        return PagoMapper.entityToDTO(pagoEstancia);
    }






}
