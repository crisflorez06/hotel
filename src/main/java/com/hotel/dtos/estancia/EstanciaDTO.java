package com.hotel.dtos.estancia;

import com.hotel.dtos.ocupante.OcupanteDTO;
import com.hotel.dtos.pago.PagoDTO;
import com.hotel.dtos.reserva.ReservaDTO;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.ModoOcupacion;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EstanciaDTO {

    private Long id;
    private String codigoFolio;
    private String codigoReserva;
    private Long idReserva;
    private LocalDateTime entradaReal;
    private LocalDateTime salidaEstimada;
    private LocalDateTime salidaReal;
    private EstadoEstancia estado;
    private ModoOcupacion modoOcupacion;
    private OcupanteDTO cliente;
    private List<OcupanteDTO> acompanantes;
    private String notas;
    private List<PagoDTO> pagos;

}
