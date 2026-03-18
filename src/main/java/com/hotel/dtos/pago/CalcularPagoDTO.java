package com.hotel.dtos.pago;

import com.hotel.models.enums.TipoCalculo;
import com.hotel.models.enums.TipoUnidad;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CalcularPagoDTO {

    private Long idEstancia;

    @NotNull(message = "El tipo de unidad es obligatorio")
    private TipoUnidad tipoUnidad;

    @NotNull(message = "El número de personas es obligatorio")
    @Min(value = 1, message = "Debe haber al menos una persona")
    private Integer numeroPersonas;

    @NotNull(message = "La fecha de entrada es obligatoria")
    private LocalDateTime fechaEntrada;

    @NotNull(message = "La fecha de salida es obligatoria")
    private LocalDateTime fechaSalida;

    @NotNull(message = "El tipo de calculo es obligatorio")
    private TipoCalculo tipoCalculo;


}
