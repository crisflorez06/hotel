package com.hotel.dtos.estancia;

import com.hotel.dtos.pago.PagoNuevoRequestDTO;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SalidaEstanciaDTO {

    @NotNull(message = "idEstancia es obligatorio")
    Long idEstancia;

    @NotNull(message = "fechaSalidaReal es obligatoria")
    LocalDateTime fechaSalidaReal;

    String notasSalida;

    @NotNull(message = "pagoEstancia es obligatorio")
    PagoNuevoRequestDTO pagoEstancia;

}
