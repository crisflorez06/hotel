package com.hotel.dtos;

import lombok.Data;

import java.util.List;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.Piso;
import com.hotel.models.enums.TipoUnidad;

@Data
public class UnidadDTO {
    private Long id;
    private String codigo;
    private TipoUnidad tipo;
    private Piso piso;
    private EstadoOperativo estado;
    private List<HabitacionDTO> habitaciones;
}
