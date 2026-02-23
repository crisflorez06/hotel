package com.hotel.services;

import com.fasterxml.jackson.databind.JsonNode;
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

        comprobarEventoDb(eventoDb, TipoEvento.CREACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 4);

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

        comprobarEventoDb(eventoDb, TipoEvento.CREACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 4);

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

        comprobarEventoDb(eventoDb, TipoEvento.CREACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 4);

    }

    @Test
    void falloCreandoEstanciaNuevaPorEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());

        crearEstanciaExistente(unidad.getHabitaciones(), true);

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

        crearReservaExistente(unidad.getHabitaciones(), true);

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
    void falloCreandoEstanciaConFechaSalidaAnteriorAEntrada_test() {

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


        Estancia estanciaExistente = crearEstanciaExistente(unidad.getHabitaciones(), true);

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

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 4);


    }

    @Test
    void exitoEditandoEstanciaApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());

        Estancia estanciaExistente = crearEstanciaExistente(unidad.getHabitaciones(), true);

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

        Estancia estanciaExistente = crearEstanciaExistente(listaHabitacion, true);

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
                0);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                3);

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


        Estancia estanciaExistente = crearEstanciaExistente(unidad1.getHabitaciones(), true);

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
                2);

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

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);


    }

    @Test
    void exitoEditandoEstanciaConCambioApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad unidad1 = crearApartaestudio(EstadoOperativo.OCUPADO);
        Unidad unidad2 = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());

        Estancia estanciaExistente = crearEstanciaExistente(unidad1.getHabitaciones(), true);

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
                2);

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

        Estancia estanciaExistente = crearEstanciaExistente(listaHabitacion, true);

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
                2);

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

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);
    }

    @Test
    void exitoEditandoEstanciaConCambioApartamentoAApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.OCUPADO);
        Unidad apartaestudio = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(apartamento.getHabitaciones(), true);

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
                2);

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

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);


    }

    @Test
    void exitoEditandoEstanciaConCambioApartamentoAHabitacionMismaUnidad_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = apartamento.getHabitaciones().getFirst();

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(apartamento.getHabitaciones(), true);

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
                2);

        comprobarUnidadYHabitacionesDb(
                apartamentoDb,
                EstadoOperativo.PARCIALMENTE,
                1);

        comprobarHabitacionesDb(List.of(habitacion), estanciaDb);

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


        Estancia estanciaExistente = crearEstanciaExistente(apartamento1.getHabitaciones(), true);

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
                2);

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

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);


    }

    @Test
    void exitoEditandoEstanciaConCambioApartaestudioAApartamento_test() {

        // ---------- GIVEN ----------
        Unidad apartaestudio = crearApartaestudio(EstadoOperativo.OCUPADO);
        Unidad apartamento = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(apartaestudio.getHabitaciones(), true);

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
                2);

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


        Estancia estanciaExistente = crearEstanciaExistente(apartaestudio.getHabitaciones(), true);

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
                2);

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

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);


    }

    @Test
    void exitoEditandoEstanciaConCambioHabitacionAApartamentoMismaUnidad_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = apartamento.getHabitaciones().getFirst();

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(List.of(habitacion), true);

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
                2);

        comprobarUnidadYHabitacionesDb(
                apartamentoDb,
                EstadoOperativo.OCUPADO,
                3);

        comprobarHabitacionesDb(apartamentoDb.getHabitaciones(), estanciaDb);

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

        Estancia estanciaExistente = crearEstanciaExistente(List.of(habitacion), true);

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
                2);

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


        Estancia estanciaExistente = crearEstanciaExistente(List.of(habitacion), true);

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
                2);

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

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);


    }

    @Test
    void exitoEditandoEstanciaConCambioUnidadSinCambioFecha_test() {

        // ---------- GIVEN ----------
        Unidad unidad1 = crearApartamento(EstadoOperativo.OCUPADO);
        Unidad unidad2 = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(unidad1.getHabitaciones(), true);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad2.getTipo(),
                unidad2.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                null,
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
                2);

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

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 3);


    }

    @Test
    void exitoEditandoEstanciaAEstadoExcedido_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(unidad.getHabitaciones(), true);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-5),
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
                EstadoEstancia.EXCEDIDA,
                null,
                3,
                1);

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                3);

        comprobarOcupantesDb(estanciaDb.getOcupantes(), clienteNuevo, ocupantesNuevos);

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 4);


    }

    @Test
    void exitoEditandoEstanciaConEstadoExcedido_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());


        Estancia estanciaExistente = crearEstanciaExistente(unidad.getHabitaciones(), true);
        estanciaExistente.setEntradaReal(LocalDateTime.now().plusDays(-5));
        estanciaExistente.setEstado(EstadoEstancia.EXCEDIDA);
        estanciaRepository.save(estanciaExistente);

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

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 4);


    }

    @Test
    void exitoEditandoEstanciaSinAcompanantes_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());


        Estancia estanciaExistente = crearEstanciaExistente(unidad.getHabitaciones(), true);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                null,
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

        comprobarEventoDb(eventoDb, TipoEvento.MODIFICACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 4);


    }

    @Test
    void exitoEditandoEstanciaSinCambios_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estanciaExistente = crearEstanciaExistente(unidad.getHabitaciones(), true);
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
                estanciaExistente.getEntradaReal(),
                null
        );

        // ---------- WHEN ----------
        Long eventosAntes = eventoRepository.count();

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

        comprobarOcupantesDb(estanciaDb.getOcupantes(), cliente, ocupantes);

        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);




    }

    @Test
    void falloEditandoEstanciaPorReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante clienteNuevo = crearCliente(clienteEditarData());
        List<Ocupante> ocupantesNuevos = crearAcompanantesSinCliente(acompanantesDataEditar());

        crearReservaExistente(unidad.getHabitaciones(), true);

        Estancia estanciaExistente = crearEstanciaExistente(unidad.getHabitaciones(), true);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                null,
                null
        );

        // Snapshot BD antes
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();
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


        Estancia estanciaExistente = crearEstanciaExistente(unidad1.getHabitaciones(), true);
        crearEstanciaExistente(unidad2.getHabitaciones(), true);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad2.getTipo(),
                unidad2.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2),
                null
        );

        long estanciasAntes = estanciaRepository.count();
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


        Estancia estanciaExistente = crearEstanciaExistente(unidad.getHabitaciones(), true);
        estanciaExistente.setEntradaReal(LocalDateTime.now().plusDays(-5));
        estanciaExistente.setEstado(EstadoEstancia.FINALIZADA);
        estanciaRepository.save(estanciaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                clienteNuevo,
                ocupantesNuevos,
                LocalDateTime.now().plusDays(-2),
                null
        );

        long estanciasAntes = estanciaRepository.count();
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

        comprobarUnidadYHabitacionesDb(
                unidadDb,
                EstadoOperativo.OCUPADO,
                3);


    }

    @Test
    void falloEditandoEstanciaConFechaSalidaAnteriorAEntrada_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);


        Estancia estanciaExistente = crearEstanciaExistente(unidad.getHabitaciones(), true);


        EstanciaRequestDTO request = errorFechasEstanciaRequestDTO(unidad);

        long estanciasAntes = estanciaRepository.count();
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

        Estancia estanciaExistente = crearEstanciaExistente(unidad.getHabitaciones(), true);


        EstanciaRequestDTO request = estanciaRequestDTO(unidad.getTipo(), unidad.getCodigo(), clienteNuevo, ocupantesNuevos, LocalDateTime.now().plusDays(1),null);

        long estanciasAntes = estanciaRepository.count();
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

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false);

        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantes,
                LocalDateTime.now().minusDays(1),
                null
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

        comprobarEventoDb(eventoDb, TipoEvento.ACTIVACION_ESTANCIA, estanciaDb.getCodigoFolio(), reservaDb.getCodigo(), 4);

    }

    @Test
    void exitoActivandoEstanciaApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = ocupanteRepository.save(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false);

        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantes,
                LocalDateTime.now().minusDays(1),
                null
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

        comprobarEventoDb(eventoDb, TipoEvento.ACTIVACION_ESTANCIA, estanciaDb.getCodigoFolio(), reservaDb.getCodigo(), 4);
    }

    @Test
    void exitoActivandoEstanciaHabitacion_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();


        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());

        Reserva reserva = crearReservaExistente(List.of(habitacion), false);

        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantes,
                LocalDateTime.now().minusDays(1),
                null
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
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                null,
                1,
                0
        );

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.PARCIALMENTE, 1);

        comprobarEventoDb(eventoDb, TipoEvento.ACTIVACION_ESTANCIA, estanciaDb.getCodigoFolio(), reservaDb.getCodigo(), 4);

    }

    @Test
    void exitoActivandoEstanciaConPagoEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ANTICIPO_ESTANCIA);
        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantes,
                LocalDateTime.now().minusDays(1),
                pagoRequest
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
                request.getPago().getMonto(),
                BigDecimal.valueOf(0),
                request.getPago().getEstado(),
                1,
                EstadoPago.MODIFICADO,
                0,
                0,
                1,
                0);

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 3);

        comprobarEventoDb(eventoDb, TipoEvento.ACTIVACION_ESTANCIA, estanciaDb.getCodigoFolio(), reservaDb.getCodigo(), 4);

    }

    @Test
    void exitoActivandoEstanciaConPagoReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());


        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), true);

        BigDecimal monto = reserva.getEstancia().getPagos().stream()
                .filter(pago -> pago.getTipoPago() == TipoPago.ANTICIPO_RESERVA)
                .map(Pago::getMonto)
                .findFirst().orElseThrow();

        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantes,
                LocalDateTime.now().minusDays(1),
                null
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
                BigDecimal.valueOf(0),
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                0,
                0,
                1);

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 3);

        comprobarEventoDb(eventoDb, TipoEvento.ACTIVACION_ESTANCIA, estanciaDb.getCodigoFolio(), reservaDb.getCodigo(), 4);

    }

    @Test
    void exitoActivandoEstanciaConPagoEstanciaYReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), true);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ANTICIPO_ESTANCIA);
        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantes,
                LocalDateTime.now().minusDays(1),
                pagoRequest
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
                2
        );

        BigDecimal montoReserva = estanciaDb.getPagos().stream()
                .filter(pago -> pago.getTipoPago() == TipoPago.ANTICIPO_RESERVA)
                .map(Pago::getMonto)
                .findFirst().orElseThrow();

        BigDecimal montoEstancia = request.getPago().getMonto();

        BigDecimal montoTotal = montoReserva.add(montoEstancia);



        comprobarPagosDb(
                estanciaDb.getPagos(),
                montoTotal,
                BigDecimal.valueOf(0),
                request.getPago().getEstado(),
                2,
                EstadoPago.MODIFICADO,
                0,
                0,
                1,
                1);

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 3);

        comprobarEventoDb(eventoDb, TipoEvento.ACTIVACION_ESTANCIA, estanciaDb.getCodigoFolio(), reservaDb.getCodigo(), 4);

    }

    @Test
    void exitoActivandoEstanciaConEstadoExcedido_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false);

        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantes,
                LocalDateTime.now().plusDays(-5),
                null
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

        comprobarEventoDb(eventoDb, TipoEvento.ACTIVACION_ESTANCIA, estanciaDb.getCodigoFolio(), reservaDb.getCodigo(), 4);

    }

    @Test
    void exitoActivandoEstanciaSinAcompanates_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = crearCliente(clienteData());

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false);

        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                null,
                LocalDateTime.now().minusDays(1),
                null
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

        comprobarEventoDb(eventoDb, TipoEvento.ACTIVACION_ESTANCIA, estanciaDb.getCodigoFolio(), reservaDb.getCodigo(), 4);

    }

    @Test
    void falloActivandoEstanciaPorEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());

        Estancia estanciaExistente = crearEstanciaExistente(unidad.getHabitaciones(), true);

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false);

        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantes,
                LocalDateTime.now().minusDays(1),
                null
        );

        // ---------- WHEN ----------
        long estanciasAntes = estanciaRepository.count();

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

        comprobarEstanciaDb(
                estanciaDbReserva,
                reservaDb,
                1,
                null,
                null,
                null,
                "",
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

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false);
        Reserva reservaExistente = crearReservaExistente(unidad.getHabitaciones(), false);


        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantes,
                LocalDateTime.now().minusDays(1),
                null
        );

        // ---------- WHEN ----------
        long estanciasAntes = estanciaRepository.count();
        long reservaAntes = reservaRepository.count();

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

        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        assertThat(reservaExistenteDb.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);

        comprobarEstanciaDb(
                estanciaDbReservaExistente,
                reservaExistenteDb,
                1,
                null,
                null,
                null,
                "",
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                null,
                3,
                0
        );

        comprobarEstanciaDb(
                estanciaDbReserva,
                reservaDb,
                1,
                null,
                null,
                null,
                "",
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

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false);
        Estancia estancia = estanciaRepository.findByReserva_Id(reserva.getId()).orElseThrow();
        estancia.setEstado(EstadoEstancia.ACTIVA);
        estanciaRepository.save(estancia);


        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantes,
                LocalDateTime.now().minusDays(1),
                null
        );

        // ---------- WHEN ----------

        long estanciasAntes = estanciaRepository.count();

        assertThatThrownBy(() -> estanciaService.activarEstancia(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La estancia debe estar en estado RESERVADA para activarla. Estado actual:");

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();


        assertThat(estanciasAntes).isEqualTo(estanciaRepository.count());
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                1,
                null,
                null,
                null,
                "",
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

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false);
        reserva.setEstado(EstadoReserva.COMPLETADA);
        reservaRepository.save(reserva);



        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantes,
                LocalDateTime.now().minusDays(1),
                null
        );

        // ---------- WHEN ----------

        long estanciasAntes = estanciaRepository.count();

        assertThatThrownBy(() -> estanciaService.activarEstancia(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("La reserva debe estar CONFIRMADA para activar la estancia. Estado actual:");

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reserva.getId()).orElseThrow();
        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();


        assertThat(estanciasAntes).isEqualTo(estanciaRepository.count());
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.COMPLETADA);

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                1,
                null,
                null,
                null,
                "",
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

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false);

        ActivarEstanciaDTO request =errorFechasActivarEstanciaRequestDTO(reserva.getId());

        // ---------- WHEN ----------
        long estanciasAntes = estanciaRepository.count();

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
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);

        comprobarEstanciaDb(
                estanciaDbReserva,
                reservaDb,
                1,
                null,
                null,
                null,
                "",
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

        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false);

        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                cliente,
                acompanantes,
                LocalDateTime.now().plusDays(1),
                null
        );
        // ---------- WHEN ----------
        long estanciasAntes = estanciaRepository.count();

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
        assertThat(reservaDb.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);

        comprobarEstanciaDb(
                estanciaDbReserva,
                reservaDb,
                1,
                null,
                null,
                null,
                "",
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                null,
                3,
                0
        );

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
                LocalDateTime.now().minusDays(1),
                null
        );
        // ---------- WHEN ----------
        long estanciasAntes = estanciaRepository.count();

        assertThatThrownBy(() -> estanciaService.activarEstancia(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El id de reserva no puede ser nulo para activar una estancia");


        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        assertThat(estanciasAntes).isEqualTo(estanciaRepository.count());

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);

    }

    /**
     * eliminarEstancia(Long idEstancia)
     */

    @Test
    void exitoEliminandoEstanciaApartamento_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false);

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
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
        comprobarEventoDb(eventoDb, TipoEvento.ELIMINACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 1);

    }

    @Test
    void exitoEliminandoEstanciaApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);

        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false);

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

        Estancia estancia = crearEstanciaExistente(listaHabitacion, false);


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
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.PARCIALMENTE, 2);
        comprobarEventoDb(eventoDb, TipoEvento.ELIMINACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 1);
    }

    @Test
    void exitoEliminandoEstanciaApartamentoConPagoEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), true);

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
        assertThat(estanciaDb.getPagos()).hasSize(estancia.getPagos().size());
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                BigDecimal.ZERO,
                monto,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                1,
                0,
                1,
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
        assertThat(estanciaDb.getPagos()).hasSize(estancia.getPagos().size());
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                BigDecimal.ZERO,
                monto,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                1,
                0,
                0,
                1);

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
        assertThat(estanciaDb.getPagos()).hasSize(estancia.getPagos().size());
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                BigDecimal.ZERO,
                monto,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                2,
                0,
                1,
                1);

        comprobarEventoDb(eventoDb, TipoEvento.ELIMINACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 1);
    }

    @Test
    void falloEliminandoEstanciaConEstadoFinalizada_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false);
        estancia.setEstado(EstadoEstancia.FINALIZADA);
        estanciaRepository.save(estancia);

        // ---------- WHEN ----------
        long estanciasAntes = estanciaRepository.count();


        assertThatThrownBy(() -> estanciaService.eliminarEstancia(estancia.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo se pueden eliminar estancias en estado ACTIVA o EXCEDIDA. Estado actual");

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        assertThat(estanciasAntes).isEqualTo(estanciaRepository.count());

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();


        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.FINALIZADA);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 3);

    }

    /** obtenerEstanciaPorId(Long idEstancia) */
    @Test
    void exitoObteniendoEstanciaApartamento_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), true);
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
                monto,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                0,
                1,
                0);
    }

    @Test
    void exitoObteniendoEstanciaApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), true);
        BigDecimal monto = estancia.getPagos().stream()
                .map(Pago::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


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
                monto,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                0,
                1,
                0);
    }

    @Test
    void exitoObteniendoEstanciaHabitacion_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();
        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Estancia estancia = crearEstanciaExistente(listaHabitacion, true);
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
                monto,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                0,
                1,
                0);
    }

    /**
     * finalizarEstancia(SalidaEstanciaDTO request)
     */

    @Test
    void exitoFinalizandoEstanciaApartamento_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
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

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                pagoRequest.getMonto(),
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                1,
                0,
                0);
        comprobarEventoDb(eventoDb, TipoEvento.FINALIZACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);
    }

    @Test
    void exitoFinalizandoEstanciaApartaestudio_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);

        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
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

        comprobarPagosDb(
                estanciaDb.getPagos(),
                pagoRequest.getMonto(),
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                1,
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

        Estancia estancia = crearEstanciaExistente(listaHabitacion, false);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
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

        comprobarPagosDb(
                estanciaDb.getPagos(),
                pagoRequest.getMonto(),
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                1,
                0,
                0);
        comprobarEventoDb(eventoDb, TipoEvento.FINALIZACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);
    }

    @Test
    void exitoFinalizandoEstanciaConPagoEstanciaAnticipado_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), true);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
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

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                monto,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                2,
                EstadoPago.MODIFICADO,
                0,
                1,
                1,
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

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                monto,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                2,
                EstadoPago.MODIFICADO,
                0,
                1,
                0,
                1);
        comprobarEventoDb(eventoDb, TipoEvento.FINALIZACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);
    }

    @Test
    void exitoFinalizandoEstanciaConPagoEstanciaYReservaAnticipado_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estancia = crearEstanciaConReservaExistente(unidad.getHabitaciones(), true, true);
        Reserva reserva = estancia.getReserva();

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
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

        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.DISPONIBLE, 0);
        comprobarPagosDb(
                estanciaDb.getPagos(),
                monto,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                3,
                EstadoPago.MODIFICADO,
                0,
                1,
                1,
                1);
        comprobarEventoDb(eventoDb, TipoEvento.FINALIZACION_ESTANCIA, estanciaDb.getCodigoFolio(), null, 5);
    }

    @Test
    void falloFinalizandoEstanciaConEstadoFinalizado_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false);
        estancia.setEstado(EstadoEstancia.RESERVADA);
        estanciaRepository.save(estancia);

        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
        SalidaEstanciaDTO request = salidaEstanciaRequestDTO(estancia.getId(), LocalDateTime.now(), pagoRequest);


        // ---------- WHEN ----------
        long estanciasAntes = estanciaRepository.count();

        assertThatThrownBy(() -> estanciaService.finalizarEstancia(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo se pueden finalizar estancias en estado ACTIVA o EXCEDIDA. Estado actual:");


        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        assertThat(estanciasAntes).isEqualTo(estanciaRepository.count());

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();



        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.RESERVADA);
        comprobarUnidadYHabitacionesDb(unidadDb, EstadoOperativo.OCUPADO, 3);

    }
}
