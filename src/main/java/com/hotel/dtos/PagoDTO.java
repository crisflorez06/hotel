package com.hotel.dtos;

import com.hotel.models.enums.EstadoPago;
import com.hotel.models.enums.MedioPago;
import com.hotel.models.enums.TipoPago;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class PagoDTO {

    private Long id;
    private BigDecimal montoPagoReserva;
    private BigDecimal montoPagoEstancia;
    private MedioPago medioPago;
    private LocalDateTime fecha;
    private EstadoPago estado;
}
