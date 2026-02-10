package com.hotel.resolvers;

import com.hotel.models.Estancia;
import com.hotel.models.Reserva;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.repositories.EstanciaRepository;
import com.hotel.repositories.ReservaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class EstanciaReservaResolver {

    private static final Logger logger = LoggerFactory.getLogger(EstanciaReservaResolver.class);

    private final EstanciaRepository estanciaRepository;
    private final ReservaRepository reservaRepository;

    public EstanciaReservaResolver(EstanciaRepository estanciaRepository,
                                  ReservaRepository reservaRepository) {
        this.estanciaRepository = estanciaRepository;
        this.reservaRepository = reservaRepository;
    }

    public Estancia crearEstanciaDesdeReserva(Reserva reserva) {

        if (!reserva.getEstado().equals(EstadoReserva.CONFIRMADA)) {
            throw new IllegalStateException("La reserva con id: " + reserva.getId() + " no está en estado CONFIRMADA, no se puede crear la estancia");
        }
        Estancia estancia = new Estancia();
        estancia.setCodigoFolio("EST-" + System.currentTimeMillis());
        estancia.setEntradaReal(reserva.getEntradaEstimada());
        estancia.setSalidaEstimada(reserva.getSalidaEstimada());
        estancia.setModoOcupacion(reserva.getModoOcupacion());
        estancia.setEstado(EstadoEstancia.RESERVADA);
        estancia.setNotas("Estancia creada desde reserva: " + reserva.getNotas());
        estancia.setHabitaciones(reserva.getHabitaciones());
        estancia.setReserva(reserva);
        reserva.setEstancia(estancia);

        return estanciaRepository.save(estancia);
    }

    public Reserva buscarReservaPorIdDesdeEstancia(Long reservaId) {
        return reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró una reserva con id: " + reservaId));
    }

    public void actualizarEstadoReservaDesdeEstancia(Reserva reserva, EstadoReserva nuevoEstado) {
        reserva.setEstado(nuevoEstado);
        reservaRepository.save(reserva);
    }
}
