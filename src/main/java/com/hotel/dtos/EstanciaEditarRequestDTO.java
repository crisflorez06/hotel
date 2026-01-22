package com.hotel.dtos;

import com.hotel.models.enums.TipoUnidad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EstanciaEditarRequestDTO {


    private Long idCliente;
    private LocalDateTime entradaReal;
    private LocalDateTime salidaEstimada;
    private List<Long> idAcompanantes;
    private String notas;

}
