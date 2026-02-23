package com.hotel.dtos.estancia;

import com.hotel.dtos.pago.PagoNuevoRequestDTO;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ActivarEstanciaDTO {

    @NotNull (message = "idReserva es obligatorio")
    private Long idReserva;

    @NotNull (message = "idCliente es obligatorio")
    private Long idCliente;

    @NotNull (message = "entradaReal es obligatoria")
    private LocalDateTime entradaReal;

    @NotNull (message = "salidaEstimada es obligatoria")
    private LocalDateTime salidaEstimada;

    private PagoNuevoRequestDTO pago;

    private List<Long> idAcompanantes;

    private String notas;

}
