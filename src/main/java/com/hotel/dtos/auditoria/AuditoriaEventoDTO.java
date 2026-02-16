package com.hotel.dtos.auditoria;

import com.hotel.models.enums.TipoEntidad;
import com.hotel.models.enums.TipoEvento;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaEventoDTO {

    private Long id;
    private TipoEntidad entidad;
    private Long idEntidad;
    private String codigoEstancia;
    private String codigoReserva;
    private TipoEvento tipoEvento;
    private String detalle;
    private LocalDateTime fecha;
}
