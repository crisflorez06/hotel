package com.hotel.controllers;

import com.hotel.dtos.auditoria.AuditoriaEventoDTO;
import com.hotel.models.enums.TipoEntidad;
import com.hotel.models.enums.TipoEvento;
import com.hotel.services.AuditoriaEventoService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/eventos")
public class AuditoriaEventoController {

    private final AuditoriaEventoService auditoriaEventoService;

    public AuditoriaEventoController(AuditoriaEventoService auditoriaEventoService) {
        this.auditoriaEventoService = auditoriaEventoService;
    }

    @GetMapping
    public Page<AuditoriaEventoDTO> buscarEventos(
            @RequestParam(required = false) List<TipoEvento> tiposEvento,
            @RequestParam(required = false) List<TipoEntidad> entidades,
            @RequestParam(required = false) Long idEntidad,
            @RequestParam(required = false) String codigoEstancia,
            @RequestParam(required = false) String codigoReserva,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @RequestParam(required = false) String detalle,
            Pageable pageable) {
        return auditoriaEventoService.buscarEventos(
                tiposEvento,
                entidades,
                idEntidad,
                codigoEstancia,
                codigoReserva,
                fechaDesde,
                fechaHasta,
                detalle,
                pageable);
    }
}
