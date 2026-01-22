package com.hotel.mappers;

import com.hotel.dtos.PagoNuevoRequestDTO;
import com.hotel.models.Pago;

import java.time.LocalDateTime;

public class PagoMapper {

    public static Pago requestNuevoToEntity(PagoNuevoRequestDTO request) {
        Pago entity = new Pago();

        entity.setMonto(request.getMonto());
        entity.setMedioPago(request.getMedioPago());
        entity.setFecha(request.getFecha());
        entity.setFechaCreacion(LocalDateTime.now());
        entity.setEstado(request.getEstado());

        return entity;
    }
}
