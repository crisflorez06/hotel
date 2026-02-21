package com.hotel.dtos.dashboard;

import com.hotel.models.enums.TipoUnidad;
import lombok.Data;

@Data
public class DashboardConteoUnidadDTO {

    private TipoUnidad tipoUnidad;
    private Long disponible;
    private Long ocupado;
    private Long parcialmente;
    private Long total;
}
