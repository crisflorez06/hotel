package com.hotel.services;

import com.hotel.dtos.DetalleDTO;
import com.hotel.mappers.EstanciaMapper;
import com.hotel.mappers.ReservaMapper;
import com.hotel.mappers.UnidadMapper;
import com.hotel.models.Estancia;
import com.hotel.models.Habitacion;
import com.hotel.models.Reserva;
import com.hotel.repositories.EstanciaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DetalleService {

    private final EstanciaRepository estanciaRepository;

    public DetalleService(EstanciaRepository estanciaRepository) {
        this.estanciaRepository = estanciaRepository;
    }

    @Transactional(readOnly = true)
    public DetalleDTO obtenerDetallePorEstanciaId(Long idEstancia) {
        Estancia estancia = estanciaRepository.findById(idEstancia)
                .orElseThrow(() -> new IllegalArgumentException("Estancia no encontrada con id: " + idEstancia));

        DetalleDTO detalle = new DetalleDTO();
        detalle.setEstancia(EstanciaMapper.entityToDTO(estancia));

        List<Habitacion> habitaciones = estancia.getHabitaciones();
        if (habitaciones != null && !habitaciones.isEmpty() && habitaciones.getFirst().getUnidad() != null) {
            detalle.setUnidad(UnidadMapper.entityToDto(habitaciones.getFirst().getUnidad()));
        }

        Reserva reserva = estancia.getReserva();
        if (reserva != null) {
            detalle.setReserva(ReservaMapper.entityToCalendarioDTO(reserva));
        }

        return detalle;
    }
}
