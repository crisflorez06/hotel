package com.hotel.services;

import com.hotel.models.AjusteTemporada;
import com.hotel.models.enums.Temporada;
import com.hotel.repositories.AjusteTemporadaRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.IncorrectResultSizeDataAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AjusteTemporadaServiceTest {

    @Mock
    private AjusteTemporadaRepository ajusteTemporadaRepository;

    @Mock
    private AuditoriaEventoService auditoriaEventoService;

    @InjectMocks
    private AjusteTemporadaService ajusteTemporadaService;

    @Test
    void exitoObteniendoTemporadaActivaCorrigeDuplicadoActivas_test() {

        // ---------- GIVEN ----------
        AjusteTemporada baja = new AjusteTemporada(1L, Temporada.BAJA, false);
        AjusteTemporada alta = new AjusteTemporada(2L, Temporada.ALTA, true);

        doThrow(new IncorrectResultSizeDataAccessException(1))
                .when(ajusteTemporadaRepository)
                .findByActivoTrue();
        when(ajusteTemporadaRepository.findByTemporada(Temporada.BAJA)).thenReturn(Optional.of(baja));
        when(ajusteTemporadaRepository.findByTemporada(Temporada.ALTA)).thenReturn(Optional.of(alta));

        // ---------- WHEN ----------
        Temporada activa = ajusteTemporadaService.obtenerTemporadaActiva();

        // ---------- THEN ----------
        assertThat(activa).isEqualTo(Temporada.BAJA);
        assertThat(baja.isActivo()).isTrue();
        assertThat(alta.isActivo()).isFalse();
        verify(ajusteTemporadaRepository).saveAll(argThat(ajustes -> {
            List<AjusteTemporada> lista = new ArrayList<>();
            ajustes.forEach(lista::add);
            return lista.size() == 2
                    && lista.contains(baja)
                    && lista.contains(alta);
        }));
    }

    @Test
    void exitoObteniendoTemporadaActivaCreaRegistrosSiFaltan_test() {

        // ---------- GIVEN ----------
        AjusteTemporada bajaGuardada = new AjusteTemporada(10L, Temporada.BAJA, false);
        AjusteTemporada altaGuardada = new AjusteTemporada(11L, Temporada.ALTA, false);

        when(ajusteTemporadaRepository.findByActivoTrue()).thenReturn(Optional.empty());
        when(ajusteTemporadaRepository.findByTemporada(Temporada.BAJA)).thenReturn(Optional.empty());
        when(ajusteTemporadaRepository.findByTemporada(Temporada.ALTA)).thenReturn(Optional.empty());
        when(ajusteTemporadaRepository.save(any(AjusteTemporada.class))).thenAnswer(invocation -> {
            AjusteTemporada ajuste = invocation.getArgument(0);
            if (ajuste.getTemporada() == Temporada.BAJA) {
                return bajaGuardada;
            }
            return altaGuardada;
        });

        // ---------- WHEN ----------
        Temporada activa = ajusteTemporadaService.obtenerTemporadaActiva();

        // ---------- THEN ----------
        assertThat(activa).isEqualTo(Temporada.BAJA);
        assertThat(bajaGuardada.isActivo()).isTrue();
        assertThat(altaGuardada.isActivo()).isFalse();
        verify(ajusteTemporadaRepository).saveAll(List.of(bajaGuardada, altaGuardada));
    }

    @Test
    void exitoObteniendoTemporadaActivaConInconsistenciaRetornaBajaSinCorregir_test() {

        // ---------- GIVEN ----------
        AjusteTemporada baja = new AjusteTemporada(20L, Temporada.BAJA, true);
        AjusteTemporada alta = new AjusteTemporada(21L, Temporada.ALTA, false);

        when(ajusteTemporadaRepository.findByActivoTrue()).thenReturn(Optional.empty());
        when(ajusteTemporadaRepository.findByTemporada(Temporada.BAJA)).thenReturn(Optional.of(baja));
        when(ajusteTemporadaRepository.findByTemporada(Temporada.ALTA)).thenReturn(Optional.of(alta));

        // ---------- WHEN ----------
        Temporada activa = ajusteTemporadaService.obtenerTemporadaActiva();

        // ---------- THEN ----------
        assertThat(activa).isEqualTo(Temporada.BAJA);
    }

    @Test
    void exitoObteniendoTemporadaActivaConInconsistenciaRetornaAltaSinCorregir_test() {

        // ---------- GIVEN ----------
        AjusteTemporada baja = new AjusteTemporada(30L, Temporada.BAJA, false);
        AjusteTemporada alta = new AjusteTemporada(31L, Temporada.ALTA, true);

        when(ajusteTemporadaRepository.findByActivoTrue()).thenReturn(Optional.empty());
        when(ajusteTemporadaRepository.findByTemporada(Temporada.BAJA)).thenReturn(Optional.of(baja));
        when(ajusteTemporadaRepository.findByTemporada(Temporada.ALTA)).thenReturn(Optional.of(alta));

        // ---------- WHEN ----------
        Temporada activa = ajusteTemporadaService.obtenerTemporadaActiva();

        // ---------- THEN ----------
        assertThat(activa).isEqualTo(Temporada.ALTA);
    }

    @Test
    void exitoObteniendoTemporadaActivaConAmbasActivasYSinConsultaActivaCorrigeABaja_test() {

        // ---------- GIVEN ----------
        AjusteTemporada baja = new AjusteTemporada(40L, Temporada.BAJA, true);
        AjusteTemporada alta = new AjusteTemporada(41L, Temporada.ALTA, true);

        when(ajusteTemporadaRepository.findByActivoTrue()).thenReturn(Optional.empty());
        when(ajusteTemporadaRepository.findByTemporada(Temporada.BAJA)).thenReturn(Optional.of(baja));
        when(ajusteTemporadaRepository.findByTemporada(Temporada.ALTA)).thenReturn(Optional.of(alta));

        // ---------- WHEN ----------
        Temporada activa = ajusteTemporadaService.obtenerTemporadaActiva();

        // ---------- THEN ----------
        assertThat(activa).isEqualTo(Temporada.BAJA);
        assertThat(baja.isActivo()).isTrue();
        assertThat(alta.isActivo()).isFalse();
        verify(ajusteTemporadaRepository).saveAll(List.of(baja, alta));
    }
}
