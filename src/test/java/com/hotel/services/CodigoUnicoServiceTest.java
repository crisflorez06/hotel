package com.hotel.services;

import com.hotel.repositories.EstanciaRepository;
import com.hotel.repositories.ReservaRepository;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodigoUnicoServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private EstanciaRepository estanciaRepository;

    @InjectMocks
    private CodigoUnicoService codigoUnicoService;

    @Test
    void exitoGenerandoCodigoReservaFormatoValido_test() {

        // ---------- GIVEN ----------
        when(reservaRepository.existsByCodigo(anyString())).thenReturn(false);

        // ---------- WHEN ----------
        String codigo = codigoUnicoService.generarCodigoReserva();

        // ---------- THEN ----------
        assertThat(codigo).matches("^RES-[1-9][0-9]{4}[A-HJ-NP-Z]$");
        verify(reservaRepository, times(1)).existsByCodigo(codigo);
    }

    @Test
    void exitoGenerandoCodigoEstanciaFormatoValido_test() {

        // ---------- GIVEN ----------
        when(estanciaRepository.existsByCodigoFolio(anyString())).thenReturn(false);

        // ---------- WHEN ----------
        String codigo = codigoUnicoService.generarCodigoEstancia();

        // ---------- THEN ----------
        assertThat(codigo).matches("^EST-[1-9][0-9]{4}[A-HJ-NP-Z]$");
        verify(estanciaRepository, times(1)).existsByCodigoFolio(codigo);
    }

    @Test
    void exitoGenerandoCodigoReservaReintentandoColision_test() {

        // ---------- GIVEN ----------
        AtomicInteger intentos = new AtomicInteger(0);
        when(reservaRepository.existsByCodigo(anyString()))
                .thenAnswer(invocation -> intentos.getAndIncrement() == 0);

        // ---------- WHEN ----------
        String codigo = codigoUnicoService.generarCodigoReserva();

        // ---------- THEN ----------
        assertThat(codigo).startsWith("RES-");
        verify(reservaRepository, times(2)).existsByCodigo(anyString());
    }

    @Test
    void falloGenerandoCodigoReservaTrasMaximoReintentos_test() {

        // ---------- GIVEN ----------
        when(reservaRepository.existsByCodigo(anyString())).thenReturn(true);

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> codigoUnicoService.generarCodigoReserva())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se pudo generar un codigo unico para reserva");
    }

    @Test
    void falloGenerandoCodigoEstanciaTrasMaximoReintentos_test() {

        // ---------- GIVEN ----------
        when(estanciaRepository.existsByCodigoFolio(anyString())).thenReturn(true);

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> codigoUnicoService.generarCodigoEstancia())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se pudo generar un codigo unico para estancia");
    }
}
