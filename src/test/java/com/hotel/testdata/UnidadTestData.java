package com.hotel.testdata;

import com.hotel.models.Unidad;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.Piso;
import com.hotel.models.enums.TipoUnidad;

import java.util.ArrayList;

public final class UnidadTestData {

    private UnidadTestData() {
    }

    public static Unidad unidadData(TipoUnidad tipo, EstadoOperativo estado) {
        Unidad u = new Unidad();
        u.setCodigo(TestDataUtils.randomCodigo("UN-")); // <= 20 caracteres
        u.setTipo(tipo);
        u.setPiso(Piso.PISO_1);
        u.setEstadoOperativo(estado);
        u.setHabitaciones(new ArrayList<>());
        return u;
    }

}
