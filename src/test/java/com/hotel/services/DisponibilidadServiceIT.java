package com.hotel.services;

import com.hotel.models.Estancia;
import com.hotel.models.Reserva;
import com.hotel.models.Unidad;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.services.support.AbstractServiceIT;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DisponibilidadServiceIT extends AbstractServiceIT {

    @Autowired
    private DisponibilidadService disponibilidadService;

    @Test
    void exitoVerificandoDisponibilidadNuevoSinConflictos_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        LocalDateTime entrada = LocalDateTime.now().plusDays(10);
        LocalDateTime salida = entrada.plusDays(2);

        // ---------- WHEN ----------
        String detalleNoDisponible = disponibilidadService.verificarDisponibilidadNuevo(
                unidad.getHabitaciones(),
                entrada,
                salida
        );

        // ---------- THEN ----------
        assertThat(detalleNoDisponible).isEmpty();
    }

    @Test
    void falloVerificandoDisponibilidadNuevoPorReservaSolapada_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        LocalDateTime entrada = LocalDateTime.now().plusDays(3);
        LocalDateTime salida = entrada.plusDays(1);

        // ---------- WHEN ----------
        String detalleNoDisponible = disponibilidadService.verificarDisponibilidadNuevo(
                unidad.getHabitaciones(),
                entrada,
                salida
        );

        // ---------- THEN ----------
        assertThat(detalleNoDisponible)
                .contains("existe una reserva para las habitaciones con codigo");
    }

    @Test
    void falloVerificandoDisponibilidadNuevoPorEstanciaActiva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        LocalDateTime entrada = LocalDateTime.now();
        LocalDateTime salida = entrada.plusDays(2);

        // ---------- WHEN ----------
        String detalleNoDisponible = disponibilidadService.verificarDisponibilidadNuevo(
                unidad.getHabitaciones(),
                entrada,
                salida
        );

        // ---------- THEN ----------
        assertThat(detalleNoDisponible)
                .contains("existe una estancia para las habitaciones con codigo");
    }

    @Test
    void falloVerificandoDisponibilidadNuevoPorEstanciaExcedida_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.EXCEDIDA);
        LocalDateTime entrada = LocalDateTime.now().plusDays(20);
        LocalDateTime salida = entrada.plusDays(2);

        // ---------- WHEN ----------
        String detalleNoDisponible = disponibilidadService.verificarDisponibilidadNuevo(
                unidad.getHabitaciones(),
                entrada,
                salida
        );

        // ---------- THEN ----------
        assertThat(detalleNoDisponible)
                .contains("existe una estancia para las habitaciones con codigo");
    }

    @Test
    void falloVerificandoDisponibilidadNuevoPorEstadoInconsistente_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        LocalDateTime entrada = LocalDateTime.now().plusDays(2);
        LocalDateTime salida = entrada.plusDays(2);

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> disponibilidadService.verificarDisponibilidadNuevo(
                unidad.getHabitaciones(),
                entrada,
                salida
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Estado inconsistente");
    }

    @Test
    void falloVerificandoDisponibilidadNuevoConListaVacia_test() {

        // ---------- GIVEN ----------
        LocalDateTime entrada = LocalDateTime.now().plusDays(2);
        LocalDateTime salida = entrada.plusDays(2);

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> disponibilidadService.verificarDisponibilidadNuevo(
                List.of(),
                entrada,
                salida
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La lista de habitaciones no puede ser nula o vacía");
    }

    @Test
    void exitoVerificandoDisponibilidadEditarIgnorandoMismaReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        LocalDateTime entrada = LocalDateTime.now().plusDays(3);
        LocalDateTime salida = entrada.plusDays(1);

        // ---------- WHEN ----------
        String detalleNoDisponible = disponibilidadService.verificarDisponibilidadEditar(
                reserva,
                null,
                unidad.getHabitaciones(),
                entrada,
                salida
        );

        // ---------- THEN ----------
        assertThat(detalleNoDisponible).isEmpty();
    }

    @Test
    void falloVerificandoDisponibilidadEditarPorOtraReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Reserva reservaEditar = crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        LocalDateTime entrada = LocalDateTime.now().plusDays(3);
        LocalDateTime salida = entrada.plusDays(1);

        // ---------- WHEN ----------
        String detalleNoDisponible = disponibilidadService.verificarDisponibilidadEditar(
                reservaEditar,
                null,
                unidad.getHabitaciones(),
                entrada,
                salida
        );

        // ---------- THEN ----------
        assertThat(detalleNoDisponible)
                .contains("existe una reserva para las habitaciones con codigo");
    }

    @Test
    void exitoVerificandoDisponibilidadEditarIgnorandoMismaEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        LocalDateTime entrada = LocalDateTime.now();
        LocalDateTime salida = entrada.plusDays(2);

        // ---------- WHEN ----------
        String detalleNoDisponible = disponibilidadService.verificarDisponibilidadEditar(
                null,
                estancia,
                unidad.getHabitaciones(),
                entrada,
                salida
        );

        // ---------- THEN ----------
        assertThat(detalleNoDisponible).isEmpty();
    }

    @Test
    void falloVerificandoDisponibilidadEditarPorOtraEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estanciaEditar = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        LocalDateTime entrada = LocalDateTime.now();
        LocalDateTime salida = entrada.plusDays(2);

        // ---------- WHEN ----------
        String detalleNoDisponible = disponibilidadService.verificarDisponibilidadEditar(
                null,
                estanciaEditar,
                unidad.getHabitaciones(),
                entrada,
                salida
        );

        // ---------- THEN ----------
        assertThat(detalleNoDisponible)
                .contains("existe una estancia para las habitaciones con codigo");
    }

    @Test
    void exitoVerificandoDisponibilidadEditarSinConflictos_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        LocalDateTime entrada = LocalDateTime.now().plusDays(10);
        LocalDateTime salida = entrada.plusDays(2);

        // ---------- WHEN ----------
        String detalleNoDisponible = disponibilidadService.verificarDisponibilidadEditar(
                reserva,
                null,
                unidad.getHabitaciones(),
                entrada,
                salida
        );

        // ---------- THEN ----------
        assertThat(detalleNoDisponible).isEmpty();
    }

    @Test
    void falloVerificandoDisponibilidadEditarConListaVacia_test() {

        // ---------- GIVEN ----------
        LocalDateTime entrada = LocalDateTime.now().plusDays(2);
        LocalDateTime salida = entrada.plusDays(2);

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> disponibilidadService.verificarDisponibilidadEditar(
                null,
                null,
                List.of(),
                entrada,
                salida
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La lista de habitaciones no puede ser nula o vacía");
    }

    @Test
    void falloVerificandoDisponibilidadEditarPorEstadoInconsistente_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        LocalDateTime entrada = LocalDateTime.now().plusDays(2);
        LocalDateTime salida = entrada.plusDays(2);

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> disponibilidadService.verificarDisponibilidadEditar(
                null,
                null,
                unidad.getHabitaciones(),
                entrada,
                salida
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Estado inconsistente");
    }

    @Test
    void falloVerificandoDisponibilidadNuevoConFechasIguales_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        LocalDateTime entrada = LocalDateTime.now().plusDays(2);

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> disponibilidadService.verificarDisponibilidadNuevo(
                unidad.getHabitaciones(),
                entrada,
                entrada
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de inicio debe ser anterior a la fecha de fin");
    }

    @Test
    void falloVerificandoDisponibilidadNuevoConFechaInicioPosterior_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        LocalDateTime entrada = LocalDateTime.now().plusDays(5);
        LocalDateTime salida = LocalDateTime.now().plusDays(3);

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> disponibilidadService.verificarDisponibilidadNuevo(
                unidad.getHabitaciones(),
                entrada,
                salida
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de inicio debe ser anterior a la fecha de fin");
    }

    @Test
    void falloVerificandoDisponibilidadNuevoConFechasNulas_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> disponibilidadService.verificarDisponibilidadNuevo(
                unidad.getHabitaciones(),
                null,
                LocalDateTime.now().plusDays(1)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Las fechas de inicio y fin son obligatorias");
    }

    @Test
    void falloVerificandoDisponibilidadEditarConFechasNulas_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> disponibilidadService.verificarDisponibilidadEditar(
                null,
                null,
                unidad.getHabitaciones(),
                LocalDateTime.now().plusDays(1),
                null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Las fechas de inicio y fin son obligatorias");
    }

    @Test
    void falloVerificandoDisponibilidadNuevoPorEstanciaActivaSinSalidaEstimada_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        estancia.setSalidaEstimada(null);
        estanciaRepository.save(estancia);

        LocalDateTime entrada = LocalDateTime.now().plusDays(20);
        LocalDateTime salida = entrada.plusDays(2);

        // ---------- WHEN ----------
        String detalleNoDisponible = disponibilidadService.verificarDisponibilidadNuevo(
                unidad.getHabitaciones(),
                entrada,
                salida
        );

        // ---------- THEN ----------
        assertThat(detalleNoDisponible)
                .contains("existe una estancia para las habitaciones con codigo");
    }

    @Test
    void exitoVerificandoDisponibilidadNuevoSinSolapeConEstanciaActiva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        LocalDateTime entrada = LocalDateTime.now().plusDays(10);
        LocalDateTime salida = entrada.plusDays(2);

        // ---------- WHEN ----------
        String detalleNoDisponible = disponibilidadService.verificarDisponibilidadNuevo(
                unidad.getHabitaciones(),
                entrada,
                salida
        );

        // ---------- THEN ----------
        assertThat(detalleNoDisponible).isEmpty();
    }

    @Test
    void exitoVerificandoDisponibilidadEditarConReservaYEstanciaNulasSinConflicto_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        LocalDateTime entrada = LocalDateTime.now().plusDays(10);
        LocalDateTime salida = entrada.plusDays(2);

        // ---------- WHEN ----------
        String detalleNoDisponible = disponibilidadService.verificarDisponibilidadEditar(
                null,
                null,
                unidad.getHabitaciones(),
                entrada,
                salida
        );

        // ---------- THEN ----------
        assertThat(detalleNoDisponible).isEmpty();
    }

    @Test
    void falloVerificandoDisponibilidadEditarPorReservaYEstanciaSimultaneaConflictoReal_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Reserva reservaEditar = crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);

        LocalDateTime entrada = LocalDateTime.now().plusDays(3);
        LocalDateTime salida = entrada.plusDays(1);

        // ---------- WHEN ----------
        String detalleNoDisponible = disponibilidadService.verificarDisponibilidadEditar(
                reservaEditar,
                reservaEditar.getEstancia(),
                unidad.getHabitaciones(),
                entrada,
                salida
        );

        // ---------- THEN ----------
        assertThat(detalleNoDisponible)
                .contains("existe una reserva para las habitaciones con codigo");
    }
}
