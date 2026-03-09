package com.hotel.dtos.reserva;

import com.hotel.dtos.pago.PagoDTO;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.models.enums.TipoUnidad;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class ReservaCalendarioDTO {

    private Long id;
    private Long idEstancia;
    private LocalDateTime inicio;
    private LocalDateTime fin;
    private String codigoReserva;
    private String codigoUnidad;
    private TipoUnidad tipoUnidad;
    private Integer numeroPersonas;
    private String nombreCliente;
    private Long idCliente;
    private BigDecimal totalAnticipo;
    private EstadoReserva estadoReserva;
}
