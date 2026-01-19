package com.hotel.mappers;

import com.hotel.dtos.HabitacionDTO;
import com.hotel.models.Habitacion;

public class HabitacionMapper {

    public static HabitacionDTO entityToDto(Habitacion entity) {
        HabitacionDTO dto = new HabitacionDTO();

        dto.setId(entity.getId());
        dto.setCodigo(entity.getCodigo());
        dto.setEstado(entity.getEstadoOperativo());
        dto.setPiso(entity.getUnidad().getPiso());
        dto.setCodigoUnidad(entity.getUnidad().getCodigo());

        return dto;
    }
}
