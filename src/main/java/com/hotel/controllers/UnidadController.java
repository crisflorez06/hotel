package com.hotel.controllers;

import com.hotel.dtos.UnidadDTO;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.Piso;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.services.UnidadService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recepcion/unidades")
public class UnidadController {

    private final UnidadService unidadService;

    public UnidadController(UnidadService unidadService) {
        this.unidadService = unidadService;
    }

    @GetMapping
    public List<UnidadDTO> buscarUnidades(
            @RequestParam(required = false) TipoUnidad tipo,
            @RequestParam(required = false, name = "estado") List<EstadoOperativo> estados,
            @RequestParam(required = false, name = "piso") List<Piso> pisos,
            @RequestParam(required = false) String codigo) {
        return unidadService.buscarUnidades(tipo, estados, pisos, codigo);
    }
}
