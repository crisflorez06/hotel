package com.hotel.dtos.estancia;

import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.ModoOcupacion;
import com.hotel.models.enums.TipoDocumento;
import com.hotel.models.enums.TipoUnidad;
import java.math.BigDecimal;
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
    private TipoDocumento tipoDocumentoCliente;
    private String numeroDocumentoCliente;
    private Integer totalPersonas;
    private EstadoEstancia estadoEstancia;
    private ModoOcupacion modoOcupacion;
    private LocalDateTime entradaReal;
    private LocalDateTime salidaEstimada;
    private LocalDateTime salidaReal;
    private String notas;
    private Boolean tieneReservaAsociada;
    private Long idReservaAsociada;
    private String codigoReservaAsociada;
    private BigDecimal totalPagoEstancia;
    private Integer cantidadPagosModificadosOEliminados;
}
