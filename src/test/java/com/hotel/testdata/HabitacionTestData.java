package com.hotel.testdata;

import com.hotel.models.Habitacion;
import com.hotel.models.Unidad;
import com.hotel.models.enums.EstadoOperativo;

public final class HabitacionTestData {

    private HabitacionTestData() {
    }

    public static Habitacion habitacionData(Unidad unidad, String codigo, EstadoOperativo estado) {
        Habitacion h = new Habitacion();
        h.setUnidad(unidad);
        h.setCodigo(codigo);
        h.setEstadoOperativo(estado);
        return h;
    }

}
