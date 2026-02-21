package com.hotel.dtos.dashboard;

import java.util.List;
import lombok.Data;

@Data
public class DashboardAlertasDTO {

    private DashboardResumenDTO.AlertaResumen resumen;
    private List<DashboardAlertaItemDTO> items;
    private Integer page;
    private Integer size;
    private Long totalItems;
    private Integer totalPages;
}
