package com.hotel.mappers;

import com.hotel.dtos.auditoria.AuditoriaEventoDTO;
import com.hotel.models.AuditoriaEvento;

public class AuditoriaEventoMapper {

    private AuditoriaEventoMapper() {
    }

    public static AuditoriaEventoDTO entityToDTO(AuditoriaEvento evento) {
        if (evento == null) {
            return null;
        }
        return new AuditoriaEventoDTO(
                evento.getId(),
                evento.getEntidad(),
                evento.getIdEntidad(),
                evento.getCodigoEstancia(),
                evento.getCodigoReserva(),
                evento.getTipoEvento(),
                evento.getDetalle(),
                evento.getFecha());
    }
}
