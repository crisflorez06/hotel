package com.hotel.dtos;

import java.time.LocalDateTime;

import com.hotel.models.enums.EstadoReserva;
import com.hotel.models.enums.TipoUnidad;
import lombok.Data;

@Data
public class ReservaCalendarioDTO {

    private Long id;
    private LocalDateTime inicio;
    private LocalDateTime fin;
    private EstadoReserva estado;
    private String codigoUnidad;
    private TipoUnidad tipoUnidad;
    private Integer numeroPersonas;
    private String nombreCliente;
}
