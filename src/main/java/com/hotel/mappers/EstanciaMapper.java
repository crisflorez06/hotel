package com.hotel.mappers;

import com.hotel.dtos.estancia.EstanciaCalendarioDTO;
import com.hotel.dtos.estancia.EstanciaDTO;
import com.hotel.dtos.estancia.EstanciaRequestDTO;
import com.hotel.dtos.estancia.ActivarEstanciaDTO;
import com.hotel.models.Estancia;
import com.hotel.models.Ocupante;
import com.hotel.models.Pago;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.TipoOcupante;

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

    public static EstanciaCalendarioDTO entityToCalendarioDTO(Estancia estancia) {
        EstanciaCalendarioDTO dto = new EstanciaCalendarioDTO();

        dto.setId(estancia.getId());
        dto.setInicio(estancia.getEntradaReal());
        if(estancia.getSalidaReal() != null) {
            dto.setFin(estancia.getSalidaReal());
        } else {
            dto.setFin(estancia.getSalidaEstimada());
        }
        dto.setCodigoEstancia(estancia.getCodigoFolio());
        dto.setNumeroPersonas(estancia.getOcupantes().size());
        dto.setNombreCliente(estancia.getOcupantes().stream()
                .filter(ocupante -> ocupante.getTipoOcupante() == TipoOcupante.CLIENTE)
                .findFirst()
                .map(ocupante -> ocupante.getNombres() + " " + ocupante.getApellidos())
                .orElse("Cliente sin nombre"));

        dto.setIdCliente(estancia.getOcupantes().stream()
                .filter(ocupante -> ocupante.getTipoOcupante() == TipoOcupante.CLIENTE)
                .findFirst()
                .map(Ocupante::getId)
                .orElse(null));

        if (estancia.getPagos() != null) {
            dto.setTotalPagado(estancia.getPagos().stream()
                    .filter(pago -> pago.getTipoPago() == com.hotel.models.enums.TipoPago.ESTANCIA)
                    .map(Pago::getMonto)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
        }
        dto.setEstadoEstancia(estancia.getEstado());

        return dto;
    }

    private static String generateCodigoEstancia() {
        return "EST-" + System.currentTimeMillis();
    }
}
