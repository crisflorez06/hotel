package com.hotel.mappers;

import com.hotel.dtos.EstanciaNuevoRequestDTO;
import com.hotel.dtos.OcupanteDTO;
import com.hotel.models.Acompanante;
import com.hotel.models.Cliente;
import com.hotel.models.Estancia;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class OcupanteMapper {

    public static OcupanteDTO clienteToOcupanteDto(Cliente cliente) {
        OcupanteDTO dto = new OcupanteDTO();

        dto.setId(cliente.getId());
        dto.setNombres(cliente.getNombres());
        dto.setApellidos(cliente.getApellidos());
        dto.setTipoDocumento(cliente.getTipoDocumento());
        dto.setNumeroDocumento(cliente.getNumeroDocumento());
        dto.setEmail(cliente.getEmail());
        dto.setTelefono(cliente.getTelefono());

        return dto;
    }

    public static List<OcupanteDTO> listaAcompananteToOcupanteDto(List<Acompanante> acompanantes) {
        List<OcupanteDTO> listaAcompanantes = new ArrayList<>();

        for (Acompanante acomp : acompanantes) {
            OcupanteDTO dto = new OcupanteDTO();

            dto.setId(acomp.getId());
            dto.setNombres(acomp.getNombres());
            dto.setApellidos(acomp.getApellidos());
            dto.setTipoDocumento(acomp.getTipoDocumento());
            dto.setNumeroDocumento(acomp.getNumeroDocumento());
            dto.setEmail(acomp.getEmail());
            dto.setTelefono(acomp.getTelefono());

            listaAcompanantes.add(dto);
        }

        return listaAcompanantes;
    }

    private static String generateCodigoEstancia() {
        return "EST-" + System.currentTimeMillis();
    }
}
