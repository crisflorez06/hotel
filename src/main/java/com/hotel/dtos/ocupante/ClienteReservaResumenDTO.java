package com.hotel.dtos.ocupante;

import com.hotel.models.enums.EstadoReserva;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ClienteReservaResumenDTO {
    private Long id;
    private String codigoReserva;
    private EstadoReserva estado;
    private LocalDateTime entradaEstimada;
    private LocalDateTime salidaEstimada;
}
