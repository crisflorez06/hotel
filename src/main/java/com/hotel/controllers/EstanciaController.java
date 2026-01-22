package com.hotel.controllers;

import com.hotel.dtos.EstanciaEditarRequestDTO;
import com.hotel.dtos.EstanciaNuevoRequestDTO;
import com.hotel.services.EstanciaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/estancias")
public class EstanciaController {

    private final EstanciaService estanciaService;

    public EstanciaController(EstanciaService estanciaService) {
        this.estanciaService = estanciaService;
    }

    @PostMapping
    public ResponseEntity<Void> crearEstancia(@Valid @RequestBody EstanciaNuevoRequestDTO request) {
        estanciaService.crearEstancia(request);
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> editarEstancia(
            @PathVariable("id") Long id,
            @RequestBody EstanciaEditarRequestDTO request) {
        estanciaService.editarEstancia(request, id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminarEstancia(@PathVariable("id") Long id) {
        estanciaService.eliminarEstancia(id);
        return ResponseEntity.noContent().build();
    }
}
