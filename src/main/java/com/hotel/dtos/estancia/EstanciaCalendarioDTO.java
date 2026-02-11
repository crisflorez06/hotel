package com.hotel.dtos.estancia;

import com.hotel.models.enums.EstadoEstancia;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EstanciaCalendarioDTO {

    private Long id;
    private LocalDateTime inicio;
    private LocalDateTime fin;
    private String codigoEstancia;
    private String codigoUnidad;
    private String tipoUnidad;
    private Integer numeroPersonas;
    private String nombreCliente;
    private Long idCliente;
    private BigDecimal totalPagado;
    private EstadoEstancia estadoEstancia;
}
