package com.hotel.controllers;

import com.hotel.dtos.DetalleDTO;
import com.hotel.services.DetalleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/detalle")
public class DetalleController {

    private final DetalleService detalleService;

    public DetalleController(DetalleService detalleService) {
        this.detalleService = detalleService;
    }

    @GetMapping("/estancia/{id}")
    public ResponseEntity<DetalleDTO> obtenerDetallePorEstancia(@PathVariable("id") Long id) {
        return ResponseEntity.ok(detalleService.obtenerDetallePorEstanciaId(id));
    }
}
