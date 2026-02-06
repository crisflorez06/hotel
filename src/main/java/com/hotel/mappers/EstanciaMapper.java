package com.hotel.mappers;

import com.hotel.dtos.estancia.EstanciaDTO;
import com.hotel.dtos.estancia.EstanciaRequestDTO;
import com.hotel.dtos.estancia.ActivarEstanciaDTO;
import com.hotel.models.Estancia;
import com.hotel.models.enums.EstadoEstancia;

import java.time.LocalDateTime;

public class EstanciaMapper {

    public static Estancia requestToEntity(EstanciaRequestDTO request) {
        Estancia entity = new Estancia();

        entity.setCodigoFolio(generateCodigoEstancia());
        entity.setEntradaReal(request.getEntradaReal());
        entity.setSalidaEstimada(request.getSalidaEstimada());
        entity.setNotas("Notas al registrar: " + request.getNotas());
        entity.setEstado(EstadoEstancia.ACTIVA);

        return entity;
    }

    public static EstanciaDTO entityToDTO(Estancia entity) {
        EstanciaDTO dto = new EstanciaDTO();

        dto.setId(entity.getId());
        dto.setCodigoFolio(entity.getCodigoFolio());
        dto.setEntradaReal(entity.getEntradaReal());
        dto.setSalidaEstimada(entity.getSalidaEstimada());
        dto.setModoOcupacion(entity.getModoOcupacion());
        dto.setNotas(entity.getNotas());
        dto.setOcupantes(OcupanteMapper.listaOcupanteToDto(entity.getOcupantes()));
        if(entity.getPagos() != null) {
            dto.setPagos(PagoMapper.entityListToDTOList(entity.getPagos()));
        }

        return dto;
    }

    public static void activarToEntity(ActivarEstanciaDTO request, Estancia entity) {

        if (!request.getEntradaReal().equals(entity.getEntradaReal())) {
            entity.setEntradaReal(request.getEntradaReal());
        }
        if (!request.getSalidaEstimada().equals(entity.getSalidaEstimada())) {
            entity.setSalidaEstimada(request.getSalidaEstimada());
        }

        entity.setEstado(EstadoEstancia.ACTIVA);

    }

    private static String generateCodigoEstancia() {
        return "EST-" + System.currentTimeMillis();
    }
}
