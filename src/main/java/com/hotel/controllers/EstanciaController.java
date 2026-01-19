package com.hotel.controllers;

import com.hotel.dtos.EstanciaRequestDTO;
import com.hotel.services.EstanciaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/estancias")
public class EstanciaController {

    private final EstanciaService estanciaService;

    public EstanciaController(EstanciaService estanciaService) {
        this.estanciaService = estanciaService;
    }

    @PostMapping
    public ResponseEntity<Void> crearEstancia(@Valid @RequestBody EstanciaRequestDTO request) {
        estanciaService.crearEstancia(request);
        return ResponseEntity.status(201).build();
    }
}
