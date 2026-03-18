package com.hotel.services;

import com.hotel.dtos.HabitacionDTO;
import com.hotel.models.Habitacion;
import com.hotel.models.Unidad;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.models.enums.Piso;
import com.hotel.services.support.AbstractServiceIT;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
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
class HabitacionServiceIT extends AbstractServiceIT {

    @Autowired
    private HabitacionService habitacionService;

    @Test
    void exitoBuscandoHabitacionesSoloDeApartamentoSinFiltros_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.DISPONIBLE);
        Unidad apartaestudio = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        // ---------- WHEN ----------
        List<HabitacionDTO> resultado = habitacionService.buscarHabitaciones(null, null, null);

        // ---------- THEN ----------
        assertThat(resultado)
                .extracting(HabitacionDTO::getCodigo)
                .containsAll(apartamento.getHabitaciones().stream().map(Habitacion::getCodigo).toList());
        assertThat(resultado)
                .extracting(HabitacionDTO::getCodigo)
                .doesNotContain(apartaestudio.getHabitaciones().getFirst().getCodigo());
    }

    @Test
    void exitoBuscandoHabitacionesConFiltroEstado_test() {

        // ---------- GIVEN ----------
        Unidad apartamentoDisponible = crearApartamento(EstadoOperativo.DISPONIBLE);
        Unidad apartamentoOcupado = crearApartamento(EstadoOperativo.OCUPADO);

        // ---------- WHEN ----------
        List<HabitacionDTO> resultado = habitacionService.buscarHabitaciones(
                List.of(EstadoOperativo.OCUPADO),
                null,
                null
        );

        // ---------- THEN ----------
        assertThat(resultado).isNotEmpty();
        assertThat(resultado).allMatch(item -> item.getEstado() == EstadoOperativo.OCUPADO);
        assertThat(resultado)
                .extracting(HabitacionDTO::getCodigo)
                .containsAnyElementsOf(apartamentoOcupado.getHabitaciones().stream().map(Habitacion::getCodigo).toList())
                .doesNotContainAnyElementsOf(apartamentoDisponible.getHabitaciones().stream().map(Habitacion::getCodigo).toList());
    }

    @Test
    void exitoBuscandoHabitacionesConFiltroPiso_test() {

        // ---------- GIVEN ----------
        Unidad piso1 = crearApartamento(EstadoOperativo.DISPONIBLE);
        Unidad piso2 = crearApartamento(EstadoOperativo.DISPONIBLE);
        piso2.setPiso(Piso.PISO_2);
        unidadRepository.save(piso2);

        // ---------- WHEN ----------
        List<HabitacionDTO> resultado = habitacionService.buscarHabitaciones(
                null,
                List.of(Piso.PISO_2),
                null
        );

        // ---------- THEN ----------
        assertThat(resultado).isNotEmpty();
        assertThat(resultado).allMatch(item -> item.getPiso() == Piso.PISO_2);
        assertThat(resultado)
                .extracting(HabitacionDTO::getCodigo)
                .containsAnyElementsOf(piso2.getHabitaciones().stream().map(Habitacion::getCodigo).toList())
                .doesNotContainAnyElementsOf(piso1.getHabitaciones().stream().map(Habitacion::getCodigo).toList());
    }

    @Test
    void exitoBuscandoHabitacionesConFiltroCodigoParcial_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.DISPONIBLE);
        String codigoParcial = apartamento.getHabitaciones().getFirst().getCodigo().substring(3, 8).toLowerCase();

        // ---------- WHEN ----------
        List<HabitacionDTO> resultado = habitacionService.buscarHabitaciones(null, null, codigoParcial);

        // ---------- THEN ----------
        assertThat(resultado).isNotEmpty();
        assertThat(resultado)
                .allMatch(item -> item.getCodigo().toLowerCase().contains(codigoParcial));
    }

    @Test
    void exitoBuscandoHabitacionesConFiltrosCombinados_test() {

        // ---------- GIVEN ----------
        Unidad objetivo = crearApartamento(EstadoOperativo.OCUPADO);
        objetivo.setPiso(Piso.PISO_3);
        unidadRepository.save(objetivo);
        Habitacion habitacionObjetivo = objetivo.getHabitaciones().getFirst();

        crearApartamento(EstadoOperativo.DISPONIBLE);

        // ---------- WHEN ----------
        List<HabitacionDTO> resultado = habitacionService.buscarHabitaciones(
                List.of(EstadoOperativo.OCUPADO),
                List.of(Piso.PISO_3),
                habitacionObjetivo.getCodigo().substring(0, 7)
        );

        // ---------- THEN ----------
        assertThat(resultado).hasSize(1);
        assertThat(resultado).allMatch(item -> item.getEstado() == EstadoOperativo.OCUPADO);
        assertThat(resultado).allMatch(item -> item.getPiso() == Piso.PISO_3);
    }

    @Test
    void exitoBuscandoHabitacionesSinResultados_test() {

        // ---------- GIVEN ----------
        crearApartamento(EstadoOperativo.DISPONIBLE);

        // ---------- WHEN ----------
        List<HabitacionDTO> resultado = habitacionService.buscarHabitaciones(
                List.of(EstadoOperativo.OCUPADO),
                List.of(Piso.PISO_1),
                "NO-EXISTE"
        );

        // ---------- THEN ----------
        assertThat(resultado).isEmpty();
    }

    @Test
    void exitoBuscandoHabitacionesAgregaInformacionAdicionalPorModoIndividual_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = apartamento.getHabitaciones().getFirst();
        crearReservaExistente(List.of(habitacion), false, EstadoReserva.CONFIRMADA);

        // ---------- WHEN ----------
        List<HabitacionDTO> resultado = habitacionService.buscarHabitaciones(null, null, habitacion.getCodigo());

        // ---------- THEN ----------
        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().getInformacionAdicional()).contains("RESERVA");
    }

    @Test
    void exitoBuscandoHabitacionesNoUsaReservasCompletasParaInfoIndividual_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = apartamento.getHabitaciones().getFirst();
        crearReservaExistente(apartamento.getHabitaciones(), false, EstadoReserva.CONFIRMADA);

        // ---------- WHEN ----------
        List<HabitacionDTO> resultado = habitacionService.buscarHabitaciones(null, null, habitacion.getCodigo());

        // ---------- THEN ----------
        assertThat(resultado).hasSize(1);
        String info = resultado.getFirst().getInformacionAdicional();
        assertThat(info).contains("ESTANCIA").contains("RESERVA");
        assertThat(info).doesNotContain("idReserva");
    }

    @Test
    void exitoBuscandoHabitacionesNoUsaEstanciasCompletasParaInfoIndividual_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = apartamento.getHabitaciones().getFirst();
        crearEstanciaExistente(apartamento.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        // ---------- WHEN ----------
        List<HabitacionDTO> resultado = habitacionService.buscarHabitaciones(null, null, habitacion.getCodigo());

        // ---------- THEN ----------
        assertThat(resultado).hasSize(1);
        String info = resultado.getFirst().getInformacionAdicional();
        assertThat(info).contains("ESTANCIA").contains("RESERVA");
        assertThat(info).doesNotContain("idEstancia");
    }

    @Test
    void exitoBuscandoHabitacionesConReservaYEstanciaIndividual_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = apartamento.getHabitaciones().getFirst();
        crearReservaExistente(List.of(habitacion), false, EstadoReserva.CONFIRMADA);
        crearEstanciaExistente(List.of(habitacion), false, EstadoEstancia.ACTIVA);

        // ---------- WHEN ----------
        List<HabitacionDTO> resultado = habitacionService.buscarHabitaciones(null, null, habitacion.getCodigo());

        // ---------- THEN ----------
        assertThat(resultado).hasSize(1);
        String info = resultado.getFirst().getInformacionAdicional();
        assertThat(info).contains("idReserva");
        assertThat(info).contains("idEstancia");
    }

    @Test
    void exitoBuscandoHabitacionPorCodigo_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = apartamento.getHabitaciones().getFirst();

        // ---------- WHEN ----------
        Habitacion resultado = habitacionService.buscarPorCodigo(habitacion.getCodigo());

        // ---------- THEN ----------
        assertThat(resultado.getId()).isEqualTo(habitacion.getId());
        assertThat(resultado.getCodigo()).isEqualTo(habitacion.getCodigo());
    }

    @Test
    void falloBuscandoHabitacionPorCodigoInexistente_test() {

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> habitacionService.buscarPorCodigo("HB-NO-EXISTE"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Habitacion no encontrada con codigo: HB-NO-EXISTE");
    }

    @Test
    void exitoBuscandoHabitacionesConCodigoBlankIgnoraFiltroCodigo_test() {

        // ---------- GIVEN ----------
        crearApartamento(EstadoOperativo.DISPONIBLE);

        // ---------- WHEN ----------
        List<HabitacionDTO> sinFiltro = habitacionService.buscarHabitaciones(null, null, null);
        List<HabitacionDTO> conBlank = habitacionService.buscarHabitaciones(null, null, "   ");

        // ---------- THEN ----------
        assertThat(conBlank).hasSize(sinFiltro.size());
    }

    @Test
    void exitoBuscandoHabitacionesConEstadosYPisosVaciosIgnoraFiltros_test() {

        // ---------- GIVEN ----------
        crearApartamento(EstadoOperativo.DISPONIBLE);

        // ---------- WHEN ----------
        List<HabitacionDTO> sinFiltro = habitacionService.buscarHabitaciones(null, null, null);
        List<HabitacionDTO> conVacios = habitacionService.buscarHabitaciones(List.of(), List.of(), null);

        // ---------- THEN ----------
        assertThat(conVacios).hasSize(sinFiltro.size());
    }
}
