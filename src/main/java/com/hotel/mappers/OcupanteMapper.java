package com.hotel.mappers;

import com.hotel.dtos.ocupante.OcupanteDTO;
import com.hotel.dtos.ocupante.OcupanteNuevoRequestDTO;
import com.hotel.models.Ocupante;
import java.time.LocalDateTime;
import java.util.List;

public class OcupanteMapper {

    public static List<OcupanteDTO> listaOcupanteToDto(List<Ocupante> ocupantes) {
        return ocupantes.stream()
                .map(OcupanteMapper::ocupanteToDto)
                .toList();
    }



    public static Ocupante requestNuevoToEntity(OcupanteNuevoRequestDTO request) {
        Ocupante entity = new Ocupante();

        entity.setNombres(request.getNombres());
        entity.setApellidos(request.getApellidos());
        entity.setTipoDocumento(request.getTipoDocumento());
        entity.setNumeroDocumento(request.getNumeroDocumento());
        entity.setTelefono(request.getTelefono());
        entity.setEmail(request.getEmail());
        entity.setTipoOcupante(request.getTipoOcupante());
        entity.setCreadoEn(LocalDateTime.now());

        return entity;
    }

    public static OcupanteDTO ocupanteToDto(Ocupante ocupante) {
        OcupanteDTO dto = new OcupanteDTO();

        dto.setId(ocupante.getId());
        dto.setNombres(ocupante.getNombres());
        dto.setApellidos(ocupante.getApellidos());
        dto.setTipoDocumento(ocupante.getTipoDocumento());
        dto.setNumeroDocumento(ocupante.getNumeroDocumento());
        dto.setEmail(ocupante.getEmail());
        dto.setTelefono(ocupante.getTelefono());
        dto.setTipoOcupante(ocupante.getTipoOcupante());
        dto.setCreadoEn(ocupante.getCreadoEn());

        return dto;
    }

}
