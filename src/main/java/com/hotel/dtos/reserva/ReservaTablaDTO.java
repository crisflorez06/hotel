package com.hotel.dtos.reserva;

import com.hotel.models.enums.CanalReserva;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.models.enums.ModoOcupacion;
import com.hotel.models.enums.TipoUnidad;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class ReservaTablaDTO {

    private Long id;
    private String codigoReserva;
    private String codigoEstancia;
    private String codigoUnidad;
    private TipoUnidad tipoUnidad;
    private String nombreCliente;
    private Long idCliente;
    private String numeroDocumentoCliente;
    private Integer numeroPersonas;
    private CanalReserva canalReserva;
    private ModoOcupacion modoOcupacion;
    private EstadoReserva estadoReserva;
    private LocalDateTime fechaCreacion;
    private LocalDateTime entradaEstimada;
    private LocalDateTime salidaEstimada;
    private Boolean tieneEstanciaAsociada;
    private BigDecimal totalPagoReserva;
    private Integer cantidadPagosModificadosOEliminados;
}
