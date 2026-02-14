package com.hotel.dtos.ocupante;

import com.hotel.models.enums.EstadoEstancia;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ClienteEstanciaResumenDTO {
    private Long id;
    private String codigoEstancia;
    private EstadoEstancia estado;
    private LocalDateTime entradaReal;
    private LocalDateTime salidaEstimada;
    private LocalDateTime salidaReal;
}
