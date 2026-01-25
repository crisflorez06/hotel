package com.hotel.services;

import com.hotel.models.Estancia;
import com.hotel.models.EstanciaHabitacion;
import com.hotel.models.Habitacion;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.EstanciaHabitacionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstanciaHabitacionService {

    private final EstanciaHabitacionRepository estanciaHabitacionRepository;
    private final HabitacionService habitacionService;

    public EstanciaHabitacionService(EstanciaHabitacionRepository estanciaHabitacionRepository, HabitacionService habitacionService) {
        this.habitacionService = habitacionService;
        this.estanciaHabitacionRepository = estanciaHabitacionRepository;
    }

    public List<EstanciaHabitacion> llenarHabitaciones(Estancia estancia, String codigoUnidad, TipoUnidad tipoUnidad) {

        List<Habitacion> habitaciones = habitacionService.clasificarHabitacionesPorTipoUnidad(codigoUnidad, tipoUnidad);

        return habitaciones.stream()
                .map(habitacion -> {
                    EstanciaHabitacion eh = new EstanciaHabitacion();
                    eh.setEstancia(estancia);
                    eh.setHabitacion(habitacion);
                    eh.setEstadoOcupacion(true);

                    return eh;
                })
                .toList();
    }

    public List<EstanciaHabitacion> vaciarHabitaciones(Estancia estancia) {

        List<EstanciaHabitacion> estanciaHabitaciones =
                buscarEstanciaHabitacionesPorEstancia(estancia);

        estanciaHabitaciones.forEach(eh ->
                eh.setEstadoOcupacion(false)
        );

        return estanciaHabitacionRepository.saveAll(estanciaHabitaciones);
    }

    public Estancia obtenerEstanciaActivaPorHabitacionId(Long habitacionId) {
        return estanciaHabitacionRepository.findActiveEstanciaByHabitacionId(habitacionId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No hay estancia activa para la habitacion con id: " + habitacionId));
    }

    private List<EstanciaHabitacion> buscarEstanciaHabitacionesPorEstancia(Estancia estancia) {
        return estanciaHabitacionRepository.findByEstancia_Id(estancia.getId());
    }
}
