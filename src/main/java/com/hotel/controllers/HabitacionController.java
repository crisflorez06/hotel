package com.hotel.controllers;

import com.hotel.dtos.HabitacionDTO;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.Piso;
import com.hotel.services.HabitacionService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recepcion/habitaciones")
public class HabitacionController {

    private final HabitacionService habitacionService;

    public HabitacionController(HabitacionService habitacionService) {
        this.habitacionService = habitacionService;
    }

    @GetMapping
    public List<HabitacionDTO> buscarHabitaciones(
            @RequestParam(required = false, name = "estado") List<EstadoOperativo> estados,
            @RequestParam(required = false, name = "piso") List<Piso> pisos,
            @RequestParam(required = false) String codigo) {
        return habitacionService.buscarHabitaciones(estados, pisos, codigo);
    }
}
