package com.hotel.dtos.dashboard;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoriaMontoConteoDTO {

    private String categoria;
    private BigDecimal monto;
    private Long conteoPagos;
}
