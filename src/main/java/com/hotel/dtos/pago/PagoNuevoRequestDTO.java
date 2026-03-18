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

    @NotNull(message = "tipoPago es obligatorio")
    private TipoPago tipoPago;

    @NotNull(message = "monto es obligatorio")
    private BigDecimal monto;

    private BigDecimal montoEstimado;

    @NotNull(message = "medioPago es obligatorio")
    private MedioPago medioPago;

    @NotNull(message = "fecha de registro es obligatorio")
    private LocalDateTime fecha;

    private String notas;

    private EstadoPago estado;


}
