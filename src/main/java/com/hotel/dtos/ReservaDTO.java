package com.hotel.dtos;

import com.hotel.models.enums.ModoOcupacion;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReservaDTO {

    private String id;
    private LocalDateTime fechaCreacion;

}
