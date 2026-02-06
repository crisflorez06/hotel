package com.hotel.dtos.reserva;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReservaDTO {

    private String id;
    private LocalDateTime fechaCreacion;

}
