package com.hotel.dtos.pago;

import com.hotel.models.enums.EstadoPago;
import com.hotel.models.enums.MedioPago;
import com.hotel.models.enums.TipoPago;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PagoNuevoRequestDTO {

    private TipoPago tipoPago;

    @NotNull(message = "monto es obligatorio")
    private BigDecimal monto;

    @NotNull(message = "medioPago es obligatorio")
    private MedioPago medioPago;

    @NotNull(message = "fecha de registro es obligatorio")
    private LocalDateTime fecha;

    @NotNull(message = "estado es obligatorio")
    private EstadoPago estado;

}
