package com.hotel.controllers;

import com.hotel.dtos.tarifa.TarifaBaseActualizacionDTO;
import com.hotel.dtos.tarifa.TarifaBaseDTO;
import com.hotel.services.TarifaBaseAdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ajustes/tarifas-base")
public class TarifaBaseController {

    private final TarifaBaseAdminService tarifaBaseAdminService;

    public TarifaBaseController(TarifaBaseAdminService tarifaBaseAdminService) {
        this.tarifaBaseAdminService = tarifaBaseAdminService;
    }

    @GetMapping
    public List<TarifaBaseDTO> listarTarifasBase() {
        return tarifaBaseAdminService.listarTarifasBase();
    }

    @PutMapping
    public List<TarifaBaseDTO> actualizarTarifasBase(@Valid @RequestBody TarifaBaseActualizacionDTO actualizacion) {
        return tarifaBaseAdminService.actualizarTarifasBase(actualizacion);
    }
}
