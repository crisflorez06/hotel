package com.hotel.services;

import com.hotel.dtos.estancia.ActivarEstanciaDTO;
import com.hotel.dtos.estancia.EstanciaDTO;
import com.hotel.dtos.estancia.EstanciaRequestDTO;
import com.hotel.dtos.estancia.SalidaEstanciaDTO;
import com.hotel.dtos.pago.PagoNuevoRequestDTO;
import com.hotel.models.*;
import com.hotel.models.enums.*;
import com.hotel.repositories.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.hotel.services.support.AbstractServiceIT;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static com.hotel.testdata.EstanciaTestData.*;
import static com.hotel.testdata.PagoTestData.pagoNuevoRequestDTO;
import static com.hotel.testdata.ReservaTestData.reservaData;
import static com.hotel.testdata.TestDataUtils.randomCodigo;
import static com.hotel.testdata.OcupanteTestData.acompanantesData;
import static com.hotel.testdata.OcupanteTestData.clienteData;
import static com.hotel.testutils.AssertionsHelper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EstanciaServiceIT extends AbstractServiceIT {


    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private EstanciaService estanciaService;

    @Autowired
    private EstanciaRepository estanciaRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    /**
     * crearEstanciaNueva(EstanciaRequestDTO request)
     */
    @Test
    void exitoCreandoEstanciaNuevaApartamentoConPago_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        // Cliente
        Ocupante cliente = clienteData();


        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);
        EstanciaRequestDTO request = estanciaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, acompanantesData(),null, pagoRequest);

        // ---------- WHEN ----------
        Estancia estancia = estanciaService.crearEstanciaNueva(request);

        // ---------- THEN (validación real en BD) ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                1);

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 3);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                500000,
                0,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                1);
    }

    @Test
    void exitoCreandoEstanciaNuevaApartaestudioConPago_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        // Cliente
        Ocupante cliente = clienteData();


        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);
        EstanciaRequestDTO request = estanciaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, acompanantesData(),null, pagoRequest);

        // ---------- WHEN ----------
        Estancia estancia = estanciaService.crearEstanciaNueva(request);

        // ---------- THEN (validación real en BD) ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();


        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                1,
                1);

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO,1);
        comprobarPagosDb(estanciaDb.getPagos(),
                500000,
                0,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                1);

    }

    @Test
    void exitoCreandoEstanciaNuevaHabitacionConPago_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        // Cliente
        Ocupante cliente = clienteData();


        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);
        EstanciaRequestDTO request = estanciaRequestDTO(TipoUnidad.HABITACION, habitacion.getCodigo(), cliente, acompanantesData(),null, pagoRequest);

        // ---------- WHEN ----------
        Estancia estancia = estanciaService.crearEstanciaNueva(request);

        // ---------- THEN (validación real en BD) ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                null,
                1,
                1);

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.PARCIALMENTE, 1);
        comprobarPagosDb(estanciaDb.getPagos(),
                500000,
                0,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                1);

    }

    @Test
    void exitoCreandoEstanciaNuevaApartamentoSinPago_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        // Cliente
        Ocupante cliente = clienteData();


        EstanciaRequestDTO request = estanciaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, acompanantesData(), null,null);

        // ---------- WHEN ----------
        Estancia estancia = estanciaService.crearEstanciaNueva(request);

        // ---------- THEN (validación real en BD) ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                0);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                3
        );

    }

    @Test
    void exitoCreandoEstanciaNuevaApartaestudioSinPago_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        // Cliente
        Ocupante cliente = clienteData();


        EstanciaRequestDTO request = estanciaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, acompanantesData(), null,null);

        // ---------- WHEN ----------
        Estancia estancia = estanciaService.crearEstanciaNueva(request);

        // ---------- THEN (validación real en BD) ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();


        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                1,
                0);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                1
        );
    }

    @Test
    void exitoCreandoEstanciaNuevaHabitacionSinPago_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        // Cliente
        Ocupante cliente = clienteData();


        EstanciaRequestDTO request = estanciaRequestDTO(TipoUnidad.HABITACION, habitacion.getCodigo(), cliente, acompanantesData(), null,null);

        // ---------- WHEN ----------
        Estancia estancia = estanciaService.crearEstanciaNueva(request);

        // ---------- THEN (validación real en BD) ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                null,
                1,
                0);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.PARCIALMENTE,
                1
        );

    }

    @Test
    void falloCreandoEstanciaNuevaApartamentoConEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante cliente = clienteData();

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null);

        estanciaRepository.save(estanciaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.APARTAMENTO,
                unidad.getCodigo(),
                cliente,
                acompanantesData(),
                null,
                null
        );

        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();


        // ---------- WHEN + THEN (excepción) ----------
        assertThatThrownBy(() -> estanciaService.crearEstanciaNueva(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede crear la estancia:")
                .hasMessageContaining("existe una estancia para las habitaciones con codigo");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();

        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        // No se creó estanciaData
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 3);
    }

    @Test
    void falloCreandoEstanciaNuevaApartaestudioConEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);

        Ocupante cliente = clienteData();

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null);
        estanciaRepository.save(estanciaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                cliente,
                acompanantesData(),
                null,
                null
        );

        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();


        // ---------- WHEN + THEN (excepción) ----------
        assertThatThrownBy(() -> estanciaService.crearEstanciaNueva(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede crear la estancia:")
                .hasMessageContaining("existe una estancia para las habitaciones con codigo");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();

        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        // No se creó estanciaData
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 1);
    }

    @Test
    void falloCreandoEstanciaNuevaHabitacionConEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Habitacion habitacion = unidad.getHabitaciones().getFirst();
        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Ocupante cliente = clienteData();

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                listaHabitacion,
                null);

        estanciaRepository.save(estanciaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                cliente,
                acompanantesData(),
                null,
                null
        );

        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();


        // ---------- WHEN + THEN (excepción) ----------
        assertThatThrownBy(() -> estanciaService.crearEstanciaNueva(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede crear la estancia:")
                .hasMessageContaining("existe una estancia para las habitaciones con codigo");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();

        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        // No se creó estanciaData
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 3);
    }

    @Test
    void falloCreandoEstanciaNuevaApartamentoConReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante cliente = clienteData();

        Reserva reservaExistente = reservaData(
                cliente,
                ModoOcupacion.COMPLETO,
                EstadoReserva.CONFIRMADA,
                unidad.getHabitaciones(),
                null
        );
        reservaRepository.save(reservaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.APARTAMENTO,
                unidad.getCodigo(),
                cliente,
                acompanantesData(),
                null,
                null
        );

        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();


        // ---------- WHEN + THEN (excepción) ----------
        assertThatThrownBy(() -> estanciaService.crearEstanciaNueva(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede crear la estancia:")
                .hasMessageContaining("existe una reserva para las habitaciones con codigo");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();

        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        // No se creó estanciaData
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservaAntes);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
    }

    @Test
    void falloCreandoEstanciaNuevaApartaestudioConReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        Ocupante cliente = clienteData();

        Reserva reservaExistente = reservaData(
                cliente,
                ModoOcupacion.COMPLETO,
                EstadoReserva.CONFIRMADA,
                unidad.getHabitaciones(),
                null
        );

        reservaRepository.save(reservaExistente);


        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                cliente,
                acompanantesData(),
                null,
                null
        );

        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();


        // ---------- WHEN + THEN (excepción) ----------
        assertThatThrownBy(() -> estanciaService.crearEstanciaNueva(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede crear la estancia:")
                .hasMessageContaining("existe una reserva para las habitaciones con codigo");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();

        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        // No se creó estanciaData
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservaAntes);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
    }

    @Test
    void falloCreandoEstanciaNuevaHabitacionConReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Habitacion habitacion = unidad.getHabitaciones().getFirst();
        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Ocupante cliente = clienteData();

        Reserva reservaExistente = reservaData(
                cliente,
                ModoOcupacion.INDIVIDUAL,
                EstadoReserva.CONFIRMADA,
                listaHabitacion,
                null
        );

        reservaRepository.save(reservaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                cliente,
                acompanantesData(),
                null,
                null
        );

        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();


        // ---------- WHEN + THEN (excepción) ----------
        assertThatThrownBy(() -> estanciaService.crearEstanciaNueva(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede crear la estancia:")
                .hasMessageContaining("existe una reserva para las habitaciones con codigo");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();

        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        // No se creó estanciaData
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservaAntes);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
    }

    /**
     * activarEstancia(ActivarEstanciaDTO request)
     */

    @Test
    void exitoActivandoEstanciaApartamentoPagoEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = clienteData();

        Reserva reserva = reservaRepository.save(
                reservaData(cliente, ModoOcupacion.COMPLETO, EstadoReserva.CONFIRMADA, unidad.getHabitaciones(), null)
        );

        Estancia estanciaReservada = estanciaRepository.save(
                estanciaData(reserva, null, ModoOcupacion.COMPLETO, EstadoEstancia.RESERVADA, unidad.getHabitaciones(), null)
        );

        reserva.setEstancia(estanciaReservada);
        reservaRepository.save(reserva);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);
        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().minusDays(1),
                pagoRequest
        );

        // ---------- WHEN ----------
        Estancia estancia = estanciaService.activarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();

        assertThat(estanciaDb.getId()).isEqualTo(estanciaReservada.getId());
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.COMPLETADA);

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                estanciaReservada.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                1
        );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 3);
        comprobarPagosDb(estanciaDb.getPagos(),
                500000,
                0,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                1);

    }

    @Test
    void exitoActivandoEstanciaApartaestudioPagoEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = clienteData();

        Reserva reserva = reservaRepository.save(
                reservaData(cliente, ModoOcupacion.COMPLETO, EstadoReserva.CONFIRMADA, unidad.getHabitaciones(), null)
        );

        Estancia estanciaReservada = estanciaRepository.save(
                estanciaData(reserva, null, ModoOcupacion.COMPLETO, EstadoEstancia.RESERVADA, unidad.getHabitaciones(), null)
        );

        reserva.setEstancia(estanciaReservada);
        reservaRepository.save(reserva);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);
        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().minusDays(1),
                pagoRequest
        );

        // ---------- WHEN ----------
        Estancia estancia = estanciaService.activarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();

        assertThat(estanciaDb.getId()).isEqualTo(estanciaReservada.getId());
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.COMPLETADA);


        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                estanciaReservada.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                1,
                1
        );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 1);
        comprobarPagosDb(estanciaDb.getPagos(),
                500000,
                0,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                1);

    }

    @Test
    void exitoActivandoEstanciaHabitacionPagoEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Ocupante cliente = clienteData();

        Reserva reserva = reservaRepository.save(
                reservaData(cliente, ModoOcupacion.INDIVIDUAL, EstadoReserva.CONFIRMADA, listaHabitacion, null)
        );

        Estancia estanciaReservada = estanciaRepository.save(
                estanciaData(reserva, null, ModoOcupacion.INDIVIDUAL, EstadoEstancia.RESERVADA, listaHabitacion, null)
        );

        reserva.setEstancia(estanciaReservada);
        reservaRepository.save(reserva);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);
        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().minusDays(1),
                pagoRequest
        );

        // ---------- WHEN ----------
        Estancia estancia = estanciaService.activarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();

        assertThat(estanciaDb.getId()).isEqualTo(estanciaReservada.getId());
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.COMPLETADA);

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                estanciaReservada.getNotas(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                null,
                1,
                1
        );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.PARCIALMENTE, 1);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                500000,
                0,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                1);

    }

    @Test
    void exitoActivandoEstanciaApartamentoPagoReservaYEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = clienteData();

        Reserva reserva = reservaRepository.save(
                reservaData(cliente, ModoOcupacion.COMPLETO, EstadoReserva.CONFIRMADA, unidad.getHabitaciones(), null)
        );

        Estancia estanciaReservada = estanciaRepository.save(
                estanciaData(reserva, null, ModoOcupacion.COMPLETO, EstadoEstancia.RESERVADA, unidad.getHabitaciones(), null)
        );
        crearPagoInicialEnEstancia(estanciaReservada, TipoPago.RESERVA);

        reserva.setEstancia(estanciaReservada);
        reservaRepository.save(reserva);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);
        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().minusDays(1),
                pagoRequest
        );

        // ---------- WHEN ----------
        Estancia estancia = estanciaService.activarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();

        assertThat(estanciaDb.getId()).isEqualTo(estanciaReservada.getId());
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.COMPLETADA);

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                estanciaReservada.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                2
        );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 3);
        comprobarPagosDb(estanciaDb.getPagos(),
                500500,
                0,
                EstadoPago.COMPLETADO,
                2,
                EstadoPago.MODIFICADO,
                0,
                1);

    }

    @Test
    void exitoActivandoEstanciaApartaestudioPagoReservaYEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = clienteData();

        Reserva reserva = reservaRepository.save(
                reservaData(cliente, ModoOcupacion.COMPLETO, EstadoReserva.CONFIRMADA, unidad.getHabitaciones(), null)
        );

        Estancia estanciaReservada = estanciaRepository.save(
                estanciaData(reserva, null, ModoOcupacion.COMPLETO, EstadoEstancia.RESERVADA, unidad.getHabitaciones(), null)
        );
        crearPagoInicialEnEstancia(estanciaReservada, TipoPago.RESERVA);

        reserva.setEstancia(estanciaReservada);
        reservaRepository.save(reserva);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);
        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().minusDays(1),
                pagoRequest
        );

        // ---------- WHEN ----------
        Estancia estancia = estanciaService.activarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();

        assertThat(estanciaDb.getId()).isEqualTo(estanciaReservada.getId());
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.COMPLETADA);

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                estanciaReservada.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                1,
                2
        );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 1);
        comprobarPagosDb(estanciaDb.getPagos(),
                500500,
                0,
                EstadoPago.COMPLETADO,
                2,
                EstadoPago.MODIFICADO,
                0,
                1);

    }

    @Test
    void exitoActivandoEstanciaHabitacionPagoReservaYEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Ocupante cliente = clienteData();

        Reserva reserva = reservaRepository.save(
                reservaData(cliente, ModoOcupacion.INDIVIDUAL, EstadoReserva.CONFIRMADA, listaHabitacion, null)
        );

        Estancia estanciaReservada = estanciaRepository.save(
                estanciaData(reserva, null, ModoOcupacion.INDIVIDUAL, EstadoEstancia.RESERVADA, listaHabitacion, null)
        );
        crearPagoInicialEnEstancia(estanciaReservada, TipoPago.RESERVA);

        reserva.setEstancia(estanciaReservada);
        reservaRepository.save(reserva);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);
        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().minusDays(1),
                pagoRequest
        );

        // ---------- WHEN ----------
        Estancia estancia = estanciaService.activarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();

        assertThat(estanciaDb.getId()).isEqualTo(estanciaReservada.getId());
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.COMPLETADA);

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                estanciaReservada.getNotas(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                null,
                1,
                2
        );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.PARCIALMENTE, 1);
        comprobarPagosDb(estanciaDb.getPagos(),
                500500,
                0,
                EstadoPago.COMPLETADO,
                2,
                EstadoPago.MODIFICADO,
                0,
                1);

    }

    @Test
    void exitoActivandoEstanciaApartamentoPagoReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = clienteData();

        Reserva reserva = reservaRepository.save(
                reservaData(cliente, ModoOcupacion.COMPLETO, EstadoReserva.CONFIRMADA, unidad.getHabitaciones(), null)
        );

        Estancia estanciaReservada = estanciaRepository.save(
                estanciaData(reserva, null, ModoOcupacion.COMPLETO, EstadoEstancia.RESERVADA, unidad.getHabitaciones(), null)
        );
        crearPagoInicialEnEstancia(estanciaReservada, TipoPago.RESERVA);

        reserva.setEstancia(estanciaReservada);
        reservaRepository.save(reserva);

        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().minusDays(1),
                null
        );

        // ---------- WHEN ----------
        Estancia estancia = estanciaService.activarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();

        assertThat(estanciaDb.getId()).isEqualTo(estanciaReservada.getId());
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.COMPLETADA);

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                estanciaReservada.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                1
        );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 3);
        comprobarPagosDb(estanciaDb.getPagos(),
                500,
                0,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                0);

    }

    @Test
    void exitoActivandoEstanciaApartaestudioPagoReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = clienteData();

        Reserva reserva = reservaRepository.save(
                reservaData(cliente, ModoOcupacion.COMPLETO, EstadoReserva.CONFIRMADA, unidad.getHabitaciones(), null)
        );

        Estancia estanciaReservada = estanciaRepository.save(
                estanciaData(reserva, null, ModoOcupacion.COMPLETO, EstadoEstancia.RESERVADA, unidad.getHabitaciones(), null)
        );
        crearPagoInicialEnEstancia(estanciaReservada, TipoPago.RESERVA);

        reserva.setEstancia(estanciaReservada);
        reservaRepository.save(reserva);

        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().minusDays(1),
                null
        );

        // ---------- WHEN ----------
        Estancia estancia = estanciaService.activarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();

        assertThat(estanciaDb.getId()).isEqualTo(estanciaReservada.getId());
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.COMPLETADA);

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                estanciaReservada.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                1,
                1
        );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 1);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                500,
                0,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                0);
    }

    @Test
    void exitoActivandoEstanciaHabitacionPagoReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Ocupante cliente = clienteData();

        Reserva reserva = reservaRepository.save(
                reservaData(cliente, ModoOcupacion.INDIVIDUAL, EstadoReserva.CONFIRMADA, listaHabitacion, null)
        );

        Estancia estanciaReservada = estanciaRepository.save(
                estanciaData(reserva, null, ModoOcupacion.INDIVIDUAL, EstadoEstancia.RESERVADA, listaHabitacion, null)
        );
        crearPagoInicialEnEstancia(estanciaReservada, TipoPago.RESERVA);

        reserva.setEstancia(estanciaReservada);
        reservaRepository.save(reserva);

        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().minusDays(1),
                null
        );

        // ---------- WHEN ----------
        Estancia estancia = estanciaService.activarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();

        assertThat(estanciaDb.getId()).isEqualTo(estanciaReservada.getId());
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.COMPLETADA);

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                estanciaReservada.getNotas(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                null,
                1,
                1
        );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.PARCIALMENTE, 1);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                500,
                0,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                0);
    }

    @Test
    void exitoActivandoEstanciaApartamentoSinPagos_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = clienteData();

        Reserva reserva = reservaRepository.save(
                reservaData(cliente, ModoOcupacion.COMPLETO, EstadoReserva.CONFIRMADA, unidad.getHabitaciones(), null)
        );

        Estancia estanciaReservada = estanciaRepository.save(
                estanciaData(reserva, null, ModoOcupacion.COMPLETO, EstadoEstancia.RESERVADA, unidad.getHabitaciones(), null)
        );

        reserva.setEstancia(estanciaReservada);
        reservaRepository.save(reserva);

        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().minusDays(1),
                null
        );

        // ---------- WHEN ----------
        Estancia estancia = estanciaService.activarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();

        assertThat(estanciaDb.getId()).isEqualTo(estanciaReservada.getId());
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.COMPLETADA);

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                estanciaReservada.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                0
        );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 3);
    }

    @Test
    void exitoActivandoEstanciaApartaestudioSinPagos_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = clienteData();

        Reserva reserva = reservaRepository.save(
                reservaData(cliente, ModoOcupacion.COMPLETO, EstadoReserva.CONFIRMADA, unidad.getHabitaciones(), null)
        );

        Estancia estanciaReservada = estanciaRepository.save(
                estanciaData(reserva, null, ModoOcupacion.COMPLETO, EstadoEstancia.RESERVADA, unidad.getHabitaciones(), null)
        );

        reserva.setEstancia(estanciaReservada);
        reservaRepository.save(reserva);

        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().minusDays(1),
                null
        );

        // ---------- WHEN ----------
        Estancia estancia = estanciaService.activarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();

        assertThat(estanciaDb.getId()).isEqualTo(estanciaReservada.getId());
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.COMPLETADA);

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                estanciaReservada.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                1,
                0
        );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 1);
    }

    @Test
    void exitoActivandoEstanciaHabitacionSinPagos_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Ocupante cliente = clienteData();

        Reserva reserva = reservaRepository.save(
                reservaData(cliente, ModoOcupacion.INDIVIDUAL, EstadoReserva.CONFIRMADA, listaHabitacion, null)
        );

        Estancia estanciaReservada = estanciaRepository.save(
                estanciaData(reserva, null, ModoOcupacion.INDIVIDUAL, EstadoEstancia.RESERVADA, listaHabitacion, null)
        );

        reserva.setEstancia(estanciaReservada);
        reservaRepository.save(reserva);

        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().minusDays(1),
                null
        );

        // ---------- WHEN ----------
        Estancia estancia = estanciaService.activarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();

        assertThat(estanciaDb.getId()).isEqualTo(estanciaReservada.getId());
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.COMPLETADA);

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                estanciaReservada.getNotas(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                null,
                1,
                0
        );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.PARCIALMENTE, 1);
    }

    /**
     * editarEstancia(EstanciaRequestDTO request, Long idEstancia)
     */

    @Test
    void exitoEditandoEstanciaApartamento_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante clienteNuevo = crearOcupante();

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                acompanantesData(),
                LocalDateTime.now().plusDays(-2),
                null
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estancia.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                0);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                3);

    }

    @Test
    void exitoEditandoEstanciaApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);

        Ocupante cliente = clienteData();

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().plusDays(-2),
                null
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                1,
                0);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                1);

    }

    @Test
    void exitoEditandoEstanciaHabitacion_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        Ocupante cliente = clienteData();

        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                listaHabitacion,
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().plusDays(-2),
                null
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                null,
                1,
                0);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                3);
    }

    @Test
    void exitoEditandoEstanciaApartamentoPagoNuevo_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante clienteNuevo = crearOcupante();

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                acompanantesData(),
                LocalDateTime.now().plusDays(-2),
                pagoRequest
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estancia.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                1);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                3);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                500000,
                0,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                1);
    }

    @Test
    void exitoEditandoEstanciaApartaestudioPagoNuevo_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);

        Ocupante cliente = clienteData();

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().plusDays(-2),
                pagoRequest
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                1,
                1);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                1);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                500000,
                0,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                1);

    }

    @Test
    void exitoEditandoEstanciaHabitacionPagoNuevo_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        Ocupante cliente = clienteData();

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);

        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                listaHabitacion,
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().plusDays(-2),
                pagoRequest
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                null,
                1,
                1);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                3);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                500000,
                0,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                1);
    }

    @Test
    void exitoEditandoEstanciaApartamentoConCambioPagoEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante clienteNuevo = crearOcupante();

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);
        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                acompanantesData(),
                LocalDateTime.now().plusDays(-2),
                pagoRequest
        );

        long estanciasAntes = estanciaRepository.count();

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                2);

        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                3);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                500000,
                1000,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                1,
                2);
    }

    @Test
    void exitoEditandoEstanciaApartaestudioConCambioPagoEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);

        Ocupante cliente = clienteData();

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);
        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().plusDays(-2),
                pagoRequest
        );

        long estanciasAntes = estanciaRepository.count();

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                1,
                2);

        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                1);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                500000,
                1000,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                1,
                2);
    }

    @Test
    void exitoEditandoEstanciaHabitacionConCambioPagoEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();
        Ocupante cliente = clienteData();

        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                listaHabitacion,
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);
        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().plusDays(-2),
                pagoRequest
        );

        long estanciasAntes = estanciaRepository.count();

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                null,
                1,
                2);

        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                3);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                500000,
                1000,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                1,
                2);
    }

    @Test
    void exitoEditandoEstanciaApartamentoConCambioPagoReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante clienteNuevo = crearOcupante();

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        crearPagoInicialEnEstancia(estancia, TipoPago.RESERVA);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);
        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                acompanantesData(),
                LocalDateTime.now().plusDays(-2),
                pagoRequest
        );

        long estanciasAntes = estanciaRepository.count();

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                2);

        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                3);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                500500,
                0,
                EstadoPago.COMPLETADO,
                2,
                EstadoPago.MODIFICADO,
                0,
                1);
    }

    @Test
    void exitoEditandoEstanciaApartaestudioConCambioPagoReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);

        Ocupante cliente = clienteData();

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        crearPagoInicialEnEstancia(estancia, TipoPago.RESERVA);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);
        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().plusDays(-2),
                pagoRequest
        );

        long estanciasAntes = estanciaRepository.count();

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                1,
                2);

        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                1);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                500500,
                0,
                EstadoPago.COMPLETADO,
                2,
                EstadoPago.MODIFICADO,
                0,
                1);
    }

    @Test
    void exitoEditandoEstanciaHabitacionConCambioPagoReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();
        Ocupante cliente = clienteData();

        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                listaHabitacion,
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        crearPagoInicialEnEstancia(estancia, TipoPago.RESERVA);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);
        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().plusDays(-2),
                pagoRequest
        );

        long estanciasAntes = estanciaRepository.count();

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                null,
                1,
                2);

        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                3);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                500500,
                0,
                EstadoPago.COMPLETADO,
                2,
                EstadoPago.MODIFICADO,
                0,
                1);
    }

    @Test
    void exitoEditandoEstanciaApartamentoConCambioPagoEstanciaYReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante clienteNuevo = crearOcupante();

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        crearPagoInicialEnEstancia(estancia, TipoPago.RESERVA);
        crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);
        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                acompanantesData(),
                LocalDateTime.now().plusDays(-2),
                pagoRequest
        );

        long estanciasAntes = estanciaRepository.count();

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                3);

        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                3);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                500500,
                1000,
                EstadoPago.COMPLETADO,
                2,
                EstadoPago.MODIFICADO,
                1,
                2);
    }

    @Test
    void exitoEditandoEstanciaApartaestudioConCambioPagoEstanciaYReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);

        Ocupante cliente = clienteData();

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        crearPagoInicialEnEstancia(estancia, TipoPago.RESERVA);
        crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);
        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().plusDays(-2),
                pagoRequest
        );

        long estanciasAntes = estanciaRepository.count();

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                1,
                3);

        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                1);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                500500,
                1000,
                EstadoPago.COMPLETADO,
                2,
                EstadoPago.MODIFICADO,
                1,
                2);
    }

    @Test
    void exitoEditandoEstanciaHabitacionConCambioPagoEstanciaYReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();
        Ocupante cliente = clienteData();

        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                listaHabitacion,
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        crearPagoInicialEnEstancia(estancia, TipoPago.RESERVA);
        crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);
        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().plusDays(-2),
                pagoRequest
        );

        long estanciasAntes = estanciaRepository.count();

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                null,
                1,
                3);

        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                3);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                500500,
                1000,
                EstadoPago.COMPLETADO,
                2,
                EstadoPago.MODIFICADO,
                1,
                2);
    }

    @Test
    void falloEditandoEstanciaApartamentoConReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante cliente = clienteData();

        Reserva reservaExistente = reservaData(
                cliente,
                ModoOcupacion.COMPLETO,
                EstadoReserva.CONFIRMADA,
                unidad.getHabitaciones(),
                null
        );
        reservaRepository.save(reservaExistente);

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                cliente,
                acompanantesData(),
                null,
                null
        );

        long estanciasAntes = estanciaRepository.count();
        long reservasAntes = reservaRepository.count();

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> estanciaService.editarEstancia(request, estancia.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede editar la estancia:")
                .hasMessageContaining("existe una reserva para las habitaciones con codigo");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();

        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        // No se creó estanciaData
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservasAntes);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 3);
    }

    @Test
    void falloEditandoEstanciaApartaestudioConReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);

        Ocupante cliente = clienteData();

        Reserva reservaExistente = reservaData(
                cliente,
                ModoOcupacion.COMPLETO,
                EstadoReserva.CONFIRMADA,
                unidad.getHabitaciones(),
                null
        );
        reservaRepository.save(reservaExistente);

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                cliente,
                acompanantesData(),
                null,
                null
        );

        long estanciasAntes = estanciaRepository.count();
        long reservasAntes = reservaRepository.count();

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> estanciaService.editarEstancia(request, estancia.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede editar la estancia:")
                .hasMessageContaining("existe una reserva para las habitaciones con codigo");

        entityManager.flush();
        entityManager.clear();

        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        // No se creó estanciaData
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservasAntes);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 1);
    }

    @Test
    void falloEditandoEstanciaHabitacionConReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Ocupante cliente = clienteData();

        Reserva reservaExistente = reservaData(
                cliente,
                ModoOcupacion.INDIVIDUAL,
                EstadoReserva.CONFIRMADA,
                listaHabitacion,
                null
        );
        reservaRepository.save(reservaExistente);

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.RESERVADA,
                listaHabitacion,
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                cliente,
                acompanantesData(),
                null,
                null
        );

        long estanciasAntes = estanciaRepository.count();
        long reservasAntes = reservaRepository.count();

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> estanciaService.editarEstancia(request, estancia.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede editar la estancia:")
                .hasMessageContaining("existe una reserva para las habitaciones con codigo");

        entityManager.flush();
        entityManager.clear();

        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        // No se creó estanciaData
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservasAntes);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 3);
    }

    @Test
    void falloEditandoEstanciaApartamentoCambioCodigo_test() {

        // ---------- GIVEN ----------
        Unidad unidad1 = crearApartamento(EstadoOperativo.OCUPADO);
        Unidad unidad2 = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante cliente = clienteData();

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad1.getHabitaciones(),
                null
        );

        Estancia estancia = estanciaRepository.save(estanciaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad2.getTipo(),
                unidad2.getCodigo(),
                cliente,
                acompanantesData(),
                null,
                null
        );

        long estanciasAntes = estanciaRepository.count();

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> estanciaService.editarEstancia(request, estancia.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede cambiar el codigo de la unidad asignada a la estancia");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();

        // No se creó estanciaData
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
    }

    @Test
    void falloEditandoEstanciaApartaestudioCambioCodigo_test() {

        // ---------- GIVEN ----------
        Unidad unidad1 = crearApartaestudio(EstadoOperativo.OCUPADO);
        Unidad unidad2 = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        Ocupante cliente = clienteData();

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad1.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad2.getTipo(),
                unidad2.getCodigo(),
                cliente,
                acompanantesData(),
                null,
                null
        );

        long estanciasAntes = estanciaRepository.count();

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> estanciaService.editarEstancia(request, estancia.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede cambiar el codigo de la unidad asignada a la estancia");

        entityManager.flush();
        entityManager.clear();

        // No se creó estanciaData
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
    }

    @Test
    void falloEditandoEstanciaHabitacionCambioCodigo_test() {

        // ---------- GIVEN ----------
        Unidad unidad1 = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion1 = unidad1.getHabitaciones().getFirst();

        Unidad unidad2 = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion2 = unidad2.getHabitaciones().getFirst();

        List<Habitacion> listaHabitacion1 = new ArrayList<>();
        listaHabitacion1.add(habitacion1);

        Ocupante cliente = clienteData();

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.RESERVADA,
                listaHabitacion1,
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion2.getCodigo(),
                cliente,
                acompanantesData(),
                null,
                null
        );

        long estanciasAntes = estanciaRepository.count();

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> estanciaService.editarEstancia(request, estancia.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede cambiar el codigo de la unidad asignada a la estancia");


        entityManager.flush();
        entityManager.clear();


        // No se creó estanciaData
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
    }

    /**
     * eliminarEstancia(Long idEstancia)
     */

    @Test
    void exitoEliminandoEstanciaApartamento_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        // ---------- WHEN ----------
        Void result = estanciaService.eliminarEstancia(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        assertThat(result).isNull();
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.CANCELADA);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
    }

    @Test
    void exitoEliminandoEstanciaApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        // ---------- WHEN ----------
        Void result = estanciaService.eliminarEstancia(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        assertThat(result).isNull();
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.CANCELADA);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
    }

    @Test
    void exitoEliminandoEstanciaHabitacion_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                listaHabitacion,
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        // ---------- WHEN ----------
        Void result = estanciaService.eliminarEstancia(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        assertThat(result).isNull();
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.CANCELADA);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.PARCIALMENTE, 2);
    }

    @Test
    void exitoEliminandoEstanciaApartamentoConPagoEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);
        crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA);

        // ---------- WHEN ----------
        Void result = estanciaService.eliminarEstancia(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        assertThat(result).isNull();
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.CANCELADA);
        assertThat(estanciaDb.getPagos()).hasSize(1);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                0,
                1000,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                1,
                1);
    }

    @Test
    void exitoEliminandoEstanciaApartamentoConPagoReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Ocupante cliente = clienteData();

        Reserva reserva = reservaRepository.save(
                reservaData(cliente, ModoOcupacion.COMPLETO, EstadoReserva.CONFIRMADA, unidad.getHabitaciones(), null)
        );

        Estancia estanciaExistente = estanciaData(
                reserva,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);
        reserva.setEstancia(estancia);
        reservaRepository.save(reserva);

        crearPagoInicialEnEstancia(estancia, TipoPago.RESERVA);

        // ---------- WHEN ----------
        Void result = estanciaService.eliminarEstancia(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        assertThat(result).isNull();
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.CANCELADA);
        assertThat(estanciaDb.getPagos()).hasSize(1);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                0,
                500,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                1,
                0);
    }

    @Test
    void exitoEliminandoEstanciaApartamentoConPagoReservaYEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Ocupante cliente = clienteData();

        Reserva reserva = reservaRepository.save(
                reservaData(cliente, ModoOcupacion.COMPLETO, EstadoReserva.CONFIRMADA, unidad.getHabitaciones(), null)
        );

        Estancia estanciaExistente = estanciaData(
                reserva,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);
        reserva.setEstancia(estancia);
        reservaRepository.save(reserva);

        crearPagoInicialEnEstancia(estancia, TipoPago.RESERVA);
        crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA);

        // ---------- WHEN ----------
        Void result = estanciaService.eliminarEstancia(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        assertThat(result).isNull();
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.CANCELADA);
        assertThat(estanciaDb.getPagos()).hasSize(2);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                0,
                1500,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                2,
                1);
    }

    @Test
    void exitoEliminandoEstanciaApartaestudioConPagoEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);
        crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA);

        // ---------- WHEN ----------
        Void result = estanciaService.eliminarEstancia(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        assertThat(result).isNull();
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.CANCELADA);
        assertThat(estanciaDb.getPagos()).hasSize(1);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                0,
                1000,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                1,
                1);
    }

    @Test
    void exitoEliminandoEstanciaApartaestudioConPagoReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);
        Ocupante cliente = clienteData();

        Reserva reserva = reservaRepository.save(
                reservaData(cliente, ModoOcupacion.COMPLETO, EstadoReserva.CONFIRMADA, unidad.getHabitaciones(), null)
        );

        Estancia estanciaExistente = estanciaData(
                reserva,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);
        reserva.setEstancia(estancia);
        reservaRepository.save(reserva);

        crearPagoInicialEnEstancia(estancia, TipoPago.RESERVA);

        // ---------- WHEN ----------
        Void result = estanciaService.eliminarEstancia(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        assertThat(result).isNull();
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.CANCELADA);
        assertThat(estanciaDb.getPagos()).hasSize(1);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                0,
                500,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                1,
                0);
    }

    @Test
    void exitoEliminandoEstanciaApartaestudioConPagoReservaYEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);
        Ocupante cliente = clienteData();

        Reserva reserva = reservaRepository.save(
                reservaData(cliente, ModoOcupacion.COMPLETO, EstadoReserva.CONFIRMADA, unidad.getHabitaciones(), null)
        );

        Estancia estanciaExistente = estanciaData(
                reserva,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);
        reserva.setEstancia(estancia);
        reservaRepository.save(reserva);

        crearPagoInicialEnEstancia(estancia, TipoPago.RESERVA);
        crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA);

        // ---------- WHEN ----------
        Void result = estanciaService.eliminarEstancia(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        assertThat(result).isNull();
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.CANCELADA);
        assertThat(estanciaDb.getPagos()).hasSize(2);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                0,
                1500,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                2,
                1);
    }

    @Test
    void exitoEliminandoEstanciaHabitacionConPagoEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                listaHabitacion,
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);
        crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA);

        // ---------- WHEN ----------
        Void result = estanciaService.eliminarEstancia(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        assertThat(result).isNull();
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.CANCELADA);
        assertThat(estanciaDb.getPagos()).hasSize(1);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.PARCIALMENTE, 2);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                0,
                1000,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                1,
                1);
    }

    @Test
    void exitoEliminandoEstanciaHabitacionConPagoReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Ocupante cliente = clienteData();

        Reserva reserva = reservaRepository.save(
                reservaData(cliente, ModoOcupacion.INDIVIDUAL, EstadoReserva.CONFIRMADA, listaHabitacion, null)
        );

        Estancia estanciaExistente = estanciaData(
                reserva,
                null,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                listaHabitacion,
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);
        reserva.setEstancia(estancia);
        reservaRepository.save(reserva);

        crearPagoInicialEnEstancia(estancia, TipoPago.RESERVA);

        // ---------- WHEN ----------
        Void result = estanciaService.eliminarEstancia(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        assertThat(result).isNull();
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.CANCELADA);
        assertThat(estanciaDb.getPagos()).hasSize(1);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.PARCIALMENTE, 2);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                0,
                500,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                1,
                0);
    }

    @Test
    void exitoEliminandoEstanciaHabitacionConPagoReservaYEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Ocupante cliente = clienteData();

        Reserva reserva = reservaRepository.save(
                reservaData(cliente, ModoOcupacion.INDIVIDUAL, EstadoReserva.CONFIRMADA, listaHabitacion, null)
        );

        Estancia estanciaExistente = estanciaData(
                reserva,
                null,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                listaHabitacion,
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);
        reserva.setEstancia(estancia);
        reservaRepository.save(reserva);

        crearPagoInicialEnEstancia(estancia, TipoPago.RESERVA);
        crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA);

        // ---------- WHEN ----------
        Void result = estanciaService.eliminarEstancia(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        assertThat(result).isNull();
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.CANCELADA);
        assertThat(estanciaDb.getPagos()).hasSize(2);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.PARCIALMENTE, 2);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                0,
                1500,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                2,
                1);
    }

    /**
     * obtenerEstancia(String codigoUnidad, TipoUnidad tipoUnidad)
     */

    @Test
    void exitoObteniendoEstanciaApartamento_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Ocupante cliente = crearOcupante();
        List<Ocupante> ocupantes = new ArrayList<>();
        ocupantes.add(cliente);
        ocupantes.addAll(ocupanteRepository.saveAll(acompanantesData()));

        Estancia estanciaExistente = estanciaData(
                null,
                ocupantes,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);
        crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA);

        entityManager.flush();
        entityManager.clear();

        // ---------- WHEN ----------
        EstanciaDTO result = estanciaService.obtenerEstancia(unidad.getCodigo(), TipoUnidad.APARTAMENTO);

        // ---------- THEN ----------
        assertThat(result).isNotNull();
        Estancia estanciaDb = estanciaRepository.findById(result.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                estancia.getEntradaReal(),
                estancia.getSalidaEstimada(),
                null,
                estancia.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                1
        );
        comprobarPagosDb(
                estanciaDb.getPagos(),
                1000,
                0,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                1);
    }

    @Test
    void exitoObteniendoEstanciaApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);
        Ocupante cliente = crearOcupante();
        List<Ocupante> ocupantes = new ArrayList<>();
        ocupantes.add(cliente);
        ocupantes.addAll(ocupanteRepository.saveAll(acompanantesData()));

        Estancia estanciaExistente = estanciaData(
                null,
                ocupantes,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);
        crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA);

        entityManager.flush();
        entityManager.clear();

        // ---------- WHEN ----------
        EstanciaDTO result = estanciaService.obtenerEstancia(unidad.getCodigo(), TipoUnidad.APARTAESTUDIO);

        // ---------- THEN ----------
        assertThat(result).isNotNull();
        Estancia estanciaDb = estanciaRepository.findById(result.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                estancia.getEntradaReal(),
                estancia.getSalidaEstimada(),
                null,
                estancia.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                1,
                1
        );
        comprobarPagosDb(
                estanciaDb.getPagos(),
                1000,
                0,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                1);
    }

    @Test
    void exitoObteniendoEstanciaHabitacion_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();
        Ocupante cliente = crearOcupante();
        List<Ocupante> ocupantes = new ArrayList<>();
        ocupantes.add(cliente);
        ocupantes.addAll(ocupanteRepository.saveAll(acompanantesData()));

        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Estancia estanciaExistente = estanciaData(
                null,
                ocupantes,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                listaHabitacion,
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);
        crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA);

        entityManager.flush();
        entityManager.clear();

        // ---------- WHEN ----------
        EstanciaDTO result = estanciaService.obtenerEstancia(habitacion.getCodigo(), TipoUnidad.HABITACION);

        // ---------- THEN ----------
        assertThat(result).isNotNull();
        Estancia estanciaDb = estanciaRepository.findById(result.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                estancia.getEntradaReal(),
                estancia.getSalidaEstimada(),
                null,
                estancia.getNotas(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                null,
                1,
                1
        );
        comprobarPagosDb(
                estanciaDb.getPagos(),
                1000,
                0,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                1);
    }

    /**
     * finalizarEstancia(SalidaEstanciaDTO request)
     */

    @Test
    void exitoFinalizandoEstanciaApartamento_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);

        SalidaEstanciaDTO request = salidaEstanciaRequestDTO(estancia.getId(), LocalDateTime.now(), pagoRequest);

        // ---------- WHEN ----------
        estanciaService.finalizarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();


        comprobarEstanciaDb(
                estanciaDb,
                null,
                0,
                estanciaExistente.getEntradaReal(),
                estanciaExistente.getSalidaEstimada(),
                request.getFechaSalidaReal(),
                request.getNotasSalida(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.FINALIZADA,
                BigDecimal.valueOf(500000),
                3,
                1
                );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                500000,
                0,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                1);
    }

    @Test
    void exitoFinalizandoEstanciaApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);

        SalidaEstanciaDTO request = salidaEstanciaRequestDTO(estancia.getId(), LocalDateTime.now(), pagoRequest);

        // ---------- WHEN ----------
        estanciaService.finalizarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();


        comprobarEstanciaDb(
                estanciaDb,
                null,
                0,
                estanciaExistente.getEntradaReal(),
                estanciaExistente.getSalidaEstimada(),
                request.getFechaSalidaReal(),
                request.getNotasSalida(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.FINALIZADA,
                BigDecimal.valueOf(500000),
                1,
                1
        );
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                500000,
                0,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                1);
    }

    @Test
    void exitoFinalizandoEstanciaHabitacion_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                listaHabitacion,
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);

        SalidaEstanciaDTO request = salidaEstanciaRequestDTO(estancia.getId(), LocalDateTime.now(), pagoRequest);

        // ---------- WHEN ----------
        estanciaService.finalizarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();


        comprobarEstanciaDb(
                estanciaDb,
                null,
                0,
                estanciaExistente.getEntradaReal(),
                estanciaExistente.getSalidaEstimada(),
                request.getFechaSalidaReal(),
                request.getNotasSalida(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.FINALIZADA,
                BigDecimal.valueOf(500000),
                1,
                1
        );
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.PARCIALMENTE, 2);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                500000,
                0,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                1);
    }

    @Test
    void exitoFinalizandoEstanciaApartamentoConPago_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);
        crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);

        SalidaEstanciaDTO request = salidaEstanciaRequestDTO(estancia.getId(), LocalDateTime.now(), pagoRequest);

        // ---------- WHEN ----------
        estanciaService.finalizarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();


        comprobarEstanciaDb(
                estanciaDb,
                null,
                0,
                estanciaExistente.getEntradaReal(),
                estanciaExistente.getSalidaEstimada(),
                request.getFechaSalidaReal(),
                request.getNotasSalida(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.FINALIZADA,
                BigDecimal.valueOf(501000.00),
                3,
                2
        );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                501000,
                0,
                EstadoPago.COMPLETADO,
                2,
                EstadoPago.MODIFICADO,
                0,
                2);
    }

    @Test
    void exitoFinalizandoEstanciaApartaestudioConPago_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);
        crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);

        SalidaEstanciaDTO request = salidaEstanciaRequestDTO(estancia.getId(), LocalDateTime.now(), pagoRequest);

        // ---------- WHEN ----------
        estanciaService.finalizarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();


        comprobarEstanciaDb(
                estanciaDb,
                null,
                0,
                estanciaExistente.getEntradaReal(),
                estanciaExistente.getSalidaEstimada(),
                request.getFechaSalidaReal(),
                request.getNotasSalida(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.FINALIZADA,
                BigDecimal.valueOf(501000.00),
                1,
                2
        );
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                501000,
                0,
                EstadoPago.COMPLETADO,
                2,
                EstadoPago.MODIFICADO,
                0,
                2);
    }

    @Test
    void exitoFinalizandoEstanciaHabitacionConPago_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                listaHabitacion,
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);
        crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);

        SalidaEstanciaDTO request = salidaEstanciaRequestDTO(estancia.getId(), LocalDateTime.now(), pagoRequest);

        // ---------- WHEN ----------
        estanciaService.finalizarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();


        comprobarEstanciaDb(
                estanciaDb,
                null,
                0,
                estanciaExistente.getEntradaReal(),
                estanciaExistente.getSalidaEstimada(),
                request.getFechaSalidaReal(),
                request.getNotasSalida(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.FINALIZADA,
                BigDecimal.valueOf(501000.00),
                1,
                2
        );
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.PARCIALMENTE, 2);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                501000,
                0,
                EstadoPago.COMPLETADO,
                2,
                EstadoPago.MODIFICADO,
                0,
                2);
    }

    @Test
    void exitoFinalizandoEstanciaApartamentoPagoReservaYEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Ocupante cliente = clienteData();

        Reserva reserva = reservaRepository.save(
                reservaData(cliente, ModoOcupacion.COMPLETO, EstadoReserva.CONFIRMADA, unidad.getHabitaciones(), null)
        );

        Estancia estanciaExistente = estanciaData(
                reserva,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);
        reserva.setEstancia(estancia);
        reservaRepository.save(reserva);

        crearPagoInicialEnEstancia(estancia, TipoPago.RESERVA);
        crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);

        SalidaEstanciaDTO request = salidaEstanciaRequestDTO(estancia.getId(), LocalDateTime.now(), pagoRequest);

        // ---------- WHEN ----------
        estanciaService.finalizarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();


        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                0,
                estanciaExistente.getEntradaReal(),
                estanciaExistente.getSalidaEstimada(),
                request.getFechaSalidaReal(),
                request.getNotasSalida(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.FINALIZADA,
                BigDecimal.valueOf(501500.00),
                3,
                3
        );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                501500,
                0,
                EstadoPago.COMPLETADO,
                3,
                EstadoPago.MODIFICADO,
                0,
                2);
    }

    @Test
    void exitoFinalizandoEstanciaApartaestudioPagoReservaYEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);
        Ocupante cliente = clienteData();

        Reserva reserva = reservaRepository.save(
                reservaData(cliente, ModoOcupacion.COMPLETO, EstadoReserva.CONFIRMADA, unidad.getHabitaciones(), null)
        );

        Estancia estanciaExistente = estanciaData(
                reserva,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);
        reserva.setEstancia(estancia);
        reservaRepository.save(reserva);

        crearPagoInicialEnEstancia(estancia, TipoPago.RESERVA);
        crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);

        SalidaEstanciaDTO request = salidaEstanciaRequestDTO(estancia.getId(), LocalDateTime.now(), pagoRequest);

        // ---------- WHEN ----------
        estanciaService.finalizarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();


        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                0,
                estanciaExistente.getEntradaReal(),
                estanciaExistente.getSalidaEstimada(),
                request.getFechaSalidaReal(),
                request.getNotasSalida(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.FINALIZADA,
                BigDecimal.valueOf(501500.00),
                1,
                3
        );
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                501500,
                0,
                EstadoPago.COMPLETADO,
                3,
                EstadoPago.MODIFICADO,
                0,
                2);
    }

    @Test
    void exitoFinalizandoEstanciaHabitacionPagoReservaYEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Ocupante cliente = clienteData();

        Reserva reserva = reservaRepository.save(
                reservaData(cliente, ModoOcupacion.INDIVIDUAL, EstadoReserva.CONFIRMADA, listaHabitacion, null)
        );

        Estancia estanciaExistente = estanciaData(
                reserva,
                null,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                listaHabitacion,
                null
        );
        Estancia estancia = estanciaRepository.save(estanciaExistente);
        reserva.setEstancia(estancia);
        reservaRepository.save(reserva);

        crearPagoInicialEnEstancia(estancia, TipoPago.RESERVA);
        crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA);

        SalidaEstanciaDTO request = salidaEstanciaRequestDTO(estancia.getId(), LocalDateTime.now(), pagoRequest);

        // ---------- WHEN ----------
        estanciaService.finalizarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();


        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                0,
                estanciaExistente.getEntradaReal(),
                estanciaExistente.getSalidaEstimada(),
                request.getFechaSalidaReal(),
                request.getNotasSalida(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.FINALIZADA,
                BigDecimal.valueOf(501500.00),
                1,
                3
        );
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.PARCIALMENTE, 2);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                501500,
                0,
                EstadoPago.COMPLETADO,
                3,
                EstadoPago.MODIFICADO,
                0,
                2);
    }

    /**
     * Metodos auxiliares para crear datos de prueba
     */

    @Test
    void validarCambioDeCodigoHabitacion_test() {
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        String codigoOriginal = habitacion.getCodigo();
        String nuevoCodigo = randomCodigo("HB-");

        habitacion.setCodigo(nuevoCodigo);
        habitacionRepository.save(habitacion);

        entityManager.flush();
        entityManager.clear();

        Habitacion habitacionDb = habitacionRepository.findById(habitacion.getId()).orElseThrow();
        assertThat(habitacionDb.getCodigo()).isEqualTo(nuevoCodigo);
    }

}
