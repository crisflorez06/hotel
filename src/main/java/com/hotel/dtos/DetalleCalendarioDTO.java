package com.hotel.dtos;

import com.hotel.dtos.estancia.EstanciaCalendarioDTO;
import com.hotel.dtos.reserva.ReservaCalendarioDTO;
import java.util.List;
import lombok.Data;

@Data
public class DetalleCalendarioDTO {
    private List<EstanciaCalendarioDTO> estancias;
    private List<ReservaCalendarioDTO> reservas;
}
