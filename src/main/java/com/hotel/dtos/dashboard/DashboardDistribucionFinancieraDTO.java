package com.hotel.dtos.dashboard;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class DashboardDistribucionFinancieraDTO {

    private BigDecimal ingresosTotales;
    private BigDecimal ingresosPagos;
    private BigDecimal gastos;
    private BigDecimal neto;
    private Long ingresosPendientesCantidad;
    private BigDecimal ingresosPendientesMonto;

    private List<CategoriaMontoDTO> ingresosPorTipoPago;
    private List<CategoriaMontoConteoDTO> ingresosPorMedioPago;
}
