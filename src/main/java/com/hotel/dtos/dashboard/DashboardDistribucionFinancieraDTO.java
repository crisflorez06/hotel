package com.hotel.dtos.dashboard;

import java.util.List;
import lombok.Data;

@Data
public class DashboardDistribucionFinancieraDTO {

    private List<CategoriaMontoDTO> ingresosPorTipoPago;
    private List<CategoriaMontoDTO> ingresosPorMedioPago;
    private List<CategoriaMontoDTO> gastosPorConcepto;
}
