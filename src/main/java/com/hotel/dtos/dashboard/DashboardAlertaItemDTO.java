package com.hotel.dtos.dashboard;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class DashboardAlertaItemDTO {

    private String tipo;
    private String severidad;
    private String codigoReserva;
    private String codigoEstancia;
    private LocalDateTime fechaReferencia;
    private String mensaje;
}
