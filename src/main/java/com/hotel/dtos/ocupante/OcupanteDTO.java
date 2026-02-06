package com.hotel.dtos.ocupante;

import com.hotel.models.enums.TipoDocumento;
import com.hotel.models.enums.TipoOcupante;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class OcupanteDTO {

    private Long id;
    private String nombres;
    private String apellidos;
    private TipoDocumento tipoDocumento;
    private String numeroDocumento;
    private String email;
    private String telefono;
    private TipoOcupante tipoOcupante;
    private LocalDateTime creadoEn;
}
