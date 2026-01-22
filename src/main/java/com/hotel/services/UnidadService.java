package com.hotel.services;

import com.hotel.dtos.UnidadDTO;
import com.hotel.mappers.UnidadMapper;
import com.hotel.models.EstanciaHabitacion;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnidadService {

    private static final Logger logger = LoggerFactory.getLogger(EstanciaService.class);

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
    public void cambiarEstadoUnidad(String codigoUnidad, List<Habitacion> habitacionesModificadas, EstadoOperativo nuevoEstado) {
        logger.info("Cambiando estado de la unidad con codigo: {}", codigoUnidad);
        Unidad unidad = buscarPorCodigo(codigoUnidad);

        logger.info("Cambiando estado de la unidad asociada a las habitaciones modificadas");
        List<Habitacion> habitacionesUnidad = actualizarHabitacionesUnidad(unidad, habitacionesModificadas, nuevoEstado);

        EstadoOperativo primerEstado = habitacionesUnidad.getFirst().getEstadoOperativo();

        boolean todosIguales = habitacionesUnidad.stream()
                .allMatch(h -> h.getEstadoOperativo() == primerEstado);

        EstadoOperativo estadoCalculado = todosIguales
                ? primerEstado
                : EstadoOperativo.PARCIALMENTE;


        logger.info("Unidad {} -> Estado calculado: {}", codigoUnidad, estadoCalculado);
        unidad.setEstadoOperativo(estadoCalculado);
        unidadRepository.save(unidad);
    }

    private List<Habitacion> actualizarHabitacionesUnidad(Unidad unidad, List<Habitacion> habitacionesModificadas, EstadoOperativo nuevoEstado) {
        logger.info("Actualizando habitaciones de la unidad asociada a las habitaciones modificadas");
        List<Habitacion> habitacionesUnidad = unidad.getHabitaciones();


        logger.info("Habitaciones de la unidad antes de la actualizacion: {}", habitacionesUnidad);
        for (Habitacion h: habitacionesUnidad) {
            for (Habitacion hm : habitacionesModificadas) {
                logger.info("habitacion modificada con codigo: {}", hm.getCodigo() + " y estado: " + hm.getEstadoOperativo());
                if (h.getCodigo().equals(hm.getCodigo())) {
                    h.setEstadoOperativo(nuevoEstado);
                    logger.info("Habitacion actualizada: {}", h.getCodigo());
                }
            }
            logger.info("habitacion procesada a estado: {}", h.getCodigo() + " -> " + h.getEstadoOperativo());
        }
        logger.info("Habitaciones de la unidad despues de la actualizacion: {}", habitacionesUnidad);
        return habitacionesUnidad;
    }

}
