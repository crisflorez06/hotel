package com.hotel.dtos;

import lombok.Data;

@Data
public class OcupanteDTO {

    private Long id;
    private String nombres;
    private String apellidos;
    private String tipoDocumento;
    private String numeroDocumento;
    private String email;
    private String telefono;
}
