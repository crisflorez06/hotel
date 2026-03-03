package com.hotel.resolvers;

import com.hotel.models.Estancia;
import com.hotel.models.Reserva;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.repositories.EstanciaRepository;
import com.hotel.repositories.ReservaRepository;
import com.hotel.services.CodigoUnicoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EstanciaReservaResolver {

    private static final Logger logger = LoggerFactory.getLogger(EstanciaReservaResolver.class);

    private final EstanciaRepository estanciaRepository;
    private final ReservaRepository reservaRepository;
    private final CodigoUnicoService codigoUnicoService;

    public EstanciaReservaResolver(EstanciaRepository estanciaRepository,
                                  ReservaRepository reservaRepository,
                                  CodigoUnicoService codigoUnicoService) {
        this.estanciaRepository = estanciaRepository;
        this.reservaRepository = reservaRepository;
        this.codigoUnicoService = codigoUnicoService;
    }

    public Estancia crearEstanciaDesdeReserva(Reserva reserva) {

        if (!reserva.getEstado().equals(EstadoReserva.CONFIRMADA)) {
            throw new IllegalStateException("La reserva con id: " + reserva.getId() + " no está en estado CONFIRMADA, no se puede crear la estancia");
        }
        Estancia estancia = new Estancia();
        estancia.setCodigoFolio(codigoUnicoService.generarCodigoEstancia());
        estancia.setModoOcupacion(reserva.getModoOcupacion());
        estancia.setEstado(EstadoEstancia.RESERVADA);
        estancia.setNotas(null);
        estancia.setHabitaciones(reserva.getHabitaciones());
        estancia.setReserva(reserva);
        reserva.setEstancia(estancia);

        return estanciaRepository.save(estancia);
    }

    public Reserva buscarReservaPorIdDesdeEstancia(Long reservaId) {
        return reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró una reserva con id: " + reservaId));
    }

    public Estancia buscarEstanciaPorId(Long estanciaId) {
        return estanciaRepository.findById(estanciaId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró una estancia con id: " + estanciaId));
    }


    public void actualizarEstadoReservaDesdeEstancia(Reserva reserva, EstadoReserva nuevoEstado) {
        reserva.setEstado(nuevoEstado);
        reservaRepository.save(reserva);
    }
}
