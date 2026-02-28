package com.hotel.services;

import com.hotel.dtos.pago.CalcularPagoDTO;
import com.hotel.dtos.pago.PagoDTO;
import com.hotel.dtos.pago.PagoNuevoRequestDTO;
import com.hotel.mappers.PagoMapper;
import com.hotel.models.AuditoriaEvento;
import com.hotel.models.Estancia;
import com.hotel.models.Pago;
import com.hotel.models.Reserva;
import com.hotel.models.Unidad;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.EstadoPago;
import com.hotel.models.enums.TipoEntidad;
import com.hotel.models.enums.TipoEvento;
import com.hotel.models.enums.TipoPago;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.AuditoriaEventoRepository;
import com.hotel.services.support.AbstractServiceIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static com.hotel.testdata.PagoTestData.pagoData;
import static com.hotel.testdata.PagoTestData.pagoNuevoRequestDTO;
import static com.hotel.testutils.AssertionsHelper.comprobarEventoDb;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PagoServiceIT extends AbstractServiceIT {

    @Autowired
    private PagoService pagoService;

    @Autowired
    private AuditoriaEventoRepository eventoRepository;

    @Test
    void exitoCreandoPagoParaEstanciaActiva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false);
        PagoNuevoRequestDTO request = pagoNuevoRequestDTO(TipoPago.ANTICIPO_ESTANCIA);

        // ---------- WHEN ----------
        Pago pago = pagoService.crearPago(request, estancia);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Pago pagoDb = pagoRepository.findById(pago.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.PAGO,
                pagoDb.getId()).orElseThrow();

        assertThat(pagoDb.getEstancia().getId()).isEqualTo(estancia.getId());
        assertThat(pagoDb.getTipoPago()).isEqualTo(request.getTipoPago());
        assertThat(pagoDb.getMonto()).isEqualByComparingTo(request.getMonto());
        assertThat(pagoDb.getEstado()).isEqualTo(request.getEstado());

        comprobarEventoDb(
                eventoDb,
                TipoEvento.CREACION_PAGO,
                estancia.getCodigoFolio(),
                null,
                4
        );
    }

    @Test
    void exitoCreandoPagoParaEstanciaReservadaSinCodigoEstanciaEnEvento_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false);
        Estancia estancia = reserva.getEstancia();
        PagoNuevoRequestDTO request = pagoNuevoRequestDTO(TipoPago.ANTICIPO_RESERVA);

        // ---------- WHEN ----------
        Pago pago = pagoService.crearPago(request, estancia);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Pago pagoDb = pagoRepository.findById(pago.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.PAGO,
                pagoDb.getId()).orElseThrow();

        assertThat(pagoDb.getEstancia().getId()).isEqualTo(estancia.getId());
        assertThat(pagoDb.getTipoPago()).isEqualTo(TipoPago.ANTICIPO_RESERVA);

        comprobarEventoDb(
                eventoDb,
                TipoEvento.CREACION_PAGO,
                null,
                reserva.getCodigo(),
                4
        );
    }

    @Test
    void exitoCreandoPagoPorCambioUnidad_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false);
        CalcularPagoDTO calcularPagoDTO = PagoMapper.entityToCalcularPagoDTO(
                TipoUnidad.APARTAMENTO,
                estancia.getOcupantes().size(),
                estancia.getEntradaReal(),
                LocalDateTime.now()
        );
        BigDecimal montoEsperado = pagoService.obtenerEstimacionPago(calcularPagoDTO);

        // ---------- WHEN ----------
        Pago pago = pagoService.crearPagoPorCambioUnidad(estancia, TipoUnidad.APARTAMENTO);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Pago pagoDb = pagoRepository.findById(pago.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.PAGO,
                pagoDb.getId()).orElseThrow();

        assertThat(pagoDb.getEstancia().getId()).isEqualTo(estancia.getId());
        assertThat(pagoDb.getTipoPago()).isEqualTo(TipoPago.CAMBIO_UNIDAD);
        assertThat(pagoDb.getMonto()).isEqualByComparingTo(montoEsperado);
        assertThat(pagoDb.getEstado()).isEqualTo(EstadoPago.PENDIENTE);

        comprobarEventoDb(
                eventoDb,
                TipoEvento.CREACION_PAGO,
                estancia.getCodigoFolio(),
                null,
                3
        );
    }

    @Test
    void exitoReemplazandoPagoExistente_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), true);
        Pago pagoAnterior = estancia.getPagos().getFirst();
        PagoNuevoRequestDTO request = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);

        // ---------- WHEN ----------
        Pago pagoNuevo = pagoService.reemplazarPago(request, pagoAnterior, estancia);

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Pago pagoAnteriorDb = pagoRepository.findById(pagoAnterior.getId()).orElseThrow();
        Pago pagoNuevoDb = pagoRepository.findById(pagoNuevo.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.PAGO,
                pagoAnteriorDb.getId()).orElseThrow();

        assertThat(pagoAnteriorDb.getEstado()).isEqualTo(EstadoPago.MODIFICADO);
        assertThat(pagoNuevoDb.getEstancia().getId()).isEqualTo(estancia.getId());
        assertThat(pagoNuevoDb.getTipoPago()).isEqualTo(request.getTipoPago());
        assertThat(pagoNuevoDb.getMonto()).isEqualByComparingTo(request.getMonto());
        assertThat(pagoNuevoDb.getEstado()).isEqualTo(request.getEstado());

        comprobarEventoDb(
                eventoDb,
                TipoEvento.MODIFICACION_PAGO,
                estancia.getCodigoFolio(),
                null,
                3
        );
    }

    @Test
    void exitoBuscandoUltimoPagoPorEstanciaYTipo_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false);

        Pago pagoAntiguo = pagoData(estancia, TipoPago.ANTICIPO_ESTANCIA);
        pagoAntiguo.setFechaCreacion(LocalDateTime.now().minusDays(2));
        pagoRepository.save(pagoAntiguo);

        Pago pagoReciente = pagoData(estancia, TipoPago.ANTICIPO_ESTANCIA);
        pagoReciente.setFechaCreacion(LocalDateTime.now().minusDays(1));
        pagoRepository.save(pagoReciente);

        // ---------- WHEN ----------
        Pago pagoEncontrado = pagoService.buscarUltimoPagoPorEstanciaYTipo(
                estancia.getId(),
                TipoPago.ANTICIPO_ESTANCIA
        ).orElseThrow();

        // ---------- THEN ----------
        assertThat(pagoEncontrado.getId()).isEqualTo(pagoReciente.getId());
    }

    @Test
    void exitoEliminandoPagosPorEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), true);
        Pago pago = estancia.getPagos().getFirst();

        // ---------- WHEN ----------
        pagoService.eliminarPagos(estancia.getId());

        // ---------- THEN ----------
        entityManager.flush();
        entityManager.clear();

        Pago pagoDb = pagoRepository.findById(pago.getId()).orElseThrow();
        AuditoriaEvento eventoDb = eventoRepository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(
                TipoEntidad.PAGO,
                pagoDb.getId()).orElseThrow();

        assertThat(pagoDb.getEstado()).isEqualTo(EstadoPago.ELIMINADO);

        comprobarEventoDb(
                eventoDb,
                TipoEvento.ELIMINACION_PAGO,
                estancia.getCodigoFolio(),
                null,
                1
        );
    }

    @Test
    void exitoSumandoTotalPagosPorEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false);

        Pago pagoCompletado = pagoData(estancia, TipoPago.ANTICIPO_ESTANCIA);
        pagoCompletado.setMonto(BigDecimal.valueOf(700));
        pagoCompletado.setEstado(EstadoPago.COMPLETADO);
        pagoRepository.save(pagoCompletado);

        Pago pagoPendiente = pagoData(estancia, TipoPago.ESTANCIA_COMPLETADA);
        pagoPendiente.setMonto(BigDecimal.valueOf(300));
        pagoPendiente.setEstado(EstadoPago.PENDIENTE);
        pagoRepository.save(pagoPendiente);

        Pago pagoEliminado = pagoData(estancia, TipoPago.CAMBIO_UNIDAD);
        pagoEliminado.setMonto(BigDecimal.valueOf(900));
        pagoEliminado.setEstado(EstadoPago.ELIMINADO);
        pagoRepository.save(pagoEliminado);

        // ---------- WHEN ----------
        BigDecimal total = pagoService.sumarTotalPagosPorEstancia(estancia.getId());

        // ---------- THEN ----------
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void exitoBuscandoPagosConFiltrosYMapeo_test() {

        // ---------- GIVEN ----------
        Unidad unidadEstancia = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estanciaActiva = crearEstanciaExistente(unidadEstancia.getHabitaciones(), false);
        Pago pagoEstancia = pagoService.crearPago(
                pagoNuevoRequestDTO(TipoPago.ANTICIPO_ESTANCIA),
                estanciaActiva
        );

        Unidad unidadReserva = crearApartamento(EstadoOperativo.DISPONIBLE);
        Reserva reserva = crearReservaExistente(unidadReserva.getHabitaciones(), false);
        Estancia estanciaReservada = reserva.getEstancia();
        pagoService.crearPago(
                pagoNuevoRequestDTO(TipoPago.ANTICIPO_RESERVA),
                estanciaReservada
        );

        entityManager.flush();
        entityManager.clear();

        // ---------- WHEN ----------
        Page<PagoDTO> pagosEstancia = pagoService.buscarPagos(
                List.of(EstadoPago.COMPLETADO),
                null,
                TipoPago.ANTICIPO_ESTANCIA,
                estanciaActiva.getCodigoFolio(),
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );

        Page<PagoDTO> pagosReserva = pagoService.buscarPagos(
                List.of(EstadoPago.COMPLETADO),
                null,
                TipoPago.ANTICIPO_RESERVA,
                null,
                reserva.getCodigo(),
                null,
                null,
                PageRequest.of(0, 10)
        );

        // ---------- THEN ----------
        assertThat(pagosEstancia.getContent()).hasSize(1);
        PagoDTO dtoEstancia = pagosEstancia.getContent().getFirst();
        assertThat(dtoEstancia.getId()).isEqualTo(pagoEstancia.getId());
        assertThat(dtoEstancia.getCodigoEstancia()).isEqualTo(estanciaActiva.getCodigoFolio());
        assertThat(dtoEstancia.getCodigoReserva()).isNull();

        assertThat(pagosReserva.getContent()).hasSize(1);
        PagoDTO dtoReserva = pagosReserva.getContent().getFirst();
        assertThat(dtoReserva.getCodigoReserva()).isEqualTo(reserva.getCodigo());
        assertThat(dtoReserva.getCodigoEstancia()).isNull();
        assertThat(dtoReserva.getTipoPago()).isEqualTo(TipoPago.ANTICIPO_RESERVA);
    }
}
