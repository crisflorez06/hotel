package com.hotel.dtos.reserva;

import com.hotel.dtos.pago.PagoNuevoRequestDTO;
import com.hotel.models.enums.CanalReserva;
import com.hotel.models.enums.TipoUnidad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ReservaNuevaRequestDTO {


    @NotNull(message = "tipoUnidad es obligatorio")
    private TipoUnidad tipoUnidad;

    @NotBlank(message = "codigo es obligatorio")
    private String codigo;

    @NotNull(message = "idOcupante es obligatorio")
    private Long idOcupante;

    @NotNull(message = "numeroPersonas es obligatorio")
    private Integer numeroPersonas;

    @NotNull(message = "entradaEstimada es obligatoria")
    private LocalDate entradaEstimada;

    @NotNull(message = "salidaEstimada es obligatoria")
    private LocalDate salidaEstimada;

    @NotNull(message = "canalReserva es obligatorio")
    private CanalReserva canalReserva;

    private String notas;

    private PagoNuevoRequestDTO pago;

}
