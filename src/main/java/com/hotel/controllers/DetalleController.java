package com.hotel.controllers;

import com.hotel.dtos.DetalleCalendarioUnidadDTO;
import com.hotel.dtos.dashboard.DashboardDistribucionFinancieraDTO;
import com.hotel.dtos.dashboard.DashboardResumenDTO;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.services.DetalleService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping("/calendario")
    public ResponseEntity<List<DetalleCalendarioUnidadDTO>> obtenerCalendario(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) TipoUnidad tipoUnidad,
            @RequestParam(required = false) String codigoUnidad,
            @RequestParam(required = false) List<EstadoReserva> estadosReserva,
            @RequestParam(required = false) List<EstadoEstancia> estadosEstancia) {
        return ResponseEntity.ok(detalleService.obtenerCalendario(
                desde,
                hasta,
                tipoUnidad,
                codigoUnidad,
                estadosReserva,
                estadosEstancia));
    }

    @GetMapping("/dashboard/resumen")
    public ResponseEntity<DashboardResumenDTO> obtenerDashboardResumen() {
        return ResponseEntity.ok(detalleService.obtenerDashboardResumen());
    }

    @GetMapping("/dashboard/financiero/distribucion")
    public ResponseEntity<DashboardDistribucionFinancieraDTO> obtenerDistribucionFinanciera(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return ResponseEntity.ok(detalleService.obtenerDistribucionFinanciera(desde, hasta));
    }
}
