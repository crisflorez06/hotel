package com.hotel.mappers;


import com.hotel.dtos.ReservaNuevaRequestDTO;
import com.hotel.models.Reserva;
import com.hotel.models.enums.EstadoReserva;

import java.time.LocalDateTime;

public class ReservaMapper {

    public static Reserva requestNuevoToEntity(ReservaNuevaRequestDTO request) {
        Reserva entity = new Reserva();

        entity.setCodigo(generateCodigoReserva());
        entity.setFechaCreacion(LocalDateTime.now());
        entity.setEntradaEstimada(request.getEntradaEstimada());
        entity.setSalidaEstimada(request.getSalidaEstimada());
        entity.setEstado(EstadoReserva.CONFIRMADA);
        entity.setCanalReserva(request.getCanalReserva());
        entity.setNotas("Notas al registrar: " + request.getNotas());


        return entity;
    }

    private static String generateCodigoReserva() {
        return "RES-" + System.currentTimeMillis();
    }
}
