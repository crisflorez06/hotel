package com.hotel.dtos.estancia;

import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.ModoOcupacion;
import com.hotel.models.enums.TipoUnidad;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class EstanciaTablaDTO {
    private Long id;
    private String codigoEstancia;
    private String codigoUnidad;
    private TipoUnidad tipoUnidad;
    private String nombreCliente;
    private Long idCliente;
    private String numeroDocumentoCliente;
    private EstadoEstancia estadoEstancia;
    private ModoOcupacion modoOcupacion;
    private LocalDateTime entradaReal;
    private LocalDateTime salidaEstimada;
    private LocalDateTime salidaReal;
    private Boolean tieneReservaAsociada;
    private Long idReservaAsociada;
    private String codigoReservaAsociada;
}
