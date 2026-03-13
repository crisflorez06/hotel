package com.hotel.services;

import com.hotel.dtos.reserva.ReservaDTO;
import com.hotel.dtos.reserva.ReservaTablaDTO;
import com.hotel.dtos.reserva.ReservaRequestDTO;
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
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static com.hotel.testdata.EstanciaTestData.*;
import static com.hotel.testdata.OcupanteTestData.*;
import static com.hotel.testdata.OcupanteTestData.acompanantesDataEditar;
import static com.hotel.testdata.OcupanteTestData.clienteEditarData;
import static com.hotel.testdata.PagoTestData.pagoNuevoRequestDTO;
import static com.hotel.testdata.ReservaTestData.*;
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
    private ReservaRepository reservaRepository;

    @Autowired
    private EstanciaRepository estanciaRepository;

    @Autowired
    private AuditoriaEventoRepository eventoRepository;

    @Autowired
    private PagoRepository pagoRepository;

    /**
     * crearReserva(ReservaRequestDTO request)
     */
    @Test
    void exitoCreandoReservaNuevaApartamento_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante cliente = crearCliente(clienteData());

        ReservaRequestDTO request = reservaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, null);

        // ---------- WHEN ----------
        ReservaDTO reserva = reservaService.crearReserva(request);

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
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                null,
                3,
                0);

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), null, reservaDb);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarEventoDb(eventoDb, TipoEvento.CREACION_RESERVA, null, reservaDb.getCodigo(), 4);

    }

    @Test
    void exitoCreandoReservaNuevaApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        Ocupante cliente = crearCliente(clienteData());

        ReservaRequestDTO request = reservaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, null);

        // ---------- WHEN ----------
        ReservaDTO reserva = reservaService.crearReserva(request);

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
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                null,
                1,
                0);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), null, reservaDb);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);


        comprobarEventoDb(eventoDb, TipoEvento.CREACION_RESERVA, null, reservaDb.getCodigo(), 4);

    }

    @Test
    void exitoCreandoReservaNuevaHabitacion_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        Ocupante cliente = crearCliente(clienteData());

        ReservaRequestDTO request = reservaRequestDTO(TipoUnidad.HABITACION, habitacion.getCodigo(), cliente, null);

        // ---------- WHEN ----------
        ReservaDTO reserva = reservaService.crearReserva(request);

        // ---------- THEN (validación real en BD) ----------
        entityManager.flush();
        entityManager.clear();

        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reserva.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Habitacion habitacionDb = unidadDb.getHabitaciones().stream()
                .filter(h -> h.getCodigo().equals(habitacion.getCodigo()))
                .findFirst().orElseThrow();
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
                null,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.RESERVADA,
                null,
                1,
                0);

        comprobarHabitacionesDb(List.of(habitacionDb), null, reservaDb);
        comprobarHabitacionesDb(List.of(habitacionDb), estanciaDb, null);

        comprobarEventoDb(eventoDb, TipoEvento.CREACION_RESERVA, null, reservaDb.getCodigo(), 4);

    }


    @Test
    void falloCreandoReservaNuevaPorReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante cliente = crearCliente(clienteData());

        crearReservaExistente(unidad.getHabitaciones(), true, EstadoReserva.CONFIRMADA);

        ReservaRequestDTO request = reservaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, null);


        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();
        long eventosAntes = eventoRepository.count();

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
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);

    }

    @Test
    void falloCreandoReservaNuevaPorEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante cliente = crearCliente(clienteData());

        crearEstanciaExistente(unidad.getHabitaciones(), true, EstadoEstancia.ACTIVA);

        ReservaRequestDTO request = reservaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, LocalDate.now().plusDays(1));


        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();
        long eventosAntes = eventoRepository.count();

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
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);

    }

    @Test
    void falloCreandoReservaConFechaSalidaAnteriorAEntrada_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        ReservaRequestDTO request = errorFechasReservaRequestDTO(unidad);


        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();
        long eventosAntes = eventoRepository.count();


        // ---------- WHEN + THEN (excepción) ----------
        assertThatThrownBy(() -> reservaService.crearReserva(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de salida estimada no puede ser anterior a la fecha de entrada");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();


        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservaAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);

    }

    @Test
    void falloCreandoEstanciaConFechaEntradaAnteriorAAhora_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante cliente = crearCliente(clienteData());

        ReservaRequestDTO request = reservaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, LocalDate.now().plusDays(-1));


        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();
        long eventosAntes = eventoRepository.count();


        // ---------- WHEN + THEN (excepción) ----------
        assertThatThrownBy(() -> reservaService.crearReserva(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de entrada debe ser posterior a la fecha actual");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();


        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservaAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);
    }

    /**
     * editarReserva(ReservaRequestDTO request, Long idReserva)
     */

    @Test
    void exitoEditandoReservaApartamento_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());


        Reserva reservaExistente = crearReservaExistente(unidad.getHabitaciones(), true, EstadoReserva.CONFIRMADA);

        ReservaRequestDTO request = reservaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                LocalDate.now().plusDays(3)
        );

        request.setCanalReserva(CanalReserva.PLATAFORMA_AIRBINB);
        request.setNumeroPersonas(4);

        // ---------- WHEN ----------
        reservaService.editarReserva(request, reservaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Reserva reservaDb = reservaRepository.findById(reservaExistente.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reservaExistente.getId()).orElseThrow();
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
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                null,
                3,
                1);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), null, reservaDb);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);


        comprobarOcupantesDb(List.of(reservaDb.getCliente()), clienteNuevo, null);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_RESERVA, null, reservaDb.getCodigo(), 5);


    }

    @Test
    void exitoEditandoReservaApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());


        Reserva reservaExistente = crearReservaExistente(unidad.getHabitaciones(), true, EstadoReserva.CONFIRMADA);

        ReservaRequestDTO request = reservaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                LocalDate.now().plusDays(3)
        );

        request.setCanalReserva(CanalReserva.PLATAFORMA_AIRBINB);
        request.setNumeroPersonas(4);

        // ---------- WHEN ----------
        reservaService.editarReserva(request, reservaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();



        Reserva reservaDb = reservaRepository.findById(reservaExistente.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reservaExistente.getId()).orElseThrow();
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
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                null,
                1,
                1);


        comprobarOcupantesDb(List.of(reservaDb.getCliente()), clienteNuevo, null);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), null, reservaDb);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);
        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_RESERVA, null, reservaDb.getCodigo(), 5);

    }

    @Test
    void exitoEditandoReservaHabitacion_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();
        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);


        Ocupante clienteNuevo = crearCliente(clienteEditarData());


        Reserva reservaExistente = crearReservaExistente(listaHabitacion, true, EstadoReserva.CONFIRMADA);

        ReservaRequestDTO request = reservaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                clienteNuevo,
                LocalDate.now().plusDays(3)
        );

        request.setCanalReserva(CanalReserva.PLATAFORMA_AIRBINB);
        request.setNumeroPersonas(4);

        // ---------- WHEN ----------
        reservaService.editarReserva(request, reservaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Reserva reservaDb = reservaRepository.findById(reservaExistente.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reservaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Habitacion habitacionDb = unidadDb.getHabitaciones().stream()
                .filter(h -> h.getCodigo().equals(habitacion.getCodigo()))
                .findFirst().orElseThrow();
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
                null,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.RESERVADA,
                null,
                1,
                1);


        comprobarOcupantesDb(List.of(reservaDb.getCliente()), clienteNuevo, null);

        comprobarHabitacionesDb(List.of(habitacionDb), null, reservaDb);
        comprobarHabitacionesDb(List.of(habitacionDb), estanciaDb, null);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_RESERVA, null, reservaDb.getCodigo(), 5);


    }

    @Test
    void exitoEditandoReservaConCambioApartamento_test() {

        // ---------- GIVEN ----------
        Unidad unidad1 = crearApartamento(EstadoOperativo.DISPONIBLE);
        Unidad unidad2 = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());


        Reserva reservaExistente = crearReservaExistente(unidad1.getHabitaciones(), true, EstadoReserva.CONFIRMADA);

        ReservaRequestDTO request = reservaRequestDTO(
                unidad2.getTipo(),
                unidad2.getCodigo(),
                clienteNuevo,
                LocalDate.now().plusDays(3)
        );

        request.setCanalReserva(CanalReserva.PLATAFORMA_AIRBINB);
        request.setNumeroPersonas(4);

        // ---------- WHEN ----------
        reservaService.editarReserva(request, reservaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Reserva reservaDb = reservaRepository.findById(reservaExistente.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reservaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad2.getId()).orElseThrow();
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
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                null,
                3,
                1);


        comprobarOcupantesDb(List.of(reservaDb.getCliente()), clienteNuevo, null);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), null, reservaDb);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_RESERVA, null, reservaDb.getCodigo(), 6);

    }

    @Test
    void exitoEditandoReservaConCambioApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad unidad1 = crearApartaestudio(EstadoOperativo.DISPONIBLE);
        Unidad unidad2 = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());


        Reserva reservaExistente = crearReservaExistente(unidad1.getHabitaciones(), true, EstadoReserva.CONFIRMADA);

        ReservaRequestDTO request = reservaRequestDTO(
                unidad2.getTipo(),
                unidad2.getCodigo(),
                clienteNuevo,
                LocalDate.now().plusDays(3)
        );

        request.setCanalReserva(CanalReserva.PLATAFORMA_AIRBINB);
        request.setNumeroPersonas(4);

        // ---------- WHEN ----------
        reservaService.editarReserva(request, reservaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Reserva reservaDb = reservaRepository.findById(reservaExistente.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reservaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad2.getId()).orElseThrow();
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
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                null,
                1,
                1);


        comprobarOcupantesDb(List.of(reservaDb.getCliente()), clienteNuevo, null);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), null, reservaDb);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_RESERVA, null, reservaDb.getCodigo(), 6);

    }

    @Test
    void exitoEditandoReservaConCambioHabitacion_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion1 = unidad.getHabitaciones().getFirst();
        Habitacion habitacion2 = unidad.getHabitaciones().getLast();
        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion1);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());


        Reserva reservaExistente = crearReservaExistente(listaHabitacion, true, EstadoReserva.CONFIRMADA);

        ReservaRequestDTO request = reservaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion2.getCodigo(),
                clienteNuevo,
                LocalDate.now().plusDays(3)
        );

        request.setCanalReserva(CanalReserva.PLATAFORMA_AIRBINB);
        request.setNumeroPersonas(4);

        // ---------- WHEN ----------
        reservaService.editarReserva(request, reservaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Reserva reservaDb = reservaRepository.findById(reservaExistente.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reservaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Habitacion habitacionDb = unidadDb.getHabitaciones().stream()
                .filter(h -> h.getCodigo().equals(habitacion2.getCodigo()))
                .findFirst().orElseThrow();
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
                null,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.RESERVADA,
                null,
                1,
                1);


        comprobarOcupantesDb(List.of(reservaDb.getCliente()), clienteNuevo, null);
        comprobarHabitacionesDb(List.of(habitacionDb), null, reservaDb);
        comprobarHabitacionesDb(List.of(habitacionDb), estanciaDb, null);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_RESERVA, null, reservaDb.getCodigo(), 6);

    }

    @Test
    void exitoEditandoReservaConCambioApartamentoAApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.DISPONIBLE);
        Unidad apartaestudio = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());


        Reserva reservaExistente = crearReservaExistente(apartamento.getHabitaciones(), true, EstadoReserva.CONFIRMADA);

        ReservaRequestDTO request = reservaRequestDTO(
                apartaestudio.getTipo(),
                apartaestudio.getCodigo(),
                clienteNuevo,
                LocalDate.now().plusDays(3)
        );

        request.setCanalReserva(CanalReserva.PLATAFORMA_AIRBINB);
        request.setNumeroPersonas(4);

        // ---------- WHEN ----------
        reservaService.editarReserva(request, reservaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Reserva reservaDb = reservaRepository.findById(reservaExistente.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reservaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(apartaestudio.getId()).orElseThrow();
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
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                null,
                1,
                1);


        comprobarOcupantesDb(List.of(reservaDb.getCliente()), clienteNuevo, null);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), null, reservaDb);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_RESERVA, null, reservaDb.getCodigo(), 6);

    }

    @Test
    void exitoEditandoReservaConCambioApartamentoAHabitacionMismaUnidad_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        Ocupante clienteNuevo = crearCliente(clienteEditarData());


        Reserva reservaExistente = crearReservaExistente(unidad.getHabitaciones(), true, EstadoReserva.CONFIRMADA);

        ReservaRequestDTO request = reservaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                clienteNuevo,
                LocalDate.now().plusDays(3)
        );

        request.setCanalReserva(CanalReserva.PLATAFORMA_AIRBINB);
        request.setNumeroPersonas(4);

        // ---------- WHEN ----------
        reservaService.editarReserva(request, reservaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Reserva reservaDb = reservaRepository.findById(reservaExistente.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reservaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Habitacion habitacionDb = unidadDb.getHabitaciones().stream()
                .filter(h -> h.getCodigo().equals(habitacion.getCodigo()))
                .findFirst().orElseThrow();
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
                null,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.RESERVADA,
                null,
                1,
                1);
        comprobarHabitacionesDb(List.of(habitacionDb), null, reservaDb);
        comprobarHabitacionesDb(List.of(habitacionDb), estanciaDb, null);

        comprobarOcupantesDb(List.of(reservaDb.getCliente()), clienteNuevo, null);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_RESERVA, null, reservaDb.getCodigo(), 6);


    }

    @Test
    void exitoEditandoReservaConCambioApartamentoAHabitacionDiferenteUnidad_test() {

        // ---------- GIVEN ----------
        Unidad apartamento1 = crearApartamento(EstadoOperativo.DISPONIBLE);
        Unidad apartamento2 = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = apartamento2.getHabitaciones().getFirst();

        Ocupante clienteNuevo = crearCliente(clienteEditarData());


        Reserva reservaExistente = crearReservaExistente(apartamento1.getHabitaciones(), true, EstadoReserva.CONFIRMADA);

        ReservaRequestDTO request = reservaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                clienteNuevo,
                LocalDate.now().plusDays(3)
        );

        request.setCanalReserva(CanalReserva.PLATAFORMA_AIRBINB);
        request.setNumeroPersonas(4);

        // ---------- WHEN ----------
        reservaService.editarReserva(request, reservaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Reserva reservaDb = reservaRepository.findById(reservaExistente.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reservaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(apartamento2.getId()).orElseThrow();
        Habitacion habitacionDb = unidadDb.getHabitaciones().stream()
                .filter(h -> h.getCodigo().equals(habitacion.getCodigo()))
                .findFirst().orElseThrow();
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
                null,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.RESERVADA,
                null,
                1,
                1);
        comprobarHabitacionesDb(List.of(habitacionDb), null, reservaDb);
        comprobarHabitacionesDb(List.of(habitacionDb), estanciaDb, null);


        comprobarOcupantesDb(List.of(reservaDb.getCliente()), clienteNuevo, null);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_RESERVA, null, reservaDb.getCodigo(), 6);


    }

    @Test
    void exitoEditandoReservaConCambioApartaestudioAApartamento_test() {

        // ---------- GIVEN ----------
        Unidad apartaestudio = crearApartaestudio(EstadoOperativo.DISPONIBLE);
        Unidad apartamento = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());


        Reserva reservaExistente = crearReservaExistente(apartaestudio.getHabitaciones(), true, EstadoReserva.CONFIRMADA);

        ReservaRequestDTO request = reservaRequestDTO(
                apartamento.getTipo(),
                apartamento.getCodigo(),
                clienteNuevo,
                LocalDate.now().plusDays(3)
        );

        request.setCanalReserva(CanalReserva.PLATAFORMA_AIRBINB);
        request.setNumeroPersonas(4);

        // ---------- WHEN ----------
        reservaService.editarReserva(request, reservaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Reserva reservaDb = reservaRepository.findById(reservaExistente.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reservaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(apartamento.getId()).orElseThrow();
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
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                null,
                3,
                1);


        comprobarOcupantesDb(List.of(reservaDb.getCliente()), clienteNuevo, null);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), null, reservaDb);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_RESERVA, null, reservaDb.getCodigo(), 6);

    }

    @Test
    void exitoEditandoReservaConCambioApartaestudioAHabitacion_test() {

        // ---------- GIVEN ----------
        Unidad apartaestudio = crearApartaestudio(EstadoOperativo.DISPONIBLE);
        Unidad apartamento = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = apartamento.getHabitaciones().getFirst();

        Ocupante clienteNuevo = crearCliente(clienteEditarData());


        Reserva reservaExistente = crearReservaExistente(apartaestudio.getHabitaciones(), true, EstadoReserva.CONFIRMADA);

        ReservaRequestDTO request = reservaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                clienteNuevo,
                LocalDate.now().plusDays(3)
        );

        request.setCanalReserva(CanalReserva.PLATAFORMA_AIRBINB);
        request.setNumeroPersonas(4);

        // ---------- WHEN ----------
        reservaService.editarReserva(request, reservaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();



        Reserva reservaDb = reservaRepository.findById(reservaExistente.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reservaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(apartamento.getId()).orElseThrow();
        Habitacion habitacionDb = unidadDb.getHabitaciones().stream()
                .filter(h -> h.getCodigo().equals(habitacion.getCodigo()))
                .findFirst().orElseThrow();
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
                null,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.RESERVADA,
                null,
                1,
                1);


        comprobarOcupantesDb(List.of(reservaDb.getCliente()), clienteNuevo, null);
        comprobarHabitacionesDb(List.of(habitacionDb), null, reservaDb);
        comprobarHabitacionesDb(List.of(habitacionDb), estanciaDb, null);
        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_RESERVA, null, reservaDb.getCodigo(), 6);

    }

    @Test
    void exitoEditandoReservaConCambioHabitacionAApartamentoMismaUnidad_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        Ocupante clienteNuevo = crearCliente(clienteEditarData());


        Reserva reservaExistente = crearReservaExistente(List.of(habitacion), true, EstadoReserva.CONFIRMADA);

        ReservaRequestDTO request = reservaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                LocalDate.now().plusDays(3)
        );

        request.setCanalReserva(CanalReserva.PLATAFORMA_AIRBINB);
        request.setNumeroPersonas(4);

        // ---------- WHEN ----------
        reservaService.editarReserva(request, reservaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Reserva reservaDb = reservaRepository.findById(reservaExistente.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reservaExistente.getId()).orElseThrow();
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
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                null,
                3,
                1);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), null, reservaDb);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarOcupantesDb(List.of(reservaDb.getCliente()), clienteNuevo, null);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_RESERVA, null, reservaDb.getCodigo(), 6);


    }

    @Test
    void exitoEditandoReservaConCambioHabitacionAApartamentoDiferenteUnidad_test() {

        // ---------- GIVEN ----------
        Unidad apartamento1 = crearApartamento(EstadoOperativo.DISPONIBLE);
        Unidad apartamento2 = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = apartamento2.getHabitaciones().getFirst();

        Ocupante clienteNuevo = crearCliente(clienteEditarData());


        Reserva reservaExistente = crearReservaExistente(List.of(habitacion), true, EstadoReserva.CONFIRMADA);

        ReservaRequestDTO request = reservaRequestDTO(
                apartamento1.getTipo(),
                apartamento1.getCodigo(),
                clienteNuevo,
                LocalDate.now().plusDays(3)
        );

        request.setCanalReserva(CanalReserva.PLATAFORMA_AIRBINB);
        request.setNumeroPersonas(4);

        // ---------- WHEN ----------
        reservaService.editarReserva(request, reservaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Reserva reservaDb = reservaRepository.findById(reservaExistente.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reservaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(apartamento1.getId()).orElseThrow();
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
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                null,
                3,
                1);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), null, reservaDb);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);


        comprobarOcupantesDb(List.of(reservaDb.getCliente()), clienteNuevo, null);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_RESERVA, null, reservaDb.getCodigo(), 6);


    }

    @Test
    void exitoEditandoReservaConCambioHabitacionAApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad apartaestudio = crearApartaestudio(EstadoOperativo.DISPONIBLE);
        Unidad apartamento = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = apartamento.getHabitaciones().getFirst();

        Ocupante clienteNuevo = crearCliente(clienteEditarData());


        Reserva reservaExistente = crearReservaExistente(List.of(habitacion), true, EstadoReserva.CONFIRMADA);

        ReservaRequestDTO request = reservaRequestDTO(
                apartaestudio.getTipo(),
                apartaestudio.getCodigo(),
                clienteNuevo,
                LocalDate.now().plusDays(3)
        );

        request.setCanalReserva(CanalReserva.PLATAFORMA_AIRBINB);
        request.setNumeroPersonas(4);

        // ---------- WHEN ----------
        reservaService.editarReserva(request, reservaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();



        Reserva reservaDb = reservaRepository.findById(reservaExistente.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reservaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(apartaestudio.getId()).orElseThrow();
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
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                null,
                1,
                1);


        comprobarOcupantesDb(List.of(reservaDb.getCliente()), clienteNuevo, null);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), null, reservaDb);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);
        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_RESERVA, null, reservaDb.getCodigo(), 6);

    }

    @Test
    void exitoEditandoReservaConCambioUnidadSinCambioFecha_test() {

        // ---------- GIVEN ----------
        Unidad unidad1 = crearApartamento(EstadoOperativo.DISPONIBLE);
        Unidad unidad2 = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());


        Reserva reservaExistente = crearReservaExistente(unidad1.getHabitaciones(), true, EstadoReserva.CONFIRMADA);

        ReservaRequestDTO request = reservaRequestDTO(
                unidad2.getTipo(),
                unidad2.getCodigo(),
                clienteNuevo,
                null
        );

        request.setEntradaEstimada(reservaExistente.getEntradaEstimada().toLocalDate());
        request.setSalidaEstimada(reservaExistente.getSalidaEstimada().toLocalDate());
        request.setCanalReserva(CanalReserva.PLATAFORMA_AIRBINB);
        request.setNumeroPersonas(4);

        // ---------- WHEN ----------
        LocalDate entradaOriginal = reservaExistente.getEntradaEstimada().toLocalDate();
        LocalDate salidaOriginal = reservaExistente.getSalidaEstimada().toLocalDate();

        reservaService.editarReserva(request, reservaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Reserva reservaDb = reservaRepository.findById(reservaExistente.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reservaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad2.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.RESERVA,
                reservaDb.getId()).orElseThrow();

        assertThat(reservaDb.getEntradaEstimada().toLocalDate()).isEqualTo(entradaOriginal);
        assertThat(reservaDb.getSalidaEstimada().toLocalDate()).isEqualTo(salidaOriginal);

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
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                null,
                3,
                1);


        comprobarOcupantesDb(List.of(reservaDb.getCliente()), clienteNuevo, null);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), null, reservaDb);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_RESERVA, null, reservaDb.getCodigo(), 4);

    }

    @Test
    void exitoEditandoReservaSinCambios_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Reserva reservaExistente = crearReservaExistente(unidad.getHabitaciones(), true, EstadoReserva.CONFIRMADA);

        Ocupante cliente = reservaExistente.getCliente();



        ReservaRequestDTO request = reservaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                cliente,
                null
        );

        request.setEntradaEstimada(reservaExistente.getEntradaEstimada().toLocalDate());
        request.setSalidaEstimada(reservaExistente.getSalidaEstimada().toLocalDate());


        // ---------- WHEN ----------
        Long eventosAntes = eventoRepository.count();

        reservaService.editarReserva(request, reservaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Reserva reservaDb = reservaRepository.findById(reservaExistente.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reservaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

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
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                null,
                3,
                1);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), null, reservaDb);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);


        comprobarOcupantesDb(List.of(reservaDb.getCliente()), cliente, null);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);


    }

    @Test
    void falloEditandoReservaNuevaPorReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());


        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), true, EstadoReserva.CONFIRMADA);
        crearReservaExistente(unidad.getHabitaciones(), true, EstadoReserva.CONFIRMADA);

        ReservaRequestDTO request = reservaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                LocalDate.now().plusDays(3)
        );

        request.setCanalReserva(CanalReserva.PLATAFORMA_AIRBINB);
        request.setNumeroPersonas(4);

        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();
        long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> reservaService.editarReserva(request, reserva.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede editar la reserva:")
                .hasMessageContaining("existe una reserva para las habitaciones con codigo");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();

        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservaAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);

    }

    @Test
    void falloEditandoReservaNuevaPorEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante clienteNuevo = crearCliente(clienteData());

        crearEstanciaExistente(unidad.getHabitaciones(), true, EstadoEstancia.ACTIVA);
        Reserva reservaExistente = crearReservaExistente(unidad.getHabitaciones(), true, EstadoReserva.CONFIRMADA);

        ReservaRequestDTO request = reservaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                LocalDate.now().plusDays(1)
        );

        request.setCanalReserva(CanalReserva.PLATAFORMA_AIRBINB);
        request.setNumeroPersonas(4);

        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();
        long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> reservaService.editarReserva(request, reservaExistente.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede editar la reserva:")
                .hasMessageContaining("existe una estancia para las habitaciones con codigo");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();

        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservaAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);

    }

    @Test
    void falloEditandoReservaConFechaSalidaAnteriorAEntrada_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);


        Reserva reservaExistente = crearReservaExistente(unidad.getHabitaciones(), true, EstadoReserva.CONFIRMADA);

        ReservaRequestDTO request = errorFechasReservaRequestDTO(unidad);


        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();
        long eventosAntes = eventoRepository.count();

        // ---------- WHEN + THEN (excepción) ----------
        assertThatThrownBy(() -> reservaService.editarReserva(request, reservaExistente.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de salida estimada no puede ser anterior a la fecha de entrada");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();


        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservaAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);

    }

    @Test
    void falloEditandoEstanciaConFechaEntradaAnteriorAAhora_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante cliente = crearCliente(clienteData());

        Reserva reservaExistente = crearReservaExistente(unidad.getHabitaciones(), true, EstadoReserva.CONFIRMADA);

        ReservaRequestDTO request = reservaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, LocalDate.now().plusDays(-1));


        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();
        long eventosAntes = eventoRepository.count();


        // ---------- WHEN + THEN (excepción) ----------
        assertThatThrownBy(() -> reservaService.editarReserva(request, reservaExistente.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de entrada debe ser posterior a la fecha actual");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();


        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservaAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);
    }

    @Test
    void falloEditandoReservaPorEstadoDiferenteAConfirmada_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());


        Reserva reservaExistente = crearReservaExistente(unidad.getHabitaciones(), true, EstadoReserva.CONFIRMADA);
        reservaExistente.setEstado(EstadoReserva.COMPLETADA);
        reservaRepository.save(reservaExistente);

        ReservaRequestDTO request = reservaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                LocalDate.now().plusDays(3)
        );

        request.setCanalReserva(CanalReserva.PLATAFORMA_AIRBINB);
        request.setNumeroPersonas(4);

        // ---------- WHEN ----------
        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();
        long eventosAntes = eventoRepository.count();


        // ---------- WHEN + THEN (excepción) ----------
        assertThatThrownBy(() -> reservaService.editarReserva(request, reservaExistente.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo se puede editar una reserva en estado CONFIRMADA");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();


        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservaAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);

    }

    /**
     * eliminarReserva(Long idReserva)
     */

    @Test
    void exitoEliminandoReservaApartamento_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);

        // ---------- WHEN ----------
        Void result = reservaService.eliminarReserva(reserva.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reserva.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.RESERVA,
                reservaDb.getId()).orElseThrow();

        assertThat(result).isNull();
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.CANCELADA);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), null, reservaDb);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);
        comprobarEventoDb(eventoDb, TipoEvento.ELIMINACION_RESERVA, null, reservaDb.getCodigo(), 1);
    }

    @Test
    void exitoEliminandoReservaApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);

        // ---------- WHEN ----------
        Void result = reservaService.eliminarReserva(reserva.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reserva.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.RESERVA,
                reservaDb.getId()).orElseThrow();

        assertThat(result).isNull();
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.CANCELADA);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), null, reservaDb);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);
        comprobarEventoDb(eventoDb, TipoEvento.ELIMINACION_RESERVA, null, reservaDb.getCodigo(), 1);
    }

    @Test
    void exitoEliminandoReservaHabitacion_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        Reserva reserva = crearReservaExistente(List.of(habitacion), false, EstadoReserva.CONFIRMADA);

        // ---------- WHEN ----------
        Void result = reservaService.eliminarReserva(reserva.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reserva.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Habitacion habitacionDb = unidadDb.getHabitaciones().stream()
                .filter(h -> h.getCodigo().equals(habitacion.getCodigo()))
                .findFirst().orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.RESERVA,
                reservaDb.getId()).orElseThrow();

        assertThat(result).isNull();
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.CANCELADA);
        comprobarHabitacionesDb(List.of(habitacionDb), null, reservaDb);
        comprobarHabitacionesDb(List.of(habitacionDb), estanciaDb, null);
        comprobarEventoDb(eventoDb, TipoEvento.ELIMINACION_RESERVA, null, reservaDb.getCodigo(), 1);
    }

    @Test
    void exitoEliminandoReservaConPagoAnterior_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), true, EstadoReserva.CONFIRMADA);
        BigDecimal monto = reserva.getEstancia().getPagos().stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ---------- WHEN ----------
        Void result = reservaService.eliminarReserva(reserva.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reserva.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.RESERVA,
                reservaDb.getId()).orElseThrow();

        assertThat(result).isNull();
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.CANCELADA);
        assertThat(estanciaDb.getPagos()).hasSize(reserva.getEstancia().getPagos().size());
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), null, reservaDb);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                BigDecimal.ZERO,
                monto,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                1,
                0,
                0,
                1,
                0);
        comprobarEventoDb(eventoDb, TipoEvento.ELIMINACION_RESERVA, null, reservaDb.getCodigo(), 1);
    }

    @Test
    void falloEliminandoReservaPorEstadoDiferenteAConfirmada_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), true, EstadoReserva.CONFIRMADA);
        reserva.setEstado(EstadoReserva.COMPLETADA);
        reservaRepository.save(reserva);

        // ---------- WHEN ----------
        long reservasAntes = reservaRepository.count();
        long estanciasAntes = estanciaRepository.count();
        long eventosAntes = eventoRepository.count();

        assertThatThrownBy(() -> reservaService.eliminarReserva(reserva.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo se puede eliminar una reserva en estado CONFIRMADA");

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        assertThat(reservaRepository.count()).isEqualTo(reservasAntes);
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);

        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reserva.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.COMPLETADA);
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.RESERVADA);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), null, reservaDb);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);
    }

    /**
     * buscarReservasPorNumeroDocumento(String numeroDocumento)
     */

    @Test
    void exitoBuscandoReservasPorNumeroDocumentoExacto_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), true, EstadoReserva.CONFIRMADA);
        Ocupante cliente = reserva.getCliente();
        cliente.setNumeroDocumento("CC-RESERVA-1001");
        cliente.setNombres("Mario");
        cliente.setApellidos("Lopez");
        ocupanteRepository.save(cliente);

        BigDecimal totalAnticipoEsperado = reserva.getEstancia().getPagos().stream()
                .filter(pago -> pago.getTipoPago() == TipoPago.ANTICIPO_RESERVA)
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ---------- WHEN ----------
        List<ReservaDTO> resultado = reservaService.buscarReservasPorNumeroDocumento("CC-RESERVA-1001");

        // ---------- THEN ----------
        assertThat(resultado).hasSize(1);

        ReservaDTO dto = resultado.getFirst();
        assertThat(dto.getId()).isEqualTo(reserva.getId());
        assertThat(dto.getCodigoReserva()).isEqualTo(reserva.getCodigo());
        assertThat(dto.getEstadoReserva()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(dto.getNumeroPersonas()).isEqualTo(reserva.getNumeroPersonas());
        assertThat(dto.getCliente().getId()).isEqualTo(cliente.getId());
        assertThat(dto.getCliente().getNombres()).isEqualTo("Mario Lopez");
        assertThat(dto.getEntradaEstimada()).isEqualTo(reserva.getEntradaEstimada());
        assertThat(dto.getSalidaEstimada()).isEqualTo(reserva.getSalidaEstimada());
    }

    @Test
    void exitoBuscandoReservasPorNumeroDocumentoParcial_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.DISPONIBLE);
        Unidad apartaestudio = crearApartaestudio(EstadoOperativo.DISPONIBLE);
        Unidad unidadHabitacion = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidadHabitacion.getHabitaciones().getFirst();

        Reserva reservaApartamento = crearReservaExistente(apartamento.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        reservaApartamento.getCliente().setNumeroDocumento("DOC-777-APT");
        reservaApartamento.getCliente().setNombres("Laura");
        reservaApartamento.getCliente().setApellidos("Ruiz");
        ocupanteRepository.save(reservaApartamento.getCliente());

        Reserva reservaApartaestudio = crearReservaExistente(apartaestudio.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        reservaApartaestudio.getCliente().setNumeroDocumento("DOC-777-AE");
        reservaApartaestudio.getCliente().setNombres("Pablo");
        reservaApartaestudio.getCliente().setApellidos("Diaz");
        ocupanteRepository.save(reservaApartaestudio.getCliente());

        Reserva reservaHabitacion = crearReservaExistente(List.of(habitacion), false, EstadoReserva.CONFIRMADA);
        reservaHabitacion.getCliente().setNumeroDocumento("DOC-777-HB");
        reservaHabitacion.getCliente().setNombres("Sofia");
        reservaHabitacion.getCliente().setApellidos("Mesa");
        ocupanteRepository.save(reservaHabitacion.getCliente());

        // ---------- WHEN ----------
        List<ReservaDTO> resultado = reservaService.buscarReservasPorNumeroDocumento("777");

        // ---------- THEN ----------
        assertThat(resultado).hasSize(3);

        Map<String, ReservaDTO> reservasPorCodigo = resultado.stream()
                .collect(java.util.stream.Collectors.toMap(ReservaDTO::getCodigoReserva, dto -> dto));

        assertThat(reservasPorCodigo).containsKeys(
                reservaApartamento.getCodigo(),
                reservaApartaestudio.getCodigo(),
                reservaHabitacion.getCodigo()
        );

        assertThat(reservasPorCodigo.get(reservaApartamento.getCodigo()).getCodigoReserva())
                .isEqualTo(reservaApartamento.getCodigo());

        assertThat(reservasPorCodigo.get(reservaApartaestudio.getCodigo()).getCodigoReserva())
                .isEqualTo(reservaApartaestudio.getCodigo());

        assertThat(reservasPorCodigo.get(reservaHabitacion.getCodigo()).getCodigoReserva())
                .isEqualTo(reservaHabitacion.getCodigo());
    }

    @Test
    void exitoBuscandoReservasPorNumeroDocumentoExcluyeEstadosDiferentesAConfirmada_test() {

        // ---------- GIVEN ----------
        Unidad unidadConfirmada = crearApartamento(EstadoOperativo.DISPONIBLE);
        Unidad unidadCancelada = crearApartamento(EstadoOperativo.DISPONIBLE);

        Reserva reservaConfirmada = crearReservaExistente(unidadConfirmada.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        reservaConfirmada.getCliente().setNumeroDocumento("FILTRO-555");
        ocupanteRepository.save(reservaConfirmada.getCliente());

        Reserva reservaCancelada = crearReservaExistente(unidadCancelada.getHabitaciones(), false, EstadoReserva.CANCELADA);
        reservaCancelada.getCliente().setNumeroDocumento("FILTRO-555");
        ocupanteRepository.save(reservaCancelada.getCliente());
        reservaRepository.save(reservaCancelada);

        // ---------- WHEN ----------
        List<ReservaDTO> resultado = reservaService.buscarReservasPorNumeroDocumento("FILTRO-555");

        // ---------- THEN ----------
        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().getId()).isEqualTo(reservaConfirmada.getId());
        assertThat(resultado.getFirst().getCodigoReserva()).isEqualTo(reservaConfirmada.getCodigo());
        assertThat(resultado.getFirst().getEstadoReserva()).isEqualTo(EstadoReserva.CONFIRMADA);
    }

    @Test
    void falloBuscandoReservasPorNumeroDocumentoNulo_test() {

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> reservaService.buscarReservasPorNumeroDocumento(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("numeroDocumento es obligatorio");
    }

    @Test
    void falloBuscandoReservasPorNumeroDocumentoVacio_test() {

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> reservaService.buscarReservasPorNumeroDocumento("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("numeroDocumento es obligatorio");
    }

    /**
     * buscarReservasTabla(...)
     */

    @Test
    void exitoBuscandoReservasTablaConFiltrosYMapeo_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), true, EstadoReserva.CONFIRMADA);
        Ocupante cliente = reserva.getCliente();
        cliente.setNombres("Andrea");
        cliente.setApellidos("Morales");
        cliente.setNumeroDocumento("TABLA-RES-001");
        ocupanteRepository.save(cliente);

        BigDecimal totalPagoReservaEsperado = reserva.getEstancia().getPagos().stream()
                .filter(pago -> pago.getTipoPago() == TipoPago.ANTICIPO_RESERVA)
                .filter(pago -> pago.getEstado() == EstadoPago.COMPLETADO)
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Unidad otraUnidad = crearApartaestudio(EstadoOperativo.DISPONIBLE);
        Reserva otraReserva = crearReservaExistente(otraUnidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        otraReserva.getCliente().setNumeroDocumento("OTRO-999");
        ocupanteRepository.save(otraReserva.getCliente());

        entityManager.flush();
        entityManager.clear();

        // ---------- WHEN ----------
        Page<ReservaTablaDTO> resultado = reservaService.buscarReservasTabla(
                List.of(EstadoReserva.CONFIRMADA),
                List.of(CanalReserva.MOSTRADOR),
                ModoOcupacion.COMPLETO,
                TipoUnidad.APARTAMENTO,
                reserva.getCodigo(),
                unidad.getCodigo(),
                "Andrea Morales",
                "TABLA-RES",
                cliente.getId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true,
                PageRequest.of(0, 10)
        );

        // ---------- THEN ----------
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent()).hasSize(1);

        ReservaTablaDTO dto = resultado.getContent().getFirst();
        assertThat(dto.getId()).isEqualTo(reserva.getId());
        assertThat(dto.getCodigoReserva()).isEqualTo(reserva.getCodigo());
        assertThat(dto.getCodigoEstancia()).isNull();
        assertThat(dto.getCodigoUnidad()).isEqualTo(unidad.getCodigo());
        assertThat(dto.getTipoUnidad()).isEqualTo(TipoUnidad.APARTAMENTO);
        assertThat(dto.getNombreCliente()).isEqualTo("Andrea Morales");
        assertThat(dto.getIdCliente()).isEqualTo(cliente.getId());
        assertThat(dto.getNumeroDocumentoCliente()).isEqualTo("TABLA-RES-001");
        assertThat(dto.getNumeroPersonas()).isEqualTo(reserva.getNumeroPersonas());
        assertThat(dto.getCanalReserva()).isEqualTo(CanalReserva.MOSTRADOR);
        assertThat(dto.getModoOcupacion()).isEqualTo(ModoOcupacion.COMPLETO);
        assertThat(dto.getEstadoReserva()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(dto.getTieneEstanciaAsociada()).isFalse();
        assertThat(dto.getTotalPagoReserva()).isEqualByComparingTo(totalPagoReservaEsperado);
        assertThat(dto.getCantidadPagosModificadosOEliminados()).isZero();
    }

    @Test
    void exitoBuscandoReservasTablaConOrdenPorDefectoYPaginacion_test() {

        // ---------- GIVEN ----------
        Unidad unidad1 = crearApartamento(EstadoOperativo.DISPONIBLE);
        Unidad unidad2 = crearApartamento(EstadoOperativo.DISPONIBLE);
        Unidad unidad3 = crearApartamento(EstadoOperativo.DISPONIBLE);

        Reserva reservaAntigua = crearReservaExistente(unidad1.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        reservaAntigua.setFechaCreacion(LocalDateTime.now().minusDays(3));
        reservaRepository.save(reservaAntigua);

        Reserva reservaMedia = crearReservaExistente(unidad2.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        reservaMedia.setFechaCreacion(LocalDateTime.now().minusDays(2));
        reservaRepository.save(reservaMedia);

        Reserva reservaReciente = crearReservaExistente(unidad3.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        reservaReciente.setFechaCreacion(LocalDateTime.now().minusDays(1));
        reservaRepository.save(reservaReciente);

        entityManager.flush();
        entityManager.clear();

        // ---------- WHEN ----------
        Page<ReservaTablaDTO> pagina0 = reservaService.buscarReservasTabla(
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                PageRequest.of(0, 2)
        );

        Page<ReservaTablaDTO> pagina1 = reservaService.buscarReservasTabla(
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null,
                PageRequest.of(1, 2)
        );

        // ---------- THEN ----------
        assertThat(pagina0.getContent()).hasSize(2);
        assertThat(pagina0.getContent().get(0).getId()).isEqualTo(reservaReciente.getId());
        assertThat(pagina0.getContent().get(1).getId()).isEqualTo(reservaMedia.getId());

        assertThat(pagina1.getContent()).hasSize(1);
        assertThat(pagina1.getContent().getFirst().getId()).isEqualTo(reservaAntigua.getId());
    }

}
