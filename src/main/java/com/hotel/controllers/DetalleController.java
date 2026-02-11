package com.hotel.controllers;

import com.hotel.dtos.DetalleCalendarioDTO;
import com.hotel.dtos.DetalleDTO;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.services.DetalleService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @GetMapping("/calendario")
    public ResponseEntity<DetalleCalendarioDTO> obtenerCalendario(
            @RequestParam String mes,
            @RequestParam(required = false) TipoUnidad tipoUnidad,
            @RequestParam(required = false) String codigoUnidad,
            @RequestParam(required = false) List<EstadoReserva> estadosReserva,
            @RequestParam(required = false) List<EstadoEstancia> estadosEstancia) {
        return ResponseEntity.ok(detalleService.obtenerCalendario(
                mes,
                tipoUnidad,
                codigoUnidad,
                estadosReserva,
                estadosEstancia));
    }
}
