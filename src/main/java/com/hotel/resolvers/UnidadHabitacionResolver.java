package com.hotel.resolvers;

import com.hotel.models.Estancia;
import com.hotel.models.Habitacion;
import com.hotel.models.Reserva;
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

    public String determinarCodigoUnidad(List<Habitacion> habitaciones) {
        Unidad unidadPrincipal = habitaciones.getFirst().getUnidad();

        if (unidadPrincipal.getTipo() == TipoUnidad.APARTAESTUDIO) {
            return unidadPrincipal.getCodigo();
        }

        if (habitaciones.size() == 1) {
            return habitaciones.getFirst().getCodigo();
        }

        return unidadPrincipal.getCodigo();

    }

    public TipoUnidad determinarTipoUnidad(List<Habitacion> habitaciones) {
        Unidad unidadPrincipal = habitaciones.getFirst().getUnidad();

        if (unidadPrincipal.getTipo() == TipoUnidad.APARTAESTUDIO) {
            return TipoUnidad.APARTAESTUDIO;
        }

        if (habitaciones.size() == 1) {
            return TipoUnidad.HABITACION;
        }

        return TipoUnidad.APARTAMENTO;
    }

    public boolean codigoHaCambiado(List<Habitacion> habitacionesAhora, Estancia estancia, Reserva reserva) {
        String codigosAhora = determinarCodigoUnidad(habitacionesAhora);
        String codigosAntes;

        if(reserva != null && reserva.getHabitaciones() != null) {
            codigosAntes = determinarCodigoUnidad(reserva.getHabitaciones());
        } else if(estancia != null && estancia.getHabitaciones() != null) {
             codigosAntes = determinarCodigoUnidad(estancia.getHabitaciones());
        } else {
            return false;
        }

        return !codigosAhora.equals(codigosAntes);
    }

}
