package com.hotel.dtos.tarifa;

import com.hotel.models.enums.TipoUnidad;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class TarifaBaseDTO {

    private TipoUnidad tipoUnidad;
    private BigDecimal precioDiaTemBaja;
    private BigDecimal precioDiaTemAlta;
    private BigDecimal precioEstadiaCorta;
    private BigDecimal precioPersonaAdicionalTemBaja;
    private BigDecimal precioPersonaAdicionalTemAlta;
    private BigDecimal precioEstadiaPersonaAdicionalCorta;
}
