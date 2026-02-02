package com.hotel.dtos;

import com.hotel.models.enums.ModoOcupacion;
import com.hotel.models.enums.TipoUnidad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EstanciaDTO {

    private Long id;
    private String codigoFolio;
    private LocalDateTime fechaCreacion;
    private LocalDateTime entradaReal;
    private LocalDateTime salidaEstimada;
    private ModoOcupacion modoOcupacion;
    private List<OcupanteDTO> ocupantes;
    private String notas;
    private PagoDTO pago;

}
