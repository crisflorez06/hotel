package com.hotel.dtos.ocupante;

import com.hotel.models.enums.TipoDocumento;
import java.util.List;
import lombok.Data;

@Data
public class ClienteTablaDTO {
    private String nombres;
    private String apellidos;
    private TipoDocumento tipoDocumento;
    private String numeroDocumento;
    private String telefono;
    private String email;
    private String tipoOcupante;
    private List<ClienteReservaResumenDTO> reservas;
    private List<ClienteEstanciaResumenDTO> estancias;
}
