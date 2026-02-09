package com.hotel.testdata;

import com.hotel.models.*;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.ModoOcupacion;

import java.time.LocalDateTime;
import java.util.List;

public final class EstanciaTestData {

    private EstanciaTestData(){};

    public static Estancia estanciaData(
            Reserva reserva,
            List<Ocupante> ocupantes,
            ModoOcupacion modoOcupacion,
            EstadoEstancia estadoEstancia,
            List<Habitacion> habitaciones,
            List<Pago> pagos
    ) {
        Estancia estancia = new Estancia();

        estancia.setCodigoFolio(TestDataUtils.randomCodigo("EST-"));
        estancia.setReserva(reserva);
        estancia.setOcupantes(ocupantes);
        estancia.setEntradaReal(LocalDateTime.now());
        estancia.setSalidaEstimada(estancia.getEntradaReal().plusDays(2));
        estancia.setModoOcupacion(modoOcupacion);
        estancia.setEstado(estadoEstancia);
        estancia.setNotas("Estancia de prueba");
        estancia.setHabitaciones(habitaciones);
        estancia.setPagos(pagos);

        return estancia;
    }

}
