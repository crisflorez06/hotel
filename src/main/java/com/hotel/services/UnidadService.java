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
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnidadService {

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

    @Transactional
    public void actualizarEstadoUnidad(String codigo, EstadoOperativo nuevoEstado) {
        Unidad unidad = buscarPorCodigo(codigo);
        unidad.setEstadoOperativo(nuevoEstado);
        unidadRepository.save(unidad);
    }

    public List<Habitacion> buscarHabitacionesPorCodigoUnidad(String codigoUnidad) {
        Unidad unidad = buscarPorCodigo(codigoUnidad);
        return unidad.getHabitaciones();
    }

}
