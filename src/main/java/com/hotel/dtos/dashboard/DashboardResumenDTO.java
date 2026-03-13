package com.hotel.dtos.dashboard;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class DashboardResumenDTO {

    private OperativoResumen operativo;
    private AlertaResumen alertas;

    @Data
    public static class OperativoResumen {
        private Long estanciasActivas;
        private Long reservasConfirmadas;
        private Map<String, Long> reservasPorCanal;
        private Long apartamentosOcupados;
        private Long apartamentosReservados;
        private Long apartaestudioOcupados;
        private Long apartaestudioReservados;
        private Long apartamentosTotales;
        private Long apartaestudioTotales;
        private Long habitacionesOcupadas;
        private Long habitacionesReservadas;
        private Long habitacionesTotales;
        private BigDecimal ocupacionPorcentaje;
        private BigDecimal reservadasPorcentaje;
        private List<EstanciaMensualDTO> estanciasUltimos12Meses;
    }

    @Data
    public static class AlertaResumen {
        private Long reservasExpiradas;
        private Long estanciasExcedidas;
        private Long totalAlertas;
    }
}
