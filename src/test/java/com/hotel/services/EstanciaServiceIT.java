package com.hotel.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.hotel.dtos.estancia.EstanciaRequestDTO;
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
import static com.hotel.testdata.OcupanteTestData.*;
import static com.hotel.testdata.PagoTestData.pagoNuevoRequestDTO;
import static com.hotel.testdata.ReservaTestData.reservaData;
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


        EstanciaRequestDTO request = estanciaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, acompanantes, null,null);

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

        comprobarEventoDb(eventoDb, TipoEvento.CREACION_ESTANCIA, estanciaDb.getCodigoFolio(), null);

    }

    @Test
    void exitoCreandoEstanciaNuevaApartaestudio_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        // Cliente
        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());


        EstanciaRequestDTO request = estanciaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, acompanantes, null,null);

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

        comprobarEventoDb(eventoDb, TipoEvento.CREACION_ESTANCIA, estanciaDb.getCodigoFolio(), null);

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


        EstanciaRequestDTO request = estanciaRequestDTO(TipoUnidad.HABITACION, habitacion.getCodigo(), cliente, acompanantes, null,null);

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

        comprobarEventoDb(eventoDb, TipoEvento.CREACION_ESTANCIA, estanciaDb.getCodigoFolio(), null);

    }

    @Test
    void exitoCreandoEstanciaNuevaConPago_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        // Cliente
        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());


        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ANTICIPO_ESTANCIA);
        EstanciaRequestDTO request = estanciaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, acompanantes,null, pagoRequest);

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
                1);

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 3);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                request.getPago().getMonto(),
                BigDecimal.valueOf(0),
                request.getPago().getEstado(),
                1,
                EstadoPago.MODIFICADO,
                0,
                0,
                1,
                0);

        comprobarEventoDb(eventoDb, TipoEvento.CREACION_ESTANCIA, estanciaDb.getCodigoFolio(), null);
    }

    @Test
    void exitoCreandoEstanciaConEstadoExcedida_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        // Cliente
        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());


        EstanciaRequestDTO request = estanciaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, acompanantes, LocalDateTime.now().plusDays(-4),null);

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

        comprobarEventoDb(eventoDb, TipoEvento.CREACION_ESTANCIA, estanciaDb.getCodigoFolio(), null);

    }

    @Test
    void exitoCreandoEstanciaNuevaSinAcompanantes_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        // Cliente
        Ocupante cliente = crearCliente(clienteData());


        EstanciaRequestDTO request = estanciaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, null, null,null);

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

        comprobarEventoDb(eventoDb, TipoEvento.CREACION_ESTANCIA, estanciaDb.getCodigoFolio(), null);

    }

    @Test
    void falloCreandoEstanciaNuevaPorEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());

        crearEstanciaExistente(unidad.getHabitaciones());

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                cliente,
                acompanantes,
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
    void falloCreandoEstanciaNuevaPorReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());

        crearReservaExistente(unidad.getHabitaciones());

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                cliente,
               acompanantes,
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
    void falloCreandoEstanciaConFechaDeSalidaPosteriorFechaEntrada_test() {

        // ---------- GIVEN ----------
        // Unidad tipo APARTAMENTO con 3 habitaciones DISPONIBLES
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        EstanciaRequestDTO request = errorFechasEstanciaRequestDTO(unidad);


        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();


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


        EstanciaRequestDTO request = estanciaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, acompanantes, LocalDateTime.now().plusDays(1),null);


        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();


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


        Estancia estanciaExistente = crearEstanciaExistente(unidad.getHabitaciones());

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2),
                null
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

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null);


    }

    @Test
    void exitoEditandoEstanciaApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());

        Estancia estanciaExistente = crearEstanciaExistente(unidad.getHabitaciones());

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2),
                null
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

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null);

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

        Estancia estanciaExistente = crearEstanciaExistente(listaHabitacion);

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2),
                null
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
                EstadoOperativo.OCUPADO,
                3);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null);
    }

    @Test
    void exitoEditandoEstanciaConReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaConReservaExistente(unidad.getHabitaciones());

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2),
                null
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
                1);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                3);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null);


    }

    @Test
    void exitoEditandoEstanciaConCambioApartamento_test() {

        // ---------- GIVEN ----------
        Unidad unidad1 = crearApartamento(EstadoOperativo.OCUPADO);
        Unidad unidad2 = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(unidad1.getHabitaciones());

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad2.getTipo(),
                unidad2.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2),
                null
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

        comprobarHabitacionesDb(unidadDb2.getHabitaciones(), estanciaDb);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null);


    }

    @Test
    void exitoEditandoEstanciaConCambioApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad unidad1 = crearApartaestudio(EstadoOperativo.OCUPADO);
        Unidad unidad2 = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());

        Estancia estanciaExistente = crearEstanciaExistente(unidad1.getHabitaciones());

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad2.getTipo(),
                unidad2.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2),
                null
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

        comprobarHabitacionesDb(unidadDb2.getHabitaciones(), estanciaDb);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null);

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

        Estancia estanciaExistente = crearEstanciaExistente(listaHabitacion);

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion2.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2),
                null
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

        comprobarHabitacionesDb(List.of(habitacion2), estanciaDb);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null);
    }

    @Test
    void exitoEditandoEstanciaConCambioApartamentoAApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.OCUPADO);
        Unidad apartaestudio = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(apartamento.getHabitaciones());

        EstanciaRequestDTO request = estanciaRequestDTO(
                apartaestudio.getTipo(),
                apartaestudio.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2),
                null
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
                1);

        comprobarUnidadYHabitacionesDb(
                apartamentoDb,
                EstadoOperativo.DISPONIBLE,
                0);

        comprobarUnidadYHabitacionesDb(
                apartaestudioDb,
                EstadoOperativo.OCUPADO,
                1);

        comprobarHabitacionesDb(apartaestudioDb.getHabitaciones(), estanciaDb);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null);


    }

    @Test
    void exitoEditandoEstanciaConCambioApartamentoAHabitacionMismaUnidad_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = apartamento.getHabitaciones().getFirst();

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(apartamento.getHabitaciones());

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2),
                null
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
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                null,
                1,
                1);

        comprobarUnidadYHabitacionesDb(
                apartamentoDb,
                EstadoOperativo.PARCIALMENTE,
                1);

        comprobarHabitacionesDb(List.of(habitacion), estanciaDb);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null);


    }

    @Test
    void exitoEditandoEstanciaConCambioApartamentoAHabitacionDiferenteUnidad_test() {

        // ---------- GIVEN ----------
        Unidad apartamento1 = crearApartamento(EstadoOperativo.OCUPADO);
        Unidad apartamento2 = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = apartamento2.getHabitaciones().getFirst();

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(apartamento1.getHabitaciones());

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2),
                null
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
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                null,
                1,
                1);

        comprobarUnidadYHabitacionesDb(
                apartamentoDb1,
                EstadoOperativo.DISPONIBLE,
                0);

        comprobarUnidadYHabitacionesDb(
                apartamentoDb2,
                EstadoOperativo.PARCIALMENTE,
                1);

        comprobarHabitacionesDb(List.of(habitacion), estanciaDb);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null);


    }

    @Test
    void exitoEditandoEstanciaConCambioApartaestudioAApartamento_test() {

        // ---------- GIVEN ----------
        Unidad apartaestudio = crearApartaestudio(EstadoOperativo.OCUPADO);
        Unidad apartamento = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(apartaestudio.getHabitaciones());

        EstanciaRequestDTO request = estanciaRequestDTO(
                apartamento.getTipo(),
                apartamento.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2),
                null
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
                1);

        comprobarUnidadYHabitacionesDb(
                apartamentoDb,
                EstadoOperativo.OCUPADO,
                3);

        comprobarUnidadYHabitacionesDb(
                apartaestudioDb,
                EstadoOperativo.DISPONIBLE,
                0);

        comprobarHabitacionesDb(apartamentoDb.getHabitaciones(), estanciaDb);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null);


    }

    @Test
    void exitoEditandoEstanciaConCambioApartaestudioAHabitacion_test() {

        // ---------- GIVEN ----------
        Unidad apartaestudio = crearApartaestudio(EstadoOperativo.OCUPADO);
        Unidad apartamento = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = apartamento.getHabitaciones().getFirst();

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(apartaestudio.getHabitaciones());

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2),
                null
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
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                null,
                1,
                1);

        comprobarUnidadYHabitacionesDb(
                apartamentoDb,
                EstadoOperativo.PARCIALMENTE,
                1);

        comprobarUnidadYHabitacionesDb(
                apartaestudioDb,
                EstadoOperativo.DISPONIBLE,
                0);

        comprobarHabitacionesDb(List.of(habitacion), estanciaDb);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null);


    }

    @Test
    void exitoEditandoEstanciaConCambioHabitacionAApartamentoMismaUnidad_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = apartamento.getHabitaciones().getFirst();

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(List.of(habitacion));

        EstanciaRequestDTO request = estanciaRequestDTO(
                apartamento.getTipo(),
                apartamento.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2),
                null
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
                1);

        comprobarUnidadYHabitacionesDb(
                apartamentoDb,
                EstadoOperativo.OCUPADO,
                3);

        comprobarHabitacionesDb(apartamentoDb.getHabitaciones(), estanciaDb);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null);


    }

    @Test
    void exitoEditandoEstanciaConCambioHabitacionAApartamentoDiferenteUnidad_test() {

        // ---------- GIVEN ----------
        Unidad apartamento1 = crearApartamento(EstadoOperativo.PARCIALMENTE);
        Unidad apartamento2 = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = apartamento1.getHabitaciones().getFirst();

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());

        Estancia estanciaExistente = crearEstanciaExistente(List.of(habitacion));

        EstanciaRequestDTO request = estanciaRequestDTO(
                apartamento2.getTipo(),
                apartamento2.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2),
                null
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
                1);

        comprobarUnidadYHabitacionesDb(
                apartamentoDb1,
                EstadoOperativo.DISPONIBLE,
                0);

        comprobarUnidadYHabitacionesDb(
                apartamentoDb2,
                EstadoOperativo.OCUPADO,
                3);

        comprobarHabitacionesDb(apartamentoDb2.getHabitaciones(), estanciaDb);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null);


    }

    @Test
    void exitoEditandoEstanciaConCambioHabitacionAApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad apartaestudio = crearApartaestudio(EstadoOperativo.DISPONIBLE);
        Unidad apartamento = crearApartamento(EstadoOperativo.PARCIALMENTE);
        Habitacion habitacion = apartamento.getHabitaciones().getFirst();

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(List.of(habitacion));

        EstanciaRequestDTO request = estanciaRequestDTO(
                apartaestudio.getTipo(),
                apartaestudio.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2),
                null
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
                1);

        comprobarUnidadYHabitacionesDb(
                apartamentoDb,
                EstadoOperativo.DISPONIBLE,
                0);

        comprobarUnidadYHabitacionesDb(
                apartaestudioDb,
                EstadoOperativo.OCUPADO,
                1);

        comprobarHabitacionesDb(apartaestudioDb.getHabitaciones(), estanciaDb);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null);


    }


}
