package com.hotel.dtos;

import com.hotel.dtos.estancia.EstanciaDTO;
import com.hotel.dtos.reserva.ReservaCalendarioDTO;
import lombok.Data;

import java.util.List;

@Data
public class DetalleDTO {
    private UnidadDTO unidad;
    private EstanciaDTO estancia;
    private ReservaCalendarioDTO reserva;

}
