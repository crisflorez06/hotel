package com.hotel.controllers;

import com.hotel.dtos.CalcularPagoDTO;
import com.hotel.dtos.PagoDTO;
import com.hotel.models.enums.EstadoPago;
import com.hotel.models.enums.MedioPago;
import com.hotel.models.enums.TipoPago;
import com.hotel.services.PagoService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            Pageable pageable) {
        return pagoService.buscarPagos(estados, mediosPago, tipoPago, fechaDesde, fechaHasta, pageable);
    }

    @PostMapping("/total")
    public double calcularTotal(@RequestBody CalcularPagoDTO request) {
        return pagoService.calcularTotalPagos(request);
    }
}
