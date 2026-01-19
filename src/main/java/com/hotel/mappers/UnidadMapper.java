package com.hotel.mappers;

import com.hotel.dtos.UnidadDTO;
import com.hotel.models.Unidad;
import com.hotel.models.enums.TipoUnidad;

import java.util.stream.Collectors;
import java.util.Collections;

public class UnidadMapper {

    public static UnidadDTO entityToDto(Unidad entity) {
        UnidadDTO dto = new UnidadDTO();

        dto.setId(entity.getId());
        dto.setCodigo(entity.getCodigo());
        dto.setTipo(entity.getTipo());
        dto.setPiso(entity.getPiso());
        dto.setEstado(entity.getEstadoOperativo());

        if (TipoUnidad.APARTAMENTO.equals(entity.getTipo())) {
            dto.setHabitaciones(entity.getHabitaciones().stream()
                    .map(HabitacionMapper::entityToDto).collect(Collectors.toList()));
        } else {
            dto.setHabitaciones(Collections.emptyList());
        }
        return dto;
    }
}
