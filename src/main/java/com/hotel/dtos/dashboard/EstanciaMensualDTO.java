package com.hotel.dtos.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EstanciaMensualDTO {

    private String periodo;
    private Long cantidad;
}
