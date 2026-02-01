package com.hotel.services;

import com.hotel.dtos.HabitacionDTO;
import com.hotel.mappers.HabitacionMapper;
import com.hotel.models.Habitacion;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.Piso;
import com.hotel.repositories.HabitacionRepository;
import com.hotel.repositories.ReservaRepository;
import com.hotel.specifications.HabitacionSpecification;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class HabitacionService {

    private static final Logger logger = LoggerFactory.getLogger(HabitacionService.class);

    private final HabitacionRepository habitacionRepository;

    public HabitacionService(HabitacionRepository habitacionRepository) {
        this.habitacionRepository = habitacionRepository;
    }

    public List<HabitacionDTO> buscarHabitaciones(
            List<EstadoOperativo> estados,
            List<Piso> pisos,
            String codigo) {
        return habitacionRepository.findAll(HabitacionSpecification.byFilters(estados, pisos, codigo)).stream()
                .map(HabitacionMapper::entityToDto)
                .collect(Collectors.toList());
    }

    public Habitacion buscarPorCodigo(String codigo) {
        return habitacionRepository.findByCodigo(codigo)
                .orElseThrow( () -> new EntityNotFoundException("Habitacion no encontrada con codigo: " + codigo));
    }


}
