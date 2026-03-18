package com.hotel.dtos.pago;

import com.hotel.models.enums.EstadoPago;
import com.hotel.models.enums.MedioPago;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import com.hotel.models.enums.TipoPago;

@Data
public class PagoDTO {

    private Long id;
    private String codigoReserva;
    private String codigoEstancia;
    private BigDecimal monto;
    private MedioPago medioPago;
    private LocalDateTime fecha;
    private String notas;
    private EstadoPago estado;
    private TipoPago tipoPago;
}
