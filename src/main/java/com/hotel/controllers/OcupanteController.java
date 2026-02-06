package com.hotel.controllers;

import com.hotel.dtos.ocupante.OcupanteDTO;
import com.hotel.dtos.ocupante.OcupanteNuevoRequestDTO;
import com.hotel.services.OcupanteService;
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
@RequestMapping("/ocupantes")
public class OcupanteController {

    private final OcupanteService ocupanteService;

    public OcupanteController(OcupanteService ocupanteService) {
        this.ocupanteService = ocupanteService;
    }

    @PostMapping
    public ResponseEntity<OcupanteDTO> crearOcupante(@Valid @RequestBody OcupanteNuevoRequestDTO request) {
        OcupanteDTO ocupante = ocupanteService.crearOcupante(request);
        return ResponseEntity.status(201).body(ocupante);
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<OcupanteDTO>> buscarPorNumeroDocumento(@RequestParam("documento") String documento) {
        List<OcupanteDTO> ocupantes = ocupanteService.buscarPorNumeroDocumento(documento);
        return ResponseEntity.ok(ocupantes);
    }
}
