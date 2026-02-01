package com.hotel.resolvers;

import com.hotel.models.Habitacion;
import com.hotel.models.Unidad;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.services.HabitacionService;
import com.hotel.services.UnidadService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class UnidadHabitacionResolver {

    private final HabitacionService habitacionService;
    private final UnidadService unidadService;

    public UnidadHabitacionResolver(HabitacionService habitacionService, UnidadService unidadService) {
        this.habitacionService = habitacionService;
        this.unidadService = unidadService;
    }

    public List<Habitacion> buscarListaHabitaciones(String codigo, TipoUnidad tipoUnidad) {
        List<Habitacion> habitaciones;

        if(tipoUnidad.equals(TipoUnidad.HABITACION)){
            habitaciones = List.of(habitacionService.buscarPorCodigo(codigo));
        } else {
            habitaciones = unidadService.buscarHabitacionesPorCodigoUnidad(codigo);
        }

        return new ArrayList<>(habitaciones);
    }

}
