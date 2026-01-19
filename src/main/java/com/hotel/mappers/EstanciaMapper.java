package com.hotel.mappers;

import com.hotel.dtos.EstanciaRequestDTO;
import com.hotel.models.Estancia;

import java.time.LocalDateTime;

public class EstanciaMapper {

    public static Estancia requestToEntity(EstanciaRequestDTO request) {
        Estancia entity = new Estancia();

        entity.setCodigoFolio(generateCodigoEstancia());
        entity.setFechaCreacion(LocalDateTime.now());
        entity.setEntradaReal(request.getEntradaReal());
        entity.setSalidaEstimada(request.getSalidaEstimada());
        entity.setNotas("Notas al registrar: " + request.getNotas());
        entity.setActivo(true);

        return entity;
    }

    private static String generateCodigoEstancia() {
        return "EST-" + System.currentTimeMillis();
    }
}
