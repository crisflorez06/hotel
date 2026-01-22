package com.hotel.services;

import com.hotel.dtos.HabitacionDTO;
import com.hotel.mappers.HabitacionMapper;
import com.hotel.models.EstanciaHabitacion;
import com.hotel.models.Habitacion;
import com.hotel.models.Unidad;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.Piso;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.HabitacionRepository;
import com.hotel.specifications.HabitacionSpecification;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HabitacionService {

    private static final Logger logger = LoggerFactory.getLogger(EstanciaService.class);

    private final HabitacionRepository habitacionRepository;
    private final UnidadService unidadService;

    public HabitacionService(HabitacionRepository habitacionRepository, UnidadService unidadService) {
        this.unidadService = unidadService;
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


    @Transactional
    public void cambiarEstadoHabitaciones(String codigo, EstadoOperativo estado, TipoUnidad tipoUnidad) {
        logger.info("Cambiando estado de las habitaciones asociadas al codigo: {} y tipoUnidad: {}", codigo, tipoUnidad);
        List<Habitacion> habitacionesAfectadas = clasificarHabitacionesPorTipoUnidad(codigo, tipoUnidad);
        String codigoUnidad = "";

        if(tipoUnidad.equals(TipoUnidad.HABITACION)){
            codigoUnidad = habitacionesAfectadas.getFirst().getUnidad().getCodigo();
        } else {
            codigoUnidad = codigo;
        }

        logger.info("Cambiando estado de las habitaciones afectadas");
        unidadService.cambiarEstadoUnidad(codigoUnidad, habitacionesAfectadas, estado);

    }

    public Boolean verificarDisponiblidad(String codigo, TipoUnidad tipoUnidad) {
        logger.info("Verificando disponibilidad para codigo: {} y tipoUnidad: {}", codigo, tipoUnidad);
        if (tipoUnidad.equals(TipoUnidad.HABITACION)) {
            Habitacion habitacion = buscarPorCodigo(codigo);
            return habitacion.getEstadoOperativo() == EstadoOperativo.DISPONIBLE;
        } else {
            Unidad unidad = unidadService.buscarPorCodigo(codigo);
            return unidad.getEstadoOperativo() == EstadoOperativo.DISPONIBLE;
        }

    }

    public List<Habitacion> clasificarHabitacionesPorTipoUnidad(String codigoUnidad, TipoUnidad tipoUnidad) {
        List<Habitacion> habitaciones;

        if(tipoUnidad.equals(TipoUnidad.HABITACION)){
            habitaciones = List.of(buscarPorCodigo(codigoUnidad));
        } else {
            habitaciones = unidadService.buscarHabitacionesPorCodigoUnidad(codigoUnidad);
        }

        return habitaciones;
    }


}
