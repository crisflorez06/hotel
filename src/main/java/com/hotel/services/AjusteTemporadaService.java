package com.hotel.services;

import com.hotel.models.AjusteTemporada;
import com.hotel.models.enums.Temporada;
import com.hotel.repositories.AjusteTemporadaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AjusteTemporadaService {

    private final AjusteTemporadaRepository ajusteTemporadaRepository;

    public AjusteTemporadaService(AjusteTemporadaRepository ajusteTemporadaRepository) {
        this.ajusteTemporadaRepository = ajusteTemporadaRepository;
    }

    @Transactional
    public void cambiarTemporadaActiva(Temporada temporada) {
        AjusteTemporada seleccionada = findOrCreate(temporada);
        Temporada otraTemporada = temporada == Temporada.ALTA ? Temporada.BAJA : Temporada.ALTA;
        AjusteTemporada otra = findOrCreate(otraTemporada);

        seleccionada.setActivo(true);
        otra.setActivo(false);

        ajusteTemporadaRepository.saveAll(List.of(seleccionada, otra));
    }

    public Temporada obtenerTemporadaActiva() {
        AjusteTemporada activa = ajusteTemporadaRepository.findByActivoTrue().orElse(null);
        if (activa != null) {
            return activa.getTemporada();
        }

        AjusteTemporada baja = findOrCreate(Temporada.BAJA);
        AjusteTemporada alta = findOrCreate(Temporada.ALTA);

        if (!baja.isActivo() && !alta.isActivo()) {
            baja.setActivo(true);
            ajusteTemporadaRepository.saveAll(List.of(baja, alta));
            return Temporada.BAJA;
        }

        if (baja.isActivo() && !alta.isActivo()) {
            return Temporada.BAJA;
        }

        if (alta.isActivo() && !baja.isActivo()) {
            return Temporada.ALTA;
        }

        baja.setActivo(true);
        alta.setActivo(false);
        ajusteTemporadaRepository.saveAll(List.of(baja, alta));
        return Temporada.BAJA;
    }

    private AjusteTemporada findOrCreate(Temporada temporada) {
        return ajusteTemporadaRepository.findByTemporada(temporada)
                .orElseGet(() -> ajusteTemporadaRepository.save(new AjusteTemporada(null, temporada, false)));
    }
}
