package com.hotel.dtos.gasto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class GastoDTO {

    private Long id;
    private String concepto;
    private String descripcion;
    private BigDecimal monto;
    private LocalDateTime fecha;
    private String metodoPago;
    private String referencia;
    private String estado;
}
