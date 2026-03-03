package com.hotel.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.hotel.dtos.estancia.ActivarEstanciaDTO;
import com.hotel.dtos.estancia.EstanciaDTO;
import com.hotel.dtos.estancia.EstanciaRequestDTO;
import com.hotel.dtos.estancia.EstanciaTablaDTO;
import com.hotel.dtos.estancia.SalidaEstanciaDTO;
import com.hotel.dtos.pago.CalcularPagoDTO;
import com.hotel.dtos.pago.PagoNuevoRequestDTO;
import com.hotel.mappers.PagoMapper;
import com.hotel.models.*;
import com.hotel.models.enums.*;
import com.hotel.repositories.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.hotel.services.support.AbstractServiceIT;
import com.hotel.utils.EventoNuevoJsonBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static com.hotel.testdata.EstanciaTestData.*;
import static com.hotel.testdata.OcupanteTestData.*;
import static com.hotel.testdata.PagoTestData.pagoNuevoRequestDTO;
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
    private PagoService pagoService;

    @Autowired
    private EstanciaRepository estanciaRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private OcupanteRepository ocupanteRepository;

    @Autowired
    private AuditoriaEventoRepository eventoRepository;

    /**
     * crearEstanciaNueva(EstanciaRequestDTO request)
     */
    @Test
    void exitoCreandoEstanciaNuevaApartamento_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        // Cliente
        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());


        EstanciaRequestDTO request = estanciaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, acompanantes, null);

        // ---------- WHEN ----------
        Estancia estancia = estanciaService.crearEstanciaNueva(request);

        // ---------- THEN (validación real en BD) ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();

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

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarEventoDb(eventoDb, TipoEvento.CREACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 4);

    }

    @Test
    void exitoCreandoEstanciaNuevaApartaestudio_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        // Cliente
        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());


        EstanciaRequestDTO request = estanciaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, acompanantes, null);

        // ---------- WHEN ----------
        Estancia estancia = estanciaService.crearEstanciaNueva(request);

        // ---------- THEN (validación real en BD) ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();


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

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarEventoDb(eventoDb, TipoEvento.CREACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 4);

    }

    @Test
    void exitoCreandoEstanciaNuevaHabitacion_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        // Cliente
        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());


        EstanciaRequestDTO request = estanciaRequestDTO(TipoUnidad.HABITACION, habitacion.getCodigo(), cliente, acompanantes, null);

        // ---------- WHEN ----------
        Estancia estancia = estanciaService.crearEstanciaNueva(request);

        // ---------- THEN (validación real en BD) ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();
        Habitacion habitacionDb = unidadDb.getHabitaciones().stream()
                .filter(h -> h.getCodigo().equals(habitacion.getCodigo()))
                .findFirst().orElseThrow();

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

        comprobarHabitacionesDb(List.of(habitacionDb), estanciaDb, null);

        comprobarEventoDb(eventoDb, TipoEvento.CREACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 4);

    }

    @Test
    void exitoCreandoEstanciaConEstadoExcedida_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);


        // Cliente
        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());


        EstanciaRequestDTO request = estanciaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, acompanantes, LocalDateTime.now().plusDays(-4));

        // ---------- WHEN ----------
        Estancia estancia = estanciaService.crearEstanciaNueva(request);

        // ---------- THEN (validación real en BD) ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.EXCEDIDA,
                null,
                3,
                0);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                3
        );

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarEventoDb(eventoDb, TipoEvento.CREACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 4);

    }

    @Test
    void exitoCreandoEstanciaNuevaSinAcompanantes_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        // Cliente
        Ocupante cliente = crearCliente(clienteData());

        EstanciaRequestDTO request = estanciaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, null, null);

        // ---------- WHEN ----------
        Estancia estancia = estanciaService.crearEstanciaNueva(request);

        // ---------- THEN (validación real en BD) ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                1,
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

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarEventoDb(eventoDb, TipoEvento.CREACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 4);

    }

    @Test
    void falloCreandoEstanciaNuevaPorEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());

        crearEstanciaExistente(unidad.getHabitaciones(), true, EstadoEstancia.ACTIVA);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                cliente,
                acompanantes,
                null
        );

        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();
        long eventosAntes = eventoRepository.count();


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
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 3);
    }

    @Test
    void falloCreandoEstanciaNuevaPorReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());

        crearReservaExistente(unidad.getHabitaciones(), true, EstadoReserva.CONFIRMADA);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                cliente,
               acompanantes,
                null
        );

        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();
        long eventosAntes = eventoRepository.count();


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
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
    }

    @Test
    void falloCreandoEstanciaConFechaSalidaAnteriorAEntrada_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        EstanciaRequestDTO request = errorFechasEstanciaRequestDTO(unidad);


        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();
        long eventosAntes = eventoRepository.count();


        // ---------- WHEN + THEN (excepción) ----------
        assertThatThrownBy(() -> estanciaService.crearEstanciaNueva(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de salida debe ser posterior a la fecha de entrada");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();

        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        // No se creó estanciaData
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);

    }

    @Test
    void falloCreandoEstanciaConFechaEntradaSuperiorAAhora_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        // Cliente
        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());


        EstanciaRequestDTO request = estanciaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, acompanantes, LocalDateTime.now().plusDays(1));


        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();
        long eventosAntes = eventoRepository.count();


        // ---------- WHEN + THEN (excepción) ----------
        assertThatThrownBy(() -> estanciaService.crearEstanciaNueva(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de entrada no puede ser superior a la fecha actual, crear reserva si se desea una estancia futura");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();

        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        // No se creó estanciaData
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);

    }


    /**
     * editarEstancia(EstanciaRequestDTO request, Long idEstancia)
     */

    @Test
    void exitoEditandoEstanciaApartamento_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(unidad.getHabitaciones(), true, EstadoEstancia.ACTIVA);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2)
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                2,
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

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 4);


    }

    @Test
    void exitoEditandoEstanciaApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());

        Estancia estanciaExistente = crearEstanciaExistente(unidad.getHabitaciones(), true, EstadoEstancia.ACTIVA);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2)
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                2,
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

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 4);

    }

    @Test
    void exitoEditandoEstanciaHabitacion_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Habitacion habitacion = unidad.getHabitaciones().getFirst();
        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());

        Estancia estanciaExistente = crearEstanciaExistente(listaHabitacion, true, EstadoEstancia.ACTIVA);

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2)
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();
        Habitacion habitacionDb = unidadDb.getHabitaciones().stream()
                .filter(h -> h.getCodigo().equals(habitacion.getCodigo()))
                .findFirst().orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                2,
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

        comprobarHabitacionesDb(List.of(habitacionDb), estanciaDb, null);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 4);
    }

    @Test
    void exitoEditandoEstanciaConReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaConReservaExistente(unidad.getHabitaciones(), false, false);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2)
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                estanciaExistente.getReserva(),
                2,
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

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 4);


    }

    @Test
    void exitoEditandoEstanciaConCambioApartamento_test() {

        // ---------- GIVEN ----------
        Unidad unidad1 = crearApartamento(EstadoOperativo.OCUPADO);
        Unidad unidad2 = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(unidad1.getHabitaciones(), true, EstadoEstancia.ACTIVA);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad2.getTipo(),
                unidad2.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2)
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Unidad unidadDb1 = unidadRepository.findById(unidad1.getId()).orElseThrow();
        Unidad unidadDb2 = unidadRepository.findById(unidad2.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                2,
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
                unidadDb1,
                EstadoOperativo.DISPONIBLE,
                0);

        comprobarUnidadYHabitacionesDb(
                unidadDb2,
                EstadoOperativo.OCUPADO,
                3);

        comprobarHabitacionesDb(unidadDb2.getHabitaciones(), estanciaDb, null);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);


    }

    @Test
    void exitoEditandoEstanciaConCambioApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad unidad1 = crearApartaestudio(EstadoOperativo.OCUPADO);
        Unidad unidad2 = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());

        Estancia estanciaExistente = crearEstanciaExistente(unidad1.getHabitaciones(), true, EstadoEstancia.ACTIVA);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad2.getTipo(),
                unidad2.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2)
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Unidad unidadDb1 = unidadRepository.findById(unidad1.getId()).orElseThrow();
        Unidad unidadDb2 = unidadRepository.findById(unidad2.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                2,
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
                unidadDb1,
                EstadoOperativo.DISPONIBLE,
                0);

        comprobarUnidadYHabitacionesDb(
                unidadDb2,
                EstadoOperativo.OCUPADO,
                1);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarHabitacionesDb(unidadDb2.getHabitaciones(), estanciaDb, null);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);

    }

    @Test
    void exitoEditandoEstanciaConCambioHabitacion_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.PARCIALMENTE);

        Habitacion habitacion1 = unidad.getHabitaciones().getFirst();
        Habitacion habitacion2 = unidad.getHabitaciones().getLast();
        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion1);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());

        Estancia estanciaExistente = crearEstanciaExistente(listaHabitacion, true, EstadoEstancia.ACTIVA);

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion2.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2)
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                2,
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
                EstadoOperativo.PARCIALMENTE,
                1);

        Habitacion h1Db = unidadDb.getHabitaciones().stream()
                .filter(h -> h.getCodigo().equals(habitacion1.getCodigo()))
                .findFirst().orElseThrow();

        Habitacion h2Db = unidadDb.getHabitaciones().stream()
                .filter(h -> h.getCodigo().equals(habitacion2.getCodigo()))
                .findFirst().orElseThrow();

        assertThat(h1Db.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
        assertThat(h2Db.getEstadoOperativo()).isEqualTo(EstadoOperativo.OCUPADO);

        comprobarHabitacionesDb(List.of(h2Db), estanciaDb, null);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);
    }

    @Test
    void exitoEditandoEstanciaConCambioApartamentoAApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.OCUPADO);
        Unidad apartaestudio = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(apartamento.getHabitaciones(), true, EstadoEstancia.ACTIVA);
        BigDecimal montoPago = estanciaExistente.getPagos().stream().map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal montoPendiente = calcularMontoPendienteCambioUnidad(estanciaExistente, apartamento.getTipo());

        EstanciaRequestDTO request = estanciaRequestDTO(
                apartaestudio.getTipo(),
                apartaestudio.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2)
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Unidad apartamentoDb = unidadRepository.findById(apartamento.getId()).orElseThrow();
        Unidad apartaestudioDb = unidadRepository.findById(apartaestudio.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();


        comprobarEstanciaDb(
                estanciaDb,
                null,
                2,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                1,
                2);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                montoPago,
                BigDecimal.ZERO,
                montoPendiente,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.PENDIENTE,
                1,
                0,
                1,
                0,
                1);

        comprobarUnidadYHabitacionesDb(
                apartamentoDb,
                EstadoOperativo.DISPONIBLE,
                0);

        comprobarUnidadYHabitacionesDb(
                apartaestudioDb,
                EstadoOperativo.OCUPADO,
                1);

        comprobarHabitacionesDb(apartaestudioDb.getHabitaciones(), estanciaDb, null);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);


    }

    @Test
    void exitoEditandoEstanciaConCambioApartamentoAHabitacionMismaUnidad_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = apartamento.getHabitaciones().getFirst();

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(apartamento.getHabitaciones(), true, EstadoEstancia.ACTIVA);
        BigDecimal montoPago = estanciaExistente.getPagos().stream().map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal montoPendiente = calcularMontoPendienteCambioUnidad(estanciaExistente, apartamento.getTipo());

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2)
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Unidad apartamentoDb = unidadRepository.findById(apartamento.getId()).orElseThrow();
        Habitacion habitacionDb = apartamentoDb.getHabitaciones().stream()
                .filter(h -> h.getCodigo().equals(habitacion.getCodigo()))
                .findFirst().orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();


        comprobarEstanciaDb(
                estanciaDb,
                null,
                2,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                null,
                1,
                2);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                montoPago,
                BigDecimal.ZERO,
                montoPendiente,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.PENDIENTE,
                1,
                0,
                1,
                0,
                1);

        comprobarUnidadYHabitacionesDb(
                apartamentoDb,
                EstadoOperativo.PARCIALMENTE,
                1);

        comprobarHabitacionesDb(List.of(habitacionDb), estanciaDb, null);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);


    }

    @Test
    void exitoEditandoEstanciaConCambioApartamentoAHabitacionDiferenteUnidad_test() {

        // ---------- GIVEN ----------
        Unidad apartamento1 = crearApartamento(EstadoOperativo.OCUPADO);
        Unidad apartamento2 = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = apartamento2.getHabitaciones().getFirst();

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(apartamento1.getHabitaciones(), true, EstadoEstancia.ACTIVA);
        BigDecimal montoPago = estanciaExistente.getPagos().stream().map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal montoPendiente = calcularMontoPendienteCambioUnidad(estanciaExistente, apartamento1.getTipo());

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2)
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Unidad apartamentoDb1 = unidadRepository.findById(apartamento1.getId()).orElseThrow();
        Unidad apartamentoDb2 = unidadRepository.findById(apartamento2.getId()).orElseThrow();
        Habitacion habitacionDb = apartamentoDb2.getHabitaciones().stream()
                .filter(h -> h.getCodigo().equals(habitacion.getCodigo()))
                .findFirst().orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();
        AuditoriaEvento eventoPagoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                2,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                null,
                1,
                2);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                montoPago,
                BigDecimal.ZERO,
                montoPendiente,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.PENDIENTE,
                1,
                0,
                1,
                0,
                1);

        comprobarUnidadYHabitacionesDb(
                apartamentoDb1,
                EstadoOperativo.DISPONIBLE,
                0);

        comprobarUnidadYHabitacionesDb(
                apartamentoDb2,
                EstadoOperativo.PARCIALMENTE,
                1);

        comprobarHabitacionesDb(List.of(habitacionDb), estanciaDb, null);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);


    }

    @Test
    void exitoEditandoEstanciaConCambioApartaestudioAApartamento_test() {

        // ---------- GIVEN ----------
        Unidad apartaestudio = crearApartaestudio(EstadoOperativo.OCUPADO);
        Unidad apartamento = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(apartaestudio.getHabitaciones(), true, EstadoEstancia.ACTIVA);
        BigDecimal montoPago = estanciaExistente.getPagos().stream().map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal montoPendiente = calcularMontoPendienteCambioUnidad(estanciaExistente, apartaestudio.getTipo());


        EstanciaRequestDTO request = estanciaRequestDTO(
                apartamento.getTipo(),
                apartamento.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2)
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Unidad apartamentoDb = unidadRepository.findById(apartamento.getId()).orElseThrow();
        Unidad apartaestudioDb = unidadRepository.findById(apartaestudio.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();


        comprobarEstanciaDb(
                estanciaDb,
                null,
                2,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                2);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                montoPago,
                BigDecimal.ZERO,
                montoPendiente,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.PENDIENTE,
                1,
                0,
                1,
                0,
                1);

        comprobarUnidadYHabitacionesDb(
                apartamentoDb,
                EstadoOperativo.OCUPADO,
                3);

        comprobarUnidadYHabitacionesDb(
                apartaestudioDb,
                EstadoOperativo.DISPONIBLE,
                0);

        comprobarHabitacionesDb(apartamentoDb.getHabitaciones(), estanciaDb, null);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);


    }

    @Test
    void exitoEditandoEstanciaConCambioApartaestudioAHabitacion_test() {

        // ---------- GIVEN ----------
        Unidad apartaestudio = crearApartaestudio(EstadoOperativo.OCUPADO);
        Unidad apartamento = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = apartamento.getHabitaciones().getFirst();

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(apartaestudio.getHabitaciones(), true, EstadoEstancia.ACTIVA);
        BigDecimal montoPago = estanciaExistente.getPagos().stream().map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal montoPendiente = calcularMontoPendienteCambioUnidad(estanciaExistente, apartaestudio.getTipo());

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2)
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Unidad apartamentoDb = unidadRepository.findById(apartamento.getId()).orElseThrow();
        Habitacion habitacionDb = apartamentoDb.getHabitaciones().stream()
                .filter(h -> h.getCodigo().equals(habitacion.getCodigo()))
                .findFirst().orElseThrow();
        Unidad apartaestudioDb = unidadRepository.findById(apartaestudio.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();


        comprobarEstanciaDb(
                estanciaDb,
                null,
                2,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                null,
                1,
                2);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                montoPago,
                BigDecimal.ZERO,
                montoPendiente,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.PENDIENTE,
                1,
                0,
                1,
                0,
                1);

        comprobarUnidadYHabitacionesDb(
                apartamentoDb,
                EstadoOperativo.PARCIALMENTE,
                1);

        comprobarUnidadYHabitacionesDb(
                apartaestudioDb,
                EstadoOperativo.DISPONIBLE,
                0);

        comprobarHabitacionesDb(List.of(habitacionDb), estanciaDb, null);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);


    }

    @Test
    void exitoEditandoEstanciaConCambioHabitacionAApartamentoMismaUnidad_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = apartamento.getHabitaciones().getFirst();

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(List.of(habitacion), true, EstadoEstancia.ACTIVA);
        BigDecimal montoPago = estanciaExistente.getPagos().stream().map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal montoPendiente = calcularMontoPendienteCambioUnidad(estanciaExistente, TipoUnidad.HABITACION);

        EstanciaRequestDTO request = estanciaRequestDTO(
                apartamento.getTipo(),
                apartamento.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2)
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Unidad apartamentoDb = unidadRepository.findById(apartamento.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();


        comprobarEstanciaDb(
                estanciaDb,
                null,
                2,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                2);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                montoPago,
                BigDecimal.ZERO,
                montoPendiente,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.PENDIENTE,
                1,
                0,
                1,
                0,
                1);

        comprobarUnidadYHabitacionesDb(
                apartamentoDb,
                EstadoOperativo.OCUPADO,
                3);

        comprobarHabitacionesDb(apartamentoDb.getHabitaciones(), estanciaDb, null);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);


    }

    @Test
    void exitoEditandoEstanciaConCambioHabitacionAApartamentoDiferenteUnidad_test() {

        // ---------- GIVEN ----------
        Unidad apartamento1 = crearApartamento(EstadoOperativo.PARCIALMENTE);
        Unidad apartamento2 = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = apartamento1.getHabitaciones().getFirst();

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());

        Estancia estanciaExistente = crearEstanciaExistente(List.of(habitacion), true, EstadoEstancia.ACTIVA);
        BigDecimal montoPago = estanciaExistente.getPagos().stream().map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal montoPendiente = calcularMontoPendienteCambioUnidad(estanciaExistente, TipoUnidad.HABITACION);


        EstanciaRequestDTO request = estanciaRequestDTO(
                apartamento2.getTipo(),
                apartamento2.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2)
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Unidad apartamentoDb1 = unidadRepository.findById(apartamento1.getId()).orElseThrow();
        Unidad apartamentoDb2 = unidadRepository.findById(apartamento2.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();


        comprobarEstanciaDb(
                estanciaDb,
                null,
                2,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                2);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                montoPago,
                BigDecimal.ZERO,
                montoPendiente,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.PENDIENTE,
                1,
                0,
                1,
                0,
                1);

        comprobarUnidadYHabitacionesDb(
                apartamentoDb1,
                EstadoOperativo.DISPONIBLE,
                0);

        comprobarUnidadYHabitacionesDb(
                apartamentoDb2,
                EstadoOperativo.OCUPADO,
                3);

        comprobarHabitacionesDb(apartamentoDb2.getHabitaciones(), estanciaDb, null);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);


    }

    @Test
    void exitoEditandoEstanciaConCambioHabitacionAApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad apartaestudio = crearApartaestudio(EstadoOperativo.DISPONIBLE);
        Unidad apartamento = crearApartamento(EstadoOperativo.PARCIALMENTE);
        Habitacion habitacion = apartamento.getHabitaciones().getFirst();

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(List.of(habitacion), true, EstadoEstancia.ACTIVA);
        BigDecimal montoPago = estanciaExistente.getPagos().stream().map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal montoPendiente = calcularMontoPendienteCambioUnidad(estanciaExistente, TipoUnidad.HABITACION);


        EstanciaRequestDTO request = estanciaRequestDTO(
                apartaestudio.getTipo(),
                apartaestudio.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2)
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Unidad apartamentoDb = unidadRepository.findById(apartamento.getId()).orElseThrow();
        Unidad apartaestudioDb = unidadRepository.findById(apartaestudio.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();


        comprobarEstanciaDb(
                estanciaDb,
                null,
                2,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                1,
                2);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                montoPago,
                BigDecimal.ZERO,
                montoPendiente,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.PENDIENTE,
                1,
                0,
                1,
                0,
                1);

        comprobarUnidadYHabitacionesDb(
                apartamentoDb,
                EstadoOperativo.DISPONIBLE,
                0);

        comprobarUnidadYHabitacionesDb(
                apartaestudioDb,
                EstadoOperativo.OCUPADO,
                1);

        comprobarHabitacionesDb(apartaestudioDb.getHabitaciones(), estanciaDb, null);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);


    }

    @Test
    void exitoEditandoEstanciaConCambioUnidadSinCambioFecha_test() {

        // ---------- GIVEN ----------
        Unidad unidad1 = crearApartamento(EstadoOperativo.OCUPADO);
        Unidad unidad2 = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(unidad1.getHabitaciones(), true, EstadoEstancia.ACTIVA);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad2.getTipo(),
                unidad2.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                null
        );

        request.setEntradaReal(estanciaExistente.getEntradaReal());
        request.setSalidaEstimada(estanciaExistente.getSalidaEstimada());

        // ---------- WHEN ----------
        LocalDateTime entradaOriginal = estanciaExistente.getEntradaReal();
        LocalDateTime salidaOriginal = estanciaExistente.getSalidaEstimada();

        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Unidad unidadDb1 = unidadRepository.findById(unidad1.getId()).orElseThrow();
        Unidad unidadDb2 = unidadRepository.findById(unidad2.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();

        assertThat(estanciaDb.getEntradaReal()).isEqualToIgnoringNanos(entradaOriginal);
        assertThat(estanciaDb.getSalidaEstimada()).isEqualToIgnoringNanos(salidaOriginal);

        comprobarEstanciaDb(
                estanciaDb,
                null,
                2,
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
                unidadDb1,
                EstadoOperativo.DISPONIBLE,
                0);

        comprobarUnidadYHabitacionesDb(
                unidadDb2,
                EstadoOperativo.OCUPADO,
                3);

        comprobarHabitacionesDb(unidadDb2.getHabitaciones(), estanciaDb, null);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 3);


    }

    @Test
    void exitoEditandoEstanciaAEstadoExcedido_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(unidad.getHabitaciones(), true, EstadoEstancia.ACTIVA);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-5)
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                2,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.EXCEDIDA,
                null,
                3,
                1);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                3);

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 4);


    }

    @Test
    void exitoEditandoEstanciaConEstadoExcedido_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(unidad.getHabitaciones(), true, EstadoEstancia.ACTIVA);
        estanciaExistente.setEntradaReal(LocalDateTime.now().plusDays(-5));
        estanciaExistente.setEstado(EstadoEstancia.EXCEDIDA);
        estanciaRepository.save(estanciaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2)
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                2,
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

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 4);


    }

    @Test
    void exitoEditandoEstanciaSinAcompanantes_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());


        Estancia estanciaExistente = crearEstanciaExistente(unidad.getHabitaciones(), true, EstadoEstancia.ACTIVA);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                null,
                LocalDateTime.now().plusDays(-2)
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();

        comprobarEstanciaDb(
                estanciaDb,
                null,
                1,
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

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, null);

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 4);


    }

    @Test
    void exitoEditandoEstanciaSinCambios_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estanciaExistente = crearEstanciaExistente(unidad.getHabitaciones(), true, EstadoEstancia.ACTIVA);
        Ocupante cliente = estanciaExistente.getOcupantes().stream()
                .filter(o -> o.getTipoOcupante().equals(TipoOcupante.CLIENTE))
                .findFirst().orElseThrow();
        List<Ocupante> ocupantes = estanciaExistente.getOcupantes().stream()
                .filter(o -> o.getTipoOcupante().equals(TipoOcupante.ACOMPANANTE))
                .toList();

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                cliente,
                ocupantes,
                null
        );

        request.setEntradaReal(estanciaExistente.getEntradaReal());
        request.setSalidaEstimada(estanciaExistente.getSalidaEstimada());

        // ---------- WHEN ----------
        Long eventosAntes = eventoRepository.count();

        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
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

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), cliente, ocupantes);

        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);




    }

    @Test
    void falloEditandoEstanciaPorReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());

        crearReservaExistente(unidad.getHabitaciones(), true, EstadoReserva.CONFIRMADA);

        Estancia estanciaExistente = crearEstanciaExistente(unidad.getHabitaciones(), true, EstadoEstancia.ACTIVA);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                null
        );

        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();
        long eventosAntes = eventoRepository.count();
        LocalDateTime entradaAntes = estanciaExistente.getEntradaReal();
        LocalDateTime salidaAntes = estanciaExistente.getSalidaEstimada();
        String notasAntes = estanciaExistente.getNotas();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> estanciaService.editarEstancia(request, estanciaExistente.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede editar la estancia:")
                .hasMessageContaining("existe una reserva para las habitaciones con codigo");

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservaAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                entradaAntes,
                salidaAntes,
                null,
                notasAntes,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                1);

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                3);



    }

    @Test
    void falloEditandoEstanciaPorEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad1 = crearApartamento(EstadoOperativo.OCUPADO);
        Unidad unidad2 = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(unidad1.getHabitaciones(), true, EstadoEstancia.ACTIVA);
        crearEstanciaExistente(unidad2.getHabitaciones(), true, EstadoEstancia.ACTIVA);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad2.getTipo(),
                unidad2.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2)
        );

        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();
        long eventosAntes = eventoRepository.count();
        LocalDateTime entradaAntes = estanciaExistente.getEntradaReal();
        LocalDateTime salidaAntes = estanciaExistente.getSalidaEstimada();
        String notasAntes = estanciaExistente.getNotas();

        // ---------- WHEN + THEN (excepción) ----------
        assertThatThrownBy(() -> estanciaService.editarEstancia(request, estanciaExistente.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede editar la estancia:")
                .hasMessageContaining("existe una estancia para las habitaciones con codigo");


        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Unidad unidadDb1 = unidadRepository.findById(unidad1.getId()).orElseThrow();
        Unidad unidadDb2 = unidadRepository.findById(unidad2.getId()).orElseThrow();

        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservaAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                entradaAntes,
                salidaAntes,
                null,
                notasAntes,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                1);

        comprobarHabitacionesDb(unidadDb1.getHabitaciones(), estanciaDb, null);

        comprobarUnidadYHabitacionesDb(
                unidadDb1,
                EstadoOperativo.OCUPADO,
                3);

        comprobarUnidadYHabitacionesDb(
                unidadDb2,
                EstadoOperativo.OCUPADO,
                3);


    }

    @Test
    void falloEditandoEstanciaPorEstadoDiferenteAExcedidaOActiva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(unidad.getHabitaciones(), true, EstadoEstancia.ACTIVA);
        estanciaExistente.setEntradaReal(LocalDateTime.now().plusDays(-5));
        estanciaExistente.setEstado(EstadoEstancia.FINALIZADA);
        estanciaRepository.save(estanciaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2)
        );

        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();
        long eventosAntes = eventoRepository.count();
        LocalDateTime entradaAntes = estanciaExistente.getEntradaReal();
        LocalDateTime salidaAntes = estanciaExistente.getSalidaEstimada();
        String notasAntes = estanciaExistente.getNotas();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> estanciaService.editarEstancia(request, estanciaExistente.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo se pueden editar estancias en estado ACTIVA o EXCEDIDA. Estado actual");

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservaAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                entradaAntes,
                salidaAntes,
                null,
                notasAntes,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.FINALIZADA,
                null,
                3,
                1);

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                3);


    }

    @Test
    void falloEditandoEstanciaConFechaSalidaAnteriorAEntrada_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);


        Estancia estanciaExistente = crearEstanciaExistente(unidad.getHabitaciones(), true, EstadoEstancia.ACTIVA);


        EstanciaRequestDTO request = errorFechasEstanciaRequestDTO(unidad);

        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();
        long eventosAntes = eventoRepository.count();
        LocalDateTime entradaAntes = estanciaExistente.getEntradaReal();
        LocalDateTime salidaAntes = estanciaExistente.getSalidaEstimada();
        String notasAntes = estanciaExistente.getNotas();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> estanciaService.editarEstancia(request, estanciaExistente.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de salida debe ser posterior a la fecha de entrada");

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservaAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                entradaAntes,
                salidaAntes,
                null,
                notasAntes,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                1);

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                3);


    }

    @Test
    void falloEditandoEstanciaConFechaEntradaSuperiorAAhora_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());

        Estancia estanciaExistente = crearEstanciaExistente(unidad.getHabitaciones(), true, EstadoEstancia.ACTIVA);


        EstanciaRequestDTO request = estanciaRequestDTO(unidad.getTipo(), unidad.getCodigo(), clienteNuevo, ocupantesNuevos, LocalDateTime.now().plusDays(1));

        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();
        long eventosAntes = eventoRepository.count();
        LocalDateTime entradaAntes = estanciaExistente.getEntradaReal();
        LocalDateTime salidaAntes = estanciaExistente.getSalidaEstimada();
        String notasAntes = estanciaExistente.getNotas();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> estanciaService.editarEstancia(request, estanciaExistente.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de entrada no puede ser superior a la fecha actual, crear reserva si se desea una estancia futura");

        // ---------- THEN ----------

        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservaAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                entradaAntes,
                salidaAntes,
                null,
                notasAntes,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                1);

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                3);


    }

    /**
     * activarEstancia(ActivarEstanciaDTO request)
     */

    @Test
    void exitoActivandoEstanciaApartamento_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);

        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantes,
                LocalDateTime.now().minusDays(1)
        );

        // ---------- WHEN ----------
        long estanciasAntes = estanciaRepository.count();
        Estancia estancia = estanciaService.activarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();

        assertThat(estanciasAntes).isEqualTo(estanciaRepository.count());
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.COMPLETADA);

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                0
        );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 3);

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarEventoDb(eventoDb, TipoEvento.ACTIVACION_ESTANCIA, estanciaDb.getCodigoFolio(), reservaDb.getCodigo(), 4);

    }

    @Test
    void exitoActivandoEstanciaApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = ocupanteRepository.save(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);

        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantes,
                LocalDateTime.now().minusDays(1)
        );

        // ---------- WHEN ----------
        long estanciasAntes = estanciaRepository.count();
        Estancia estancia = estanciaService.activarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();

        assertThat(estanciasAntes).isEqualTo(estanciaRepository.count());
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.COMPLETADA);

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                1,
                0
        );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 1);

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarEventoDb(eventoDb, TipoEvento.ACTIVACION_ESTANCIA, estanciaDb.getCodigoFolio(), reservaDb.getCodigo(), 4);
    }

    @Test
    void exitoActivandoEstanciaHabitacion_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();


        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());

        Reserva reserva = crearReservaExistente(List.of(habitacion), false, EstadoReserva.CONFIRMADA);

        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantes,
                LocalDateTime.now().minusDays(1)
        );

        // ---------- WHEN ----------
        long estanciasAntes = estanciaRepository.count();
        Estancia estancia = estanciaService.activarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Habitacion habitacionDb = unidadDb.getHabitaciones().stream()
                .filter(h -> h.getCodigo().equals(habitacion.getCodigo()))
                .findFirst().orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();

        assertThat(estanciasAntes).isEqualTo(estanciaRepository.count());
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.COMPLETADA);

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                null,
                1,
                0
        );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.PARCIALMENTE, 1);

        comprobarHabitacionesDb(List.of(habitacionDb), estanciaDb, null);

        comprobarEventoDb(eventoDb, TipoEvento.ACTIVACION_ESTANCIA, estanciaDb.getCodigoFolio(), reservaDb.getCodigo(), 4);

    }

    @Test
    void exitoActivandoEstanciaConPagoReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());


        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), true, EstadoReserva.CONFIRMADA);

        BigDecimal monto = reserva.getEstancia().getPagos().stream()
                .filter(pago -> pago.getTipoPago() == TipoPago.ANTICIPO_RESERVA)
                .map(Pago::getMonto)
                .findFirst().orElseThrow();

        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantes,
                LocalDateTime.now().minusDays(1)
        );

        // ---------- WHEN ----------
        long estanciasAntes = estanciaRepository.count();
        Estancia estancia = estanciaService.activarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();


        assertThat(estanciasAntes).isEqualTo(estanciaRepository.count());
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.COMPLETADA);

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                1
        );



        comprobarPagosDb(
                estanciaDb.getPagos(),
                monto,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.ELIMINADO,
                0,
                0,
                0,
                1,
                0);

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 3);

        comprobarEventoDb(eventoDb, TipoEvento.ACTIVACION_ESTANCIA, estanciaDb.getCodigoFolio(), reservaDb.getCodigo(), 4);

    }


    @Test
    void exitoActivandoEstanciaConEstadoExcedido_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);

        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantes,
                LocalDateTime.now().plusDays(-5)
        );

        // ---------- WHEN ----------
        long estanciasAntes = estanciaRepository.count();
        Estancia estancia = estanciaService.activarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();

        assertThat(estanciasAntes).isEqualTo(estanciaRepository.count());
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.COMPLETADA);

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                3,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.EXCEDIDA,
                null,
                3,
                0
        );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 3);

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarEventoDb(eventoDb, TipoEvento.ACTIVACION_ESTANCIA, estanciaDb.getCodigoFolio(), reservaDb.getCodigo(), 4);

    }

    @Test
    void exitoActivandoEstanciaSinAcompanates_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = crearCliente(clienteData());

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);

        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                null,
                LocalDateTime.now().minusDays(1)
        );

        // ---------- WHEN ----------
        long estanciasAntes = estanciaRepository.count();
        Estancia estancia = estanciaService.activarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();

        assertThat(estanciasAntes).isEqualTo(estanciaRepository.count());
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.COMPLETADA);

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                1,
                request.getEntradaReal(),
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                0
        );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 3);

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarEventoDb(eventoDb, TipoEvento.ACTIVACION_ESTANCIA, estanciaDb.getCodigoFolio(), reservaDb.getCodigo(), 4);

    }

    @Test
    void falloActivandoEstanciaPorEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());

        Estancia estanciaExistente = crearEstanciaExistente(unidad.getHabitaciones(), true, EstadoEstancia.ACTIVA);

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);

        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantes,
                LocalDateTime.now().minusDays(1)
        );

        // ---------- WHEN ----------
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();
        long eventosAntes = eventoRepository.count();

        assertThatThrownBy(() -> estanciaService.activarEstancia(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede activar la estancia:")
                .hasMessageContaining("existe una estancia para las habitaciones con codigo");


        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estanciaExistente.getId()).orElseThrow();
        Estancia estanciaDbReserva = estanciaRepository.findByReserva_Id(reserva.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();

        assertThat(estanciasAntes).isEqualTo(estanciaRepository.count());
        assertThat(reservaAntes).isEqualTo(reservaRepository.count());
        assertThat(eventosAntes).isEqualTo(eventoRepository.count());
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);

        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                estanciaExistente.getEntradaReal(),
                estanciaExistente.getSalidaEstimada(),
                null,
                estanciaExistente.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                1
        );

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarEstanciaDb(
                estanciaDbReserva,
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
                0
        );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 3);

    }

    @Test
    void falloActivandoEstanciaPorReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        Reserva reservaExistente = crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);


        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantes,
                LocalDateTime.now().minusDays(1)
        );

        // ---------- WHEN ----------
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();
        long eventosAntes = eventoRepository.count();

        assertThatThrownBy(() -> estanciaService.activarEstancia(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede activar la estancia:")
                .hasMessageContaining("existe una reserva para las habitaciones con codigo");


        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();
        Estancia estanciaDbReserva = estanciaRepository.findByReserva_Id(reserva.getId()).orElseThrow();
        Reserva reservaExistenteDb = reservaRepository.findById(reservaExistente.getId()).orElseThrow();
        Estancia estanciaDbReservaExistente = estanciaRepository.findByReserva_Id(reservaExistente.getId()).orElseThrow();

        assertThat(estanciasAntes).isEqualTo(estanciaRepository.count());
        assertThat(reservaAntes).isEqualTo(reservaRepository.count());
        assertThat(eventosAntes).isEqualTo(eventoRepository.count());

        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(reservaExistenteDb.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);

        comprobarEstanciaDb(
                estanciaDbReservaExistente,
                reservaExistenteDb,
                0,
                null,
                null,
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                null,
                3,
                0
        );

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDbReservaExistente, null);

        comprobarEstanciaDb(
                estanciaDbReserva,
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
                0
        );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);

    }

    @Test
    void falloActivandoEstanciaPorEstadoEstanciaDiferenteAReservada_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        Estancia estancia = estanciaRepository.findByReserva_Id(reserva.getId()).orElseThrow();
        estancia.setEstado(EstadoEstancia.ACTIVA);
        estanciaRepository.save(estancia);


        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantes,
                LocalDateTime.now().minusDays(1)
        );

        // ---------- WHEN ----------

        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();
        long eventosAntes = eventoRepository.count();

        assertThatThrownBy(() -> estanciaService.activarEstancia(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La estancia debe estar en estado RESERVADA para activarla. Estado actual:");

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();


        assertThat(estanciasAntes).isEqualTo(estanciaRepository.count());
        assertThat(reservaAntes).isEqualTo(reservaRepository.count());
        assertThat(eventosAntes).isEqualTo(eventoRepository.count());
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);


        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                0,
                null,
                null,
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                0
        );



    }

    @Test
    void falloActivandoEstanciaPorEstadoReservaDiferenteAReservada_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.COMPLETADA);



        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantes,
                LocalDateTime.now().minusDays(1)
        );

        // ---------- WHEN ----------

        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();
        long eventosAntes = eventoRepository.count();

        assertThatThrownBy(() -> estanciaService.activarEstancia(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La reserva debe estar CONFIRMADA para activar la estancia. Estado actual:");

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reserva.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();


        assertThat(estanciasAntes).isEqualTo(estanciaRepository.count());
        assertThat(reservaAntes).isEqualTo(reservaRepository.count());
        assertThat(eventosAntes).isEqualTo(eventoRepository.count());
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.COMPLETADA);

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
                0
        );



    }

    @Test
    void falloActivandoEstanciaConFechaSalidaAnteriorAEntrada_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);

        ActivarEstanciaDTO request =errorFechasActivarEstanciaRequestDTO(reserva.getId());

        // ---------- WHEN ----------
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();
        long eventosAntes = eventoRepository.count();

        assertThatThrownBy(() -> estanciaService.activarEstancia(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de salida debe ser posterior a la fecha de entrada");


        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDbReserva = estanciaRepository.findByReserva_Id(reserva.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();

        assertThat(estanciasAntes).isEqualTo(estanciaRepository.count());
        assertThat(reservaAntes).isEqualTo(reservaRepository.count());
        assertThat(eventosAntes).isEqualTo(eventoRepository.count());
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);

        comprobarEstanciaDb(
                estanciaDbReserva,
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
                0
        );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);

    }

    @Test
    void falloActivandoEstanciaConFechaEntradaSuperiorAAhora_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);

        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantes,
                LocalDateTime.now().plusDays(1)
        );
        // ---------- WHEN ----------
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();
        long eventosAntes = eventoRepository.count();

        assertThatThrownBy(() -> estanciaService.activarEstancia(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de entrada no puede ser superior a la fecha actual, crear reserva si se desea una estancia futura");


        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDbReserva = estanciaRepository.findByReserva_Id(reserva.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();

        assertThat(estanciasAntes).isEqualTo(estanciaRepository.count());
        assertThat(reservaAntes).isEqualTo(reservaRepository.count());
        assertThat(eventosAntes).isEqualTo(eventoRepository.count());
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);

        comprobarEstanciaDb(
                estanciaDbReserva,
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
                0
        );

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDbReserva, null);

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);

    }

    @Test
    void falloActivandoEstanciaSinReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());


        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                null,
                cliente,
                acompanantes,
                LocalDateTime.now().minusDays(1)
        );
        // ---------- WHEN ----------
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();
        long eventosAntes = eventoRepository.count();

        assertThatThrownBy(() -> estanciaService.activarEstancia(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El id de reserva no puede ser nulo para activar una estancia");


        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        assertThat(estanciasAntes).isEqualTo(estanciaRepository.count());
        assertThat(reservaAntes).isEqualTo(reservaRepository.count());
        assertThat(eventosAntes).isEqualTo(eventoRepository.count());


        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);

    }

    /**
     * eliminarEstancia(Long idEstancia)
     */

    @Test
    void exitoEliminandoEstanciaApartamento_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        // ---------- WHEN ----------
        Void result = estanciaService.eliminarEstancia(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();

        assertThat(result).isNull();
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.CANCELADA);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
        comprobarEventoDb(eventoDb, TipoEvento.ELIMINACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 1);

    }

    @Test
    void exitoEliminandoEstanciaApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);

        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        // ---------- WHEN ----------
        Void result = estanciaService.eliminarEstancia(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();


        assertThat(result).isNull();
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.CANCELADA);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
        comprobarEventoDb(eventoDb, TipoEvento.ELIMINACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 1);
    }

    @Test
    void exitoEliminandoEstanciaHabitacion_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Estancia estancia = crearEstanciaExistente(listaHabitacion, false, EstadoEstancia.ACTIVA);


        // ---------- WHEN ----------
        Void result = estanciaService.eliminarEstancia(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();


        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Habitacion habitacionDb = unidadDb.getHabitaciones().stream()
                .filter(h -> h.getCodigo().equals(habitacion.getCodigo()))
                .findFirst().orElseThrow();        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();

        assertThat(result).isNull();
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.CANCELADA);
        comprobarHabitacionesDb(List.of(habitacionDb), estanciaDb, null);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.PARCIALMENTE, 2);
        comprobarEventoDb(eventoDb, TipoEvento.ELIMINACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 1);
    }

    @Test
    void exitoEliminandoEstanciaApartamentoConPagoEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), true, EstadoEstancia.ACTIVA);

        BigDecimal monto = estancia.getPagos().stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // ---------- WHEN ----------
        Void result = estanciaService.eliminarEstancia(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();


        assertThat(result).isNull();
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.CANCELADA);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);
        assertThat(estanciaDb.getPagos()).hasSize(estancia.getPagos().size());
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
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
                1,
                0,
                0);

        comprobarEventoDb(eventoDb, TipoEvento.ELIMINACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 1);
    }

    @Test
    void exitoEliminandoEstanciaApartamentoConPagoReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estancia = crearEstanciaConReservaExistente(unidad.getHabitaciones(), false, true);
        BigDecimal monto = estancia.getPagos().stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ---------- WHEN ----------
        Void result = estanciaService.eliminarEstancia(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();


        assertThat(result).isNull();
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.CANCELADA);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);
        assertThat(estanciaDb.getPagos()).hasSize(estancia.getPagos().size());
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);

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

        comprobarEventoDb(eventoDb, TipoEvento.ELIMINACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 1);
    }

    @Test
    void exitoEliminandoEstanciaApartamentoConPagoReservaYEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estancia = crearEstanciaConReservaExistente(unidad.getHabitaciones(), true, true);
        BigDecimal monto = estancia.getPagos().stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // ---------- WHEN ----------
        Void result = estanciaService.eliminarEstancia(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();


        assertThat(result).isNull();
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.CANCELADA);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);
        assertThat(estanciaDb.getPagos()).hasSize(estancia.getPagos().size());
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                BigDecimal.ZERO,
                monto,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                2,
                0,
                1,
                1,
                0);

        comprobarEventoDb(eventoDb, TipoEvento.ELIMINACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 1);
    }

    @Test
    void falloEliminandoEstanciaConEstadoFinalizada_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        estancia.setEstado(EstadoEstancia.FINALIZADA);
        estanciaRepository.save(estancia);

        // ---------- WHEN ----------
        long estanciasAntes = estanciaRepository.count();
        long eventosAntes = eventoRepository.count();


        assertThatThrownBy(() -> estanciaService.eliminarEstancia(estancia.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo se pueden eliminar estancias en estado ACTIVA o EXCEDIDA. Estado actual");

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        assertThat(estanciasAntes).isEqualTo(estanciaRepository.count());
        assertThat(eventosAntes).isEqualTo(eventoRepository.count());

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);


        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.FINALIZADA);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 3);

    }

    /** obtenerEstanciaPorId(Long idEstancia) */
    @Test
    void exitoObteniendoEstanciaApartamento_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), true, EstadoEstancia.ACTIVA);
        BigDecimal monto = estancia.getPagos().stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);



        entityManager.flush();
        entityManager.clear();

        // ---------- WHEN ----------
        EstanciaDTO result = estanciaService.obtenerEstancia(unidad.getCodigo(), unidad.getTipo());

        // ---------- THEN ----------
        assertThat(result).isNotNull();
        Estancia estanciaDb = estanciaRepository.findById(result.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

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
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                monto,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.ELIMINADO,
                0,
                0,
                1,
                0,
                0);
    }

    @Test
    void exitoObteniendoEstanciaApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), true, EstadoEstancia.ACTIVA);
        BigDecimal monto = estancia.getPagos().stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        entityManager.flush();
        entityManager.clear();

        // ---------- WHEN ----------
        EstanciaDTO result = estanciaService.obtenerEstancia(unidad.getCodigo(), TipoUnidad.APARTAESTUDIO);
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
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
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                monto,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.PENDIENTE,
                0,
                0,
                1,
                0,
                0);
    }

    @Test
    void exitoObteniendoEstanciaHabitacion_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();
        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Estancia estancia = crearEstanciaExistente(listaHabitacion, true, EstadoEstancia.ACTIVA);
        BigDecimal monto = estancia.getPagos().stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        entityManager.flush();
        entityManager.clear();

        // ---------- WHEN ----------
        EstanciaDTO result = estanciaService.obtenerEstancia(habitacion.getCodigo(), TipoUnidad.HABITACION);

        // ---------- THEN ----------
        assertThat(result).isNotNull();
        Estancia estanciaDb = estanciaRepository.findById(result.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Habitacion habitacionDb = unidadDb.getHabitaciones().stream()
                .filter(h -> h.getCodigo().equals(habitacion.getCodigo()))
                .findFirst().orElseThrow();

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

        comprobarHabitacionesDb(List.of(habitacionDb), estanciaDb, null);


        comprobarPagosDb(
                estanciaDb.getPagos(),
                monto,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.PENDIENTE,
                0,
                0,
                1,
                0,
                0);
    }

    @Test
    void falloObteniendoEstanciaSinEstanciaActivaOExcedida_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        entityManager.flush();
        entityManager.clear();

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> estanciaService.obtenerEstancia(unidad.getCodigo(), unidad.getTipo()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No se encontró una estancia activa o excedida para la unidad con codigo");

        // ---------- THEN ----------
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
    }

    /**
     * finalizarEstancia(SalidaEstanciaDTO request)
     */

    @Test
    void exitoFinalizandoEstanciaApartamento_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
        pagoRequest.setEstado(EstadoPago.COMPLETADO);
        SalidaEstanciaDTO request = salidaEstanciaRequestDTO(estancia.getId(), LocalDateTime.now(), pagoRequest);

        // ---------- WHEN ----------
        Void result = estanciaService.finalizarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();


        assertThat(result).isNull();
        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                estancia.getEntradaReal(),
                estancia.getSalidaEstimada(),
                request.getFechaSalidaReal(),
                request.getNotasSalida(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.FINALIZADA,
                pagoRequest.getMonto(),
                3,
                1
        );

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                pagoRequest.getMonto(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.PENDIENTE,
                0,
                1,
                0,
                0,
                0);
        comprobarEventoDb(eventoDb, TipoEvento.FINALIZACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);

    }

    @Test
    void exitoFinalizandoEstanciaApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);

        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
        pagoRequest.setEstado(EstadoPago.COMPLETADO);
        SalidaEstanciaDTO request = salidaEstanciaRequestDTO(estancia.getId(), LocalDateTime.now(), pagoRequest);

        // ---------- WHEN ----------
        Void result = estanciaService.finalizarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();



        assertThat(result).isNull();
        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                estancia.getEntradaReal(),
                estancia.getSalidaEstimada(),
                request.getFechaSalidaReal(),
                request.getNotasSalida(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.FINALIZADA,
                pagoRequest.getMonto(),
                1,
                1
        );
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                pagoRequest.getMonto(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.PENDIENTE,
                0,
                1,
                0,
                0,
                0);
        comprobarEventoDb(eventoDb, TipoEvento.FINALIZACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);
    }

    @Test
    void exitoFinalizandoEstanciaHabitacion_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();
        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Estancia estancia = crearEstanciaExistente(listaHabitacion, false, EstadoEstancia.ACTIVA);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
        pagoRequest.setEstado(EstadoPago.COMPLETADO);
        SalidaEstanciaDTO request = salidaEstanciaRequestDTO(estancia.getId(), LocalDateTime.now(), pagoRequest);

        // ---------- WHEN ----------
        Void result = estanciaService.finalizarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        Habitacion habitacionDb = unidadDb.getHabitaciones().stream()
                .filter(h -> h.getCodigo().equals(habitacion.getCodigo()))
                .findFirst().orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();



        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                estancia.getEntradaReal(),
                estancia.getSalidaEstimada(),
                request.getFechaSalidaReal(),
                request.getNotasSalida(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.FINALIZADA,
                pagoRequest.getMonto(),
                1,
                1
        );
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.PARCIALMENTE, 2);
        comprobarHabitacionesDb(List.of(habitacionDb), estanciaDb, null);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                pagoRequest.getMonto(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.PENDIENTE,
                0,
                1,
                0,
                0,
                0);
        comprobarEventoDb(eventoDb, TipoEvento.FINALIZACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);
    }

    @Test
    void exitoFinalizandoEstanciaConPagoEstanciaAnticipado_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), true, EstadoEstancia.ACTIVA);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
        pagoRequest.setEstado(EstadoPago.COMPLETADO);
        SalidaEstanciaDTO request = salidaEstanciaRequestDTO(estancia.getId(), LocalDateTime.now(), pagoRequest);

        BigDecimal monto = estancia.getPagos().stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(pagoRequest.getMonto());

        // ---------- WHEN ----------
        Void result = estanciaService.finalizarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();


        assertThat(result).isNull();
        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                estancia.getEntradaReal(),
                estancia.getSalidaEstimada(),
                request.getFechaSalidaReal(),
                request.getNotasSalida(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.FINALIZADA,
                monto,
                3,
                2
        );
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                monto,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                2,
                EstadoPago.PENDIENTE,
                0,
                1,
                1,
                0,
                0);
        comprobarEventoDb(eventoDb, TipoEvento.FINALIZACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);
    }

    @Test
    void exitoFinalizandoEstanciaConPagoReservaAnticipado_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estancia = crearEstanciaConReservaExistente(unidad.getHabitaciones(), false, true);
        Reserva reserva = estancia.getReserva();

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
        pagoRequest.setEstado(EstadoPago.COMPLETADO);
        SalidaEstanciaDTO request = salidaEstanciaRequestDTO(estancia.getId(), LocalDateTime.now(), pagoRequest);

        BigDecimal monto = estancia.getPagos().stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(pagoRequest.getMonto());

        // ---------- WHEN ----------
        Void result = estanciaService.finalizarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();


        assertThat(result).isNull();
        comprobarEstanciaDb(
                estanciaDb,
                reserva,
                1,
                estancia.getEntradaReal(),
                estancia.getSalidaEstimada(),
                request.getFechaSalidaReal(),
                request.getNotasSalida(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.FINALIZADA,
                monto,
                3,
                2
        );
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                monto,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                2,
                EstadoPago.PENDIENTE,
                0,
                1,
                0,
                1,
                0);
        comprobarEventoDb(eventoDb, TipoEvento.FINALIZACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);
    }

    @Test
    void exitoFinalizandoEstanciaConPagoEstanciaYReservaAnticipado_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estancia = crearEstanciaConReservaExistente(unidad.getHabitaciones(), true, true);
        Reserva reserva = estancia.getReserva();

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
        pagoRequest .setEstado(EstadoPago.COMPLETADO);
        SalidaEstanciaDTO request = salidaEstanciaRequestDTO(estancia.getId(), LocalDateTime.now(), pagoRequest);

        BigDecimal monto = estancia.getPagos().stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(pagoRequest.getMonto());

        // ---------- WHEN ----------
        Void result = estanciaService.finalizarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();


        assertThat(result).isNull();
        comprobarEstanciaDb(
                estanciaDb,
                reserva,
                1,
                estancia.getEntradaReal(),
                estancia.getSalidaEstimada(),
                request.getFechaSalidaReal(),
                request.getNotasSalida(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.FINALIZADA,
                monto,
                3,
                3
        );
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                monto,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                3,
                EstadoPago.PENDIENTE,
                0,
                1,
                1,
                1,
                0);
        comprobarEventoDb(eventoDb, TipoEvento.FINALIZACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);
    }

    @Test
    void exitoEditandoEstanciaConCambioUnidadYFechasLuegoFinalizandoAcumulaPagos_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.OCUPADO);
        Unidad apartaestudio = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());

        Estancia estancia = crearEstanciaExistente(apartamento.getHabitaciones(), true, EstadoEstancia.ACTIVA);

        BigDecimal montoAnticipoInicial = estancia.getPagos().stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal montoCambioUnidad = calcularMontoPendienteCambioUnidad(estancia, apartamento.getTipo());

        EstanciaRequestDTO editarRequest = estanciaRequestDTO(
                apartaestudio.getTipo(),
                apartaestudio.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2)
        );

        PagoNuevoRequestDTO pagoFinalRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
        pagoFinalRequest.setMonto(BigDecimal.valueOf(620000));
        pagoFinalRequest.setEstado(EstadoPago.COMPLETADO);

        BigDecimal totalEsperado = montoAnticipoInicial
                .add(montoCambioUnidad)
                .add(pagoFinalRequest.getMonto());

        // ---------- WHEN ----------
        estanciaService.editarEstancia(editarRequest, estancia.getId());
        SalidaEstanciaDTO salidaRequest = salidaEstanciaRequestDTO(estancia.getId(), LocalDateTime.now(), pagoFinalRequest);
        Void result = estanciaService.finalizarEstancia(salidaRequest);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        assertThat(result).isNull();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad apartamentoDb = unidadRepository.findById(apartamento.getId()).orElseThrow();
        Unidad apartaestudioDb = unidadRepository.findById(apartaestudio.getId()).orElseThrow();

        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.FINALIZADA);
        assertThat(estanciaDb.getEntradaReal()).isEqualToIgnoringNanos(editarRequest.getEntradaReal());
        assertThat(estanciaDb.getSalidaEstimada()).isEqualToIgnoringNanos(editarRequest.getSalidaEstimada());
        assertThat(estanciaDb.getSalidaReal()).isEqualToIgnoringNanos(salidaRequest.getFechaSalidaReal());
        assertThat(estanciaDb.getPrecioTotal()).isEqualByComparingTo(totalEsperado);

        assertThat(estanciaDb.getHabitaciones()).hasSize(1);
        assertThat(estanciaDb.getHabitaciones().getFirst().getUnidad().getId()).isEqualTo(apartaestudio.getId());
        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        assertThat(estanciaDb.getPagos()).hasSize(3);

        Pago anticipoDb = estanciaDb.getPagos().stream()
                .filter(p -> p.getTipoPago() == TipoPago.ANTICIPO_ESTANCIA)
                .findFirst()
                .orElseThrow();
        assertThat(anticipoDb.getEstado()).isEqualTo(EstadoPago.COMPLETADO);
        assertThat(anticipoDb.getMonto()).isEqualByComparingTo(montoAnticipoInicial);

        Pago cambioUnidadDb = estanciaDb.getPagos().stream()
                .filter(p -> p.getTipoPago() == TipoPago.CAMBIO_UNIDAD)
                .findFirst()
                .orElseThrow();
        assertThat(cambioUnidadDb.getEstado()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(cambioUnidadDb.getMonto()).isEqualByComparingTo(montoCambioUnidad);

        Pago pagoFinalDb = estanciaDb.getPagos().stream()
                .filter(p -> p.getTipoPago() == TipoPago.ESTANCIA_COMPLETADA)
                .findFirst()
                .orElseThrow();
        assertThat(pagoFinalDb.getEstado()).isEqualTo(EstadoPago.COMPLETADO);
        assertThat(pagoFinalDb.getMonto()).isEqualByComparingTo(pagoFinalRequest.getMonto());

        comprobarUnidadYHabitacionesDb(apartamentoDb, EstadoOperativo.DISPONIBLE, 0);
        comprobarUnidadYHabitacionesDb(apartaestudioDb, EstadoOperativo.DISPONIBLE, 0);
    }

    @Test
    void exitoEditandoEstanciaConCambioUnidadSinCambioFechasLuegoFinalizandoAcumulaPagos_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.OCUPADO);
        Unidad apartaestudio = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());

        Estancia estancia = crearEstanciaExistente(apartamento.getHabitaciones(), true, EstadoEstancia.ACTIVA);

        BigDecimal montoAnticipoInicial = estancia.getPagos().stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal montoCambioUnidad = calcularMontoPendienteCambioUnidad(estancia, apartamento.getTipo());

        EstanciaRequestDTO editarRequest = estanciaRequestDTO(
                apartaestudio.getTipo(),
                apartaestudio.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                estancia.getEntradaReal()
        );
        editarRequest.setSalidaEstimada(estancia.getSalidaEstimada());

        PagoNuevoRequestDTO pagoFinalRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
        pagoFinalRequest.setMonto(BigDecimal.valueOf(480000));
        pagoFinalRequest.setEstado(EstadoPago.COMPLETADO);

        BigDecimal totalEsperado = montoAnticipoInicial
                .add(montoCambioUnidad)
                .add(pagoFinalRequest.getMonto());

        // ---------- WHEN ----------
        estanciaService.editarEstancia(editarRequest, estancia.getId());
        SalidaEstanciaDTO salidaRequest = salidaEstanciaRequestDTO(estancia.getId(), LocalDateTime.now(), pagoFinalRequest);
        Void result = estanciaService.finalizarEstancia(salidaRequest);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        assertThat(result).isNull();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad apartamentoDb = unidadRepository.findById(apartamento.getId()).orElseThrow();
        Unidad apartaestudioDb = unidadRepository.findById(apartaestudio.getId()).orElseThrow();

        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.FINALIZADA);
        assertThat(estanciaDb.getEntradaReal()).isEqualToIgnoringNanos(editarRequest.getEntradaReal());
        assertThat(estanciaDb.getSalidaEstimada()).isEqualToIgnoringNanos(editarRequest.getSalidaEstimada());
        assertThat(estanciaDb.getSalidaReal()).isEqualToIgnoringNanos(salidaRequest.getFechaSalidaReal());
        assertThat(estanciaDb.getPrecioTotal()).isEqualByComparingTo(totalEsperado);

        assertThat(estanciaDb.getHabitaciones()).hasSize(1);
        assertThat(estanciaDb.getHabitaciones().getFirst().getUnidad().getId()).isEqualTo(apartaestudio.getId());
        assertThat(estanciaDb.getPagos()).hasSize(3);

        Pago cambioUnidadDb = estanciaDb.getPagos().stream()
                .filter(p -> p.getTipoPago() == TipoPago.CAMBIO_UNIDAD)
                .findFirst()
                .orElseThrow();
        assertThat(cambioUnidadDb.getEstado()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(cambioUnidadDb.getMonto()).isEqualByComparingTo(montoCambioUnidad);

        Pago pagoFinalDb = estanciaDb.getPagos().stream()
                .filter(p -> p.getTipoPago() == TipoPago.ESTANCIA_COMPLETADA)
                .findFirst()
                .orElseThrow();
        assertThat(pagoFinalDb.getMonto()).isEqualByComparingTo(pagoFinalRequest.getMonto());

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);
        comprobarUnidadYHabitacionesDb(apartamentoDb, EstadoOperativo.DISPONIBLE, 0);
        comprobarUnidadYHabitacionesDb(apartaestudioDb, EstadoOperativo.DISPONIBLE, 0);
    }

    @Test
    void exitoEditandoEstanciaSoloCambioFechasLuegoFinalizandoAcumulaPagoFinalSinCambioUnidad_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(apartamento.getHabitaciones(), true, EstadoEstancia.ACTIVA);

        BigDecimal montoAnticipoInicial = estancia.getPagos().stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Ocupante clienteActual = estancia.getOcupantes().stream()
                .filter(o -> o.getTipoOcupante() == TipoOcupante.CLIENTE)
                .findFirst()
                .orElseThrow();

        List<Ocupante> acompanantesActuales = estancia.getOcupantes().stream()
                .filter(o -> o.getTipoOcupante() == TipoOcupante.ACOMPANANTE)
                .toList();

        EstanciaRequestDTO editarRequest = estanciaRequestDTO(
                apartamento.getTipo(),
                apartamento.getCodigo(),
                clienteActual,
                acompanantesActuales,
                LocalDateTime.now().plusDays(-2)
        );

        PagoNuevoRequestDTO pagoFinalRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
        pagoFinalRequest.setMonto(BigDecimal.valueOf(530000));
        pagoFinalRequest.setEstado(EstadoPago.COMPLETADO);

        BigDecimal totalEsperado = montoAnticipoInicial.add(pagoFinalRequest.getMonto());

        // ---------- WHEN ----------
        estanciaService.editarEstancia(editarRequest, estancia.getId());
        SalidaEstanciaDTO salidaRequest = salidaEstanciaRequestDTO(estancia.getId(), LocalDateTime.now(), pagoFinalRequest);
        Void result = estanciaService.finalizarEstancia(salidaRequest);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        assertThat(result).isNull();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad apartamentoDb = unidadRepository.findById(apartamento.getId()).orElseThrow();

        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.FINALIZADA);
        assertThat(estanciaDb.getEntradaReal()).isEqualToIgnoringNanos(editarRequest.getEntradaReal());
        assertThat(estanciaDb.getSalidaEstimada()).isEqualToIgnoringNanos(editarRequest.getSalidaEstimada());
        assertThat(estanciaDb.getSalidaReal()).isEqualToIgnoringNanos(salidaRequest.getFechaSalidaReal());
        assertThat(estanciaDb.getPrecioTotal()).isEqualByComparingTo(totalEsperado);
        assertThat(estanciaDb.getPagos()).hasSize(2);
        assertThat(estanciaDb.getPagos().stream().noneMatch(p -> p.getTipoPago() == TipoPago.CAMBIO_UNIDAD)).isTrue();

        Pago pagoFinalDb = estanciaDb.getPagos().stream()
                .filter(p -> p.getTipoPago() == TipoPago.ESTANCIA_COMPLETADA)
                .findFirst()
                .orElseThrow();
        assertThat(pagoFinalDb.getMonto()).isEqualByComparingTo(pagoFinalRequest.getMonto());

        comprobarUnidadYHabitacionesDb(apartamentoDb, EstadoOperativo.DISPONIBLE, 0);
    }

    @Test
    void exitoFinalizandoEstanciaConPagoPendiente_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
        pagoRequest.setEstado(EstadoPago.PENDIENTE);
        SalidaEstanciaDTO request = salidaEstanciaRequestDTO(estancia.getId(), LocalDateTime.now(), pagoRequest);

        // ---------- WHEN ----------
        Void result = estanciaService.finalizarEstancia(request);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.ESTANCIA,
                estanciaDb.getId()).orElseThrow();

        assertThat(result).isNull();
        comprobarEstanciaDb(
                estanciaDb,
                null,
                3,
                estancia.getEntradaReal(),
                estancia.getSalidaEstimada(),
                request.getFechaSalidaReal(),
                request.getNotasSalida(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.FINALIZADA,
                pagoRequest.getMonto(),
                3,
                1
        );
        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);

        Pago pagoFinalDb = estanciaDb.getPagos().stream()
                .filter(p -> p.getTipoPago() == TipoPago.ESTANCIA_COMPLETADA)
                .findFirst()
                .orElseThrow();
        assertThat(pagoFinalDb.getEstado()).isEqualTo(EstadoPago.PENDIENTE);
        assertThat(pagoFinalDb.getMonto()).isEqualByComparingTo(pagoRequest.getMonto());

        comprobarEventoDb(eventoDb, TipoEvento.FINALIZACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);
    }

    @Test
    void falloFinalizandoEstanciaConEstadoPagoDiferenteACompletadoOPendiente_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
        pagoRequest.setEstado(EstadoPago.ELIMINADO);
        SalidaEstanciaDTO request = salidaEstanciaRequestDTO(estancia.getId(), LocalDateTime.now(), pagoRequest);

        // ---------- WHEN ----------
        long pagosAntes = pagoRepository.count();
        long eventosAntes = eventoRepository.count();

        assertThatThrownBy(() -> estanciaService.finalizarEstancia(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El estado del pago de tipo ESTANCIA_COMPLETADA debe ser COMPLETADO o PENDIENTE");

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        assertThat(pagosAntes).isEqualTo(pagoRepository.count());
        assertThat(eventosAntes).isEqualTo(eventoRepository.count());
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.ACTIVA);
        assertThat(estanciaDb.getSalidaReal()).isNull();

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 3);
    }

    @Test
    void falloFinalizandoEstanciaConEstadoFinalizado_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        estancia.setEstado(EstadoEstancia.RESERVADA);
        estanciaRepository.save(estancia);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
        SalidaEstanciaDTO request = salidaEstanciaRequestDTO(estancia.getId(), LocalDateTime.now(), pagoRequest);


        // ---------- WHEN ----------
        long estanciasAntes = estanciaRepository.count();
        long eventosAntes = eventoRepository.count();

        assertThatThrownBy(() -> estanciaService.finalizarEstancia(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo se pueden finalizar estancias en estado ACTIVA o EXCEDIDA. Estado actual:");


        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        assertThat(estanciasAntes).isEqualTo(estanciaRepository.count());
        assertThat(eventosAntes).isEqualTo(eventoRepository.count());

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        comprobarHabitacionesDb(unidadDb.getHabitaciones(), estanciaDb, null);

        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.RESERVADA);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 3);

    }

    /**
     * buscarEstanciasTabla(...)
     */

    @Test
    void exitoBuscandoEstanciasTablaConFiltrosYMapeo_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), true, EstadoEstancia.ACTIVA);
        Ocupante cliente = estancia.getOcupantes().stream()
                .filter(ocupante -> ocupante.getTipoOcupante() == TipoOcupante.CLIENTE)
                .findFirst().orElseThrow();
        cliente.setNombres("Valeria");
        cliente.setApellidos("Nunez");
        cliente.setNumeroDocumento("TABLA-EST-001");
        ocupanteRepository.save(cliente);

        BigDecimal totalPagoEsperado = estancia.getPagos().stream()
                .filter(pago -> pago.getEstado() == EstadoPago.PENDIENTE || pago.getEstado() == EstadoPago.COMPLETADO)
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Unidad otraUnidad = crearApartaestudio(EstadoOperativo.OCUPADO);
        Estancia otraEstancia = crearEstanciaExistente(otraUnidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        Ocupante otroCliente = otraEstancia.getOcupantes().stream()
                .filter(ocupante -> ocupante.getTipoOcupante() == TipoOcupante.CLIENTE)
                .findFirst().orElseThrow();
        otroCliente.setNumeroDocumento("OTRA-EST-999");
        ocupanteRepository.save(otroCliente);

        entityManager.flush();
        entityManager.clear();

        // ---------- WHEN ----------
        Page<EstanciaTablaDTO> resultado = estanciaService.buscarEstanciasTabla(
                List.of(EstadoEstancia.ACTIVA),
                TipoUnidad.APARTAMENTO,
                ModoOcupacion.COMPLETO,
                estancia.getCodigoFolio(),
                unidad.getCodigo(),
                "Valeria Nunez",
                "TABLA-EST",
                cliente.getId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                PageRequest.of(0, 10)
        );

        // ---------- THEN ----------
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent()).hasSize(1);

        EstanciaTablaDTO dto = resultado.getContent().getFirst();
        assertThat(dto.getId()).isEqualTo(estancia.getId());
        assertThat(dto.getCodigoEstancia()).isEqualTo(estancia.getCodigoFolio());
        assertThat(dto.getCodigoUnidad()).isEqualTo(unidad.getCodigo());
        assertThat(dto.getTipoUnidad()).isEqualTo(TipoUnidad.APARTAMENTO);
        assertThat(dto.getNombreCliente()).isEqualTo("Valeria Nunez");
        assertThat(dto.getIdCliente()).isEqualTo(cliente.getId());
        assertThat(dto.getNumeroDocumentoCliente()).isEqualTo("TABLA-EST-001");
        assertThat(dto.getEstadoEstancia()).isEqualTo(EstadoEstancia.ACTIVA);
        assertThat(dto.getModoOcupacion()).isEqualTo(ModoOcupacion.COMPLETO);
        assertThat(dto.getTieneReservaAsociada()).isFalse();
        assertThat(dto.getIdReservaAsociada()).isNull();
        assertThat(dto.getCodigoReservaAsociada()).isNull();
        assertThat(dto.getTotalPersonas()).isEqualTo(estancia.getOcupantes().size());
        assertThat(dto.getTotalPagoEstancia()).isEqualByComparingTo(totalPagoEsperado);
        assertThat(dto.getCantidadPagosModificadosOEliminados()).isZero();
    }

    @Test
    void exitoBuscandoEstanciasTablaConReservaAsociadaOrdenPorDefectoYPaginacion_test() {

        // ---------- GIVEN ----------
        Unidad unidad1 = crearApartamento(EstadoOperativo.OCUPADO);
        Unidad unidad2 = crearApartamento(EstadoOperativo.OCUPADO);
        Unidad unidad3 = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estanciaAntigua = crearEstanciaExistente(unidad1.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        estanciaAntigua.setEntradaReal(LocalDateTime.now().minusDays(3));
        estanciaRepository.save(estanciaAntigua);

        Estancia estanciaMedia = crearEstanciaExistente(unidad2.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        estanciaMedia.setEntradaReal(LocalDateTime.now().minusDays(2));
        estanciaRepository.save(estanciaMedia);

        Estancia estanciaConReserva = crearEstanciaConReservaExistente(unidad3.getHabitaciones(), false, true);
        estanciaConReserva.setEntradaReal(LocalDateTime.now().minusDays(1));
        estanciaRepository.save(estanciaConReserva);
        BigDecimal totalPagoConReserva = estanciaConReserva.getPagos().stream()
                .filter(pago -> pago.getEstado() == EstadoPago.PENDIENTE || pago.getEstado() == EstadoPago.COMPLETADO)
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        entityManager.flush();
        entityManager.clear();

        // ---------- WHEN ----------
        Page<EstanciaTablaDTO> conReserva = estanciaService.buscarEstanciasTabla(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                true,
                PageRequest.of(0, 10)
        );

        Page<EstanciaTablaDTO> pagina0 = estanciaService.buscarEstanciasTabla(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null,
                PageRequest.of(0, 2)
        );

        Page<EstanciaTablaDTO> pagina1 = estanciaService.buscarEstanciasTabla(
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null,
                PageRequest.of(1, 2)
        );

        // ---------- THEN ----------
        assertThat(conReserva.getContent()).hasSize(1);
        EstanciaTablaDTO dtoConReserva = conReserva.getContent().getFirst();
        assertThat(dtoConReserva.getId()).isEqualTo(estanciaConReserva.getId());
        assertThat(dtoConReserva.getTieneReservaAsociada()).isTrue();
        assertThat(dtoConReserva.getIdReservaAsociada()).isEqualTo(estanciaConReserva.getReserva().getId());
        assertThat(dtoConReserva.getCodigoReservaAsociada()).isEqualTo(estanciaConReserva.getReserva().getCodigo());
        assertThat(dtoConReserva.getTotalPagoEstancia()).isEqualByComparingTo(totalPagoConReserva);

        assertThat(pagina0.getContent()).hasSize(2);
        assertThat(pagina0.getContent().get(0).getId()).isEqualTo(estanciaConReserva.getId());
        assertThat(pagina0.getContent().get(1).getId()).isEqualTo(estanciaMedia.getId());

        assertThat(pagina1.getContent()).hasSize(1);
        assertThat(pagina1.getContent().getFirst().getId()).isEqualTo(estanciaAntigua.getId());
    }

    //metodo auxiliar
    private BigDecimal calcularMontoPendienteCambioUnidad(Estancia estancia, TipoUnidad tipoUnidadAnterior) {
        if (estancia == null || tipoUnidadAnterior == null) {
            throw new IllegalArgumentException("No se proporcionó información de estancia o tipo de unidad");
        }

        Integer totalOcupantes = estancia.getOcupantes().size();
        LocalDateTime fechaEntrada = estancia.getEntradaReal();
        LocalDateTime fechaFinalizacion = LocalDateTime.now();

        CalcularPagoDTO calcularPagoDTO = PagoMapper.entityToCalcularPagoDTO(estancia.getId(), tipoUnidadAnterior, totalOcupantes, fechaEntrada, fechaFinalizacion);

        return pagoService.obtenerEstimacionPago(calcularPagoDTO);

    }
}
