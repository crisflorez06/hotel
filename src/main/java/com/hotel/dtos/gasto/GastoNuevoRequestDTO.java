package com.hotel.dtos.gasto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class GastoNuevoRequestDTO {

    @NotBlank(message = "concepto es obligatorio")
    private String concepto;

    private String descripcion;

    @NotNull(message = "monto es obligatorio")
    private BigDecimal monto;

    @NotNull(message = "fecha es obligatoria")
    private LocalDateTime fecha;

    @NotBlank(message = "metodoPago es obligatorio")
    private String metodoPago;

    private String referencia;

    @NotBlank(message = "estado es obligatorio")
    private String estado;
}
