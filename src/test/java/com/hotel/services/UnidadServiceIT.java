package com.hotel.services;

import com.hotel.dtos.UnidadDTO;
import com.hotel.models.Habitacion;
import com.hotel.models.Reserva;
import com.hotel.models.Unidad;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.models.enums.Piso;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.services.support.AbstractServiceIT;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static com.hotel.testdata.UnidadTestData.unidadData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UnidadServiceIT extends AbstractServiceIT {

    @Autowired
    private UnidadService unidadService;

    @Test
    void exitoBuscandoUnidadPorCodigo_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        // ---------- WHEN ----------
        Unidad resultado = unidadService.buscarPorCodigo(unidad.getCodigo());

        // ---------- THEN ----------
        assertThat(resultado.getId()).isEqualTo(unidad.getId());
        assertThat(resultado.getCodigo()).isEqualTo(unidad.getCodigo());
        assertThat(resultado.getTipo()).isEqualTo(TipoUnidad.APARTAMENTO);
    }

    @Test
    void falloBuscandoUnidadPorCodigoInexistente_test() {

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> unidadService.buscarPorCodigo("UN-NO-EXISTE"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Unidad no encontrada con codigo: UN-NO-EXISTE");
    }

    @Test
    void exitoBuscandoHabitacionesPorCodigoUnidad_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        // ---------- WHEN ----------
        List<Habitacion> habitaciones = unidadService.buscarHabitacionesPorCodigoUnidad(unidad.getCodigo());

        // ---------- THEN ----------
        assertThat(habitaciones).hasSize(3);
        assertThat(habitaciones)
                .extracting(h -> h.getUnidad().getId())
                .containsOnly(unidad.getId());
    }

    @Test
    void falloBuscandoHabitacionesPorCodigoUnidadInexistente_test() {

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> unidadService.buscarHabitacionesPorCodigoUnidad("UN-NO-EXISTE"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Unidad no encontrada con codigo: UN-NO-EXISTE");
    }

    @Test
    void exitoCambiandoEstadoUnidadTodosDisponibles_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        unidad.getHabitaciones().forEach(h -> h.setEstadoOperativo(EstadoOperativo.DISPONIBLE));
        habitacionRepository.saveAll(unidad.getHabitaciones());

        // ---------- WHEN ----------
        unidadService.cambiarEstadoUnidad(unidad);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        assertThat(unidadDb.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
    }

    @Test
    void exitoCambiandoEstadoUnidadTodosReservados_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        unidad.getHabitaciones().forEach(h -> h.setEstadoOperativo(EstadoOperativo.RESERVADO));
        habitacionRepository.saveAll(unidad.getHabitaciones());

        // ---------- WHEN ----------
        unidadService.cambiarEstadoUnidad(unidad);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        assertThat(unidadDb.getEstadoOperativo()).isEqualTo(EstadoOperativo.RESERVADO);
    }

    @Test
    void exitoCambiandoEstadoUnidadConHabitacionesMixtasParcialmente_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        unidad.getHabitaciones().get(0).setEstadoOperativo(EstadoOperativo.OCUPADO);
        unidad.getHabitaciones().get(1).setEstadoOperativo(EstadoOperativo.DISPONIBLE);
        unidad.getHabitaciones().get(2).setEstadoOperativo(EstadoOperativo.RESERVADO);
        habitacionRepository.saveAll(unidad.getHabitaciones());

        // ---------- WHEN ----------
        unidadService.cambiarEstadoUnidad(unidad);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        assertThat(unidadDb.getEstadoOperativo()).isEqualTo(EstadoOperativo.PARCIALMENTE);
    }

    @Test
    void exitoBuscandoUnidadesTipoApartamentoConFiltros_test() {

        // ---------- GIVEN ----------
        Unidad unidadDisponible = crearApartamento(EstadoOperativo.DISPONIBLE);
        Unidad unidadOcupada = crearApartamento(EstadoOperativo.OCUPADO);
        unidadOcupada.setPiso(Piso.PISO_2);
        unidadRepository.save(unidadOcupada);

        // ---------- WHEN ----------
        List<UnidadDTO> resultado = unidadService.buscarUnidades(
                TipoUnidad.APARTAMENTO,
                List.of(EstadoOperativo.DISPONIBLE),
                List.of(Piso.PISO_1),
                unidadDisponible.getCodigo().substring(0, 6)
        );

        // ---------- THEN ----------
        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().getId()).isEqualTo(unidadDisponible.getId());
        assertThat(resultado.getFirst().getTipo()).isEqualTo(TipoUnidad.APARTAMENTO);
        assertThat(resultado.getFirst().getHabitaciones()).hasSize(3);
    }

    @Test
    void exitoBuscandoUnidadesTipoHabitacionRetornaSoloHabitacionesApartamento_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.DISPONIBLE);
        Unidad apartaestudio = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        // ---------- WHEN ----------
        List<UnidadDTO> resultado = unidadService.buscarUnidades(
                TipoUnidad.HABITACION,
                null,
                null,
                null
        );

        // ---------- THEN ----------
        assertThat(resultado).isNotEmpty();
        assertThat(resultado).allMatch(u -> u.getTipo() == TipoUnidad.HABITACION);
        assertThat(resultado).allMatch(u -> u.getHabitaciones().size() == 1);
        assertThat(resultado)
                .extracting(UnidadDTO::getCodigo)
                .containsAll(
                        apartamento.getHabitaciones().stream().map(Habitacion::getCodigo).toList()
                );
        assertThat(resultado)
                .extracting(UnidadDTO::getCodigo)
                .doesNotContain(apartaestudio.getHabitaciones().getFirst().getCodigo());
    }

    @Test
    void exitoBuscandoUnidadesTipoHabitacionConFiltroCodigo_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacionObjetivo = apartamento.getHabitaciones().getFirst();

        // ---------- WHEN ----------
        List<UnidadDTO> resultado = unidadService.buscarUnidades(
                TipoUnidad.HABITACION,
                List.of(EstadoOperativo.DISPONIBLE),
                List.of(Piso.PISO_1),
                habitacionObjetivo.getCodigo()
        );

        // ---------- THEN ----------
        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().getCodigo()).isEqualTo(habitacionObjetivo.getCodigo());
        assertThat(resultado.getFirst().getHabitaciones().getFirst().getCodigoUnidad()).isEqualTo(apartamento.getCodigo());
    }

    @Test
    void exitoBuscandoUnidadesAgregaInformacionAdicional_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.DISPONIBLE);
        Reserva reserva = crearReservaExistente(apartamento.getHabitaciones(), true, EstadoReserva.CONFIRMADA);

        // ---------- WHEN ----------
        List<UnidadDTO> resultado = unidadService.buscarUnidades(
                TipoUnidad.APARTAMENTO,
                null,
                null,
                reserva.getHabitaciones().getFirst().getUnidad().getCodigo()
        );

        // ---------- THEN ----------
        assertThat(resultado).hasSize(1);
        UnidadDTO unidadDto = resultado.getFirst();
        assertThat(unidadDto.getInformacionAdicional()).isNotBlank();
        assertThat(unidadDto.getInformacionAdicional()).contains("ESTANCIA");
        assertThat(unidadDto.getInformacionAdicional()).contains("RESERVA");
        assertThat(unidadDto.getHabitaciones()).allMatch(h -> h.getInformacionAdicional() != null);
    }

    @Test
    void falloCambiandoEstadoUnidadSinHabitaciones_test() {

        // ---------- GIVEN ----------
        Unidad unidadSinHabitaciones = unidadRepository.save(unidadData(TipoUnidad.APARTAMENTO, EstadoOperativo.DISPONIBLE));

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> unidadService.cambiarEstadoUnidad(unidadSinHabitaciones))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void exitoBuscandoUnidadesConTipoNuloYFiltros_test() {

        // ---------- GIVEN ----------
        Unidad apartamentoDisponible = crearApartamento(EstadoOperativo.DISPONIBLE);
        Unidad apartaestudioDisponible = crearApartaestudio(EstadoOperativo.DISPONIBLE);
        crearApartamento(EstadoOperativo.OCUPADO);

        // ---------- WHEN ----------
        List<UnidadDTO> resultado = unidadService.buscarUnidades(
                null,
                List.of(EstadoOperativo.DISPONIBLE),
                List.of(Piso.PISO_1),
                null
        );

        // ---------- THEN ----------
        assertThat(resultado)
                .extracting(UnidadDTO::getId)
                .contains(apartamentoDisponible.getId(), apartaestudioDisponible.getId());
        assertThat(resultado).allMatch(u -> u.getEstado() == EstadoOperativo.DISPONIBLE);
    }

    @Test
    void exitoBuscandoUnidadesTipoHabitacionSinCodigosDuplicados_test() {

        // ---------- GIVEN ----------
        crearApartamento(EstadoOperativo.DISPONIBLE);
        crearApartamento(EstadoOperativo.DISPONIBLE);

        // ---------- WHEN ----------
        List<UnidadDTO> resultado = unidadService.buscarUnidades(
                TipoUnidad.HABITACION,
                List.of(EstadoOperativo.DISPONIBLE),
                null,
                null
        );

        // ---------- THEN ----------
        List<String> codigos = resultado.stream().map(UnidadDTO::getCodigo).toList();
        assertThat(codigos).doesNotHaveDuplicates();
        assertThat(resultado).allMatch(u -> u.getHabitaciones().size() == 1);
        assertThat(resultado)
                .allMatch(u -> u.getCodigo().equals(u.getHabitaciones().getFirst().getCodigo()));
    }
}
