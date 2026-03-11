package com.hotel.dtos;

import com.hotel.dtos.estancia.EstanciaDTO;
import com.hotel.dtos.reserva.ReservaDTO;
import java.util.List;
import lombok.Data;

@Data
public class DetalleCalendarioHabitacionDTO {
    private HabitacionDTO habitacion;
    private List<EstanciaDTO> estancias;
    private List<ReservaDTO> reservas;
}
