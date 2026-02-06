package com.hotel.resolvers;

import com.hotel.dtos.pago.CalcularPagoDTO;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.PagoRepository;
import com.hotel.services.TarifaBaseService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class PagoResolver {

    private final TarifaBaseService tarifaBaseService;
    private final PagoRepository pagoRepository;
    private final Logger logger = org.slf4j.LoggerFactory.getLogger(PagoResolver.class);

    public PagoResolver(TarifaBaseService tarifaBaseService, PagoRepository pagoRepository) {
        this.pagoRepository = pagoRepository;
        this.tarifaBaseService = tarifaBaseService;
    }

    public double calcularEstimacionPago(CalcularPagoDTO request) {
        logger.info("[PagoResolver.calcularTotalPagos] Calculando total de pagos para el request: {}", request);

        logger.info("[PagoResolver.calcularTotalPagos] verificando si existe pago anterior en la reserva");
        double pagoReserva = request.getIdPagoReserva() != null ?
                pagoRepository.findById(request.getIdPagoReserva())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Pago no encontrado con id: " + request.getIdPagoReserva()
                        ))
                        .getMonto()
                        .doubleValue() : 0.0;
        logger.info("[PagoResolver.calcularTotalPagos] pago anterior en la reserva: {}", pagoReserva);

        logger.info("[PagoResolver.calcularTotalPagos] verificando si existe pago anterior en la estancia");
        double pagoEstancia = request.getIdPagoEstancia() != null ?
                pagoRepository.findById(request.getIdPagoEstancia())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Pago no encontrado con id: " + request.getIdPagoEstancia()
                        ))
                        .getMonto()
                        .doubleValue() : 0.0;
        logger.info("[PagoResolver.calcularTotalPagos] pago anterior en la estancia: {}", pagoEstancia);


        logger.info("[PagoResolver.calcularTotalPagos] obteniendo tarifa base por tipo de unidad");
        TipoUnidad tipoUnidad = request.getTipoUnidad();

        BigDecimal precioDia = tarifaBaseService.obtenerPrecioDiaPorTipoUnidad(tipoUnidad);

        logger.info("[PagoResolver.calcularTotalPagos] calculando total de dias entre {} y {}", request.getFechaEntrada(), request.getFechaSalida());
        Long totalDias = calcularDias(request.getFechaEntrada(), request.getFechaSalida());

        logger.info("[PagoResolver.calcularTotalPagos] calculando precio por personas adicionales si aplica");
        BigDecimal precioPersonasAdicionales = BigDecimal.ZERO;

        if(request.getNumeroPersonas() > 2) {
            precioPersonasAdicionales = calcularPrecioPersonaAdicional(
                    tipoUnidad,
                    totalDias,
                    request.getNumeroPersonas()
            );
        }
        logger.info("[PagoResolver.calcularTotalPagos] precio por personas adicionales: {}", precioPersonasAdicionales);

        BigDecimal totalPago = precioDia
                .multiply(BigDecimal.valueOf(totalDias))
                .add(precioPersonasAdicionales)
                .subtract(BigDecimal.valueOf(pagoReserva))
                .subtract(BigDecimal.valueOf(pagoEstancia));
        logger.info("[PagoResolver.calcularTotalPagos] total pago calculado: {}", totalPago);

        return totalPago.doubleValue();

    }

    private Long calcularDias(LocalDateTime fechaEntrada, LocalDateTime fechaSalida) {

        logger.info("[PagoResolver.calcularDias] Calculando dias entre {} y {}", fechaEntrada, fechaSalida);
        if (!fechaEntrada.isBefore(fechaSalida)) {
            throw new IllegalArgumentException("La fecha de entrada debe ser anterior a la salida");
        }

        return ChronoUnit.DAYS.between(
                fechaEntrada.toLocalDate(),
                fechaSalida.toLocalDate()
        );
    }

    private BigDecimal calcularPrecioPersonaAdicional(TipoUnidad tipoUnidad, Long totalDias, Integer numeroPersonas) {
        logger.info("[PagoResolver.calcularPrecioPersonaAdicional] Calculando precio por personas adicionales para tipoUnidad: {}, totalDias: {}, numeroPersonas: {}", tipoUnidad, totalDias, numeroPersonas);
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
