package com.hotel.services;

import com.hotel.dtos.UnidadDTO;
import com.hotel.mappers.UnidadMapper;
import com.hotel.models.Habitacion;
import com.hotel.models.Unidad;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.Piso;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.UnidadRepository;
import com.hotel.specifications.UnidadSpecification;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnidadService {

    private static final Logger logger = LoggerFactory.getLogger(UnidadService.class);

    private final UnidadRepository unidadRepository;

    public UnidadService(UnidadRepository unidadRepository) {
        this.unidadRepository = unidadRepository;
    }

    public List<UnidadDTO> buscarUnidades(
            TipoUnidad tipo,
            List<EstadoOperativo> estados,
            List<Piso> pisos,
            String codigo) {

        return unidadRepository.findAll(UnidadSpecification.byFilters(tipo, estados, pisos, codigo)).stream()
                .map(UnidadMapper::entityToDto)
                .collect(Collectors.toList());
    }

    public Unidad buscarPorCodigo(String codigo) {
        return unidadRepository.findByCodigo(codigo)
                .orElseThrow(() -> new EntityNotFoundException("Unidad no encontrada con codigo: " + codigo));
    }



    public List<Habitacion> buscarHabitacionesPorCodigoUnidad(String codigoUnidad) {
        Unidad unidad = buscarPorCodigo(codigoUnidad);
        return unidad.getHabitaciones();
    }

    @Transactional
    public void cambiarEstadoUnidad(Unidad unidad) {


        EstadoOperativo primerEstado = unidad.getHabitaciones().getFirst().getEstadoOperativo();

        boolean todosIguales = unidad.getHabitaciones().stream()
                .allMatch(h -> h.getEstadoOperativo() == primerEstado);

        EstadoOperativo estadoCalculado = todosIguales
                ? primerEstado
                : EstadoOperativo.PARCIALMENTE;


        unidad.setEstadoOperativo(estadoCalculado);
        unidadRepository.save(unidad);
    }





}
