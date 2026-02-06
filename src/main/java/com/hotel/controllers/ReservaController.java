package com.hotel.controllers;

import com.hotel.dtos.reserva.ReservaCalendarioDTO;
import com.hotel.dtos.reserva.ReservaNuevaRequestDTO;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.services.ReservaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/calendario")
    public List<ReservaCalendarioDTO> obtenerReservasCalendario(
            @RequestParam String mes,
            @RequestParam(required = false) TipoUnidad tipoUnidad,
            @RequestParam(required = false) String codigoUnidad) {
        return reservaService.buscarReservasCalendario(mes, tipoUnidad, codigoUnidad);
    }

    @GetMapping("/buscar-por-documento")
    public List<ReservaCalendarioDTO> buscarReservasPorDocumento(
            @RequestParam("numero") String numeroDocumento) {
        return reservaService.buscarReservasPorNumeroDocumento(numeroDocumento);
    }
}
