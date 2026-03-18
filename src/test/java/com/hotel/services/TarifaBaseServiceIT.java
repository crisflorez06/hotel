package com.hotel.services;

import com.hotel.models.TarifaBase;
import com.hotel.models.enums.Temporada;
import com.hotel.models.enums.TipoCalculo;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.TarifaBaseRepository;
import com.hotel.services.support.AbstractServiceIT;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TarifaBaseServiceIT extends AbstractServiceIT {

    @Autowired
    private TarifaBaseService tarifaBaseService;

    @Autowired
    private TarifaBaseRepository tarifaBaseRepository;

    @Autowired
    private AjusteTemporadaService ajusteTemporadaService;

    @Test
    void exitoObteniendoPrecioDiaEstandarEnTemporadaBaja_test() {

        // ---------- GIVEN ----------
        ajusteTemporadaService.cambiarTemporadaActiva(Temporada.BAJA);

        // ---------- WHEN ----------
        BigDecimal precio = tarifaBaseService.obtenerPrecioDiaPorTipoUnidad(
                TipoUnidad.APARTAMENTO,
                TipoCalculo.ESTANDAR
        );

        // ---------- THEN ----------
        assertThat(precio).isEqualByComparingTo(BigDecimal.valueOf(250000));
    }

    @Test
    void exitoObteniendoPrecioDiaEstandarEnTemporadaAlta_test() {

        // ---------- GIVEN ----------
        ajusteTemporadaService.cambiarTemporadaActiva(Temporada.ALTA);

        // ---------- WHEN ----------
        BigDecimal precio = tarifaBaseService.obtenerPrecioDiaPorTipoUnidad(
                TipoUnidad.APARTAMENTO,
                TipoCalculo.ESTANDAR
        );

        // ---------- THEN ----------
        assertThat(precio).isEqualByComparingTo(BigDecimal.valueOf(300000));
    }

    @Test
    void exitoObteniendoPrecioDiaEstadiaCortaIgnoraTemporada_test() {

        // ---------- GIVEN ----------
        ajusteTemporadaService.cambiarTemporadaActiva(Temporada.ALTA);

        // ---------- WHEN ----------
        BigDecimal precioAlta = tarifaBaseService.obtenerPrecioDiaPorTipoUnidad(
                TipoUnidad.APARTAESTUDIO,
                TipoCalculo.ESTADIA_CORTA
        );

        ajusteTemporadaService.cambiarTemporadaActiva(Temporada.BAJA);
        BigDecimal precioBaja = tarifaBaseService.obtenerPrecioDiaPorTipoUnidad(
                TipoUnidad.APARTAESTUDIO,
                TipoCalculo.ESTADIA_CORTA
        );

        // ---------- THEN ----------
        assertThat(precioAlta).isEqualByComparingTo(BigDecimal.valueOf(2200000));
        assertThat(precioBaja).isEqualByComparingTo(BigDecimal.valueOf(2200000));
    }

    @Test
    void exitoObteniendoPrecioPersonaAdicionalEstandarPorTemporada_test() {

        // ---------- GIVEN ----------
        ajusteTemporadaService.cambiarTemporadaActiva(Temporada.BAJA);
        BigDecimal precioBaja = tarifaBaseService.obtenerPrecioPersonaAdicional(
                TipoUnidad.HABITACION,
                TipoCalculo.ESTANDAR
        );

        ajusteTemporadaService.cambiarTemporadaActiva(Temporada.ALTA);
        BigDecimal precioAlta = tarifaBaseService.obtenerPrecioPersonaAdicional(
                TipoUnidad.HABITACION,
                TipoCalculo.ESTANDAR
        );

        // ---------- THEN ----------
        assertThat(precioBaja).isEqualByComparingTo(BigDecimal.valueOf(85000));
        assertThat(precioAlta).isEqualByComparingTo(BigDecimal.valueOf(90000));
    }

    @Test
    void exitoObteniendoPrecioPersonaAdicionalEstadiaCorta_test() {

        // ---------- GIVEN ----------
        ajusteTemporadaService.cambiarTemporadaActiva(Temporada.ALTA);

        // ---------- WHEN ----------
        BigDecimal precio = tarifaBaseService.obtenerPrecioPersonaAdicional(
                TipoUnidad.APARTAMENTO,
                TipoCalculo.ESTADIA_CORTA
        );

        // ---------- THEN ----------
        assertThat(precio).isEqualByComparingTo(BigDecimal.valueOf(800000));
    }

    @Test
    void falloObteniendoPrecioDiaCuandoNoExisteTarifaBase_test() {

        // ---------- GIVEN ----------
        TarifaBase tarifa = tarifaBaseRepository.findByTipoUnidad(TipoUnidad.HABITACION).orElseThrow();
        tarifaBaseRepository.delete(tarifa);

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> tarifaBaseService.obtenerPrecioDiaPorTipoUnidad(
                TipoUnidad.HABITACION,
                TipoCalculo.ESTANDAR
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("TarifaBase no encontrada para tipo unidad: HABITACION");
    }

    @Test
    void falloObteniendoPrecioPersonaAdicionalCuandoNoExisteTarifaBase_test() {

        // ---------- GIVEN ----------
        TarifaBase tarifa = tarifaBaseRepository.findByTipoUnidad(TipoUnidad.APARTAESTUDIO).orElseThrow();
        tarifaBaseRepository.delete(tarifa);

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> tarifaBaseService.obtenerPrecioPersonaAdicional(
                TipoUnidad.APARTAESTUDIO,
                TipoCalculo.ESTANDAR
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("TarifaBase no encontrada para tipo unidad: APARTAESTUDIO");
    }
}
