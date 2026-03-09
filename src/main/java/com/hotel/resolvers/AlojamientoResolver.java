package com.hotel.resolvers;

import com.hotel.models.Habitacion;
import com.hotel.models.Unidad;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.ModoOcupacion;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.services.UnidadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class AlojamientoResolver {

    private static final Logger logger = LoggerFactory.getLogger(AlojamientoResolver.class);

    private final UnidadService unidadService;

    public AlojamientoResolver(UnidadService unidadService) {
        this.unidadService = unidadService;
    }

    public void actualizarEstadoAlojamiento(List<Habitacion> habitacionesModificadas, EstadoOperativo nuevoEstado) {

        logger.info("[AlojamientoResolver.actualizarEstadoAlojamiento] Actualizando estado de las habitaciones al nuevo estado: {}", nuevoEstado);
        logger.info("[AlojamientoResolver.actualizarEstadoAlojamiento] buscando unidad de las habitaciones modificadas");
        Unidad unidad = habitacionesModificadas.getFirst().getUnidad();

        List<Habitacion> habitacionesUnidad = unidadService.buscarHabitacionesPorCodigoUnidad(unidad.getCodigo());

        logger.info("[AlojamientoResolver.actualizarEstadoAlojamiento] Habitaciones de la unidad antes de la actualizacion: {}", habitacionesUnidad);
        for (Habitacion h: habitacionesUnidad) {
            for (Habitacion hm : habitacionesModificadas) {
                if (h.getCodigo().equals(hm.getCodigo())) {
                    h.setEstadoOperativo(nuevoEstado);
                }
            }
        }
        logger.info("[AlojamientoResolver.actualizarEstadoAlojamiento] Habitaciones de la unidad despues de la actualizacion: {}", habitacionesUnidad);
        unidad.setHabitaciones(habitacionesUnidad);
        unidadService.cambiarEstadoUnidad(unidad);

    }

    public ModoOcupacion determinarModoOcupacion(TipoUnidad tipoUnidad) {

        return tipoUnidad == TipoUnidad.HABITACION ? ModoOcupacion.INDIVIDUAL : ModoOcupacion.COMPLETO;
    }




}
