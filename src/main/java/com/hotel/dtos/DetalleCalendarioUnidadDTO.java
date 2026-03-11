package com.hotel.dtos;

import com.hotel.dtos.estancia.EstanciaDTO;
import java.util.List;

import com.hotel.dtos.reserva.ReservaDTO;
import lombok.Data;

@Data
public class DetalleCalendarioUnidadDTO {
    private UnidadDTO unidad;
    private List<EstanciaDTO> estancias;
    private List<ReservaDTO> reservas;
    private List<DetalleCalendarioHabitacionDTO> habitaciones;
}
