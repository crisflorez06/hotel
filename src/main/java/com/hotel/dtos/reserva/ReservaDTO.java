package com.hotel.dtos.reserva;

import com.hotel.dtos.estancia.EstanciaDTO;
import com.hotel.dtos.ocupante.OcupanteDTO;
import com.hotel.models.enums.CanalReserva;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.models.enums.ModoOcupacion;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservaDTO {

    private Long id;
    private String codigoReserva;
    private String codigoEstancia;
    private Long idEstancia;
    private OcupanteDTO cliente;
    private LocalDateTime fechaCreacion;
    private LocalDateTime entradaEstimada;
    private LocalDateTime salidaEstimada;
    private Integer numeroPersonas;
    private CanalReserva canalReserva;
    private ModoOcupacion modoOcupacion;
    private EstadoReserva estadoReserva;
    private String notas;

}
