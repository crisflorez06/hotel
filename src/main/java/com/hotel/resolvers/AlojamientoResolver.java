package com.hotel.resolvers;

import com.hotel.models.Habitacion;
import com.hotel.models.Unidad;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.services.UnidadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AlojamientoResolver {

    private static final Logger logger = LoggerFactory.getLogger(AlojamientoResolver.class);

    private final UnidadHabitacionResolver unidadHabitacionResolver;
    private final UnidadService unidadService;

    public AlojamientoResolver(UnidadHabitacionResolver unidadHabitacionResolver, UnidadService unidadService) {
        this.unidadService = unidadService;
        this.unidadHabitacionResolver = unidadHabitacionResolver;
    }

    public Boolean verificarDisponiblidad(String codigo, TipoUnidad tipoUnidad) {
        logger.info("[AlojamientoResolver.verificarDisponiblidad] Verificando disponibilidad para codigo: {} y tipoUnidad: {}", codigo, tipoUnidad);

        List<Habitacion> habitaciones = unidadHabitacionResolver.buscarListaHabitaciones(codigo, tipoUnidad);

        for (Habitacion habitacion : habitaciones) {
            if (!habitacion.getEstadoOperativo().equals(EstadoOperativo.DISPONIBLE)) {
                logger.info("[AlojamientoResolver.verificarDisponiblidad] Habitacion con codigo: {} no está disponible (estado: {})", habitacion.getCodigo(), habitacion.getEstadoOperativo());
                return false;
            }
        }

        logger.info("[AlojamientoResolver.verificarDisponiblidad] Todas las habitaciones están disponibles para codigo: {} y tipoUnidad: {}", codigo, tipoUnidad);
        return true;

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


}
