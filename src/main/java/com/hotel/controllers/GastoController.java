package com.hotel.controllers;

import com.hotel.dtos.gasto.GastoDTO;
import com.hotel.dtos.gasto.GastoNuevoRequestDTO;
import com.hotel.services.GastoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gastos")
public class GastoController {

    private final GastoService gastoService;

    public GastoController(GastoService gastoService) {
        this.gastoService = gastoService;
    }

    @GetMapping
    public List<GastoDTO> listar() {
        return gastoService.listar();
    }

    @GetMapping("/{id}")
    public GastoDTO obtenerPorId(@PathVariable Long id) {
        return gastoService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<GastoDTO> crear(@Valid @RequestBody GastoNuevoRequestDTO gasto) {
        GastoDTO creado = gastoService.crear(gasto);
        return ResponseEntity.ok(creado);
    }

    @PutMapping("/{id}")
    public GastoDTO actualizar(@PathVariable Long id, @Valid @RequestBody GastoNuevoRequestDTO gasto) {
        return gastoService.actualizar(id, gasto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        gastoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
