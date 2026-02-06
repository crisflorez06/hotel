package com.hotel.controllers;

import com.hotel.dtos.estancia.EstanciaDTO;
import com.hotel.dtos.estancia.EstanciaRequestDTO;
import com.hotel.dtos.estancia.ActivarEstanciaDTO;
import com.hotel.dtos.estancia.SalidaEstanciaDTO;
import com.hotel.models.enums.TipoUnidad;
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
    public ResponseEntity<Void> crearEstancia(@Valid @RequestBody EstanciaRequestDTO request) {
        estanciaService.crearEstanciaNueva(request);
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> editarEstancia(
            @PathVariable("id") Long id,
            @RequestBody EstanciaRequestDTO request) {
        estanciaService.editarEstancia(request, id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminarEstancia(@PathVariable("id") Long id) {
        estanciaService.eliminarEstancia(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/finalizar")
    public ResponseEntity<Void> finalizarEstancia(@Valid @RequestBody SalidaEstanciaDTO request) {
        estanciaService.finalizarEstancia(request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/activar")
    public ResponseEntity<Void> activarEstancia(@Valid @RequestBody ActivarEstanciaDTO request) {
        estanciaService.activarEstancia(request);
        return ResponseEntity.status(201).build();
    }

    @GetMapping("/activa")
    public ResponseEntity<EstanciaDTO> obtenerEstancia(
            @RequestParam("codigo") String codigo,
            @RequestParam("tipoUnidad") TipoUnidad tipoUnidad) {
        return ResponseEntity.ok(estanciaService.obtenerEstancia(codigo, tipoUnidad));
    }
}
