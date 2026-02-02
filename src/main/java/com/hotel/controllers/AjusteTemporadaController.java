package com.hotel.controllers;

import com.hotel.models.enums.Temporada;
import com.hotel.services.AjusteTemporadaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ajustes/temporada")
public class AjusteTemporadaController {

    private final AjusteTemporadaService ajusteTemporadaService;

    public AjusteTemporadaController(AjusteTemporadaService ajusteTemporadaService) {
        this.ajusteTemporadaService = ajusteTemporadaService;
    }

    @PutMapping
    public ResponseEntity<Void> cambiarTemporada(@RequestParam Temporada temporada) {
        ajusteTemporadaService.cambiarTemporadaActiva(temporada);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/activa")
    public Temporada obtenerTemporadaActiva() {
        return ajusteTemporadaService.obtenerTemporadaActiva();
    }
}
