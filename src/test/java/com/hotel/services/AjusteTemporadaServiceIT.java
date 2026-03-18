package com.hotel.services;

import com.hotel.models.AjusteTemporada;
import com.hotel.models.AuditoriaEvento;
import com.hotel.models.enums.Temporada;
import com.hotel.models.enums.TipoEntidad;
import com.hotel.models.enums.TipoEvento;
import com.hotel.repositories.AjusteTemporadaRepository;
import com.hotel.repositories.AuditoriaEventoRepository;
import com.hotel.services.support.AbstractServiceIT;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static com.hotel.testutils.AuditoriaEventoTestHelper.parseDetalleCambios;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AjusteTemporadaServiceIT extends AbstractServiceIT {

    @Autowired
    private AjusteTemporadaService ajusteTemporadaService;

    @Autowired
    private AjusteTemporadaRepository ajusteTemporadaRepository;

    @Autowired
    private AuditoriaEventoRepository auditoriaEventoRepository;

    @Test
    void exitoObteniendoTemporadaActivaInicial_test() {

        // ---------- WHEN ----------
        Temporada activa = ajusteTemporadaService.obtenerTemporadaActiva();

        // ---------- THEN ----------
        assertThat(activa).isEqualTo(Temporada.BAJA);
    }

    @Test
    void exitoObteniendoTemporadaActivaCuandoSoloAltaEstaActiva_test() {

        // ---------- GIVEN ----------
        AjusteTemporada baja = ajusteTemporadaRepository.findByTemporada(Temporada.BAJA).orElseThrow();
        AjusteTemporada alta = ajusteTemporadaRepository.findByTemporada(Temporada.ALTA).orElseThrow();
        baja.setActivo(false);
        alta.setActivo(true);
        ajusteTemporadaRepository.save(baja);
        ajusteTemporadaRepository.save(alta);

        // ---------- WHEN ----------
        Temporada activa = ajusteTemporadaService.obtenerTemporadaActiva();

        // ---------- THEN ----------
        assertThat(activa).isEqualTo(Temporada.ALTA);
        assertThat(ajusteTemporadaRepository.findByTemporada(Temporada.BAJA).orElseThrow().isActivo()).isFalse();
        assertThat(ajusteTemporadaRepository.findByTemporada(Temporada.ALTA).orElseThrow().isActivo()).isTrue();
    }

    @Test
    void exitoCambiandoTemporadaActivaActualizaEstadosYGeneraEvento_test() {

        // ---------- GIVEN ----------
        long eventosAntes = auditoriaEventoRepository.count();

        // ---------- WHEN ----------
        ajusteTemporadaService.cambiarTemporadaActiva(Temporada.ALTA);

        // ---------- THEN ----------
        AjusteTemporada alta = ajusteTemporadaRepository.findByTemporada(Temporada.ALTA).orElseThrow();
        AjusteTemporada baja = ajusteTemporadaRepository.findByTemporada(Temporada.BAJA).orElseThrow();
        assertThat(alta.isActivo()).isTrue();
        assertThat(baja.isActivo()).isFalse();
        assertThat(ajusteTemporadaService.obtenerTemporadaActiva()).isEqualTo(Temporada.ALTA);

        assertThat(auditoriaEventoRepository.count()).isEqualTo(eventosAntes + 1);
        AuditoriaEvento evento = auditoriaEventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.AJUSTE_TEMPORADA,
                alta.getId()
        ).orElseThrow();
        assertThat(evento.getTipoEvento()).isEqualTo(TipoEvento.MODIFICACION_TEMPORADA_ACTIVA);
        Map<String, Map<String, Object>> detalle = parseDetalleCambios(evento.getDetalle());
        assertThat(detalle).containsKey("temporadaActiva");
        assertThat(detalle.get("temporadaActiva")).containsEntry("anterior", "BAJA");
        assertThat(detalle.get("temporadaActiva")).containsEntry("nuevo", "ALTA");
    }

    @Test
    void exitoCambiandoTemporadaActivaSinCambioNoGeneraEvento_test() {

        // ---------- GIVEN ----------
        long eventosAntes = auditoriaEventoRepository.count();
        assertThat(ajusteTemporadaService.obtenerTemporadaActiva()).isEqualTo(Temporada.BAJA);

        // ---------- WHEN ----------
        ajusteTemporadaService.cambiarTemporadaActiva(Temporada.BAJA);

        // ---------- THEN ----------
        assertThat(auditoriaEventoRepository.count()).isEqualTo(eventosAntes);
    }

    @Test
    void exitoObteniendoTemporadaActivaCuandoNoHayActiva_test() {

        // ---------- GIVEN ----------
        AjusteTemporada baja = ajusteTemporadaRepository.findByTemporada(Temporada.BAJA).orElseThrow();
        AjusteTemporada alta = ajusteTemporadaRepository.findByTemporada(Temporada.ALTA).orElseThrow();
        baja.setActivo(false);
        alta.setActivo(false);
        ajusteTemporadaRepository.save(baja);
        ajusteTemporadaRepository.save(alta);

        // ---------- WHEN ----------
        Temporada activa = ajusteTemporadaService.obtenerTemporadaActiva();

        // ---------- THEN ----------
        AjusteTemporada bajaDb = ajusteTemporadaRepository.findByTemporada(Temporada.BAJA).orElseThrow();
        AjusteTemporada altaDb = ajusteTemporadaRepository.findByTemporada(Temporada.ALTA).orElseThrow();
        assertThat(activa).isEqualTo(Temporada.BAJA);
        assertThat(bajaDb.isActivo()).isTrue();
        assertThat(altaDb.isActivo()).isFalse();
    }

    @Test
    void exitoObteniendoTemporadaActivaCuandoAmbasActivasCorrigeABaja_test() {

        // ---------- GIVEN ----------
        AjusteTemporada baja = ajusteTemporadaRepository.findByTemporada(Temporada.BAJA).orElseThrow();
        AjusteTemporada alta = ajusteTemporadaRepository.findByTemporada(Temporada.ALTA).orElseThrow();
        baja.setActivo(true);
        alta.setActivo(true);
        ajusteTemporadaRepository.save(baja);
        ajusteTemporadaRepository.save(alta);

        // ---------- WHEN ----------
        Temporada activa = ajusteTemporadaService.obtenerTemporadaActiva();

        // ---------- THEN ----------
        AjusteTemporada bajaDb = ajusteTemporadaRepository.findByTemporada(Temporada.BAJA).orElseThrow();
        AjusteTemporada altaDb = ajusteTemporadaRepository.findByTemporada(Temporada.ALTA).orElseThrow();
        assertThat(activa).isEqualTo(Temporada.BAJA);
        assertThat(bajaDb.isActivo()).isTrue();
        assertThat(altaDb.isActivo()).isFalse();
    }

    @Test
    void exitoCambiandoTemporadaActivaCreaRegistrosSiNoExisten_test() {

        // ---------- GIVEN ----------
        ajusteTemporadaRepository.deleteAll();

        // ---------- WHEN ----------
        ajusteTemporadaService.cambiarTemporadaActiva(Temporada.ALTA);

        // ---------- THEN ----------
        AjusteTemporada alta = ajusteTemporadaRepository.findByTemporada(Temporada.ALTA).orElseThrow();
        AjusteTemporada baja = ajusteTemporadaRepository.findByTemporada(Temporada.BAJA).orElseThrow();
        assertThat(alta.isActivo()).isTrue();
        assertThat(baja.isActivo()).isFalse();
        assertThat(ajusteTemporadaService.obtenerTemporadaActiva()).isEqualTo(Temporada.ALTA);
    }
}
