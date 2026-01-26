package com.hotel.dtos;

import com.hotel.models.Pago;
import com.hotel.models.enums.CanalReserva;
import com.hotel.models.enums.TipoUnidad;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
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
    private LocalDateTime entradaEstimada;

    @NotNull(message = "salidaEstimada es obligatoria")
    private LocalDateTime salidaEstimada;

    @NotNull(message = "canalReserva es obligatorio")
    private CanalReserva canalReserva;

    private String notas;

    private PagoNuevoRequestDTO pago;

}
