package com.hotel.mappers;

import com.hotel.dtos.EstanciaDTO;
import com.hotel.dtos.EstanciaNuevoRequestDTO;
import com.hotel.models.Estancia;
import com.hotel.models.enums.EstadoEstancia;

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
        entity.setEstado(EstadoEstancia.ACTIVA);

        return entity;
    }

    public static EstanciaDTO entityToDTO(Estancia entity) {
        EstanciaDTO dto = new EstanciaDTO();

        dto.setId(entity.getId());
        dto.setCodigoFolio(entity.getCodigoFolio());
        dto.setFechaCreacion(entity.getFechaCreacion());
        dto.setEntradaReal(entity.getEntradaReal());
        dto.setSalidaEstimada(entity.getSalidaEstimada());
        dto.setModoOcupacion(entity.getModoOcupacion());
        dto.setNotas(entity.getNotas());
        dto.setOcupantes(OcupanteMapper.listaOcupanteToDto(entity.getOcupantes()));

        return dto;
    }

    private static String generateCodigoEstancia() {
        return "EST-" + System.currentTimeMillis();
    }
}
