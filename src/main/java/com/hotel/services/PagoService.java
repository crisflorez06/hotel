package com.hotel.services;

import com.hotel.dtos.PagoNuevoRequestDTO;
import com.hotel.mappers.PagoMapper;
import com.hotel.models.Estancia;
import com.hotel.models.Pago;
import com.hotel.models.Reserva;
import com.hotel.models.enums.TipoPago;
import com.hotel.repositories.EstanciaRepository;
import com.hotel.repositories.PagoRepository;
import com.hotel.repositories.ReservaRepository;
import jakarta.persistence.EntityNotFoundException;
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

    public PagoService(PagoRepository pagoRepository,
                       EstanciaRepository estanciaRepository,
                       ReservaRepository reservaRepository) {
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

}
