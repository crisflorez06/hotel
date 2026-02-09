package com.hotel.testdata;

import com.hotel.models.Estancia;
import com.hotel.models.Habitacion;
import com.hotel.models.Ocupante;
import com.hotel.models.Reserva;
import com.hotel.models.enums.CanalReserva;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.models.enums.ModoOcupacion;

import java.time.LocalDateTime;
import java.util.List;

public final class ReservaTestData {

    private ReservaTestData(){}

    public static Reserva reservaData(Ocupante cliente, ModoOcupacion modoOcupacion, EstadoReserva estadoReserva, List<Habitacion> habitaciones, Estancia estancia){
        Reserva reserva = new Reserva();

        reserva.setCodigo(TestDataUtils.randomCodigo("RES-"));
        reserva.setCliente(cliente);
        reserva.setNumeroPersonas(2);
        reserva.setFechaCreacion(LocalDateTime.now());
        reserva.setEntradaEstimada(reserva.getFechaCreacion().plusDays(2));
        reserva.setSalidaEstimada(reserva.getEntradaEstimada().plusDays(2));
        reserva.setModoOcupacion(modoOcupacion);
        reserva.setEstado(estadoReserva);
        reserva.setCanalReserva(CanalReserva.MOSTRADOR);
        reserva.setNotas("prueba para nota de reserva");
        reserva.setHabitaciones(habitaciones);
        reserva.setEstancia(estancia);

        return reserva;

    }
}
