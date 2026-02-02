package com.hotel.services;

import com.hotel.models.TarifaBase;
import com.hotel.models.enums.Temporada;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.TarifaBaseRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class TarifaBaseService {

    private final TarifaBaseRepository tarifaBaseRepository;
    private final AjusteTemporadaService ajusteTemporadaService;

    public TarifaBaseService(TarifaBaseRepository tarifaBaseRepository,
                             AjusteTemporadaService ajusteTemporadaService) {
        this.tarifaBaseRepository = tarifaBaseRepository;
        this.ajusteTemporadaService = ajusteTemporadaService;
    }

    public BigDecimal obtenerPrecioDiaPorTipoUnidad(TipoUnidad tipoUnidad) {
        TarifaBase tarifaBase = tarifaBaseRepository.findByTipoUnidad(tipoUnidad)
                .orElseThrow(() -> new EntityNotFoundException(
                        "TarifaBase no encontrada para tipo unidad: " + tipoUnidad));

        Temporada temporadaActiva = ajusteTemporadaService.obtenerTemporadaActiva();
        return temporadaActiva == Temporada.ALTA
                ? tarifaBase.getPrecioDiaTemAlta()
                : tarifaBase.getPrecioDiaTemBaja();
    }

    public BigDecimal obtenerPrecioPersonaAdicional(TipoUnidad tipoUnidad) {
        TarifaBase tarifaBase = tarifaBaseRepository.findByTipoUnidad(tipoUnidad)
                .orElseThrow(() -> new EntityNotFoundException(
                        "TarifaBase no encontrada para tipo unidad: " + tipoUnidad));

        Temporada temporadaActiva = ajusteTemporadaService.obtenerTemporadaActiva();
        return temporadaActiva == Temporada.ALTA
                ? tarifaBase.getPrecioPersonaAdicionalTemAlta()
                : tarifaBase.getPrecioPersonaAdicionalTemBaja();
    }
}
