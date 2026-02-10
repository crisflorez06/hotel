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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static com.hotel.testdata.EstanciaTestData.estanciaData;
import static com.hotel.testdata.HabitacionTestData.habitacionData;
import static com.hotel.testdata.PagoTestData.pagoData;
import static com.hotel.testdata.ReservaTestData.reservaData;
import static com.hotel.testdata.TestDataUtils.randomCodigo;
import static com.hotel.testdata.OcupanteTestData.acompanantesData;
import static com.hotel.testdata.OcupanteTestData.clienteData;
import static com.hotel.testdata.UnidadTestData.unidadData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EstanciaServiceIT {


    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private EstanciaService estanciaService;

    @Autowired
    private UnidadRepository unidadRepository;

    @Autowired
    private HabitacionRepository habitacionRepository;

    @Autowired
    private OcupanteRepository ocupanteRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private EstanciaRepository estanciaRepository;

    @Autowired
    private PagoRepository pagoRepository;

    /**
     * crearEstanciaNueva(EstanciaRequestDTO request)
     */
    @Test
    void exitoCreandoEstanciaNuevaDepartamentoConPago_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        // Cliente
        Ocupante cliente = ocupanteRepository.save(clienteData());


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
    }

    @Test
    void exitoCreandoEstanciaNuevaApartaestudioConPago_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        // Cliente
        Ocupante cliente = ocupanteRepository.save(clienteData());


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
    }

    @Test
    void exitoCreandoEstanciaNuevaHabitacionConPago_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        // Cliente
        Ocupante cliente = ocupanteRepository.save(clienteData());


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
    }

    @Test
    void exitoCreandoEstanciaNuevaDepartamentoSinPago_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        // Cliente
        Ocupante cliente = ocupanteRepository.save(clienteData());


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
        Ocupante cliente = ocupanteRepository.save(clienteData());


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
        Ocupante cliente = ocupanteRepository.save(clienteData());


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

        Ocupante cliente = ocupanteRepository.save(clienteData());

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
                .hasMessageContaining("existe una estancia para la habitacion con codigo");

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

        Ocupante cliente = ocupanteRepository.save(clienteData());

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
                .hasMessageContaining("existe una estancia para la habitacion con codigo");

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

        Ocupante cliente = ocupanteRepository.save(clienteData());

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
                .hasMessageContaining("existe una estancia para la habitacion con codigo");

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

        Ocupante cliente = ocupanteRepository.save(clienteData());

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

        Ocupante cliente = ocupanteRepository.save(clienteData());

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

        Ocupante cliente = ocupanteRepository.save(clienteData());

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
        Ocupante cliente = ocupanteRepository.save(clienteData());

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
    }

    @Test
    void exitoActivandoEstanciaApartaestudioPagoEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = ocupanteRepository.save(clienteData());

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
    }

    @Test
    void exitoActivandoEstanciaHabitacionPagoEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Ocupante cliente = ocupanteRepository.save(clienteData());

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
    }

    @Test
    void exitoActivandoEstanciaApartamentoPagoReservaYEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = ocupanteRepository.save(clienteData());

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
    }

    @Test
    void exitoActivandoEstanciaApartaestudioPagoReservaYEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = ocupanteRepository.save(clienteData());

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
    }

    @Test
    void exitoActivandoEstanciaHabitacionPagoReservaYEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Ocupante cliente = ocupanteRepository.save(clienteData());

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
    }

    @Test
    void exitoActivandoEstanciaApartamentoPagoReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = ocupanteRepository.save(clienteData());

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
    }

    @Test
    void exitoActivandoEstanciaApartaestudioPagoReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = ocupanteRepository.save(clienteData());

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
    }

    @Test
    void exitoActivandoEstanciaHabitacionPagoReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Ocupante cliente = ocupanteRepository.save(clienteData());

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
    }

    @Test
    void exitoActivandoEstanciaApartamentoSinPagos_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = ocupanteRepository.save(clienteData());

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
        Ocupante cliente = ocupanteRepository.save(clienteData());

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

        Ocupante cliente = ocupanteRepository.save(clienteData());

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

        Ocupante clienteNuevo = ocupanteRepository.save(clienteData());

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

        Ocupante cliente = ocupanteRepository.save(clienteData());

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

        Ocupante cliente = ocupanteRepository.save(clienteData());

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

        Ocupante clienteNuevo = ocupanteRepository.save(clienteData());

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

    }

    @Test
    void exitoEditandoEstanciaApartaestudioPagoNuevo_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);

        Ocupante cliente = ocupanteRepository.save(clienteData());

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

    }

    @Test
    void exitoEditandoEstanciaHabitacionPagoNuevo_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        Ocupante cliente = ocupanteRepository.save(clienteData());

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
    }

    @Test
    void exitoEditandoEstanciaApartamentoConCambioPago_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante clienteNuevo = ocupanteRepository.save(clienteData());

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
    }

    @Test
    void exitoEditandoEstanciaApartaestudioConCambioPago_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);

        Ocupante cliente = ocupanteRepository.save(clienteData());

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
    }

    @Test
    void exitoEditandoEstanciaHabitacionConCambioPago_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();
        Ocupante cliente = ocupanteRepository.save(clienteData());

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
    }

    @Test
    void falloEditandoEstanciaApartamentoConReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante cliente = ocupanteRepository.save(clienteData());

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

        Ocupante cliente = ocupanteRepository.save(clienteData());

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

        Ocupante cliente = ocupanteRepository.save(clienteData());

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
    void exitoEliminandoEstanciaApartamentoConPago_test() {

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
    }

    @Test
    void exitoEliminandoEstanciaApartaestudioConPago_test() {

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
    }

    @Test
    void exitoEliminandoEstanciaHabitacionConPago_test() {

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
    }

    /**
     * obtenerEstancia(String codigoUnidad, TipoUnidad tipoUnidad)
     */

    @Test
    void exitoObteniendoEstanciaApartamento_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Ocupante cliente = ocupanteRepository.save(clienteData());
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
    }

    @Test
    void exitoObteniendoEstanciaApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);
        Ocupante cliente = ocupanteRepository.save(clienteData());
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
    }

    @Test
    void exitoObteniendoEstanciaHabitacion_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();
        Ocupante cliente = ocupanteRepository.save(clienteData());
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
    }

    @Test
    void exitoFinalizandoEstanciaApartamentoPagoReservaYEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Ocupante cliente = ocupanteRepository.save(clienteData());

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
                BigDecimal.valueOf(502000.00),
                3,
                3
        );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
    }

    @Test
    void exitoFinalizandoEstanciaApartaestudioPagoReservaYEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);
        Ocupante cliente = ocupanteRepository.save(clienteData());

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
                BigDecimal.valueOf(502000.00),
                1,
                3
        );
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
    }

    @Test
    void exitoFinalizandoEstanciaHabitacionPagoReservaYEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Ocupante cliente = ocupanteRepository.save(clienteData());

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
                BigDecimal.valueOf(502000.00),
                1,
                3
        );
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.PARCIALMENTE, 2);
    }



    private Unidad crearApartamento(EstadoOperativo estadoOperativo) {

        // 1. Crear y persistir la unidadData
        Unidad unidad = unidadRepository.save(
                unidadData(TipoUnidad.APARTAMENTO, estadoOperativo)
        );

        // 2. Crear habitaciones asociadas
        Habitacion h1 = habitacionData(unidad, randomCodigo("HB-"), estadoOperativo);
        Habitacion h2 = habitacionData(unidad, randomCodigo("HB-"), estadoOperativo);
        Habitacion h3 = habitacionData(unidad, randomCodigo("HB-"), estadoOperativo);

        List<Habitacion> habitaciones = new ArrayList<>(List.of(h1, h2, h3));

        // 3. Persistir habitaciones (lado dueño)
        habitacionRepository.saveAll(habitaciones);

        // 4. (Opcional pero recomendado) coherencia en memoria
        unidad.setHabitaciones(habitaciones);

        return unidad;
    }

    private Unidad crearApartaestudio(EstadoOperativo estadoOperativo) {

        // 1. Crear y persistir la unidadData
        Unidad unidad = unidadRepository.save(
                unidadData(TipoUnidad.APARTAESTUDIO, estadoOperativo)
        );

        // 2. Crear habitaciones asociadas
        Habitacion h1 = habitacionData(unidad, randomCodigo("HB-"), estadoOperativo);

        List<Habitacion> habitaciones = new ArrayList<>(List.of(h1));

        // 3. Persistir habitaciones (lado dueño)
        habitacionRepository.saveAll(habitaciones);

        // 4. (Opcional pero recomendado) coherencia en memoria
        unidad.setHabitaciones(habitaciones);

        return unidad;
    }

    private EstanciaRequestDTO estanciaRequestDTO(TipoUnidad tipoUnidad, String codigo, Ocupante cliente, List<Ocupante> acompanantes, LocalDateTime fecha,PagoNuevoRequestDTO pago) {
        EstanciaRequestDTO request = new EstanciaRequestDTO();

        List<Long> acompanantesIds = acompanantes.stream()
                .map(Ocupante::getId)
                .toList();

        request.setTipoUnidad(tipoUnidad);
        request.setCodigo(codigo);

        request.setIdCliente(cliente.getId());

        if(fecha != null){
            request.setEntradaReal(fecha);
            request.setSalidaEstimada(fecha.plusDays(3));
        } else {
        LocalDateTime entrada = LocalDateTime.now();
        request.setEntradaReal(entrada);
        request.setSalidaEstimada(entrada.plusDays(3));
        }
        request.setNotas("Estancia de prueba");
        request.setPago(pago);
        request.setIdAcompanantes(acompanantesIds);
        return request;
    }

    private ActivarEstanciaDTO activarEstanciaRequestDTO(Long idReserva, Ocupante cliente, List<Ocupante> acompanantes, LocalDateTime fecha, PagoNuevoRequestDTO pago) {
        ActivarEstanciaDTO request = new ActivarEstanciaDTO();

        List<Long> acompanantesIds = acompanantes.stream()
                .map(Ocupante::getId)
                .toList();

        request.setIdReserva(idReserva);
        request.setIdCliente(cliente.getId());
        request.setEntradaReal(fecha);
        request.setSalidaEstimada(fecha.plusDays(3));
        request.setPago(pago);
        request.setIdAcompanantes(acompanantesIds);
        return request;
    }

    private SalidaEstanciaDTO salidaEstanciaRequestDTO(Long idEstancia, LocalDateTime fechaSalidaReal, PagoNuevoRequestDTO pagoEstancia) {
        SalidaEstanciaDTO request = new SalidaEstanciaDTO();
        request.setIdEstancia(idEstancia);
        request.setFechaSalidaReal(fechaSalidaReal);
        request.setPagoEstancia(pagoEstancia);
        request.setNotasSalida("Salida de prueba");
        return request;
    }

    private PagoNuevoRequestDTO pagoNuevoRequestDTO(TipoPago tipoPago) {
        PagoNuevoRequestDTO pago = new PagoNuevoRequestDTO();

        pago.setTipoPago(tipoPago);
        pago.setMonto(java.math.BigDecimal.valueOf(500000));
        pago.setMedioPago(MedioPago.EFECTIVO);
        pago.setFecha(LocalDateTime.now());
        pago.setEstado(EstadoPago.COMPLETADO);

        return pago;
    }

    private Pago crearPagoInicialEnEstancia(Estancia estancia, TipoPago tipoPago) {
        return pagoRepository.save(pagoData(tipoPago, estancia));

    }

    private void comprobarEstanciaDb(
            Estancia estancia,
            Reserva reserva,
            Integer totalOcupantes,
            LocalDateTime entradaReal,
            LocalDateTime salidaEstimada,
            LocalDateTime fechaSalidaReal,
            String notas,
            ModoOcupacion modoOcupacion,
            EstadoEstancia estadoEstancia,
            BigDecimal precioTotal,
            Integer totalHabitaciones,
            Integer totalPagos
    ) {

        // ---- Folio ----
        assertThat(estancia.getCodigoFolio()).isNotBlank();

        // ---- Reserva ----
        if (reserva == null) {
            assertThat(estancia.getReserva()).isNull();
        } else {
            assertThat(estancia.getReserva()).isNotNull();
            assertThat(estancia.getReserva().getId()).isEqualTo(reserva.getId());
        }

        // ---- Ocupantes ----
        assertThat(estancia.getOcupantes()).hasSize(totalOcupantes);

        // ---- Fechas ----
        assertThat(estancia.getEntradaReal()).isEqualToIgnoringNanos(entradaReal);
        assertThat(estancia.getSalidaEstimada()).isEqualToIgnoringNanos(salidaEstimada);
        if(fechaSalidaReal == null) {
            assertThat(estancia.getSalidaReal()).isNull();
        } else {
            assertThat(estancia.getSalidaReal()).isEqualToIgnoringNanos(fechaSalidaReal);
        }

        // ---- Estados ----
        assertThat(estancia.getModoOcupacion()).isEqualTo(modoOcupacion);
        assertThat(estancia.getEstado()).isEqualTo(estadoEstancia);

        // ---- Precio total ----
        if (precioTotal == null) {
            assertThat(estancia.getPrecioTotal()).isNull();
        } else {
            assertThat(estancia.getPrecioTotal())
                    .isNotNull()
                    .isEqualByComparingTo(precioTotal);
        }

        // ---- Notas ----
        if (notas == null) {
            assertThat(estancia.getNotas()).isNull();
        } else {
            assertThat(estancia.getNotas()).contains(notas);
        }

        // ---- Habitaciones ----
        if (totalHabitaciones != null) {
            assertThat(estancia.getHabitaciones()).hasSize(totalHabitaciones);
        }

        // ---- Pagos ----
        if (totalPagos != null) {
            assertThat(estancia.getPagos()).hasSize(totalPagos);
        }
    }

    private void comprobarUnidadYHabitacionesDb(
            Unidad unidad,
            EstadoOperativo estadoOperativo,
            Integer totalOcupadasEnUnidad
    ) {
        assertThat(unidad.getEstadoOperativo()).isEqualTo(estadoOperativo);


        Integer ocupadas = Math.toIntExact(unidad.getHabitaciones().stream()
                        .filter(h -> h.getEstadoOperativo() == EstadoOperativo.OCUPADO)
                        .count());

        assertThat(ocupadas).isEqualTo(totalOcupadasEnUnidad);
    }
}
