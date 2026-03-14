package com.hotel.controllers;

import com.hotel.dtos.estancia.EstanciaDTO;
import com.hotel.dtos.estancia.EstanciaRequestDTO;
import com.hotel.dtos.estancia.ActivarEstanciaDTO;
import com.hotel.dtos.estancia.SalidaEstanciaDTO;
import com.hotel.dtos.estancia.EstanciaTablaDTO;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.ModoOcupacion;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.services.EstanciaService;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
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
    public ResponseEntity<EstanciaDTO> crearEstancia(@Valid @RequestBody EstanciaRequestDTO request) {
        EstanciaDTO creada = estanciaService.crearEstanciaNueva(request);
        return ResponseEntity.status(201).body(creada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> editarEstancia(
            @PathVariable("id") Long id,
            @Valid @RequestBody EstanciaRequestDTO request) {
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
    public ResponseEntity<EstanciaDTO> activarEstancia(@Valid @RequestBody ActivarEstanciaDTO request) {
        EstanciaDTO activada = estanciaService.activarEstancia(request);
        return ResponseEntity.status(201).body(activada);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstanciaDTO> obtenerEstancia(
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(estanciaService.obtenerEstancia(id));
    }

    @GetMapping("/tabla")
    public Page<EstanciaTablaDTO> buscarEstanciasTabla(
            @RequestParam(required = false) Long idEstancia,
            @RequestParam(required = false) List<EstadoEstancia> estados,
            @RequestParam(required = false) TipoUnidad tipoUnidad,
            @RequestParam(required = false) ModoOcupacion modoOcupacion,
            @RequestParam(required = false) String codigoEstancia,
            @RequestParam(required = false) String codigoUnidad,
            @RequestParam(required = false) String nombreCliente,
            @RequestParam(required = false) String numeroDocumentoCliente,
            @RequestParam(required = false) Long idCliente,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime entradaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime entradaHasta,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime salidaEstimadaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime salidaEstimadaHasta,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime salidaRealDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime salidaRealHasta,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime rangoGeneralDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime rangoGeneralHasta,
            @RequestParam(required = false) Boolean tieneReservaAsociada,
            Pageable pageable) {
        return estanciaService.buscarEstanciasTabla(
                idEstancia,
                estados,
                tipoUnidad,
                modoOcupacion,
                codigoEstancia,
                codigoUnidad,
                nombreCliente,
                numeroDocumentoCliente,
                idCliente,
                entradaDesde,
                entradaHasta,
                salidaEstimadaDesde,
                salidaEstimadaHasta,
                salidaRealDesde,
                salidaRealHasta,
                rangoGeneralDesde,
                rangoGeneralHasta,
                tieneReservaAsociada,
                pageable);
    }
}
