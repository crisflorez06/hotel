package com.hotel.services;

import com.hotel.dtos.estancia.EstanciaRequestDTO;
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
import static com.hotel.testdata.ReservaTestData.reservaData;
import static com.hotel.testdata.TestDataUtils.randomCodigo;
import static com.hotel.testdata.OcupanteTestData.acompanantesData;
import static com.hotel.testdata.OcupanteTestData.clienteData;
import static com.hotel.testdata.UnidadTestData.unidadData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;

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
        comprobarEstanciaDb(
                request,
                estancia,
                null,
                3,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                1);

        comprobarUnidadDb(
                unidad,
                TipoUnidad.APARTAMENTO,
                EstadoOperativo.OCUPADO,
                3);


        comprobarHabitacionesDb(
                estancia,
                unidad,
                EstadoOperativo.OCUPADO,
                3
        );

        comprobarOcupantesDb(
                request,
                estancia
        );

        comprobarPagosDb(
                request,
                estancia
        );
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
        comprobarEstanciaDb(
                request,
                estancia,
                null,
                3,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                1,
                1);

        comprobarUnidadDb(
                unidad,
                TipoUnidad.APARTAESTUDIO,
                EstadoOperativo.OCUPADO,
                1);


        comprobarHabitacionesDb(
                estancia,
                unidad,
                EstadoOperativo.OCUPADO,
                1
        );

        comprobarOcupantesDb(
                request,
                estancia
        );

        comprobarPagosDb(
                request,
                estancia
        );
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
        comprobarEstanciaDb(
                request,
                estancia,
                null,
                3,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                null,
                1,
                1);

        comprobarUnidadDb(
                unidad,
                TipoUnidad.APARTAMENTO,
                EstadoOperativo.PARCIALMENTE,
                3);


        comprobarHabitacionesDb(
                estancia,
                unidad,
                EstadoOperativo.OCUPADO,
                1
        );

        comprobarOcupantesDb(
                request,
                estancia
        );

        comprobarPagosDb(
                request,
                estancia
        );
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
        comprobarEstanciaDb(
                request,
                estancia,
                null,
                3,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                0);

        comprobarUnidadDb(
                unidad,
                TipoUnidad.APARTAMENTO,
                EstadoOperativo.OCUPADO,
                3);


        comprobarHabitacionesDb(
                estancia,
                unidad,
                EstadoOperativo.OCUPADO,
                3
        );

        comprobarOcupantesDb(
                request,
                estancia
        );

        assertThat(estancia.getPagos()).isNull();

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
        comprobarEstanciaDb(
                request,
                estancia,
                null,
                3,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                1,
                0);

        comprobarUnidadDb(
                unidad,
                TipoUnidad.APARTAESTUDIO,
                EstadoOperativo.OCUPADO,
                1);


        comprobarHabitacionesDb(
                estancia,
                unidad,
                EstadoOperativo.OCUPADO,
                1
        );

        comprobarOcupantesDb(
                request,
                estancia
        );

        assertThat(estancia.getPagos()).isNull();

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
        comprobarEstanciaDb(
                request,
                estancia,
                null,
                3,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                null,
                1,
                0);

        comprobarUnidadDb(
                unidad,
                TipoUnidad.APARTAMENTO,
                EstadoOperativo.PARCIALMENTE,
                3);


        comprobarHabitacionesDb(
                estancia,
                unidad,
                EstadoOperativo.OCUPADO,
                1
        );

        comprobarOcupantesDb(
                request,
                estancia
        );

        assertThat(estancia.getPagos()).isNull();

    }

    @Test
    void falloCreandoEstanciaNuevaApartamentoConEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante cliente = ocupanteRepository.save(clienteData());

        Estancia estanciaExistente = estanciaData(
                any(),
                any(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                any());

        estanciaExistente = estanciaRepository.save(estanciaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.APARTAMENTO,
                unidad.getCodigo(),
                cliente,
                acompanantesData(),
                null,
                any()
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

        // No se creó estanciaData
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        comprobarUnidadDb(unidad, TipoUnidad.APARTAMENTO, EstadoOperativo.OCUPADO, 3);
        comprobarHabitacionesDb(estanciaExistente, unidad, EstadoOperativo.OCUPADO, 3);
    }

    @Test
    void falloCreandoEstanciaNuevaApartaestudioConEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);

        Ocupante cliente = ocupanteRepository.save(clienteData());

        Estancia estanciaExistente = estanciaData(
                any(),
                any(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                any());

        estanciaExistente = estanciaRepository.save(estanciaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                cliente,
                acompanantesData(),
                null,
                any()
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

        // No se creó estanciaData
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        comprobarUnidadDb(unidad, TipoUnidad.APARTAESTUDIO, EstadoOperativo.OCUPADO, 1);
        comprobarHabitacionesDb(estanciaExistente, unidad, EstadoOperativo.OCUPADO, 1);
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
                any(),
                any(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                listaHabitacion,
                any());

        estanciaExistente = estanciaRepository.save(estanciaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                cliente,
                acompanantesData(),
                null,
                any()
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

        // No se creó estanciaData
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        comprobarUnidadDb(unidad, TipoUnidad.APARTAMENTO, EstadoOperativo.OCUPADO, 3);
        comprobarHabitacionesDb(estanciaExistente, unidad, EstadoOperativo.OCUPADO, 3);
    }

    @Test
    void falloCreandoEstanciaNuevaApartamentoConReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Ocupante cliente = ocupanteRepository.save(clienteData());

        Estancia estanciaExistente = estanciaData(
                any(),
                any(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                any());

        estanciaExistente = estanciaRepository.save(estanciaExistente);

        Reserva reservaExistente = reservaData(
                cliente,
                ModoOcupacion.COMPLETO,
                EstadoReserva.CONFIRMADA,
                unidad.getHabitaciones(),
                estanciaExistente
        );

        reservaExistente = reservaRepository.save(reservaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.APARTAMENTO,
                unidad.getCodigo(),
                cliente,
                acompanantesData(),
                null,
                any()
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

        // No se creó estanciaData
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservaAntes);
        comprobarUnidadDb(unidad, TipoUnidad.APARTAMENTO, EstadoOperativo.OCUPADO, 3);
        comprobarHabitacionesDb(estanciaExistente, unidad, EstadoOperativo.OCUPADO, 3);
    }

    @Test
    void falloCreandoEstanciaNuevaApartaestudioConReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);

        Ocupante cliente = ocupanteRepository.save(clienteData());

        Estancia estanciaExistente = estanciaData(
                any(),
                any(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                any());

        estanciaExistente = estanciaRepository.save(estanciaExistente);

        Reserva reservaExistente = reservaData(
                cliente,
                ModoOcupacion.COMPLETO,
                EstadoReserva.CONFIRMADA,
                unidad.getHabitaciones(),
                estanciaExistente
        );

        reservaExistente = reservaRepository.save(reservaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                cliente,
                acompanantesData(),
                null,
                any()
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

        // No se creó estanciaData
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservaAntes);
        comprobarUnidadDb(unidad, TipoUnidad.APARTAESTUDIO, EstadoOperativo.OCUPADO, 1);
        comprobarHabitacionesDb(estanciaExistente, unidad, EstadoOperativo.OCUPADO, 1);
    }

    @Test
    void falloCreandoEstanciaNuevaHabitacionConReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);

        Habitacion habitacion = unidad.getHabitaciones().getFirst();
        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Ocupante cliente = ocupanteRepository.save(clienteData());

        Estancia estanciaExistente = estanciaData(
                any(),
                any(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                listaHabitacion,
                any());

        estanciaExistente = estanciaRepository.save(estanciaExistente);

        Reserva reservaExistente = reservaData(
                cliente,
                ModoOcupacion.INDIVIDUAL,
                EstadoReserva.CONFIRMADA,
                listaHabitacion,
                estanciaExistente
        );

        reservaExistente = reservaRepository.save(reservaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                cliente,
                acompanantesData(),
                null,
                any()
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

        // No se creó estanciaData
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
        assertThat(reservaRepository.count()).isEqualTo(reservaAntes);
        comprobarUnidadDb(unidad, TipoUnidad.APARTAMENTO, EstadoOperativo.OCUPADO, 3);
        comprobarHabitacionesDb(estanciaExistente, unidad, EstadoOperativo.OCUPADO, 3);
    }

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
        estanciaExistente = estanciaRepository.save(estanciaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.APARTAMENTO,
                unidad.getCodigo(),
                clienteNuevo,
                acompanantesData(),
                LocalDateTime.now().plusDays(-2),
                null
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------
        comprobarEstanciaDb(
                request,
                estanciaExistente,
                null,
                3,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                null
        );

        comprobarUnidadDb(
                unidad,
                TipoUnidad.APARTAMENTO,
                EstadoOperativo.OCUPADO,
                3
        );

        comprobarHabitacionesDb(
                estanciaExistente,
                unidad,
                EstadoOperativo.OCUPADO,
                3
        );

        comprobarOcupantesDb(
                request,
                estanciaExistente
        );
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
        estanciaExistente = estanciaRepository.save(estanciaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.APARTAESTUDIO,
                unidad.getCodigo(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().plusDays(-2),
                null
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------
        comprobarEstanciaDb(
                request,
                estanciaExistente,
                null,
                3,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                1,
                null
        );

        comprobarUnidadDb(
                unidad,
                TipoUnidad.APARTAESTUDIO,
                EstadoOperativo.OCUPADO,
                1
        );

        comprobarHabitacionesDb(
                estanciaExistente,
                unidad,
                EstadoOperativo.OCUPADO,
                1
        );

        comprobarOcupantesDb(
                request,
                estanciaExistente
        );
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
        estanciaExistente = estanciaRepository.save(estanciaExistente);

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().plusDays(-2),
                null
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------
        comprobarEstanciaDb(
                request,
                estanciaExistente,
                null,
                3,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                null,
                1,
                null
        );

        comprobarUnidadDb(
                unidad,
                TipoUnidad.APARTAMENTO,
                EstadoOperativo.OCUPADO,
                3
        );

        comprobarHabitacionesDb(
                estanciaExistente,
                unidad,
                EstadoOperativo.OCUPADO,
                3
        );

        comprobarOcupantesDb(
                request,
                estanciaExistente
        );
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
        estanciaExistente = estanciaRepository.save(estanciaExistente);

        PagoNuevoRequestDTO pagoInicial = pagoNuevoRequestDTO(TipoPago.ESTANCIA);
        Pago pagoInicialDb = crearPagoInicialEnEstancia(estanciaExistente, pagoInicial);

        PagoNuevoRequestDTO pagoEditado = pagoNuevoRequestDTOEditado(TipoPago.ESTANCIA);
        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.APARTAMENTO,
                unidad.getCodigo(),
                clienteNuevo,
                acompanantesData(),
                LocalDateTime.now().plusDays(-2),
                pagoEditado
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------
        comprobarEstanciaDb(
                request,
                estanciaExistente,
                null,
                3,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                3,
                2
        );

        comprobarPagoHistoricoYNuevoDb(estanciaExistente, pagoInicialDb, pagoEditado);
        comprobarUnidadDb(unidad, TipoUnidad.APARTAMENTO, EstadoOperativo.OCUPADO, 3);
        comprobarHabitacionesDb(estanciaExistente, unidad, EstadoOperativo.OCUPADO, 3);
        comprobarOcupantesDb(request, estanciaExistente);
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
        estanciaExistente = estanciaRepository.save(estanciaExistente);

        PagoNuevoRequestDTO pagoInicial = pagoNuevoRequestDTO(TipoPago.ESTANCIA);
        Pago pagoInicialDb = crearPagoInicialEnEstancia(estanciaExistente, pagoInicial);

        PagoNuevoRequestDTO pagoEditado = pagoNuevoRequestDTOEditado(TipoPago.ESTANCIA);
        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.APARTAESTUDIO,
                unidad.getCodigo(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().plusDays(-2),
                pagoEditado
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------
        comprobarEstanciaDb(
                request,
                estanciaExistente,
                null,
                3,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                null,
                1,
                2
        );

        comprobarPagoHistoricoYNuevoDb(estanciaExistente, pagoInicialDb, pagoEditado);
        comprobarUnidadDb(unidad, TipoUnidad.APARTAESTUDIO, EstadoOperativo.OCUPADO, 1);
        comprobarHabitacionesDb(estanciaExistente, unidad, EstadoOperativo.OCUPADO, 1);
        comprobarOcupantesDb(request, estanciaExistente);
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
        estanciaExistente = estanciaRepository.save(estanciaExistente);

        PagoNuevoRequestDTO pagoInicial = pagoNuevoRequestDTO(TipoPago.ESTANCIA);
        Pago pagoInicialDb = crearPagoInicialEnEstancia(estanciaExistente, pagoInicial);

        PagoNuevoRequestDTO pagoEditado = pagoNuevoRequestDTOEditado(TipoPago.ESTANCIA);
        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                cliente,
                acompanantesData(),
                LocalDateTime.now().plusDays(-2),
                pagoEditado
        );

        // ---------- WHEN ----------
        estanciaService.editarEstancia(request, estanciaExistente.getId());

        // ---------- THEN ----------
        comprobarEstanciaDb(
                request,
                estanciaExistente,
                null,
                3,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.ACTIVA,
                null,
                1,
                2
        );

        comprobarPagoHistoricoYNuevoDb(estanciaExistente, pagoInicialDb, pagoEditado);
        comprobarUnidadDb(unidad, TipoUnidad.APARTAMENTO, EstadoOperativo.OCUPADO, 3);
        comprobarHabitacionesDb(estanciaExistente, unidad, EstadoOperativo.OCUPADO, 3);
        comprobarOcupantesDb(request, estanciaExistente);
    }

    @Test
    void falloEditandoEstanciaApartamentoConReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = ocupanteRepository.save(clienteData());

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                unidad.getHabitaciones(),
                null
        );
        estanciaExistente = estanciaRepository.save(estanciaExistente);

        Reserva reservaExistente = reservaData(
                cliente,
                ModoOcupacion.COMPLETO,
                EstadoReserva.CONFIRMADA,
                unidad.getHabitaciones(),
                null
        );
        reservaExistente = reservaRepository.save(reservaExistente);

        LocalDateTime entradaAntes = estanciaExistente.getEntradaReal();
        LocalDateTime salidaAntes = estanciaExistente.getSalidaEstimada();
        EstadoEstancia estadoAntes = estanciaExistente.getEstado();
        String notasAntes = estanciaExistente.getNotas();

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.APARTAMENTO,
                unidad.getCodigo(),
                cliente,
                acompanantesData(),
                null,
                null
        );

        Long idEstancia = estanciaExistente.getId();
        long reservasAntes = reservaRepository.count();

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> estanciaService.editarEstancia(request, idEstancia))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede editar la estancia:")
                .hasMessageContaining("existe una reserva para las habitaciones con codigo");

        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(idEstancia).orElseThrow();
        assertThat(estanciaDb.getEntradaReal()).isEqualToIgnoringNanos(entradaAntes);
        assertThat(estanciaDb.getSalidaEstimada()).isEqualToIgnoringNanos(salidaAntes);
        assertThat(estanciaDb.getEstado()).isEqualTo(estadoAntes);
        assertThat(estanciaDb.getNotas()).isEqualTo(notasAntes);
        assertThat(estanciaDb.getOcupantes()).isEmpty();

        assertThat(reservaRepository.count()).isEqualTo(reservasAntes);
        comprobarUnidadDb(unidad, TipoUnidad.APARTAMENTO, EstadoOperativo.DISPONIBLE, 3);
        comprobarHabitacionesDb(estanciaExistente, unidad, EstadoOperativo.DISPONIBLE, 0);
    }

    @Test
    void falloEditandoEstanciaApartaestudioConReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.DISPONIBLE);
        Ocupante cliente = ocupanteRepository.save(clienteData());

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                unidad.getHabitaciones(),
                null
        );
        estanciaExistente = estanciaRepository.save(estanciaExistente);

        Reserva reservaExistente = reservaData(
                cliente,
                ModoOcupacion.COMPLETO,
                EstadoReserva.CONFIRMADA,
                unidad.getHabitaciones(),
                null
        );
        reservaExistente = reservaRepository.save(reservaExistente);

        LocalDateTime entradaAntes = estanciaExistente.getEntradaReal();
        LocalDateTime salidaAntes = estanciaExistente.getSalidaEstimada();
        EstadoEstancia estadoAntes = estanciaExistente.getEstado();
        String notasAntes = estanciaExistente.getNotas();

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.APARTAESTUDIO,
                unidad.getCodigo(),
                cliente,
                acompanantesData(),
                null,
                null
        );

        Long idEstancia = estanciaExistente.getId();
        long reservasAntes = reservaRepository.count();

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> estanciaService.editarEstancia(request, idEstancia))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede editar la estancia:")
                .hasMessageContaining("existe una reserva para las habitaciones con codigo");

        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(idEstancia).orElseThrow();
        assertThat(estanciaDb.getEntradaReal()).isEqualToIgnoringNanos(entradaAntes);
        assertThat(estanciaDb.getSalidaEstimada()).isEqualToIgnoringNanos(salidaAntes);
        assertThat(estanciaDb.getEstado()).isEqualTo(estadoAntes);
        assertThat(estanciaDb.getNotas()).isEqualTo(notasAntes);
        assertThat(estanciaDb.getOcupantes()).isEmpty();

        assertThat(reservaRepository.count()).isEqualTo(reservasAntes);
        comprobarUnidadDb(unidad, TipoUnidad.APARTAESTUDIO, EstadoOperativo.DISPONIBLE, 1);
        comprobarHabitacionesDb(estanciaExistente, unidad, EstadoOperativo.DISPONIBLE, 0);
    }

    @Test
    void falloEditandoEstanciaHabitacionConReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        List<Habitacion> listaHabitacion = new ArrayList<>();
        listaHabitacion.add(habitacion);

        Ocupante cliente = ocupanteRepository.save(clienteData());

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.RESERVADA,
                listaHabitacion,
                null
        );
        estanciaExistente = estanciaRepository.save(estanciaExistente);

        Reserva reservaExistente = reservaData(
                cliente,
                ModoOcupacion.INDIVIDUAL,
                EstadoReserva.CONFIRMADA,
                listaHabitacion,
                null
        );
        reservaExistente = reservaRepository.save(reservaExistente);

        LocalDateTime entradaAntes = estanciaExistente.getEntradaReal();
        LocalDateTime salidaAntes = estanciaExistente.getSalidaEstimada();
        EstadoEstancia estadoAntes = estanciaExistente.getEstado();
        String notasAntes = estanciaExistente.getNotas();

        EstanciaRequestDTO request = estanciaRequestDTO(
                TipoUnidad.HABITACION,
                habitacion.getCodigo(),
                cliente,
                acompanantesData(),
                null,
                null
        );

        Long idEstancia = estanciaExistente.getId();
        long reservasAntes = reservaRepository.count();

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> estanciaService.editarEstancia(request, idEstancia))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede editar la estancia:")
                .hasMessageContaining("existe una reserva para las habitaciones con codigo");

        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(idEstancia).orElseThrow();
        assertThat(estanciaDb.getEntradaReal()).isEqualToIgnoringNanos(entradaAntes);
        assertThat(estanciaDb.getSalidaEstimada()).isEqualToIgnoringNanos(salidaAntes);
        assertThat(estanciaDb.getEstado()).isEqualTo(estadoAntes);
        assertThat(estanciaDb.getNotas()).isEqualTo(notasAntes);
        assertThat(estanciaDb.getOcupantes()).isEmpty();

        assertThat(reservaRepository.count()).isEqualTo(reservasAntes);
        comprobarUnidadDb(unidad, TipoUnidad.APARTAMENTO, EstadoOperativo.DISPONIBLE, 3);
        comprobarHabitacionesDb(estanciaExistente, unidad, EstadoOperativo.DISPONIBLE, 0);
    }

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
        estanciaExistente = estanciaRepository.save(estanciaExistente);

        Long idEstancia = estanciaExistente.getId();

        // ---------- WHEN ----------
        Void result = estanciaService.eliminarEstancia(idEstancia);

        // ---------- THEN ----------
        assertThat(result).isNull();
        comprobarEstadoEstanciaEliminadaDb(idEstancia, EstadoEstancia.CANCELADA);
        comprobarUnidadDb(unidad, TipoUnidad.APARTAMENTO, EstadoOperativo.DISPONIBLE, 3);
        comprobarHabitacionesDb(estanciaExistente, unidad, EstadoOperativo.DISPONIBLE, 0);
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
        estanciaExistente = estanciaRepository.save(estanciaExistente);

        Long idEstancia = estanciaExistente.getId();

        // ---------- WHEN ----------
        Void result = estanciaService.eliminarEstancia(idEstancia);

        // ---------- THEN ----------
        assertThat(result).isNull();
        comprobarEstadoEstanciaEliminadaDb(idEstancia, EstadoEstancia.CANCELADA);
        comprobarUnidadDb(unidad, TipoUnidad.APARTAESTUDIO, EstadoOperativo.DISPONIBLE, 1);
        comprobarHabitacionesDb(estanciaExistente, unidad, EstadoOperativo.DISPONIBLE, 0);
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
        estanciaExistente = estanciaRepository.save(estanciaExistente);

        Long idEstancia = estanciaExistente.getId();

        // ---------- WHEN ----------
        Void result = estanciaService.eliminarEstancia(idEstancia);

        // ---------- THEN ----------
        assertThat(result).isNull();
        comprobarEstadoEstanciaEliminadaDb(idEstancia, EstadoEstancia.CANCELADA);
        comprobarUnidadDb(unidad, TipoUnidad.APARTAMENTO, EstadoOperativo.PARCIALMENTE, 3);
        comprobarHabitacionesDb(estanciaExistente, unidad, EstadoOperativo.DISPONIBLE, 2);
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

    private PagoNuevoRequestDTO pagoNuevoRequestDTO(TipoPago tipoPago) {
        PagoNuevoRequestDTO pago = new PagoNuevoRequestDTO();

        pago.setTipoPago(tipoPago);
        pago.setMonto(java.math.BigDecimal.valueOf(500000));
        pago.setMedioPago(com.hotel.models.enums.MedioPago.EFECTIVO);
        pago.setFecha(LocalDateTime.now());
        pago.setEstado(com.hotel.models.enums.EstadoPago.COMPLETADO);

        return pago;
    }

    private PagoNuevoRequestDTO pagoNuevoRequestDTOEditado(TipoPago tipoPago) {
        PagoNuevoRequestDTO pago = new PagoNuevoRequestDTO();

        pago.setTipoPago(tipoPago);
        pago.setMonto(java.math.BigDecimal.valueOf(750000));
        pago.setMedioPago(com.hotel.models.enums.MedioPago.TRANSFERENCIA_BANCARIA);
        pago.setFecha(LocalDateTime.now().plusHours(3));
        pago.setEstado(com.hotel.models.enums.EstadoPago.PENDIENTE);

        return pago;
    }

    private Pago crearPagoInicialEnEstancia(Estancia estancia, PagoNuevoRequestDTO pagoRequest) {
        Pago pagoInicial = new Pago();
        pagoInicial.setTipoPago(pagoRequest.getTipoPago());
        pagoInicial.setMonto(pagoRequest.getMonto());
        pagoInicial.setMedioPago(pagoRequest.getMedioPago());
        pagoInicial.setFecha(pagoRequest.getFecha());
        pagoInicial.setFechaCreacion(LocalDateTime.now());
        pagoInicial.setEstado(pagoRequest.getEstado());
        pagoInicial.setEstancia(estancia);
        return pagoRepository.save(pagoInicial);
    }

    private void comprobarEstanciaDb(
            EstanciaRequestDTO request,
            Estancia estancia,
            Reserva reserva,
            Integer totalOcupantes,
            ModoOcupacion modoOcupacion,
            EstadoEstancia estadoEstancia,
            BigDecimal precioTotal,
            Integer totalHabitaciones,
            Integer totalPagos
    ) {
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();

        assertThat(estanciaDb.getId()).isNotNull();
        assertThat(estanciaDb.getCodigoFolio()).isNotBlank();

        // ---- Reserva ----
        if (reserva == null) {
            assertThat(estanciaDb.getReserva()).isNull();
        } else {
            assertThat(estanciaDb.getReserva()).isNotNull();
            assertThat(estanciaDb.getReserva().getId()).isEqualTo(reserva.getId());
        }

        // ---- Ocupantes ----
        if (totalOcupantes != null) {
            assertThat(estanciaDb.getOcupantes()).hasSize(totalOcupantes);
        }

        // ---- Fechas ----
        assertThat(estanciaDb.getEntradaReal()).isEqualToIgnoringNanos(request.getEntradaReal());
        assertThat(estanciaDb.getSalidaEstimada()).isEqualToIgnoringNanos(request.getSalidaEstimada());

        // ---- Estados ----
        assertThat(estanciaDb.getModoOcupacion()).isEqualTo(modoOcupacion);
        assertThat(estanciaDb.getEstado()).isEqualTo(estadoEstancia);

        // ---- Precio total ----
        if (precioTotal == null) {
            assertThat(estanciaDb.getPrecioTotal()).isNull();
        } else {
            assertThat(estanciaDb.getPrecioTotal()).isNotNull();
            assertThat(estanciaDb.getPrecioTotal()).isEqualByComparingTo(precioTotal);
        }

        // ---- Notas ----
        assertThat(estanciaDb.getNotas()).contains(request.getNotas());

        // ---- Habitaciones ----
        if (totalHabitaciones != null) {
            assertThat(estanciaDb.getHabitaciones()).hasSize(totalHabitaciones);
        }

        // ---- Pagos ----
        if (totalPagos != null) {
            assertThat(estanciaDb.getPagos()).hasSize(totalPagos);
        }
    }

    private void comprobarUnidadDb(
            Unidad unidad,
            TipoUnidad tipoUnidad,
            EstadoOperativo estadoOperativo,
            Integer totalHabitaciones
    ) {
        entityManager.flush();
        entityManager.clear();

        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        // ---- Campos propios ----
        assertThat(unidadDb.getId()).isNotNull();
        assertThat(unidadDb.getCodigo()).isEqualTo(unidad.getCodigo());
        assertThat(unidadDb.getTipo()).isEqualTo(tipoUnidad);
        assertThat(unidadDb.getEstadoOperativo()).isEqualTo(estadoOperativo);

        // ---- Habitaciones (solo conteo) ----
        if (totalHabitaciones != null) {
            assertThat(unidadDb.getHabitaciones()).hasSize(totalHabitaciones);
        }
    }

    private void comprobarHabitacionesDb(
            Estancia estancia,
            Unidad unidad,
            EstadoOperativo estadoOperativo,
            Integer totalOcupadasEnUnidad
    ) {
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

        // 1) Validar cada habitación asociada a la estanciaData
        assertThat(estanciaDb.getHabitaciones()).allSatisfy(h -> {
            assertThat(h.getId()).isNotNull();
            assertThat(h.getCodigo()).isNotBlank();
            assertThat(h.getEstadoOperativo()).isEqualTo(estadoOperativo);

            // pertenencia a la unidadData
            assertThat(h.getUnidad()).isNotNull();
            assertThat(h.getUnidad().getId()).isEqualTo(unidadDb.getId());
            assertThat(h.getUnidad().getCodigo()).isEqualTo(unidadDb.getCodigo());
        });

        // 2) Validar cuántas habitaciones ocupadas hay en TODA la unidadData (si aplica)
        Integer ocupadas = Math.toIntExact(
                unidadDb.getHabitaciones().stream()
                        .filter(h -> h.getEstadoOperativo() == EstadoOperativo.OCUPADO)
                        .count());

        assertThat(ocupadas).isEqualTo(totalOcupadasEnUnidad);
    }


    private void comprobarOcupantesDb(
            EstanciaRequestDTO request,
            Estancia estancia
    ) {
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();


        // ---- Validar que exista el clienteData principal por ID (request.idCliente) ----
        assertThat(estanciaDb.getOcupantes())
                .extracting(Ocupante::getId)
                .contains(request.getIdCliente());

        // ---- Validar acompañantes por IDs (request.idAcompanantes) ----
        if (request.getIdAcompanantes() != null && !request.getIdAcompanantes().isEmpty()) {
            assertThat(estanciaDb.getOcupantes())
                    .extracting(Ocupante::getId)
                    .containsAll(request.getIdAcompanantes());
        }

        // ---- Regla básica de calidad: que no haya duplicados ----
        assertThat(estanciaDb.getOcupantes())
                .extracting(Ocupante::getId)
                .doesNotHaveDuplicates();
    }

    private void comprobarPagosDb(
            EstanciaRequestDTO request,
            Estancia estancia
    ) {
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();

        // Debe existir al menos un pago asociado a la estanciaData
        assertThat(estanciaDb.getPagos()).isNotEmpty();

        PagoNuevoRequestDTO pagoRequest = request.getPago();
        assertThat(pagoRequest)
                .as("El request debe contener un pago para validar pagos en BD")
                .isNotNull();

        // Validar que al menos un pago coincida con el request
        assertThat(estanciaDb.getPagos()).anySatisfy(pagoDb -> {

            // Relación correcta
            assertThat(pagoDb.getEstancia()).isNotNull();
            assertThat(pagoDb.getEstancia().getId()).isEqualTo(estanciaDb.getId());

            // Campos del pago
            assertThat(pagoDb.getTipoPago()).isEqualTo(pagoRequest.getTipoPago());
            assertThat(pagoDb.getMedioPago()).isEqualTo(pagoRequest.getMedioPago());
            assertThat(pagoDb.getEstado()).isEqualTo(pagoRequest.getEstado());

            // BigDecimal: comparar por valor, no por escala
            assertThat(pagoDb.getMonto())
                    .isNotNull()
                    .isEqualByComparingTo(pagoRequest.getMonto());

            // Fecha: la BD puede truncar nanos
            assertThat(pagoDb.getFecha())
                    .isEqualToIgnoringNanos(pagoRequest.getFecha());
        });
    }

    private void comprobarPagoHistoricoYNuevoDb(
            Estancia estancia,
            Pago pagoInicial,
            PagoNuevoRequestDTO pagoRequest
    ) {
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();

        assertThat(estanciaDb.getPagos())
                .as("La estancia editada debe conservar el pago historico y el pago nuevo")
                .hasSize(2);

        Pago pagoHistoricoDb = estanciaDb.getPagos().stream()
                .filter(pagoDb -> pagoDb.getId().equals(pagoInicial.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se encontró el pago histórico original"));

        assertThat(pagoHistoricoDb.getEstancia()).isNotNull();
        assertThat(pagoHistoricoDb.getEstancia().getId()).isEqualTo(estanciaDb.getId());
        assertThat(pagoHistoricoDb.getEstado())
                .as("El pago anterior debe cambiar de estado para conservar historial")
                .isEqualTo(com.hotel.models.enums.EstadoPago.REEMBOLSADO);

        Pago pagoNuevoDb = estanciaDb.getPagos().stream()
                .filter(pagoDb -> !pagoDb.getId().equals(pagoInicial.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se encontró el pago nuevo generado en la edición"));

        assertThat(pagoNuevoDb.getEstancia()).isNotNull();
        assertThat(pagoNuevoDb.getEstancia().getId()).isEqualTo(estanciaDb.getId());
        assertThat(pagoNuevoDb.getTipoPago()).isEqualTo(pagoRequest.getTipoPago());
        assertThat(pagoNuevoDb.getMedioPago()).isEqualTo(pagoRequest.getMedioPago());
        assertThat(pagoNuevoDb.getEstado()).isEqualTo(pagoRequest.getEstado());
        assertThat(pagoNuevoDb.getMonto()).isEqualByComparingTo(pagoRequest.getMonto());
        assertThat(pagoNuevoDb.getFecha()).isEqualToIgnoringNanos(pagoRequest.getFecha());
    }

    private void comprobarEstadoEstanciaEliminadaDb(Long idEstancia, EstadoEstancia estadoEsperado) {
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(idEstancia).orElseThrow();
        assertThat(estanciaDb.getEstado()).isEqualTo(estadoEsperado);
    }
}
