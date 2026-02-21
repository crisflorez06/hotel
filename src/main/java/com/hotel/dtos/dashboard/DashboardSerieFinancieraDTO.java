package com.hotel.dtos.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class DashboardSerieFinancieraDTO {

    private LocalDate periodo;
    private BigDecimal ingresos;
    private BigDecimal gastos;
    private BigDecimal neto;
}
