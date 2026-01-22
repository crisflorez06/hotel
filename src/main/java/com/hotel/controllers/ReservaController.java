package com.hotel.controllers;

import com.hotel.dtos.ReservaNuevaRequestDTO;
import com.hotel.services.ReservaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    public ResponseEntity<Void> crearReserva(@Valid @RequestBody ReservaNuevaRequestDTO request) {
        reservaService.crearReserva(request);
        return ResponseEntity.status(201).build();
    }
}
