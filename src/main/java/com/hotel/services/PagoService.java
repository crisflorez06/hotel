package com.hotel.services;

import com.hotel.dtos.CalcularPagoDTO;
import com.hotel.dtos.PagoDTO;
import com.hotel.dtos.PagoNuevoRequestDTO;
import com.hotel.mappers.PagoMapper;
import com.hotel.models.Estancia;
import com.hotel.models.Pago;
import com.hotel.models.Reserva;
import com.hotel.models.enums.EstadoPago;
import com.hotel.models.enums.MedioPago;
import com.hotel.models.enums.TipoPago;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.EstanciaRepository;
import com.hotel.repositories.PagoRepository;
import com.hotel.repositories.ReservaRepository;
import com.hotel.specifications.PagoSpecification;
import jakarta.persistence.EntityNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
    private final ReservaRepository reservaRepository;
    private final TarifaBaseService tarifaBaseService;


    public PagoService(PagoRepository pagoRepository,
                       EstanciaRepository estanciaRepository,
                       ReservaRepository reservaRepository,
                       TarifaBaseService tarifaBaseService) {
        this.tarifaBaseService = tarifaBaseService;
        this.pagoRepository = pagoRepository;
        this.estanciaRepository = estanciaRepository;
        this.reservaRepository = reservaRepository;
    }

    @Transactional
    public Pago crearPago(PagoNuevoRequestDTO request, Long codigoDeTipoPago) {
        logger.info("[crearPago] Iniciando creación de pago con tipo: {}", request.getTipoPago());

        Pago pago = PagoMapper.requestNuevoToEntity(request);

        if (request.getTipoPago() == TipoPago.ESTANCIA) {
            logger.info("Registrando pago para estancia con id: {}", codigoDeTipoPago);
            Estancia estancia = estanciaRepository.findById(codigoDeTipoPago)
                    .orElseThrow(() -> new EntityNotFoundException("Estancia no encontrada con id: " + codigoDeTipoPago));

            if (pagoRepository.existsByEstanciaId(estancia.getId())) {
                throw new IllegalStateException("Ya existe un pago para la estancia con id: " + estancia.getId());
            }

            pago.setEstancia(estancia);
        } else {
            logger.info("Registrando pago para reserva con id: {}", codigoDeTipoPago);
            Reserva reserva = reservaRepository.findById(codigoDeTipoPago)
                    .orElseThrow(() -> new EntityNotFoundException("Reserva no encontrada con id: " + codigoDeTipoPago));

            if (pagoRepository.existsByReservaId(reserva.getId())) {
                throw new IllegalStateException("Ya existe un pago para la reserva con id: " + reserva.getId());
            }

            pago.setReserva(reserva);
        }

        return pagoRepository.save(pago);
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

    public double calcularTotalPagos(CalcularPagoDTO request) {

        TipoUnidad tipoUnidad = request.getTipoUnidad();

        BigDecimal precioDia = tarifaBaseService.obtenerPrecioDiaPorTipoUnidad(tipoUnidad);

        Long totalDias = calcularDias(request.getFechaEntrada(), request.getFechaSalida());

        BigDecimal precioPersonasAdicionales = BigDecimal.ZERO;

        if(request.getNumeroPersonas() > 2) {
            precioPersonasAdicionales = calcularPrecioPersonaAdicional(
                    tipoUnidad,
                    totalDias,
                    request.getNumeroPersonas()
            );
        }

        BigDecimal totalPago = precioDia
                .multiply(BigDecimal.valueOf(totalDias))
                .add(precioPersonasAdicionales);

        return totalPago.doubleValue();

    }

    private Long calcularDias(LocalDateTime fechaEntrada, LocalDateTime fechaSalida) {

        if (!fechaEntrada.isBefore(fechaSalida)) {
            throw new IllegalArgumentException("La fecha de entrada debe ser anterior a la salida");
        }

        return ChronoUnit.DAYS.between(
                fechaEntrada.toLocalDate(),
                fechaSalida.toLocalDate()
        );
    }

    private BigDecimal calcularPrecioPersonaAdicional(TipoUnidad tipoUnidad, Long totalDias, Integer numeroPersonas) {
        if (numeroPersonas <= 2) {
            return BigDecimal.ZERO;
        }
        BigDecimal precioPersonaAdicional = tarifaBaseService.obtenerPrecioPersonaAdicional(tipoUnidad);
        int personasAdicionales = numeroPersonas - 2;
        return precioPersonaAdicional
                .multiply(BigDecimal.valueOf(personasAdicionales))
                .multiply(BigDecimal.valueOf(totalDias));
    }

}
