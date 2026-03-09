package com.hotel.resolvers;

import com.hotel.dtos.pago.CalcularPagoDTO;
import com.hotel.models.Estancia;
import com.hotel.models.Pago;
import com.hotel.models.enums.EstadoPago;
import com.hotel.models.enums.TipoPago;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.PagoRepository;
import com.hotel.services.TarifaBaseService;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class PagoResolver {

    private final TarifaBaseService tarifaBaseService;
    private final PagoRepository pagoRepository;
    private final EstanciaReservaResolver estanciaReservaResolver;
    private final Logger logger = org.slf4j.LoggerFactory.getLogger(PagoResolver.class);

    public PagoResolver(TarifaBaseService tarifaBaseService, PagoRepository pagoRepository, EstanciaReservaResolver estanciaReservaResolver) {
        this.pagoRepository = pagoRepository;
        this.tarifaBaseService = tarifaBaseService;
        this.estanciaReservaResolver = estanciaReservaResolver;
    }

    public BigDecimal calcularEstimacionPago(CalcularPagoDTO request) {
        logger.info("[PagoResolver.calcularTotalPagos] Calculando total de pagos para el request: {}", request);

        BigDecimal pagoAnticipos = BigDecimal.ZERO;
        BigDecimal pagoPendientePorCambioUnidad = BigDecimal.ZERO;
        BigDecimal precioPersonasAdicionales = BigDecimal.ZERO;


        if(request.getIdEstancia() != null) {
            logger.info("[PagoResolver.calcularTotalPagos] Verificando estancia con id: {}", request.getIdEstancia());
            Estancia estancia = estanciaReservaResolver.buscarEstanciaPorId(request.getIdEstancia());
            if(estancia == null) {
                throw new EntityNotFoundException("Estancia no encontrada con id: " + request.getIdEstancia());
            }
            logger.info("[PagoResolver.calcularTotalPagos] Estancia encontrada: {}", estancia.getCodigoFolio());

            logger.info("[PagoResolver.calcularTotalPagos] verificando si existe pago por anticipos");
            List<Pago> pagos = pagoRepository.findByEstanciaId(request.getIdEstancia());

            if(pagos == null || pagos.isEmpty()) {
                logger.info("[PagoResolver.calcularTotalPagos] No se encontraron pagos asociados a la estancia con id: {}", request.getIdEstancia());

            } else {
                pagoAnticipos = pagos.stream()
                                .filter(p -> (p.getTipoPago() == TipoPago.ANTICIPO_RESERVA
                                        || p.getTipoPago() == TipoPago.ANTICIPO_ESTANCIA)
                                        && p.getEstado() == EstadoPago.COMPLETADO)
                                .map(Pago::getMonto)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                logger.info("[PagoResolver.calcularTotalPagos] Pago anticipos encontrado en estancia: {}", pagoAnticipos);

                pagoPendientePorCambioUnidad = pagos.stream()
                        .filter(p -> p.getTipoPago() == TipoPago.CAMBIO_UNIDAD
                                && p.getEstado() == EstadoPago.PENDIENTE)
                        .map(Pago::getMonto)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                logger.info("[PagoResolver.calcularTotalPagos] Pago pendiente por cambio de unidad encontrado en estancia: {}", pagoPendientePorCambioUnidad);
            }
        }

        logger.info("[PagoResolver.calcularTotalPagos] obteniendo tarifa base por tipo de unidad");
        TipoUnidad tipoUnidad = request.getTipoUnidad();

        BigDecimal precioDia = tarifaBaseService.obtenerPrecioDiaPorTipoUnidad(tipoUnidad);

        logger.info("[PagoResolver.calcularTotalPagos] calculando total de dias entre {} y {}", request.getFechaEntrada(), request.getFechaSalida());
        Long totalDias = calcularDias(request.getFechaEntrada(), request.getFechaSalida());

        logger.info("[PagoResolver.calcularTotalPagos] calculando precio por personas adicionales si aplica");

        if(request.getNumeroPersonas() > 2) {
            precioPersonasAdicionales = calcularPrecioPersonaAdicional(
                    tipoUnidad,
                    totalDias,
                    request.getNumeroPersonas()
            );
            logger.info("[PagoResolver.calcularTotalPagos] precio por personas adicionales: {}", precioPersonasAdicionales);
        }

        BigDecimal totalPago = precioDia
                .multiply(BigDecimal.valueOf(totalDias))
                .add(precioPersonasAdicionales)
                .add(pagoPendientePorCambioUnidad)
                .subtract(pagoAnticipos);
        logger.info("[PagoResolver.calcularTotalPagos] total pago calculado: {}", totalPago);

        return totalPago;

    }

    public BigDecimal calcularEstimacionPagoSinPagosAnteriores(CalcularPagoDTO request) {
        logger.info("[calcularEstimacionPagoSinPagosAnteriores] Calculando total de pagos para el request: {}", request);

        BigDecimal precioPersonasAdicionales = BigDecimal.ZERO;


        if(request.getIdEstancia() != null) {
            logger.info("[calcularEstimacionPagoSinPagosAnteriores] Verificando estancia con id: {}", request.getIdEstancia());
            Estancia estancia = estanciaReservaResolver.buscarEstanciaPorId(request.getIdEstancia());
            if(estancia == null) {
                throw new EntityNotFoundException("Estancia no encontrada con id: " + request.getIdEstancia());
            }
            logger.info("[calcularEstimacionPagoSinPagosAnteriores] Estancia encontrada: {}", estancia.getCodigoFolio());

            logger.info("[calcularEstimacionPagoSinPagosAnteriores] verificando si existe pago por anticipos");
            List<Pago> pagos = pagoRepository.findByEstanciaId(request.getIdEstancia());

        }

        logger.info("[calcularEstimacionPagoSinPagosAnteriores] obteniendo tarifa base por tipo de unidad");
        TipoUnidad tipoUnidad = request.getTipoUnidad();

        BigDecimal precioDia = tarifaBaseService.obtenerPrecioDiaPorTipoUnidad(tipoUnidad);

        logger.info("[calcularEstimacionPagoSinPagosAnteriores] calculando total de dias entre {} y {}", request.getFechaEntrada(), request.getFechaSalida());
        Long totalDias = calcularDias(request.getFechaEntrada(), request.getFechaSalida());

        logger.info("[calcularEstimacionPagoSinPagosAnteriores] calculando precio por personas adicionales si aplica");

        if(request.getNumeroPersonas() > 2) {
            precioPersonasAdicionales = calcularPrecioPersonaAdicional(
                    tipoUnidad,
                    totalDias,
                    request.getNumeroPersonas()
            );
            logger.info("[calcularEstimacionPagoSinPagosAnteriores] precio por personas adicionales: {}", precioPersonasAdicionales);
        }

        BigDecimal totalPago = precioDia
                .multiply(BigDecimal.valueOf(totalDias))
                .add(precioPersonasAdicionales);
        logger.info("[calcularEstimacionPagoSinPagosAnteriores] total pago calculado: {}", totalPago);

        return totalPago;

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
