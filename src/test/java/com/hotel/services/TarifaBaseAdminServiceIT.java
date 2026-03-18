package com.hotel.services;

import com.hotel.dtos.tarifa.TarifaBaseActualizacionDTO;
import com.hotel.dtos.tarifa.TarifaBaseDTO;
import com.hotel.models.AuditoriaEvento;
import com.hotel.models.TarifaBase;
import com.hotel.models.enums.TipoEntidad;
import com.hotel.models.enums.TipoEvento;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.AuditoriaEventoRepository;
import com.hotel.repositories.TarifaBaseRepository;
import com.hotel.services.support.AbstractServiceIT;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static com.hotel.testutils.AuditoriaEventoTestHelper.parseDetalleCambios;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TarifaBaseAdminServiceIT extends AbstractServiceIT {

    @Autowired
    private TarifaBaseAdminService tarifaBaseAdminService;

    @Autowired
    private TarifaBaseRepository tarifaBaseRepository;

    @Autowired
    private AuditoriaEventoRepository auditoriaEventoRepository;

    @Test
    void exitoListandoTarifasBaseEnOrdenEsperado_test() {

        // ---------- WHEN ----------
        List<TarifaBaseDTO> tarifas = tarifaBaseAdminService.listarTarifasBase();

        // ---------- THEN ----------
        assertThat(tarifas).hasSize(3);
        assertThat(tarifas.get(0).getTipoUnidad()).isEqualTo(TipoUnidad.HABITACION);
        assertThat(tarifas.get(1).getTipoUnidad()).isEqualTo(TipoUnidad.APARTAESTUDIO);
        assertThat(tarifas.get(2).getTipoUnidad()).isEqualTo(TipoUnidad.APARTAMENTO);
    }

    @Test
    void falloActualizandoTarifasBaseConRequestNulo_test() {

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> tarifaBaseAdminService.actualizarTarifasBase(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Debes enviar la tarifa base a actualizar");
    }

    @Test
    void falloListandoTarifasBaseSiFaltaTipo_test() {

        // ---------- GIVEN ----------
        TarifaBase tarifaHabitacion = tarifaBaseRepository.findByTipoUnidad(TipoUnidad.HABITACION).orElseThrow();
        tarifaBaseRepository.delete(tarifaHabitacion);

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> tarifaBaseAdminService.listarTarifasBase())
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("No existe la tarifa base para tipo de unidad: HABITACION");
    }

    @Test
    void exitoActualizandoTarifasBaseConCambiosGeneraEvento_test() {

        // ---------- GIVEN ----------
        TarifaBase tarifa = tarifaBaseRepository.findByTipoUnidad(TipoUnidad.APARTAMENTO).orElseThrow();
        long eventosAntes = auditoriaEventoRepository.count();

        TarifaBaseActualizacionDTO request = requestDesdeTarifa(tarifa);
        request.setPrecioDiaTemBaja(tarifa.getPrecioDiaTemBaja().add(BigDecimal.valueOf(12345)));
        request.setPrecioEstadiaPersonaAdicionalCorta(
                tarifa.getPrecioEstadiaPersonaAdicionalCorta().add(BigDecimal.valueOf(5000))
        );

        // ---------- WHEN ----------
        List<TarifaBaseDTO> resultado = tarifaBaseAdminService.actualizarTarifasBase(request);

        // ---------- THEN ----------
        assertThat(resultado).hasSize(3);

        TarifaBase tarifaDb = tarifaBaseRepository.findByTipoUnidad(TipoUnidad.APARTAMENTO).orElseThrow();
        assertThat(tarifaDb.getPrecioDiaTemBaja()).isEqualByComparingTo(request.getPrecioDiaTemBaja());
        assertThat(tarifaDb.getPrecioEstadiaPersonaAdicionalCorta())
                .isEqualByComparingTo(request.getPrecioEstadiaPersonaAdicionalCorta());

        assertThat(auditoriaEventoRepository.count()).isEqualTo(eventosAntes + 1);
        AuditoriaEvento evento = auditoriaEventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.TARIFA_BASE,
                tarifaDb.getId()
        ).orElseThrow();
        assertThat(evento.getTipoEvento()).isEqualTo(TipoEvento.MODIFICACION_TARIFA_BASE);

        Map<String, Map<String, Object>> detalle = parseDetalleCambios(evento.getDetalle());
        assertThat(detalle).containsKeys("precioDiaTemBaja", "precioEstadiaPersonaAdicionalCorta");
    }

    @Test
    void exitoActualizandoTarifasBaseSinCambiosNoGeneraEvento_test() {

        // ---------- GIVEN ----------
        TarifaBase tarifa = tarifaBaseRepository.findByTipoUnidad(TipoUnidad.APARTAESTUDIO).orElseThrow();
        long eventosAntes = auditoriaEventoRepository.count();
        TarifaBaseActualizacionDTO request = requestDesdeTarifa(tarifa);

        // ---------- WHEN ----------
        tarifaBaseAdminService.actualizarTarifasBase(request);

        // ---------- THEN ----------
        assertThat(auditoriaEventoRepository.count()).isEqualTo(eventosAntes);
    }

    @Test
    void exitoActualizandoTarifasBaseConCambioEnTodosLosCampos_test() {

        // ---------- GIVEN ----------
        TarifaBase tarifa = tarifaBaseRepository.findByTipoUnidad(TipoUnidad.HABITACION).orElseThrow();
        TarifaBaseActualizacionDTO request = requestDesdeTarifa(tarifa);
        request.setPrecioDiaTemBaja(tarifa.getPrecioDiaTemBaja().add(BigDecimal.valueOf(1000)));
        request.setPrecioDiaTemAlta(tarifa.getPrecioDiaTemAlta().add(BigDecimal.valueOf(1000)));
        request.setPrecioEstadiaCorta(tarifa.getPrecioEstadiaCorta().add(BigDecimal.valueOf(1000)));
        request.setPrecioPersonaAdicionalTemBaja(tarifa.getPrecioPersonaAdicionalTemBaja().add(BigDecimal.valueOf(1000)));
        request.setPrecioPersonaAdicionalTemAlta(tarifa.getPrecioPersonaAdicionalTemAlta().add(BigDecimal.valueOf(1000)));
        request.setPrecioEstadiaPersonaAdicionalCorta(
                tarifa.getPrecioEstadiaPersonaAdicionalCorta().add(BigDecimal.valueOf(1000))
        );

        // ---------- WHEN ----------
        tarifaBaseAdminService.actualizarTarifasBase(request);

        // ---------- THEN ----------
        TarifaBase tarifaDb = tarifaBaseRepository.findByTipoUnidad(TipoUnidad.HABITACION).orElseThrow();
        assertThat(tarifaDb.getPrecioDiaTemBaja()).isEqualByComparingTo(request.getPrecioDiaTemBaja());
        assertThat(tarifaDb.getPrecioDiaTemAlta()).isEqualByComparingTo(request.getPrecioDiaTemAlta());
        assertThat(tarifaDb.getPrecioEstadiaCorta()).isEqualByComparingTo(request.getPrecioEstadiaCorta());
        assertThat(tarifaDb.getPrecioPersonaAdicionalTemBaja()).isEqualByComparingTo(request.getPrecioPersonaAdicionalTemBaja());
        assertThat(tarifaDb.getPrecioPersonaAdicionalTemAlta()).isEqualByComparingTo(request.getPrecioPersonaAdicionalTemAlta());
        assertThat(tarifaDb.getPrecioEstadiaPersonaAdicionalCorta())
                .isEqualByComparingTo(request.getPrecioEstadiaPersonaAdicionalCorta());

        AuditoriaEvento evento = auditoriaEventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.TARIFA_BASE,
                tarifaDb.getId()
        ).orElseThrow();
        Map<String, Map<String, Object>> detalle = parseDetalleCambios(evento.getDetalle());
        assertThat(detalle).containsKeys(
                "precioDiaTemBaja",
                "precioDiaTemAlta",
                "precioEstadiaCorta",
                "precioPersonaAdicionalTemBaja",
                "precioPersonaAdicionalTemAlta",
                "precioEstadiaPersonaAdicionalCorta"
        );
    }

    private TarifaBaseActualizacionDTO requestDesdeTarifa(TarifaBase tarifa) {
        TarifaBaseActualizacionDTO request = new TarifaBaseActualizacionDTO();
        request.setTipoUnidad(tarifa.getTipoUnidad());
        request.setPrecioDiaTemBaja(tarifa.getPrecioDiaTemBaja());
        request.setPrecioDiaTemAlta(tarifa.getPrecioDiaTemAlta());
        request.setPrecioEstadiaCorta(tarifa.getPrecioEstadiaCorta());
        request.setPrecioPersonaAdicionalTemBaja(tarifa.getPrecioPersonaAdicionalTemBaja());
        request.setPrecioPersonaAdicionalTemAlta(tarifa.getPrecioPersonaAdicionalTemAlta());
        request.setPrecioEstadiaPersonaAdicionalCorta(tarifa.getPrecioEstadiaPersonaAdicionalCorta());
        return request;
    }
}
