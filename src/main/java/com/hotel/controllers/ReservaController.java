package com.hotel.controllers;

import com.hotel.dtos.reserva.ReservaCalendarioDTO;
import com.hotel.dtos.reserva.ReservaRequestDTO;
import com.hotel.dtos.reserva.ReservaTablaDTO;
import com.hotel.models.enums.CanalReserva;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.models.enums.ModoOcupacion;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.services.ReservaService;
import java.time.LocalDateTime;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    public ResponseEntity<Void> crearReserva(@Valid @RequestBody ReservaRequestDTO request) {
        reservaService.crearReserva(request);
        return ResponseEntity.status(201).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> editarReserva(
            @PathVariable("id") Long id,
            @Valid @RequestBody ReservaRequestDTO request) {
        reservaService.editarReserva(request, id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PutMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminarReserva(@PathVariable("id") Long id) {
        reservaService.eliminarReserva(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/buscar-por-documento")
    public List<ReservaCalendarioDTO> buscarReservasPorDocumento(
            @RequestParam("numero") String numeroDocumento) {
        return reservaService.buscarReservasPorNumeroDocumento(numeroDocumento);
    }

    @GetMapping("/tabla")
    public Page<ReservaTablaDTO> buscarReservasTabla(
            @RequestParam(required = false) List<EstadoReserva> estados,
            @RequestParam(required = false) List<CanalReserva> canales,
            @RequestParam(required = false) ModoOcupacion modoOcupacion,
            @RequestParam(required = false) TipoUnidad tipoUnidad,
            @RequestParam(required = false) String codigoReserva,
            @RequestParam(required = false) String codigoUnidad,
            @RequestParam(required = false) String nombreCliente,
            @RequestParam(required = false) String numeroDocumentoCliente,
            @RequestParam(required = false) Long idCliente,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaCreacionDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaCreacionHasta,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime entradaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime entradaHasta,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime salidaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime salidaHasta,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime rangoGeneralDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime rangoGeneralHasta,
            @RequestParam(required = false) Boolean tieneEstanciaAsociada,
            Pageable pageable) {
        return reservaService.buscarReservasTabla(
                estados,
                canales,
                modoOcupacion,
                tipoUnidad,
                codigoReserva,
                codigoUnidad,
                nombreCliente,
                numeroDocumentoCliente,
                idCliente,
                fechaCreacionDesde,
                fechaCreacionHasta,
                entradaDesde,
                entradaHasta,
                salidaDesde,
                salidaHasta,
                rangoGeneralDesde,
                rangoGeneralHasta,
                tieneEstanciaAsociada,
                pageable);
    }
}
