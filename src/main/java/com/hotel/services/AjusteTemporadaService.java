package com.hotel.services;

import com.hotel.models.AjusteTemporada;
import com.hotel.models.enums.TipoEntidad;
import com.hotel.models.enums.TipoEvento;
import com.hotel.models.enums.Temporada;
import com.hotel.repositories.AjusteTemporadaRepository;
import com.hotel.utils.EventoModificadoJsonBuilder;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AjusteTemporadaService {

    private final AjusteTemporadaRepository ajusteTemporadaRepository;
    private final AuditoriaEventoService auditoriaEventoService;

    public AjusteTemporadaService(
            AjusteTemporadaRepository ajusteTemporadaRepository,
            AuditoriaEventoService auditoriaEventoService) {
        this.ajusteTemporadaRepository = ajusteTemporadaRepository;
        this.auditoriaEventoService = auditoriaEventoService;
    }

    @Transactional
    public void cambiarTemporadaActiva(Temporada temporada) {
        Temporada temporadaActivaAnterior = obtenerTemporadaActiva();

        AjusteTemporada seleccionada = findOrCreate(temporada);
        Temporada otraTemporada = temporada == Temporada.ALTA ? Temporada.BAJA : Temporada.ALTA;
        AjusteTemporada otra = findOrCreate(otraTemporada);

        seleccionada.setActivo(true);
        otra.setActivo(false);

        ajusteTemporadaRepository.saveAll(List.of(seleccionada, otra));

        if (temporadaActivaAnterior != temporada) {
            String detalle = new EventoModificadoJsonBuilder()
                    .agregarCambio("temporadaActiva", temporadaActivaAnterior, temporada)
                    .build();

            auditoriaEventoService.crearEvento(
                    TipoEvento.MODIFICACION_TEMPORADA_ACTIVA,
                    TipoEntidad.AJUSTE_TEMPORADA,
                    seleccionada.getId(),
                    detalle,
                    null,
                    null);
        }
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
