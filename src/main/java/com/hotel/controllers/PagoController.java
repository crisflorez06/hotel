package com.hotel.controllers;

import com.hotel.dtos.pago.CalcularPagoDTO;
import com.hotel.dtos.pago.PagoDTO;
import com.hotel.dtos.pago.PagoNuevoRequestDTO;
import com.hotel.models.enums.EstadoPago;
import com.hotel.models.enums.MedioPago;
import com.hotel.models.enums.TipoPago;
import com.hotel.services.PagoService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pagos")
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @GetMapping
    public Page<PagoDTO> buscarPagos(
            @RequestParam(required = false) List<EstadoPago> estados,
            @RequestParam(required = false) List<MedioPago> mediosPago,
            @RequestParam(required = false) TipoPago tipoPago,
            @RequestParam(required = false) String codigoEstancia,
            @RequestParam(required = false) String codigoReserva,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            Pageable pageable) {
        return pagoService.buscarPagos(estados, mediosPago, tipoPago, codigoEstancia, codigoReserva, fechaDesde, fechaHasta, pageable);
    }

    @PostMapping
    public ResponseEntity<PagoDTO> crear(
            @RequestParam("idEstancia") Long idEstancia,
            @Valid @RequestBody PagoNuevoRequestDTO request) {
        PagoDTO creado = pagoService.crearPago(request, idEstancia);
        return ResponseEntity.ok(creado);
    }

    @PostMapping("/total")
    public BigDecimal calcularTotal(@Valid @RequestBody CalcularPagoDTO request) {
        return pagoService.obtenerEstimacionPago(request);
    }

    @PutMapping("/eliminar/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable("id") Long idPago) {
        pagoService.eliminarPago(idPago);
        return ResponseEntity.noContent().build();
    }
}
