package com.hotel.services;

import com.hotel.dtos.pago.PagoNuevoRequestDTO;
import com.hotel.dtos.reserva.ReservaNuevaRequestDTO;
import com.hotel.models.Estancia;
import com.hotel.models.Habitacion;
import com.hotel.models.Ocupante;
import com.hotel.models.Pago;
import com.hotel.models.Reserva;
import com.hotel.models.Unidad;
import com.hotel.models.enums.CanalReserva;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.EstadoPago;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.models.enums.MedioPago;
import com.hotel.models.enums.ModoOcupacion;
import com.hotel.models.enums.TipoPago;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.EstanciaRepository;
import com.hotel.repositories.HabitacionRepository;
import com.hotel.repositories.OcupanteRepository;
import com.hotel.repositories.PagoRepository;
import com.hotel.repositories.ReservaRepository;
import com.hotel.repositories.UnidadRepository;
import com.hotel.services.support.AbstractServiceIT;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static com.hotel.testdata.EstanciaTestData.estanciaData;
import static com.hotel.testdata.OcupanteTestData.clienteData;
import static com.hotel.testdata.PagoTestData.pagoNuevoRequestDTO;
import static com.hotel.testdata.ReservaTestData.reservaData;
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
    private PagoRepository pagoRepository;

    /**
     * crearReserva(ReservaNuevaRequestDTO request)
     */
    @Test
    void exitoCreandoReservaNuevaApartamentoConPago_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante cliente = clienteData();

        LocalDateTime entrada = LocalDateTime.now().plusDays(2);
        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.RESERVA);
        ReservaNuevaRequestDTO request = reservaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, entrada, pagoRequest);

        // ---------- WHEN ----------
        Reserva reserva = reservaService.crearReserva(request);

        // ---------- THEN (validación real en BD) ----------
        entityManager.flush();
        entityManager.clear();

        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reserva.getId()).orElseThrow();
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
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                null,
                3,
                1);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                500000,
                0,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                0
        );

        assertThat(unidadDb.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
    }

    @Test
    void exitoCreandoReservaNuevaApartaestudioConPago_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        Ocupante cliente = clienteData();

        LocalDateTime entrada = LocalDateTime.now().plusDays(2);
        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.RESERVA);
        ReservaNuevaRequestDTO request = reservaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, entrada, pagoRequest);

        // ---------- WHEN ----------
        Reserva reserva = reservaService.crearReserva(request);

        // ---------- THEN (validación real en BD) ----------
        entityManager.flush();
        entityManager.clear();

        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reserva.getId()).orElseThrow();
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
                1
        );

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                0,
                null,
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                null,
                1,
                1);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                500000,
                0,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                0
        );

        assertThat(unidadDb.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
    }

    @Test
    void exitoCreandoReservaNuevaHabitacionConPago_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        Ocupante cliente = clienteData();

        LocalDateTime entrada = LocalDateTime.now().plusDays(2);
        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.RESERVA);
        ReservaNuevaRequestDTO request = reservaRequestDTO(TipoUnidad.HABITACION, habitacion.getCodigo(), cliente, entrada, pagoRequest);

        // ---------- WHEN ----------
        Reserva reserva = reservaService.crearReserva(request);

        // ---------- THEN (validación real en BD) ----------
        entityManager.flush();
        entityManager.clear();

        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reserva.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

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
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.RESERVADA,
                null,
                1,
                1);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                500000,
                0,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.MODIFICADO,
                0,
                0
        );

        assertThat(unidadDb.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
    }

    @Test
    void exitoCreandoReservaNuevaApartamentoSinPago_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);

        Ocupante cliente = clienteData();

        LocalDateTime entrada = LocalDateTime.now().plusDays(2);
        ReservaNuevaRequestDTO request = reservaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, entrada, null);

        // ---------- WHEN ----------
        Reserva reserva = reservaService.crearReserva(request);

        // ---------- THEN (validación real en BD) ----------
        entityManager.flush();
        entityManager.clear();

        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reserva.getId()).orElseThrow();
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
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                null,
                3,
                0);

        assertThat(unidadDb.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
    }

    @Test
    void exitoCreandoReservaNuevaApartaestudioSinPago_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.DISPONIBLE);

        Ocupante cliente = clienteData();

        LocalDateTime entrada = LocalDateTime.now().plusDays(2);
        ReservaNuevaRequestDTO request = reservaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, entrada, null);

        // ---------- WHEN ----------
        Reserva reserva = reservaService.crearReserva(request);

        // ---------- THEN (validación real en BD) ----------
        entityManager.flush();
        entityManager.clear();

        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reserva.getId()).orElseThrow();
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
                1
        );

        comprobarEstanciaDb(
                estanciaDb,
                reservaDb,
                0,
                null,
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                null,
                1,
                0);

        assertThat(unidadDb.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
    }

    @Test
    void exitoCreandoReservaNuevaHabitacionSinPago_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        Ocupante cliente = clienteData();

        LocalDateTime entrada = LocalDateTime.now().plusDays(2);
        ReservaNuevaRequestDTO request = reservaRequestDTO(TipoUnidad.HABITACION, habitacion.getCodigo(), cliente, entrada, null);

        // ---------- WHEN ----------
        Reserva reserva = reservaService.crearReserva(request);

        // ---------- THEN (validación real en BD) ----------
        entityManager.flush();
        entityManager.clear();

        Reserva reservaDb = reservaRepository.findById(reserva.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findByReserva_Id(reserva.getId()).orElseThrow();
        Unidad unidadDb = unidadRepository.findById(unidad.getId()).orElseThrow();

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
                request.getSalidaEstimada(),
                null,
                request.getNotas(),
                ModoOcupacion.INDIVIDUAL,
                EstadoEstancia.RESERVADA,
                null,
                1,
                0);

        assertThat(unidadDb.getEstadoOperativo()).isEqualTo(EstadoOperativo.DISPONIBLE);
    }

    @Test
    void falloCreandoReservaNuevaApartamentoConReserva_test() {

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

        LocalDateTime entrada = reservaExistente.getEntradaEstimada().plusDays(1);
        ReservaNuevaRequestDTO request = reservaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, entrada, null);

        long reservasAntes = reservaRepository.count();
        long estanciasAntes = estanciaRepository.count();

        // ---------- WHEN + THEN (excepción) ----------
        assertThatThrownBy(() -> reservaService.crearReserva(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No es posible crear la reserva:")
                .hasMessageContaining("existe una reserva para las habitaciones con codigo");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();

        assertThat(reservaRepository.count()).isEqualTo(reservasAntes);
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
    }

    @Test
    void falloCreandoReservaNuevaApartaestudioConReserva_test() {

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

        LocalDateTime entrada = reservaExistente.getEntradaEstimada().plusDays(1);
        ReservaNuevaRequestDTO request = reservaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, entrada, null);

        long reservasAntes = reservaRepository.count();
        long estanciasAntes = estanciaRepository.count();

        // ---------- WHEN + THEN (excepción) ----------
        assertThatThrownBy(() -> reservaService.crearReserva(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No es posible crear la reserva:")
                .hasMessageContaining("existe una reserva para las habitaciones con codigo");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();

        assertThat(reservaRepository.count()).isEqualTo(reservasAntes);
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
    }

    @Test
    void falloCreandoReservaNuevaHabitacionConReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        Ocupante cliente = clienteData();

        Reserva reservaExistente = reservaData(
                cliente,
                ModoOcupacion.COMPLETO,
                EstadoReserva.CONFIRMADA,
                unidad.getHabitaciones(),
                null
        );
        reservaRepository.save(reservaExistente);

        LocalDateTime entrada = reservaExistente.getEntradaEstimada().plusDays(1);
        ReservaNuevaRequestDTO request = reservaRequestDTO(TipoUnidad.HABITACION, habitacion.getCodigo(), cliente, entrada, null);

        long reservasAntes = reservaRepository.count();
        long estanciasAntes = estanciaRepository.count();

        // ---------- WHEN + THEN (excepción) ----------
        assertThatThrownBy(() -> reservaService.crearReserva(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No es posible crear la reserva:")
                .hasMessageContaining("existe una reserva para las habitaciones con codigo");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();

        assertThat(reservaRepository.count()).isEqualTo(reservasAntes);
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
    }

    @Test
    void falloCreandoReservaNuevaApartamentoConEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Ocupante cliente = ocupanteRepository.save(clienteData());

        Estancia estanciaExistente = estanciaData(
                null,
                null,
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidad.getHabitaciones(),
                null);

        estanciaRepository.save(estanciaExistente);


        LocalDateTime entrada = LocalDateTime.now().plusDays(1);
        ReservaNuevaRequestDTO request = reservaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, entrada, null);

        long reservasAntes = reservaRepository.count();
        long estanciasAntes = estanciaRepository.count();

        // ---------- WHEN + THEN (excepción) ----------
        assertThatThrownBy(() -> reservaService.crearReserva(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No es posible crear la reserva:")
                .hasMessageContaining("existe una estancia para las habitaciones con codigo");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();

        assertThat(reservaRepository.count()).isEqualTo(reservasAntes);
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
    }

    @Test
    void falloCreandoReservaNuevaApartaestudioConEstancia_test() {

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

        LocalDateTime entrada = LocalDateTime.now().plusDays(1);
        ReservaNuevaRequestDTO request = reservaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, entrada, null);

        long reservasAntes = reservaRepository.count();
        long estanciasAntes = estanciaRepository.count();

        // ---------- WHEN + THEN (excepción) ----------
        assertThatThrownBy(() -> reservaService.crearReserva(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No es posible crear la reserva:")
                .hasMessageContaining("existe una estancia para las habitaciones con codigo");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();

        assertThat(reservaRepository.count()).isEqualTo(reservasAntes);
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
    }

    @Test
    void falloCreandoReservaNuevaHabitacionConEstancia_test() {

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

        LocalDateTime entrada = LocalDateTime.now().plusDays(1);
        ReservaNuevaRequestDTO request = reservaRequestDTO(TipoUnidad.HABITACION, habitacion.getCodigo(), cliente, entrada, null);

        long reservasAntes = reservaRepository.count();
        long estanciasAntes = estanciaRepository.count();

        // ---------- WHEN + THEN (excepción) ----------
        assertThatThrownBy(() -> reservaService.crearReserva(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No es posible crear la reserva:")
                .hasMessageContaining("existe una estancia para las habitaciones con codigo");

        // ---------- THEN (no efectos en BD) ----------
        entityManager.flush();
        entityManager.clear();

        assertThat(reservaRepository.count()).isEqualTo(reservasAntes);
        assertThat(estanciaRepository.count()).isEqualTo(estanciasAntes);
    }

    /**
     * Metodos auxiliares para crear datos de prueba
     */
    //falta revision
    @Test
    void validarCambioDeCodigoHabitacion_test() {
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacion = unidad.getHabitaciones().getFirst();

        String nuevoCodigo = randomCodigo("HB-");
        habitacion.setCodigo(nuevoCodigo);
        habitacionRepository.save(habitacion);

        entityManager.flush();
        entityManager.clear();

        Habitacion habitacionDb = habitacionRepository.findById(habitacion.getId()).orElseThrow();
        assertThat(habitacionDb.getCodigo()).isEqualTo(nuevoCodigo);
    }

    private ReservaNuevaRequestDTO reservaRequestDTO(
            TipoUnidad tipoUnidad,
            String codigo,
            Ocupante cliente,
            LocalDateTime entradaEstimada,
            PagoNuevoRequestDTO pago) {
        ReservaNuevaRequestDTO request = new ReservaNuevaRequestDTO();
        request.setTipoUnidad(tipoUnidad);
        request.setCodigo(codigo);
        request.setIdOcupante(cliente.getId());
        request.setNumeroPersonas(2);
        request.setEntradaEstimada(entradaEstimada);
        request.setSalidaEstimada(entradaEstimada.plusDays(2));
        request.setCanalReserva(CanalReserva.MOSTRADOR);
        request.setNotas("Reserva de prueba");
        request.setPago(pago);
        return request;
    }
}
