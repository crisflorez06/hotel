package com.hotel.dtos;

import com.hotel.models.enums.TipoUnidad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EstanciaRequestDTO {


    @NotNull(message = "tipoUnidad es obligatorio")
    private TipoUnidad tipoUnidad;

    @NotBlank(message = "codigo es obligatorio")
    private String codigo;

    private Long idReserva;

    @NotNull(message = "idCliente es obligatorio")
    private Long idCliente;

    @NotNull(message = "entradaReal es obligatoria")
    private LocalDateTime entradaReal;

    @NotNull(message = "salidaEstimada es obligatoria")
    private LocalDateTime salidaEstimada;

    private List<Long> idAcompanantes;

    private String notas;

}
