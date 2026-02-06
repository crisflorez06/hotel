package com.hotel.dtos.estancia;

import com.hotel.dtos.pago.PagoNuevoRequestDTO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SalidaEstanciaDTO {

    Long idEstancia;
    LocalDateTime fechaSalidaReal;
    String notasSalida;
    PagoNuevoRequestDTO pagoEstancia;

}
