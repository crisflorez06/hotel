package com.hotel.dtos.dashboard;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class DashboardResumenDTO {

    private OperativoResumen operativo;
    private FinancieroResumen financiero;
    private AlertaResumen alertas;

    @Data
    public static class OperativoResumen {
        private Long estanciasActivas;
        private Long reservasConfirmadas;
        private Long habitacionesOcupadas;
        private Long habitacionesTotales;
        private BigDecimal ocupacionPorcentaje;
    }

    @Data
    public static class FinancieroResumen {
        private BigDecimal ingresos;
        private BigDecimal gastos;
        private BigDecimal neto;
        private Long pagosPendientesCantidad;
        private BigDecimal pagosPendientesMonto;
    }

    @Data
    public static class AlertaResumen {
        private Long reservasExpiradas;
        private Long estanciasExcedidas;
        private Long pagosPendientesAntiguos;
        private Long totalAlertas;
    }
}
