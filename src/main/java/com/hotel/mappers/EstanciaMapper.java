package com.hotel.mappers;

import com.hotel.dtos.EstanciaNuevoRequestDTO;
import com.hotel.models.Estancia;

import java.time.LocalDateTime;

public class EstanciaMapper {

    public static Estancia requestNuevoToEntity(EstanciaNuevoRequestDTO request) {
        Estancia entity = new Estancia();

        entity.setCodigoFolio(generateCodigoEstancia());
        entity.setFechaCreacion(LocalDateTime.now());
        entity.setEntradaReal(request.getEntradaReal());
        entity.setSalidaEstimada(request.getSalidaEstimada());
        entity.setNotas("Notas al registrar: " + request.getNotas());
        entity.setActivo(true);
        entity.setPago(request.getPago() != null ? PagoMapper.requestNuevoToEntity(request.getPago()) : null);

        return entity;
    }

    private static String generateCodigoEstancia() {
        return "EST-" + System.currentTimeMillis();
    }
}
