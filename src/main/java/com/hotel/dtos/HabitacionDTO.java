package com.hotel.dtos;

import com.hotel.models.enums.Piso;
import lombok.Data;
import com.hotel.models.enums.EstadoOperativo;

@Data
public class HabitacionDTO {
    private Long id;
    private String codigo;
    private EstadoOperativo estado;
    private Piso piso;
    private String codigoUnidad;
    private String informacionAdicional;
}
