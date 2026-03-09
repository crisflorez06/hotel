package com.hotel.services;

import com.hotel.dtos.pago.CalcularPagoDTO;
import com.hotel.dtos.pago.PagoDTO;
import com.hotel.dtos.pago.PagoNuevoRequestDTO;
import com.hotel.mappers.PagoMapper;
import com.hotel.models.*;
import com.hotel.models.enums.*;
import com.hotel.repositories.AuditoriaEventoRepository;
import com.hotel.services.support.AbstractServiceIT;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static com.hotel.testdata.PagoTestData.*;
import static com.hotel.testutils.AssertionsHelper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PagoServiceIT extends AbstractServiceIT {

    @Autowired
    private PagoService pagoService;

    @Autowired
    private AjusteTemporadaService ajusteTemporadaService;

    @Autowired
    private AuditoriaEventoRepository eventoRepository;

    //------------------- PRUEBAS DE CREACIÓN DE PAGOS -------------------

    @Test
    void exitoCreandoPagoTipoAnticipoReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        Estancia estancia = reserva.getEstancia();
        PagoNuevoRequestDTO request = pagoNuevoRequestDTO(TipoPago.ANTICIPO_RESERVA);

        // ---------- WHEN ----------
        PagoDTO pago = pagoService.crearPago(request, estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Pago pagoDb = pagoRepository.findById(pago.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.PAGO,
                pagoDb.getId()).orElseThrow();

        comprobarPagosDb(
                estanciaDb.getPagos(),
                request.getMonto(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.ELIMINADO,
                0,
                0,
                0,
                1,
                0
        );

        comprobarPagoDb(pagoDb, request.getMonto(), estanciaDb, request.getTipoPago(), EstadoPago.COMPLETADO);

        comprobarEventoDb(
                eventoDb,
                TipoEvento.CREACION_PAGO,
                null,
                reserva.getCodigo(),
                4
        );
    }

    @Test
    void exitoCreandoPagoTipoAnticipoEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        PagoNuevoRequestDTO request = pagoNuevoRequestDTO(TipoPago.ANTICIPO_ESTANCIA);

        // ---------- WHEN ----------
        PagoDTO pago = pagoService.crearPago(request, estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Pago pagoDb = pagoRepository.findById(pago.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.PAGO,
                pagoDb.getId()).orElseThrow();

        comprobarPagosDb(
                estanciaDb.getPagos(),
                request.getMonto(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.ELIMINADO,
                0,
                0,
                1,
                0,
                0
        );

        comprobarPagoDb(pagoDb, request.getMonto(), estanciaDb, request.getTipoPago(), EstadoPago.COMPLETADO);


        comprobarEventoDb(
                eventoDb,
                TipoEvento.CREACION_PAGO,
                estancia.getCodigoFolio(),
                null,
                4
        );
    }

    @Test
    void exitoCreandoPagoTipoEstanciaCompletada_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.FINALIZADA);
        PagoNuevoRequestDTO request = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
        request.setEstado(EstadoPago.COMPLETADO);

        // ---------- WHEN ----------
        PagoDTO pago = pagoService.crearPago(request, estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Pago pagoDb = pagoRepository.findById(pago.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.PAGO,
                pagoDb.getId()).orElseThrow();

        comprobarPagosDb(
                estanciaDb.getPagos(),
                request.getMonto(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.ELIMINADO,
                0,
                1,
                0,
                0,
                0
        );

        comprobarPagoDb(pagoDb, request.getMonto(), estanciaDb, request.getTipoPago(), EstadoPago.COMPLETADO);


        comprobarEventoDb(
                eventoDb,
                TipoEvento.CREACION_PAGO,
                estancia.getCodigoFolio(),
                null,
                4
        );
    }

    @Test
    void exitoCreandoPagoTipoEstanciaCompletadaConEstadoPendiente_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.FINALIZADA);
        PagoNuevoRequestDTO request = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
        request.setEstado(EstadoPago.PENDIENTE);

        // ---------- WHEN ----------
        PagoDTO pago = pagoService.crearPago(request, estancia.getId());


        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Pago pagoDb = pagoRepository.findById(pago.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.PAGO,
                pagoDb.getId()).orElseThrow();

        comprobarPagosDb(
                estanciaDb.getPagos(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                request.getMonto(),
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.PENDIENTE,
                1,
                1,
                0,
                0,
                0
        );

        comprobarPagoDb(pagoDb, request.getMonto(), estanciaDb, request.getTipoPago(), EstadoPago.PENDIENTE);


        comprobarEventoDb(
                eventoDb,
                TipoEvento.CREACION_PAGO,
                estancia.getCodigoFolio(),
                null,
                4
        );
    }

    @Test
    void exitoCreandoPagoTipoAnticipoReservaConPagosAnteriormente_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        Estancia estancia = reserva.getEstancia();
        BigDecimal monto = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_RESERVA).getMonto();
        PagoNuevoRequestDTO request = pagoNuevoRequestDTO(TipoPago.ANTICIPO_RESERVA);

        // ---------- WHEN ----------
        PagoDTO pago = pagoService.crearPago(request, estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Pago pagoDb = pagoRepository.findById(pago.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.PAGO,
                pagoDb.getId()).orElseThrow();

        comprobarPagosDb(
                estanciaDb.getPagos(),
                request.getMonto().add(monto),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                2,
                EstadoPago.ELIMINADO,
                0,
                0,
                0,
                2,
                0
        );

        comprobarPagoDb(pagoDb, request.getMonto(), estanciaDb, request.getTipoPago(), EstadoPago.COMPLETADO);


        comprobarEventoDb(
                eventoDb,
                TipoEvento.CREACION_PAGO,
                null,
                reserva.getCodigo(),
                4
        );
    }

    @Test
    void exitoCreandoPagoTipoAnticipoEstanciaConPagosAnteriormente_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        PagoNuevoRequestDTO request = pagoNuevoRequestDTO(TipoPago.ANTICIPO_ESTANCIA);
        BigDecimal monto1 = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_RESERVA).getMonto();
        BigDecimal monto2 = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA).getMonto();


        // ---------- WHEN ----------
        PagoDTO pago = pagoService.crearPago(request, estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Pago pagoDb = pagoRepository.findById(pago.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.PAGO,
                pagoDb.getId()).orElseThrow();

        comprobarPagosDb(
                estanciaDb.getPagos(),
                request.getMonto().add(monto1).add(monto2),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                3,
                EstadoPago.ELIMINADO,
                0,
                0,
                2,
                1,
                0
        );

        comprobarPagoDb(pagoDb, request.getMonto(), estanciaDb, request.getTipoPago(), EstadoPago.COMPLETADO);


        comprobarEventoDb(
                eventoDb,
                TipoEvento.CREACION_PAGO,
                estancia.getCodigoFolio(),
                null,
                4
        );
    }

    @Test
    void falloCreandoPagoTipoAnticipoReservaPorReservaDiferenteAConfirmada_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CANCELADA);
        Estancia estancia = reserva.getEstancia();
        PagoNuevoRequestDTO request = pagoNuevoRequestDTO(TipoPago.ANTICIPO_RESERVA);

        Long pagosAntes = pagoRepository.count();
        Long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> pagoService.crearPago(request, estancia.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede crear un pago de tipo ANTICIPO_RESERVA para una reserva que no está CONFIRMADA. El estado actual de la reserva es: ");


        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();

        assertThat(pagoRepository.count()).isEqualTo(pagosAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);



        comprobarPagosDb(
                estanciaDb.getPagos(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                0,
                0,
                0,
                0,
                0
        );


    }

    @Test
    void falloCreandoPagoTipoAnticipoReservaSinReservaAsociada_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        PagoNuevoRequestDTO request = pagoNuevoRequestDTO(TipoPago.ANTICIPO_RESERVA);

        Long pagosAntes = pagoRepository.count();
        Long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> pagoService.crearPago(request, estancia.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede crear un pago de tipo ANTICIPO_RESERVA para una estancia que no tiene una reserva asociada");

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();

        assertThat(pagoRepository.count()).isEqualTo(pagosAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                0,
                0,
                0,
                0,
                0
        );
    }

    @Test
    void falloCreandoPagoTipoAnticipoEstanciaPorEstanciaDiferenteAActiva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.FINALIZADA);
        PagoNuevoRequestDTO request = pagoNuevoRequestDTO(TipoPago.ANTICIPO_ESTANCIA);

        Long pagosAntes = pagoRepository.count();
        Long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> pagoService.crearPago(request, estancia.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede crear un pago de tipo ANTICIPO_ESTANCIA para una estancia que no está activada. El estado actual de la estancia es: ");


        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();

        assertThat(pagoRepository.count()).isEqualTo(pagosAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);



        comprobarPagosDb(
                estanciaDb.getPagos(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                0,
                0,
                0,
                0,
                0
        );


    }

    @Test
    void falloCreandoPagoTipoEstanciaFinalizadaPorEstanciaDiferenteAFinalizada_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.CANCELADA);
        PagoNuevoRequestDTO request = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);

        Long pagosAntes = pagoRepository.count();
        Long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> pagoService.crearPago(request, estancia.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede crear un pago de tipo ESTANCIA_COMPLETADA para una estancia que no está FINALIZADA. El estado actual de la estancia es: ");


        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();

        assertThat(pagoRepository.count()).isEqualTo(pagosAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);



        comprobarPagosDb(
                estanciaDb.getPagos(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                0,
                0,
                0,
                0,
                0
        );


    }

    @Test
    void falloCreandoPagoTipoEstanciaFinalizadaSinEstado_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.FINALIZADA);
        PagoNuevoRequestDTO request = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);

        Long pagosAntes = pagoRepository.count();
        Long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> pagoService.crearPago(request, estancia.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El estado del pago es obligatorio para crear un pago de tipo ESTANCIA_COMPLETADA");


        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();

        assertThat(pagoRepository.count()).isEqualTo(pagosAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);



        comprobarPagosDb(
                estanciaDb.getPagos(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                0,
                0,
                0,
                0,
                0
        );


    }

    @Test
    void falloCreandoPagoTipoEstanciaFinalizadaConEstadoInvalido_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.FINALIZADA);
        PagoNuevoRequestDTO request = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
        request.setEstado(EstadoPago.ELIMINADO);

        Long pagosAntes = pagoRepository.count();
        Long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> pagoService.crearPago(request, estancia.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El estado del pago de tipo ESTANCIA_COMPLETADA debe ser COMPLETADO o PENDIENTE");


        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();

        assertThat(pagoRepository.count()).isEqualTo(pagosAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);



        comprobarPagosDb(
                estanciaDb.getPagos(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                0,
                0,
                0,
                0,
                0
        );


    }

    @Test
    void falloCreandoPagoTipoEstanciaFinalizadaPorPagoFinalizarEstanciaAnterior_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.FINALIZADA);
        BigDecimal monto = crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA_COMPLETADA).getMonto();
        PagoNuevoRequestDTO request = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);

        Long pagosAntes = pagoRepository.count();
        Long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> pagoService.crearPago(request, estancia.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe un pago de tipo ESTANCIA_COMPLETADA para esta estancia, no se puede crear otro");


        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();

        assertThat(pagoRepository.count()).isEqualTo(pagosAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);



        comprobarPagosDb(
                estanciaDb.getPagos(),
                monto,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.ELIMINADO,
                0,
                1,
                0,
                0,
                0
        );


    }

    @Test
    void falloCreandoPagoTipoCambioUnidad_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        PagoNuevoRequestDTO request = pagoNuevoRequestDTO(TipoPago.CAMBIO_UNIDAD);

        Long pagosAntes = pagoRepository.count();
        Long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> pagoService.crearPago(request, estancia.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("El tipo de pago CAMBIO_UNIDAD no es válido para crear un pago");


        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();

        assertThat(pagoRepository.count()).isEqualTo(pagosAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);



        comprobarPagosDb(
                estanciaDb.getPagos(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                0,
                0,
                0,
                0,
                0
        );


    }

    //------------------- PRUEBAS DE CREACIÓN DE PAGO POR CAMBIO DE UNIDAD -------------------

    @Test
    void exitoCreandoPagoPorCambioUnidadEnEstanciaActiva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        long pagosAntes = pagoRepository.count();
        long eventosAntes = eventoRepository.count();
        BigDecimal montoEsperado = calcularMontoEsperadoCambioUnidadAntesDeCrear(estancia, TipoUnidad.APARTAMENTO);

        // ---------- WHEN ----------
        Pago pago = pagoService.crearPagoPorCambioUnidad(estancia, TipoUnidad.APARTAMENTO);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Pago pagoDb = pagoRepository.findById(pago.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.PAGO,
                pagoDb.getId()).orElseThrow();

        assertThat(pagoRepository.count()).isEqualTo(pagosAntes + 1);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes + 1);
        comprobarPagoDb(pagoDb, montoEsperado, estanciaDb, TipoPago.CAMBIO_UNIDAD, EstadoPago.PENDIENTE);
        assertThat(pagoDb.getMedioPago()).isNull();

        comprobarEventoDb(
                eventoDb,
                TipoEvento.CREACION_PAGO,
                estanciaDb.getCodigoFolio(),
                null,
                3
        );
    }

    @Test
    void exitoCreandoPagoPorCambioUnidadSinPersonasAdicionales_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        estancia.setOcupantes(new ArrayList<>(estancia.getOcupantes().stream().limit(2).toList()));

        estanciaRepository.save(estancia);
        BigDecimal montoEsperado = calcularMontoEsperadoCambioUnidadAntesDeCrear(estancia, TipoUnidad.HABITACION);

        // ---------- WHEN ----------
        Pago pago = pagoService.crearPagoPorCambioUnidad(estancia, TipoUnidad.HABITACION);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Pago pagoDb = pagoRepository.findById(pago.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();

        comprobarPagoDb(pagoDb, montoEsperado, estanciaDb, TipoPago.CAMBIO_UNIDAD, EstadoPago.PENDIENTE);
        assertThat(estanciaDb.getOcupantes()).hasSize(2);
    }


    @Test
    void exitoCreandoPagoPorCambioUnidadEnEstanciaExcedida_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.EXCEDIDA);
        BigDecimal montoEsperado = calcularMontoEsperadoCambioUnidadAntesDeCrear(estancia, TipoUnidad.HABITACION);

        // ---------- WHEN ----------
        Pago pago = pagoService.crearPagoPorCambioUnidad(estancia, TipoUnidad.HABITACION);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Pago pagoDb = pagoRepository.findById(pago.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();

        comprobarPagoDb(pagoDb, montoEsperado, estanciaDb, TipoPago.CAMBIO_UNIDAD, EstadoPago.PENDIENTE);
        assertThat(estanciaDb.getEstado()).isEqualTo(EstadoEstancia.EXCEDIDA);
    }

    @Test
    void falloCreandoPagoPorCambioUnidadEnEstanciaFinalizada_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.FINALIZADA);

        long pagosAntes = pagoRepository.count();
        long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> pagoService.crearPagoPorCambioUnidad(estancia, TipoUnidad.APARTAMENTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede crear un pago por cambio de unidad para una estancia que no está ACTIVA o EXCEDIDA. El estado actual de la estancia es: ");

        // ---------- THEN ----------
        assertThat(pagoRepository.count()).isEqualTo(pagosAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);
    }

    @Test
    void falloCreandoPagoPorCambioUnidadSinEstancia_test() {

        // ---------- GIVEN ----------
        Long pagosAntes = pagoRepository.count();
        Long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> pagoService.crearPagoPorCambioUnidad(null, TipoUnidad.APARTAMENTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No se proporcionó información de estancia o tipo de unidad");

        // ---------- THEN ----------
        assertThat(pagoRepository.count()).isEqualTo(pagosAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);
    }

    @Test
    void falloCreandoPagoPorCambioUnidadSinTipoUnidadAnterior_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        Long pagosAntes = pagoRepository.count();
        Long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> pagoService.crearPagoPorCambioUnidad(estancia, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No se proporcionó información de estancia o tipo de unidad");

        // ---------- THEN ----------
        assertThat(pagoRepository.count()).isEqualTo(pagosAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);
    }

    //------------------- PRUEBAS DE ELIMINACIÓN DE PAGO ESTANCIA COMPLETADA -------------------

    @Test
    void exitoEliminandoPagoEstanciaCompletadaYCreandoNuevoCompletado_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.FINALIZADA);
        Pago pagoAnterior = crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA_COMPLETADA);
        PagoNuevoRequestDTO request = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
        request.setEstado(EstadoPago.COMPLETADO);

        long pagosAntes = pagoRepository.count();
        long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        pagoService.eliminarPagoEstanciaCompletada(request, pagoAnterior.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Pago pagoAnteriorDb = pagoRepository.findById(pagoAnterior.getId()).orElseThrow();
        Pago pagoNuevoDb = estanciaDb.getPagos().stream()
                .filter(p -> !p.getId().equals(pagoAnterior.getId()))
                .findFirst()
                .orElseThrow();

        assertThat(pagoRepository.count()).isEqualTo(pagosAntes + 1);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes + 2);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                request.getMonto(),
                pagoAnterior.getMonto(),
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.ELIMINADO,
                1,
                2,
                0,
                0,
                0
        );

        comprobarPagoDb(pagoAnteriorDb, pagoAnterior.getMonto(), estanciaDb, TipoPago.ESTANCIA_COMPLETADA, EstadoPago.ELIMINADO);
        comprobarPagoDb(pagoNuevoDb, request.getMonto(), estanciaDb, TipoPago.ESTANCIA_COMPLETADA, EstadoPago.COMPLETADO);

        comprobarEventoDb(
                eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(TipoEntidad.PAGO, pagoAnteriorDb.getId()).orElseThrow(),
                TipoEvento.ELIMINACION_PAGO,
                estancia.getCodigoFolio(),
                null,
                1
        );
        comprobarEventoDb(
                eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(TipoEntidad.PAGO, pagoNuevoDb.getId()).orElseThrow(),
                TipoEvento.CREACION_PAGO,
                estancia.getCodigoFolio(),
                null,
                4
        );
    }

    @Test
    void exitoEliminandoPagoEstanciaCompletadaYCreandoNuevoPendiente_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.FINALIZADA);
        Pago pagoAnterior = crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA_COMPLETADA);
        pagoAnterior.setEstado(EstadoPago.PENDIENTE);
        pagoRepository.save(pagoAnterior);

        PagoNuevoRequestDTO request = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
        request.setEstado(EstadoPago.PENDIENTE);

        // ---------- WHEN ----------
        pagoService.eliminarPagoEstanciaCompletada(request, pagoAnterior.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Pago pagoAnteriorDb = pagoRepository.findById(pagoAnterior.getId()).orElseThrow();
        Pago pagoNuevoDb = estanciaDb.getPagos().stream()
                .filter(p -> !p.getId().equals(pagoAnterior.getId()))
                .findFirst()
                .orElseThrow();

        comprobarPagosDb(
                estanciaDb.getPagos(),
                BigDecimal.ZERO,
                pagoAnterior.getMonto(),
                request.getMonto(),
                EstadoPago.PENDIENTE,
                1,
                EstadoPago.ELIMINADO,
                1,
                2,
                0,
                0,
                0
        );

        comprobarPagoDb(pagoAnteriorDb, pagoAnterior.getMonto(), estanciaDb, TipoPago.ESTANCIA_COMPLETADA, EstadoPago.ELIMINADO);
        comprobarPagoDb(pagoNuevoDb, request.getMonto(), estanciaDb, TipoPago.ESTANCIA_COMPLETADA, EstadoPago.PENDIENTE);
    }

    @Test
    void falloEliminandoPagoEstanciaCompletadaPagoNoExistente_test() {

        // ---------- GIVEN ----------
        PagoNuevoRequestDTO request = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
        request.setEstado(EstadoPago.COMPLETADO);
        long pagosAntes = pagoRepository.count();
        long eventosAntes = eventoRepository.count();
        long idInexistente = Long.MAX_VALUE;

        // ---------- WHEN ----------
        assertThatThrownBy(() -> pagoService.eliminarPagoEstanciaCompletada(request, idInexistente))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pago no encontrado con id: " + idInexistente);

        // ---------- THEN ----------
        assertThat(pagoRepository.count()).isEqualTo(pagosAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);
    }

    @Test
    void falloEliminandoPagoEstanciaCompletadaCuandoPagoNoEsDeEseTipo_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        Pago pago = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        PagoNuevoRequestDTO request = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
        request.setEstado(EstadoPago.COMPLETADO);
        long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> pagoService.eliminarPagoEstanciaCompletada(request, pago.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no es de tipo ESTANCIA_COMPLETADA");

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Pago pagoDb = pagoRepository.findById(pago.getId()).orElseThrow();
        assertThat(pagoDb.getEstado()).isEqualTo(EstadoPago.COMPLETADO);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);
    }

    @Test
    void falloEliminandoPagoEstanciaCompletadaConEstadoNoEliminable_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.FINALIZADA);
        Pago pago = crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA_COMPLETADA);
        pago.setEstado(EstadoPago.ELIMINADO);
        pagoRepository.save(pago);

        PagoNuevoRequestDTO request = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
        request.setEstado(EstadoPago.COMPLETADO);
        long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> pagoService.eliminarPagoEstanciaCompletada(request, pago.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo se puede eliminar un pago en estado COMPLETADO o PENDIENTE");

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Pago pagoDb = pagoRepository.findById(pago.getId()).orElseThrow();
        assertThat(pagoDb.getEstado()).isEqualTo(EstadoPago.ELIMINADO);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);
    }

    @Test
    void falloEliminandoPagoEstanciaCompletadaConTipoPagoRequestInvalido_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.FINALIZADA);
        Pago pagoAnterior = crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA_COMPLETADA);

        PagoNuevoRequestDTO request = pagoNuevoRequestDTO(TipoPago.ANTICIPO_ESTANCIA);
        long pagosAntes = pagoRepository.count();
        long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> pagoService.eliminarPagoEstanciaCompletada(request, pagoAnterior.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El tipo de pago en el request no es ESTANCIA_COMPLETADA");

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Pago pagoAnteriorDb = pagoRepository.findById(pagoAnterior.getId()).orElseThrow();

        assertThat(pagoRepository.count()).isEqualTo(pagosAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);
        comprobarPagoDb(pagoAnteriorDb, pagoAnterior.getMonto(), estanciaDb, TipoPago.ESTANCIA_COMPLETADA, EstadoPago.COMPLETADO);
    }

    //------------------- PRUEBAS DE ELIMINACIÓN DE PAGO -------------------

    @Test
    void exitoEliminandoPagoTipoAnticipoReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        Estancia estancia = reserva.getEstancia();
        Pago pago = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_RESERVA);
        BigDecimal monto = pago.getMonto();

        // ---------- WHEN ----------
        pagoService.eliminarPago(pago.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Pago pagoDb = pagoRepository.findById(pago.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.PAGO,
                pagoDb.getId()).orElseThrow();

        comprobarPagosDb(
                estanciaDb.getPagos(),
                BigDecimal.ZERO,
                monto,
                BigDecimal.ZERO,
                EstadoPago.ELIMINADO,
                1,
                EstadoPago.COMPLETADO,
                0,
                0,
                0,
                1,
                0
        );

        comprobarPagoDb(pagoDb, monto, estanciaDb, TipoPago.ANTICIPO_RESERVA, EstadoPago.ELIMINADO);

        comprobarEventoDb(
                eventoDb,
                TipoEvento.ELIMINACION_PAGO,
                null,
                reserva.getCodigo(),
                1
        );
    }

    @Test
    void exitoEliminandoPagoTipoAnticipoEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        Pago pago = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        BigDecimal monto = pago.getMonto();

        // ---------- WHEN ----------
        pagoService.eliminarPago(pago.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Pago pagoDb = pagoRepository.findById(pago.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.PAGO,
                pagoDb.getId()).orElseThrow();

        comprobarPagosDb(
                estanciaDb.getPagos(),
                BigDecimal.ZERO,
                monto,
                BigDecimal.ZERO,
                EstadoPago.ELIMINADO,
                1,
                EstadoPago.COMPLETADO,
                0,
                0,
                1,
                0,
                0
        );

        comprobarPagoDb(pagoDb, monto, estanciaDb, TipoPago.ANTICIPO_ESTANCIA, EstadoPago.ELIMINADO);

        comprobarEventoDb(
                eventoDb,
                TipoEvento.ELIMINACION_PAGO,
                estancia.getCodigoFolio(),
                null,
                1
        );
    }

    @Test
    void falloEliminandoPagoTipoEstanciaCompletada_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.FINALIZADA);
        Pago pago = crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA_COMPLETADA);
        BigDecimal monto = pago.getMonto();
        long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> pagoService.eliminarPago(pago.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede eliminar un pago de tipo ESTANCIA_COMPLETADA");

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Pago pagoDb = pagoRepository.findById(pago.getId()).orElseThrow();
        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();

        comprobarPagosDb(
                estanciaDb.getPagos(),
                monto,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                1,
                EstadoPago.ELIMINADO,
                0,
                1,
                0,
                0,
                0
        );

        comprobarPagoDb(pagoDb, monto, estanciaDb, TipoPago.ESTANCIA_COMPLETADA, EstadoPago.COMPLETADO);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);
    }

    @Test
    void falloEliminandoPagoNoExistente_test() {

        // ---------- GIVEN ----------
        Long idInexistente = Long.MAX_VALUE;
        Long pagosAntes = pagoRepository.count();
        Long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> pagoService.eliminarPago(idInexistente))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pago no encontrado con id: " + idInexistente);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        assertThat(pagoRepository.count()).isEqualTo(pagosAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);
    }

    @Test
    void falloEliminandoPagoYaEliminado_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        Pago pago = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        pago.setEstado(EstadoPago.ELIMINADO);
        pagoRepository.save(pago);

        Long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> pagoService.eliminarPago(pago.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Solo se puede eliminar un pago en estado COMPLETADO o PENDIENTE");

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Pago pagoDb = pagoRepository.findById(pago.getId()).orElseThrow();

        assertThat(pagoDb.getEstado()).isEqualTo(EstadoPago.ELIMINADO);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);
    }

    @Test
    void falloEliminandoPagoTipoCambioUnidad_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        Pago pago = crearPagoInicialEnEstancia(estancia, TipoPago.CAMBIO_UNIDAD);
        Long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        assertThatThrownBy(() -> pagoService.eliminarPago(pago.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se puede eliminar un pago de tipo CAMBIO_UNIDAD, este tipo de pago se maneja automáticamente por el sistema y no debe ser eliminado manualmente");

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Pago pagoDb = pagoRepository.findById(pago.getId()).orElseThrow();

        assertThat(pagoDb.getEstado()).isEqualTo(EstadoPago.COMPLETADO);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);
    }

    //------------------- PRUEBAS DE ELIMINACIÓN DE PAGOS -------------------

    @Test
    void exitoEliminandoPagosConPagoCompletadoYPendiente_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        Pago pagoCompletado = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        Pago pagoPendiente = crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA_COMPLETADA);
        pagoPendiente.setEstado(EstadoPago.PENDIENTE);
        pagoRepository.save(pagoPendiente);

        BigDecimal montoTotal = pagoCompletado.getMonto().add(pagoPendiente.getMonto());
        long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        pagoService.eliminarTodoLosPagos(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Pago pagoCompletadoDb = pagoRepository.findById(pagoCompletado.getId()).orElseThrow();
        Pago pagoPendienteDb = pagoRepository.findById(pagoPendiente.getId()).orElseThrow();

        assertThat(eventoRepository.count()).isEqualTo(eventosAntes + 2);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                BigDecimal.ZERO,
                montoTotal,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                2,
                1,
                1,
                0,
                0
        );

        comprobarPagoDb(pagoCompletadoDb, pagoCompletado.getMonto(), estanciaDb, TipoPago.ANTICIPO_ESTANCIA, EstadoPago.ELIMINADO);
        comprobarPagoDb(pagoPendienteDb, pagoPendiente.getMonto(), estanciaDb, TipoPago.ESTANCIA_COMPLETADA, EstadoPago.ELIMINADO);

        comprobarEventoDb(
                eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(TipoEntidad.PAGO, pagoCompletadoDb.getId()).orElseThrow(),
                TipoEvento.ELIMINACION_PAGO,
                estancia.getCodigoFolio(),
                null,
                1
        );
        comprobarEventoDb(
                eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(TipoEntidad.PAGO, pagoPendienteDb.getId()).orElseThrow(),
                TipoEvento.ELIMINACION_PAGO,
                estancia.getCodigoFolio(),
                null,
                1
        );
    }

    @Test
    void exitoEliminandoPagosIncluyeCambioUnidad_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        Pago pagoAnticipo = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        Pago pagoCambioUnidad = crearPagoInicialEnEstancia(estancia, TipoPago.CAMBIO_UNIDAD);
        BigDecimal montoTotal = pagoAnticipo.getMonto().add(pagoCambioUnidad.getMonto());
        long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        pagoService.eliminarTodoLosPagos(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Pago pagoAnticipoDb = pagoRepository.findById(pagoAnticipo.getId()).orElseThrow();
        Pago pagoCambioUnidadDb = pagoRepository.findById(pagoCambioUnidad.getId()).orElseThrow();

        assertThat(eventoRepository.count()).isEqualTo(eventosAntes + 2);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                BigDecimal.ZERO,
                montoTotal,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                2,
                0,
                1,
                0,
                1
        );

        comprobarPagoDb(pagoAnticipoDb, pagoAnticipo.getMonto(), estanciaDb, TipoPago.ANTICIPO_ESTANCIA, EstadoPago.ELIMINADO);
        comprobarPagoDb(pagoCambioUnidadDb, pagoCambioUnidad.getMonto(), estanciaDb, TipoPago.CAMBIO_UNIDAD, EstadoPago.ELIMINADO);
    }

    @Test
    void exitoEliminandoPagosIncluyePagoYaEliminado_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        Pago pagoCompletado = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        Pago pagoYaEliminado = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_RESERVA);
        pagoYaEliminado.setEstado(EstadoPago.ELIMINADO);
        pagoRepository.save(pagoYaEliminado);

        BigDecimal montoEliminadoEsperado = pagoCompletado.getMonto().add(pagoYaEliminado.getMonto());
        long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        pagoService.eliminarTodoLosPagos(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();
        Pago pagoCompletadoDb = pagoRepository.findById(pagoCompletado.getId()).orElseThrow();
        Pago pagoYaEliminadoDb = pagoRepository.findById(pagoYaEliminado.getId()).orElseThrow();

        assertThat(eventoRepository.count()).isEqualTo(eventosAntes + 2);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                BigDecimal.ZERO,
                montoEliminadoEsperado,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                2,
                0,
                1,
                1,
                0
        );

        assertThat(pagoCompletadoDb.getEstado()).isEqualTo(EstadoPago.ELIMINADO);
        assertThat(pagoYaEliminadoDb.getEstado()).isEqualTo(EstadoPago.ELIMINADO);
    }

    @Test
    void exitoEliminandoPagosEstanciaReservada_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        Estancia estancia = reserva.getEstancia();
        Pago pago = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_RESERVA);

        long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        pagoService.eliminarTodoLosPagos(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Pago pagoDb = pagoRepository.findById(pago.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.PAGO,
                pagoDb.getId()).orElseThrow();

        assertThat(eventoRepository.count()).isEqualTo(eventosAntes + 1);
        assertThat(pagoDb.getEstado()).isEqualTo(EstadoPago.ELIMINADO);
        comprobarEventoDb(
                eventoDb,
                TipoEvento.ELIMINACION_PAGO,
                null,
                reserva.getCodigo(),
                1
        );
    }

    @Test
    void exitoEliminandoPagosSinPagosNoHaceCambios_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        Long pagosAntes = pagoRepository.count();
        Long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        pagoService.eliminarTodoLosPagos(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();

        assertThat(pagoRepository.count()).isEqualTo(pagosAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);
        assertThat(estanciaDb.getPagos()).isEmpty();
    }

    @Test
    void exitoEliminandoPagosConTodosLosTiposYEstados_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        Pago pagoAnticipoReserva = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_RESERVA);
        pagoAnticipoReserva.setEstado(EstadoPago.PENDIENTE);
        pagoRepository.save(pagoAnticipoReserva);

        Pago pagoAnticipoEstancia = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);

        Pago pagoEstanciaCompletada = crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA_COMPLETADA);
        pagoEstanciaCompletada.setEstado(EstadoPago.PENDIENTE);
        pagoRepository.save(pagoEstanciaCompletada);

        Pago pagoCambioUnidad = crearPagoInicialEnEstancia(estancia, TipoPago.CAMBIO_UNIDAD);

        Pago pagoYaEliminado = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        pagoYaEliminado.setEstado(EstadoPago.ELIMINADO);
        pagoRepository.save(pagoYaEliminado);

        BigDecimal montoEliminadoEsperado = pagoAnticipoReserva.getMonto()
                .add(pagoAnticipoEstancia.getMonto())
                .add(pagoEstanciaCompletada.getMonto())
                .add(pagoCambioUnidad.getMonto())
                .add(pagoYaEliminado.getMonto());
        long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        pagoService.eliminarTodoLosPagos(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();

        assertThat(eventoRepository.count()).isEqualTo(eventosAntes + 5);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                BigDecimal.ZERO,
                montoEliminadoEsperado,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                5,
                1,
                2,
                1,
                1
        );
    }

    @Test
    void exitoEliminandoPagosMultiplesDelMismoTipo_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        Pago pago1 = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        Pago pago2 = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        pago2.setEstado(EstadoPago.PENDIENTE);
        pagoRepository.save(pago2);
        Pago pago3 = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);

        BigDecimal montoEliminadoEsperado = pago1.getMonto().add(pago2.getMonto()).add(pago3.getMonto());
        long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        pagoService.eliminarTodoLosPagos(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Estancia estanciaDb = estanciaRepository.findById(estancia.getId()).orElseThrow();

        assertThat(eventoRepository.count()).isEqualTo(eventosAntes + 3);

        comprobarPagosDb(
                estanciaDb.getPagos(),
                BigDecimal.ZERO,
                montoEliminadoEsperado,
                BigDecimal.ZERO,
                EstadoPago.COMPLETADO,
                0,
                EstadoPago.ELIMINADO,
                3,
                0,
                3,
                0,
                0
        );
    }

    @Test
    void exitoEliminandoPagosIdEstanciaInexistenteNoHaceCambios_test() {

        // ---------- GIVEN ----------
        Long idEstanciaInexistente = Long.MAX_VALUE;
        Long pagosAntes = pagoRepository.count();
        Long eventosAntes = eventoRepository.count();

        // ---------- WHEN ----------
        pagoService.eliminarTodoLosPagos(idEstanciaInexistente);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        assertThat(pagoRepository.count()).isEqualTo(pagosAntes);
        assertThat(eventoRepository.count()).isEqualTo(eventosAntes);
    }

    //------------------- PRUEBAS DE SUMA TOTAL DE PAGOS POR ESTANCIA -------------------


    @Test
    void exitoSumandoTotalPagosPorEstanciaSinPagos_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        // ---------- WHEN ----------
        BigDecimal total = pagoService.sumarTotalPagosPorEstancia(estancia.getId());

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void exitoSumandoTotalPagosPorEstanciaSoloEstadosVigentes_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        Pago pagoCompletado = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        Pago pagoPendiente = crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA_COMPLETADA);
        pagoPendiente.setEstado(EstadoPago.PENDIENTE);
        pagoRepository.save(pagoPendiente);

        Pago pagoEliminado = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_RESERVA);
        pagoEliminado.setEstado(EstadoPago.ELIMINADO);
        pagoRepository.save(pagoEliminado);

        BigDecimal totalEsperado = pagoCompletado.getMonto().add(pagoPendiente.getMonto());

        // ---------- WHEN ----------
        BigDecimal total = pagoService.sumarTotalPagosPorEstancia(estancia.getId());

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(totalEsperado);
    }

    @Test
    void exitoSumandoTotalPagosPorEstanciaInexistente_test() {

        // ---------- GIVEN ----------
        Long idEstanciaInexistente = Long.MAX_VALUE;

        // ---------- WHEN ----------
        BigDecimal total = pagoService.sumarTotalPagosPorEstancia(idEstanciaInexistente);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }


    //------------------- PRUEBAS DE ESTIMACIÓN DE PAGO -------------------
    /*
     * Matriz de cobertura de calcular estimacion pago
     *
     * | Dimension      | Cobertura actual |
     * |----------------|------------------|
     * | Tipo unidad    | HABITACION, APARTAESTUDIO, APARTAMENTO |
     * | Temporada      | BAJA, ALTA |
     * | Personas       | 1, 2, 3, 4, 5 |
     * | Anticipos      | Ninguno, multiples, mixtos por tipo, mixtos por estado, sobrepago |
     * | Cambio unidad  | PENDIENTE, no PENDIENTE, combinado con anticipos |
     * | Fechas         | Rango normal, mismo dia, cruce de medianoche, pocas horas entre fechas, igualdad invalida, salida anterior |
     *
     * | Grupo | Escenarios cubiertos |
     * |-------|----------------------|
     * | HABITACION | Baja con 2 personas, baja con estancia sin pagos, alta con 2 personas sin pagos, alta con 3 personas, integral con anticipos y cambio pendiente |
     * | APARTAESTUDIO | Baja con estancia sin pagos, alta con 2 personas sin pagos, alta con anticipos previos, integral con anticipos y cambio pendiente |
     * | APARTAMENTO | Baja con 1 persona, baja con 3 personas, baja con 4 personas, baja con estancia sin pagos, alta con 3 personas sin pagos, alta con 5 personas, integral con anticipos y cambio pendiente |
     * | Anticipos | Multiples anticipos de reserva completados, multiples anticipos de estancia completados, solo descuentan completados, mezcla de tipos y estados, total negativo por sobrepago |
     * | Cambio unidad | Suma pendiente, no suma completado/eliminado, combinado con anticipos en la misma formula |
     * | Fechas | Dias calendario normales, mismo dia retorna cero, cruce de medianoche cuenta un dia, pocas horas entre dos fechas puede contar dos dias, horas distintas siguen contando por fecha, error si entrada no es anterior a salida |
     */



    //HABITACION
    // Valores usados en formulas:
    // 180000 = precio dia habitacion en temporada baja
    // 200000 = precio dia habitacion en temporada alta
    // 85000 = precio persona adicional en baja
    // 90000 = precio persona adicional en alta

    // Formula esperada: 180000 x 2 dias = 360000
    @Test
    void exitoEstimacion_habitacion_baja_2Personas_conEstanciaSinPagos_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        CalcularPagoDTO request = calcularPagoRequestDTO(
                estancia.getId(),
                TipoUnidad.HABITACION,
                2,
                LocalDateTime.of(2026, 3, 1, 14, 0),
                LocalDateTime.of(2026, 3, 3, 10, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(360000));
    }

    // Formula esperada: 180000 x 3 dias = 540000
    @Test
    void exitoEstimacion_habitacion_baja_2Personas_sinPagosPrevios_test() {

        // ---------- GIVEN ----------
        CalcularPagoDTO request = calcularPagoRequestDTO(
                null,
                TipoUnidad.HABITACION,
                2,
                LocalDateTime.of(2026, 3, 1, 14, 0),
                LocalDateTime.of(2026, 3, 4, 10, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(540000));
    }

    // Formula esperada: 200000 x 2 dias = 400000
    @Test
    void exitoEstimacion_habitacion_alta_2Personas_conEstanciaSinPagos_test() {

        // ---------- GIVEN ----------
        ajusteTemporadaService.cambiarTemporadaActiva(Temporada.ALTA);
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        CalcularPagoDTO request = calcularPagoRequestDTO(
                estancia.getId(),
                TipoUnidad.HABITACION,
                2,
                LocalDateTime.of(2026, 7, 1, 14, 0),
                LocalDateTime.of(2026, 7, 3, 10, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(400000));
    }

    // Formula esperada: (200000 x 2 dias) + (90000 x 1 adicional x 2 dias) = 580000
    @Test
    void exitoEstimacion_habitacion_alta_3Personas_sinPagosPrevios_test() {

        // ---------- GIVEN ----------
        ajusteTemporadaService.cambiarTemporadaActiva(Temporada.ALTA);

        CalcularPagoDTO request = calcularPagoRequestDTO(
                null,
                TipoUnidad.HABITACION,
                3,
                LocalDateTime.of(2026, 7, 1, 14, 0),
                LocalDateTime.of(2026, 7, 3, 10, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(580000));
    }

    // Formula esperada: (180000 x 3 dias) - 120000 - 80000 = 340000
    @Test
    void exitoEstimacion_habitacion_baja_2Personas_conMultiplesAnticiposEstanciaCompletados_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        Pago pagoEstancia1 = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        pagoEstancia1.setMonto(BigDecimal.valueOf(120000));
        pagoRepository.save(pagoEstancia1);

        Pago pagoEstancia2 = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        pagoEstancia2.setMonto(BigDecimal.valueOf(80000));
        pagoRepository.save(pagoEstancia2);

        CalcularPagoDTO request = calcularPagoRequestDTO(
                estancia.getId(),
                TipoUnidad.HABITACION,
                2,
                LocalDateTime.of(2026, 3, 1, 14, 0),
                LocalDateTime.of(2026, 3, 4, 10, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(340000));
    }

    // Formula esperada: (180000 x 2 dias) + 70000 cambio pendiente = 430000
    @Test
    void exitoEstimacion_habitacion_baja_2Personas_sumaCambioUnidadPendiente_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        Pago pagoCambioUnidad = crearPagoInicialEnEstancia(estancia, TipoPago.CAMBIO_UNIDAD);
        pagoCambioUnidad.setMonto(BigDecimal.valueOf(70000));
        pagoCambioUnidad.setEstado(EstadoPago.PENDIENTE);
        pagoRepository.save(pagoCambioUnidad);

        CalcularPagoDTO request = calcularPagoRequestDTO(
                estancia.getId(),
                TipoUnidad.HABITACION,
                2,
                LocalDateTime.of(2026, 3, 1, 14, 0),
                LocalDateTime.of(2026, 3, 3, 10, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(430000));
    }

    // Formula esperada: 180000 x 2 dias; no suma cambios COMPLETADO o ELIMINADO = 360000
    @Test
    void exitoEstimacion_habitacion_baja_2Personas_noSumaCambioUnidadNoPendiente_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        Pago pagoCambioUnidadCompletado = crearPagoInicialEnEstancia(estancia, TipoPago.CAMBIO_UNIDAD);
        pagoCambioUnidadCompletado.setMonto(BigDecimal.valueOf(70000));
        pagoCambioUnidadCompletado.setEstado(EstadoPago.COMPLETADO);
        pagoRepository.save(pagoCambioUnidadCompletado);

        Pago pagoCambioUnidadEliminado = crearPagoInicialEnEstancia(estancia, TipoPago.CAMBIO_UNIDAD);
        pagoCambioUnidadEliminado.setMonto(BigDecimal.valueOf(90000));
        pagoCambioUnidadEliminado.setEstado(EstadoPago.ELIMINADO);
        pagoRepository.save(pagoCambioUnidadEliminado);

        CalcularPagoDTO request = calcularPagoRequestDTO(
                estancia.getId(),
                TipoUnidad.HABITACION,
                2,
                LocalDateTime.of(2026, 3, 1, 14, 0),
                LocalDateTime.of(2026, 3, 3, 10, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(360000));
    }

    // Formula esperada: (180000 x 1 dia) - 250000 - 100000 = -170000
    @Test
    void exitoEstimacion_habitacion_baja_2Personas_totalNegativoPorSobrepago_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        Pago anticipoReserva = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_RESERVA);
        anticipoReserva.setMonto(BigDecimal.valueOf(250000));
        pagoRepository.save(anticipoReserva);

        Pago anticipoEstancia = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        anticipoEstancia.setMonto(BigDecimal.valueOf(100000));
        pagoRepository.save(anticipoEstancia);

        CalcularPagoDTO request = calcularPagoRequestDTO(
                estancia.getId(),
                TipoUnidad.HABITACION,
                2,
                LocalDateTime.of(2026, 3, 1, 14, 0),
                LocalDateTime.of(2026, 3, 2, 10, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(-170000));
    }

    // Formula esperada: (200000 x 2 dias) + (90000 x 1 adicional x 2 dias) + 30000 cambio pendiente - 100000 anticipo = 510000
    @Test
    void exitoEstimacion_habitacion_alta_3Personas_formulaIntegral_test() {

        // ---------- GIVEN ----------
        ajusteTemporadaService.cambiarTemporadaActiva(Temporada.ALTA);
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        Pago anticipoReservaCompletado = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_RESERVA);
        anticipoReservaCompletado.setMonto(BigDecimal.valueOf(100000));
        pagoRepository.save(anticipoReservaCompletado);

        Pago anticipoEstanciaPendiente = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        anticipoEstanciaPendiente.setMonto(BigDecimal.valueOf(50000));
        anticipoEstanciaPendiente.setEstado(EstadoPago.PENDIENTE);
        pagoRepository.save(anticipoEstanciaPendiente);

        Pago cambioUnidadPendiente = crearPagoInicialEnEstancia(estancia, TipoPago.CAMBIO_UNIDAD);
        cambioUnidadPendiente.setMonto(BigDecimal.valueOf(30000));
        cambioUnidadPendiente.setEstado(EstadoPago.PENDIENTE);
        pagoRepository.save(cambioUnidadPendiente);

        CalcularPagoDTO request = calcularPagoRequestDTO(
                estancia.getId(),
                TipoUnidad.HABITACION,
                3,
                LocalDateTime.of(2026, 7, 1, 14, 0),
                LocalDateTime.of(2026, 7, 3, 10, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(510000));
    }


    //APARTAESTUDIO
    // Valores usados en formulas:
    // 230000 = precio dia apartaestudio en temporada baja
    // 300000 = precio dia apartaestudio en temporada alta
    // 85000 = precio persona adicional en baja
    // 90000 = precio persona adicional en alta

    // Formula esperada: 230000 x 2 dias = 460000
    @Test
    void exitoEstimacion_apartaestudio_baja_2Personas_conEstanciaSinPagos_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        CalcularPagoDTO request = calcularPagoRequestDTO(
                estancia.getId(),
                TipoUnidad.APARTAESTUDIO,
                2,
                LocalDateTime.of(2026, 3, 1, 15, 0),
                LocalDateTime.of(2026, 3, 3, 11, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(460000));
    }

    // Formula esperada: 300000 x 2 dias = 600000
    @Test
    void exitoEstimacion_apartaestudio_alta_2Personas_conEstanciaSinPagos_test() {

        // ---------- GIVEN ----------
        ajusteTemporadaService.cambiarTemporadaActiva(Temporada.ALTA);
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        CalcularPagoDTO request = calcularPagoRequestDTO(
                estancia.getId(),
                TipoUnidad.APARTAESTUDIO,
                2,
                LocalDateTime.of(2026, 7, 1, 15, 0),
                LocalDateTime.of(2026, 7, 3, 11, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(600000));
    }

    // Formula esperada: (230000 x 2 dias) + (85000 x 1 adicional x 2 dias) - 50000 completado = 580000
    @Test
    void exitoEstimacion_apartaestudio_baja_3Personas_descuentaSoloAnticiposCompletados_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        Pago pagoCompletado = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_RESERVA);
        pagoCompletado.setMonto(BigDecimal.valueOf(50000));
        pagoRepository.save(pagoCompletado);

        Pago pagoPendiente = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        pagoPendiente.setMonto(BigDecimal.valueOf(100000));
        pagoPendiente.setEstado(EstadoPago.PENDIENTE);
        pagoRepository.save(pagoPendiente);

        Pago pagoEliminado = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_RESERVA);
        pagoEliminado.setMonto(BigDecimal.valueOf(120000));
        pagoEliminado.setEstado(EstadoPago.ELIMINADO);
        pagoRepository.save(pagoEliminado);

        CalcularPagoDTO request = calcularPagoRequestDTO(
                estancia.getId(),
                TipoUnidad.APARTAESTUDIO,
                3,
                LocalDateTime.of(2026, 3, 1, 15, 0),
                LocalDateTime.of(2026, 3, 3, 11, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(580000));
    }

    // Formula esperada: (300000 x 4 dias) + (90000 x 1 adicional x 4 dias) - 200000 - 150000 = 1210000
    @Test
    void exitoEstimacion_apartaestudio_alta_3Personas_conAnticiposPrevios_test() {

        // ---------- GIVEN ----------
        ajusteTemporadaService.cambiarTemporadaActiva(Temporada.ALTA);
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        Pago pagoReserva = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_RESERVA);
        pagoReserva.setMonto(BigDecimal.valueOf(200000));
        pagoRepository.save(pagoReserva);

        Pago pagoEstancia = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        pagoEstancia.setMonto(BigDecimal.valueOf(150000));
        pagoRepository.save(pagoEstancia);

        CalcularPagoDTO request = calcularPagoRequestDTO(
                estancia.getId(),
                TipoUnidad.APARTAESTUDIO,
                3,
                LocalDateTime.of(2026, 7, 1, 15, 0),
                LocalDateTime.of(2026, 7, 5, 11, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(1210000));
    }

    // Formula esperada: (300000 x 2 dias) + (90000 x 1 adicional x 2 dias) + 50000 cambio pendiente - 120000 anticipo = 710000
    @Test
    void exitoEstimacion_apartaestudio_alta_3Personas_formulaIntegral_test() {

        // ---------- GIVEN ----------
        ajusteTemporadaService.cambiarTemporadaActiva(Temporada.ALTA);
        Unidad unidad = crearApartaestudio(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        Pago anticipoReservaCompletado = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_RESERVA);
        anticipoReservaCompletado.setMonto(BigDecimal.valueOf(120000));
        pagoRepository.save(anticipoReservaCompletado);

        Pago anticipoEstanciaEliminado = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        anticipoEstanciaEliminado.setMonto(BigDecimal.valueOf(40000));
        anticipoEstanciaEliminado.setEstado(EstadoPago.ELIMINADO);
        pagoRepository.save(anticipoEstanciaEliminado);

        Pago cambioUnidadPendiente = crearPagoInicialEnEstancia(estancia, TipoPago.CAMBIO_UNIDAD);
        cambioUnidadPendiente.setMonto(BigDecimal.valueOf(50000));
        cambioUnidadPendiente.setEstado(EstadoPago.PENDIENTE);
        pagoRepository.save(cambioUnidadPendiente);

        CalcularPagoDTO request = calcularPagoRequestDTO(
                estancia.getId(),
                TipoUnidad.APARTAESTUDIO,
                3,
                LocalDateTime.of(2026, 7, 1, 15, 0),
                LocalDateTime.of(2026, 7, 3, 11, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(710000));
    }


    //APARTAMENTO
    // Valores usados en formulas:
    // 250000 = precio dia apartamento en temporada baja
    // 300000 = precio dia apartamento en temporada alta
    // 85000 = precio persona adicional en baja
    // 90000 = precio persona adicional en alta

    // Formula esperada: 250000 x 2 dias = 500000
    @Test
    void exitoEstimacion_apartamento_baja_1Persona_sinPagosPrevios_test() {

        // ---------- GIVEN ----------
        CalcularPagoDTO request = calcularPagoRequestDTO(
                null,
                TipoUnidad.APARTAMENTO,
                1,
                LocalDateTime.of(2026, 3, 1, 15, 0),
                LocalDateTime.of(2026, 3, 3, 9, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(500000));
    }

    // Formula esperada: (250000 x 2 dias) - 100000 - 50000 = 350000
    @Test
    void exitoEstimacion_apartamento_baja_2Personas_conMultiplesAnticiposReservaCompletados_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        Pago pagoReserva1 = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_RESERVA);
        pagoReserva1.setMonto(BigDecimal.valueOf(100000));
        pagoRepository.save(pagoReserva1);

        Pago pagoReserva2 = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_RESERVA);
        pagoReserva2.setMonto(BigDecimal.valueOf(50000));
        pagoRepository.save(pagoReserva2);

        CalcularPagoDTO request = calcularPagoRequestDTO(
                estancia.getId(),
                TipoUnidad.APARTAMENTO,
                2,
                LocalDateTime.of(2026, 3, 1, 15, 0),
                LocalDateTime.of(2026, 3, 3, 10, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(350000));
    }

    // Formula esperada: (250000 x 2 dias) + (85000 x 1 adicional x 2 dias) = 670000
    @Test
    void exitoEstimacion_apartamento_baja_3Personas_sinPagosPrevios_test() {

        // ---------- GIVEN ----------
        CalcularPagoDTO request = calcularPagoRequestDTO(
                null,
                TipoUnidad.APARTAMENTO,
                3,
                LocalDateTime.of(2026, 3, 1, 15, 0),
                LocalDateTime.of(2026, 3, 3, 9, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(670000));
    }

    // Formula esperada: (250000 x 2 dias) + (85000 x 1 adicional x 2 dias) - 100000 - 50000 = 520000
    @Test
    void exitoEstimacion_apartamento_baja_3Personas_descuentaSoloAnticiposCompletados_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        Pago anticipoReservaCompletado = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_RESERVA);
        anticipoReservaCompletado.setMonto(BigDecimal.valueOf(100000));
        anticipoReservaCompletado.setEstado(EstadoPago.COMPLETADO);
        pagoRepository.save(anticipoReservaCompletado);

        Pago anticipoReservaPendiente = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_RESERVA);
        anticipoReservaPendiente.setMonto(BigDecimal.valueOf(80000));
        anticipoReservaPendiente.setEstado(EstadoPago.PENDIENTE);
        pagoRepository.save(anticipoReservaPendiente);

        Pago anticipoEstanciaCompletado = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        anticipoEstanciaCompletado.setMonto(BigDecimal.valueOf(50000));
        anticipoEstanciaCompletado.setEstado(EstadoPago.COMPLETADO);
        pagoRepository.save(anticipoEstanciaCompletado);

        Pago anticipoEstanciaEliminado = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        anticipoEstanciaEliminado.setMonto(BigDecimal.valueOf(90000));
        anticipoEstanciaEliminado.setEstado(EstadoPago.ELIMINADO);
        pagoRepository.save(anticipoEstanciaEliminado);

        CalcularPagoDTO request = calcularPagoRequestDTO(
                estancia.getId(),
                TipoUnidad.APARTAMENTO,
                3,
                LocalDateTime.of(2026, 3, 1, 15, 0),
                LocalDateTime.of(2026, 3, 3, 11, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(520000));
    }

    // Formula esperada: (250000 x 2 dias) + (85000 x 2 adicionales x 2 dias) = 840000
    @Test
    void exitoEstimacion_apartamento_baja_4Personas_sinPagosPrevios_test() {

        // ---------- GIVEN ----------
        CalcularPagoDTO request = calcularPagoRequestDTO(
                null,
                TipoUnidad.APARTAMENTO,
                4,
                LocalDateTime.of(2026, 3, 1, 15, 0),
                LocalDateTime.of(2026, 3, 3, 9, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(840000));
    }

    // Formula esperada: (250000 x 2 dias) + (85000 x 2 adicionales x 2 dias) + 50000 cambio pendiente - 150000 anticipo = 740000
    @Test
    void exitoEstimacion_apartamento_baja_4Personas_formulaIntegral_test() {

        // ---------- GIVEN ----------
        ajusteTemporadaService.cambiarTemporadaActiva(Temporada.BAJA);
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        Pago anticipoReservaCompletado = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_RESERVA);
        anticipoReservaCompletado.setMonto(BigDecimal.valueOf(150000));
        pagoRepository.save(anticipoReservaCompletado);

        Pago anticipoEstanciaPendiente = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        anticipoEstanciaPendiente.setMonto(BigDecimal.valueOf(30000));
        anticipoEstanciaPendiente.setEstado(EstadoPago.PENDIENTE);
        pagoRepository.save(anticipoEstanciaPendiente);

        Pago cambioUnidadPendiente = crearPagoInicialEnEstancia(estancia, TipoPago.CAMBIO_UNIDAD);
        cambioUnidadPendiente.setMonto(BigDecimal.valueOf(50000));
        cambioUnidadPendiente.setEstado(EstadoPago.PENDIENTE);
        pagoRepository.save(cambioUnidadPendiente);

        CalcularPagoDTO request = calcularPagoRequestDTO(
                estancia.getId(),
                TipoUnidad.APARTAMENTO,
                4,
                LocalDateTime.of(2026, 3, 1, 15, 0),
                LocalDateTime.of(2026, 3, 3, 11, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(740000));
    }

    // Formula esperada: (250000 x 2 dias) + (85000 x 2 adicionales x 2 dias) + 30000 + 20000 cambios pendientes - 100000 - 50000 anticipos = 740000
    @Test
    void exitoEstimacion_apartamento_baja_4Personas_combinaAnticiposYCambiosPendientes_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        Pago anticipoReserva = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_RESERVA);
        anticipoReserva.setMonto(BigDecimal.valueOf(100000));
        pagoRepository.save(anticipoReserva);

        Pago anticipoEstancia = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        anticipoEstancia.setMonto(BigDecimal.valueOf(50000));
        pagoRepository.save(anticipoEstancia);

        Pago cambioUnidadPendiente1 = crearPagoInicialEnEstancia(estancia, TipoPago.CAMBIO_UNIDAD);
        cambioUnidadPendiente1.setMonto(BigDecimal.valueOf(30000));
        cambioUnidadPendiente1.setEstado(EstadoPago.PENDIENTE);
        pagoRepository.save(cambioUnidadPendiente1);

        Pago cambioUnidadPendiente2 = crearPagoInicialEnEstancia(estancia, TipoPago.CAMBIO_UNIDAD);
        cambioUnidadPendiente2.setMonto(BigDecimal.valueOf(20000));
        cambioUnidadPendiente2.setEstado(EstadoPago.PENDIENTE);
        pagoRepository.save(cambioUnidadPendiente2);

        CalcularPagoDTO request = calcularPagoRequestDTO(
                estancia.getId(),
                TipoUnidad.APARTAMENTO,
                4,
                LocalDateTime.of(2026, 3, 1, 15, 0),
                LocalDateTime.of(2026, 3, 3, 11, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(740000));
    }

    // Formula esperada: (300000 x 2 dias) + (90000 x 1 adicional x 2 dias) = 780000
    @Test
    void exitoEstimacion_apartamento_alta_3Personas_conEstanciaSinPagos_test() {

        // ---------- GIVEN ----------
        ajusteTemporadaService.cambiarTemporadaActiva(Temporada.ALTA);
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        CalcularPagoDTO request = calcularPagoRequestDTO(
                estancia.getId(),
                TipoUnidad.APARTAMENTO,
                3,
                LocalDateTime.of(2026, 7, 1, 15, 0),
                LocalDateTime.of(2026, 7, 3, 11, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(780000));
    }

    // Formula esperada: (300000 x 2 dias) + (90000 x 3 adicionales x 2 dias) = 1140000
    @Test
    void exitoEstimacion_apartamento_alta_5Personas_sinPagosPrevios_test() {

        // ---------- GIVEN ----------
        ajusteTemporadaService.cambiarTemporadaActiva(Temporada.ALTA);

        CalcularPagoDTO request = calcularPagoRequestDTO(
                null,
                TipoUnidad.APARTAMENTO,
                5,
                LocalDateTime.of(2026, 7, 1, 15, 0),
                LocalDateTime.of(2026, 7, 3, 11, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(1140000));
    }

    //FECHAS
    // Regla usada en formulas:
    // dias = ChronoUnit.DAYS.between(fechaEntrada.toLocalDate(), fechaSalida.toLocalDate())

    // Formula esperada: 0 dias calendario = 0
    @Test
    void exitoEstimacion_fechas_mismoDiaCalendario_retornaCero_test() {

        // ---------- GIVEN ----------
        CalcularPagoDTO request = calcularPagoRequestDTO(
                null,
                TipoUnidad.HABITACION,
                2,
                LocalDateTime.of(2026, 3, 1, 8, 0),
                LocalDateTime.of(2026, 3, 1, 20, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // Formula esperada: 180000 x 1 dia calendario = 180000
    @Test
    void exitoEstimacion_fechas_cruceMedianoche_cuentaUnDia_test() {

        // ---------- GIVEN ----------
        CalcularPagoDTO request = calcularPagoRequestDTO(
                null,
                TipoUnidad.HABITACION,
                2,
                LocalDateTime.of(2026, 3, 1, 23, 0),
                LocalDateTime.of(2026, 3, 2, 1, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(180000));
    }

    // Formula esperada: 180000 x 2 dias calendario = 360000
    @Test
    void exitoEstimacion_fechas_pocasHorasEnDosFechas_cuentaDosDias_test() {

        // ---------- GIVEN ----------
        CalcularPagoDTO request = calcularPagoRequestDTO(
                null,
                TipoUnidad.HABITACION,
                2,
                LocalDateTime.of(2026, 3, 1, 23, 59),
                LocalDateTime.of(2026, 3, 3, 0, 1)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(360000));
    }

    // Formula esperada: 230000 x 2 dias calendario = 460000
    @Test
    void exitoEstimacion_fechas_horasDistintas_cuentaPorDiasCalendario_test() {

        // ---------- GIVEN ----------
        CalcularPagoDTO request = calcularPagoRequestDTO(
                null,
                TipoUnidad.APARTAESTUDIO,
                2,
                LocalDateTime.of(2026, 3, 1, 1, 0),
                LocalDateTime.of(2026, 3, 3, 23, 0)
        );

        // ---------- WHEN ----------
        BigDecimal total = pagoService.obtenerEstimacionPago(request);

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(460000));
    }


    //ERRORES

    // Formula esperada: lanza IllegalArgumentException porque fechaEntrada == fechaSalida
    @Test
    void falloEstimacion_fechas_entradaIgualSalida_test() {

        // ---------- GIVEN ----------
        CalcularPagoDTO request = calcularPagoRequestDTO(
                null,
                TipoUnidad.HABITACION,
                2,
                LocalDateTime.of(2026, 3, 4, 10, 0),
                LocalDateTime.of(2026, 3, 4, 10, 0)
        );

        // ---------- WHEN ----------
        assertThatThrownBy(() -> pagoService.obtenerEstimacionPago(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de entrada debe ser anterior a la salida");
    }

    // Formula esperada: lanza IllegalArgumentException porque fechaSalida < fechaEntrada
    @Test
    void falloEstimacion_fechas_salidaAnteriorAEntrada_test() {

        // ---------- GIVEN ----------
        CalcularPagoDTO request = calcularPagoRequestDTO(
                null,
                TipoUnidad.HABITACION,
                2,
                LocalDateTime.of(2026, 3, 4, 10, 0),
                LocalDateTime.of(2026, 3, 3, 10, 0)
        );

        // ---------- WHEN ----------
        assertThatThrownBy(() -> pagoService.obtenerEstimacionPago(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("La fecha de entrada debe ser anterior a la salida");
    }

    // Formula esperada: lanza IllegalArgumentException porque la estancia no existe
    @Test
    void falloEstimacion_estanciaInexistente_test() {

        // ---------- GIVEN ----------
        CalcularPagoDTO request = calcularPagoRequestDTO(
                Long.MAX_VALUE,
                TipoUnidad.APARTAMENTO,
                3,
                LocalDateTime.of(2026, 3, 1, 15, 0),
                LocalDateTime.of(2026, 3, 3, 11, 0)
        );

        // ---------- WHEN ----------
        assertThatThrownBy(() -> pagoService.obtenerEstimacionPago(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No se encontró una estancia con id: " + Long.MAX_VALUE);
    }


























    private BigDecimal calcularMontoEsperadoCambioUnidadAntesDeCrear(Estancia estancia, TipoUnidad tipoUnidadAnterior) {
        CalcularPagoDTO calcularPagoDTO = PagoMapper.entityToCalcularPagoDTO(
                estancia.getId(),
                tipoUnidadAnterior,
                estancia.getOcupantes().size(),
                estancia.getEntradaReal(),
                LocalDateTime.now()
        );

        return pagoService.obtenerEstimacionPago(calcularPagoDTO);
    }






}
