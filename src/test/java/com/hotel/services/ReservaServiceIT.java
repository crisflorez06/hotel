package com.hotel.services;

import com.hotel.dtos.estancia.EstanciaRequestDTO;
import com.hotel.dtos.pago.PagoNuevoRequestDTO;
import com.hotel.dtos.reserva.ReservaNuevaRequestDTO;
import com.hotel.models.*;
import com.hotel.models.enums.*;
import com.hotel.repositories.*;
import com.hotel.services.support.AbstractServiceIT;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static com.hotel.testdata.EstanciaTestData.*;
import static com.hotel.testdata.OcupanteTestData.acompanantesData;
import static com.hotel.testdata.OcupanteTestData.clienteData;
import static com.hotel.testdata.PagoTestData.pagoNuevoRequestDTO;
import static com.hotel.testdata.ReservaTestData.*;
import static com.hotel.testdata.TestDataUtils.randomCodigo;
import static com.hotel.testutils.AssertionsHelper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReservaServiceIT extends AbstractServiceIT {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ReservaService reservaService;

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
    private AuditoriaEventoRepository eventoRepository;

    @Autowired
    private PagoRepository pagoRepository;

    /**
     * crearReserva(ReservaNuevaRequestDTO request)
     */
    @Test
    void exitoCreandoReservaNuevaApartamento_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante cliente = crearCliente(clienteData());

        ReservaNuevaRequestDTO request = reservaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, null, null);

        // ---------- WHEN ----------
        Reserva reserva = reservaService.crearReserva(request);

        // ---------- THEN (validación real en BD) ----------
        entityManager.flush();
        entityManager.clear();

        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reserva.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.RESERVA,
                reservaDb.getId()).orElseThrow();

        comprobarReservaDb(
                reservaDb,
                request.getNumeroPersonas(),
                request.getEntradaEstimada(),
                request.getSalidaEstimada(),
                ModoOcupacion.COMPLETO,
                EstadoReserva.CONFIRMADA,
                request.getCanalReserva(),
                request.getNotas(),
                3
        );

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                0,
                null,
                null,
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                null,
                3,
                0);


        assertThat(unidadDb.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);

        comprobarEventoDb(eventoDb, TipoEvento.CREACION_RESERVA, null, reserva.getCodigo(), 4);

    }

    @Test
    void exitoCreandoReservaNuevaApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        Ocupante cliente = crearCliente(clienteData());

        ReservaNuevaRequestDTO request = reservaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, null, null);

        // ---------- WHEN ----------
        Reserva reserva = reservaService.crearReserva(request);

        // ---------- THEN (validación real en BD) ----------
        entityManager.flush();
        entityManager.clear();

        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reserva.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.RESERVA,
                reservaDb.getId()).orElseThrow();

        comprobarReservaDb(
                reservaDb,
                request.getNumeroPersonas(),
                request.getEntradaEstimada(),
                request.getSalidaEstimada(),
                ModoOcupacion.COMPLETO,
                EstadoReserva.CONFIRMADA,
                request.getCanalReserva(),
                request.getNotas(),
                1
        );

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                0,
                null,
                null,
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                null,
                1,
                0);


        assertThat(unidadDb.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);

        comprobarEventoDb(eventoDb, TipoEvento.CREACION_RESERVA, null, reserva.getCodigo(), 4);

    }

    @Test
    void exitoCreandoReservaNuevaHabitacion_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        Ocupante cliente = crearCliente(clienteData());

        ReservaNuevaRequestDTO request = reservaRequestDTO(TipoUnidad.HABITACION, habitacion.getCodigo(), cliente, null, null);

        // ---------- WHEN ----------
        Reserva reserva = reservaService.crearReserva(request);

        // ---------- THEN (validación real en BD) ----------
        entityManager.flush();
        entityManager.clear();

        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reserva.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.RESERVA,
                reservaDb.getId()).orElseThrow();

        comprobarReservaDb(
                reservaDb,
                request.getNumeroPersonas(),
                request.getEntradaEstimada(),
                request.getSalidaEstimada(),
                ModoOcupacion.INDIVIDUAL,
                EstadoReserva.CONFIRMADA,
                request.getCanalReserva(),
                request.getNotas(),
                1
        );

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                0,
                null,
                null,
                null,
                request.getNotas(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.RESERVADA,
                null,
                1,
                0);


        assertThat(unidadDb.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);

        comprobarEventoDb(eventoDb, TipoEvento.CREACION_RESERVA, null, reserva.getCodigo(), 4);

    }

    @Test
    void exitoCreandoReservaNuevaApartamentoConPago_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante cliente = crearCliente(clienteData());

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ANTICIPO_RESERVA);
        ReservaNuevaRequestDTO request = reservaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, null, pagoRequest);

        // ---------- WHEN ----------
        Reserva reserva = reservaService.crearReserva(request);

        // ---------- THEN (validación real en BD) ----------
        entityManager.flush();
        entityManager.clear();

        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reserva.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.RESERVA,
                reservaDb.getId()).orElseThrow();

        comprobarReservaDb(
                reservaDb,
                request.getNumeroPersonas(),
                request.getEntradaEstimada(),
                request.getSalidaEstimada(),
                ModoOcupacion.COMPLETO,
                EstadoReserva.CONFIRMADA,
                request.getCanalReserva(),
                request.getNotas(),
                3
        );

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                0,
                null,
                null,
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                null,
                3,
                1);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                request.getPago().getMonto(),
                BigDecimal.valueOf(0),
                request.getPago().getEstado(),
                1,
                EstadoPago.MODIFICADO,
                0,
                0,
                0,
                1);


        assertThat(unidadDb.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);

        comprobarEventoDb(eventoDb, TipoEvento.CREACION_RESERVA, null, reserva.getCodigo(), 4);

    }

    @Test
    void falloCreandoReservaNuevaPorReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante cliente = crearCliente(clienteData());

        crearReservaExistente(unidad.getHabitaciones(), true);

        ReservaNuevaRequestDTO request = reservaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, null, null);


        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> reservaService.crearReserva(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede crear la reserva:")
                .hasMessageContaining("existe una reserva para las habitaciones con codigo");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();

        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservaAntes);

    }

    @Test
    void falloCreandoReservaNuevaPorEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante cliente = crearCliente(clienteData());

        crearEstanciaExistente(unidad.getHabitaciones(), true);

        ReservaNuevaRequestDTO request = reservaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, LocalDate.now().plusDays(1), null);


        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> reservaService.crearReserva(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede crear la reserva:")
                .hasMessageContaining("existe una estancia para las habitaciones con codigo");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();

        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservaAntes);

    }

    @Test
    void falloCreandoReservaConFechaSalidaAnteriorAEntrada_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        ReservaNuevaRequestDTO request = errorFechasReservaRequestDTO(unidad);


        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();


        // ---------- WHEN + THEN (excepción) ----------
        assertThatThrownBy(() -> reservaService.crearReserva(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de salida estimada no puede ser anterior a la fecha de entrada");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();


        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservaAntes);

    }

    @Test
    void falloCreandoEstanciaConFechaEntradaAnteriorAAhora_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante cliente = crearCliente(clienteData());

        ReservaNuevaRequestDTO request = reservaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, LocalDate.now().plusDays(-1), null);


        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();


        // ---------- WHEN + THEN (excepción) ----------
        assertThatThrownBy(() -> reservaService.crearReserva(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de entrada debe ser posterior a la fecha actual");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();


        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservaAntes);
    }






    /**
     * Metodos auxiliares para crear datos de prueba
     */


}
