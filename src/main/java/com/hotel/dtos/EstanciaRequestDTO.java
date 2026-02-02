package com.hotel.dtos;

import com.hotel.models.enums.TipoUnidad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class EstanciaRequestDTO {


    @NotNull(message = "tipoUnidad es obligatorio")
    private TipoUnidad tipoUnidad;

    @NotBlank(message = "codigo es obligatorio")
    private String codigo;

    @NotNull(message = "idCliente es obligatorio")
    private Long idCliente;

    @NotNull(message = "entradaReal es obligatoria")
    private LocalDateTime entradaReal;

    @NotNull(message = "salidaEstimada es obligatoria")
    private LocalDateTime salidaEstimada;

    private String notas;

    private PagoNuevoRequestDTO pago;

    private List<Long> idAcompanantes;

}
