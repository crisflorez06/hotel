package com.hotel.dtos.tarifa;

import com.hotel.models.enums.TipoUnidad;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class TarifaBaseActualizacionDTO {

    @NotNull(message = "tipoUnidad es obligatorio")
    private TipoUnidad tipoUnidad;

    @NotNull(message = "precioDiaTemBaja es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "precioDiaTemBaja debe ser mayor o igual a 0")
    private BigDecimal precioDiaTemBaja;

    @NotNull(message = "precioDiaTemAlta es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "precioDiaTemAlta debe ser mayor o igual a 0")
    private BigDecimal precioDiaTemAlta;

    @NotNull(message = "precioEstadiaCorta es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "precioEstadiaCorta debe ser mayor o igual a 0")
    private BigDecimal precioEstadiaCorta;

    @NotNull(message = "precioPersonaAdicionalTemBaja es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "precioPersonaAdicionalTemBaja debe ser mayor o igual a 0")
    private BigDecimal precioPersonaAdicionalTemBaja;

    @NotNull(message = "precioPersonaAdicionalTemAlta es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "precioPersonaAdicionalTemAlta debe ser mayor o igual a 0")
    private BigDecimal precioPersonaAdicionalTemAlta;

    @NotNull(message = "precioEstadiaPersonaAdicionalCorta es obligatorio")
    @DecimalMin(value = "0.0", inclusive = true, message = "precioEstadiaPersonaAdicionalCorta debe ser mayor o igual a 0")
    private BigDecimal precioEstadiaPersonaAdicionalCorta;
}
