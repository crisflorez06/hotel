package com.hotel.dtos;

import com.hotel.models.enums.TipoDocumento;
import com.hotel.models.enums.TipoOcupante;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OcupanteNuevoRequestDTO {

    @NotBlank(message = "nombres es obligatorio")
    private String nombres;

    @NotBlank(message = "apellidos es obligatorio")
    private String apellidos;

    private TipoDocumento tipoDocumento;

    private String numeroDocumento;

    private String telefono;

    private String email;

    @NotNull(message = "tipoOcupante es obligatorio")
    private TipoOcupante tipoOcupante;
}
