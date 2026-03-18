package com.hotel.services;

import com.hotel.dtos.estancia.ActivarEstanciaDTO;
import com.hotel.dtos.estancia.EstanciaRequestDTO;
import com.hotel.dtos.estancia.SalidaEstanciaDTO;
import com.hotel.dtos.pago.PagoNuevoRequestDTO;
import com.hotel.dtos.reserva.ReservaRequestDTO;
import com.hotel.dtos.tarifa.TarifaBaseActualizacionDTO;
import com.hotel.models.AjusteTemporada;
import com.hotel.models.AuditoriaEvento;
import com.hotel.models.Estancia;
import com.hotel.models.Reserva;
import com.hotel.models.TarifaBase;
import com.hotel.models.Unidad;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.EstadoPago;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.models.enums.Temporada;
import com.hotel.models.enums.TipoEntidad;
import com.hotel.models.enums.TipoEvento;
import com.hotel.models.enums.TipoPago;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.AjusteTemporadaRepository;
import com.hotel.repositories.AuditoriaEventoRepository;
import com.hotel.repositories.TarifaBaseRepository;
import com.hotel.services.support.AbstractServiceIT;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static com.hotel.testdata.EstanciaTestData.activarEstanciaRequestDTO;
import static com.hotel.testdata.EstanciaTestData.estanciaRequestDTO;
import static com.hotel.testdata.EstanciaTestData.salidaEstanciaRequestDTO;
import static com.hotel.testdata.OcupanteTestData.clienteData;
import static com.hotel.testdata.PagoTestData.pagoNuevoRequestDTO;
import static com.hotel.testdata.ReservaTestData.reservaRequestDTO;
import static com.hotel.testutils.AuditoriaEventoTestHelper.assertEventoBasico;
import static com.hotel.testutils.AuditoriaEventoTestHelper.parseDetalleCambios;
import static com.hotel.testutils.AuditoriaEventoTestHelper.parseDetalleNuevo;
import static com.hotel.testutils.AuditoriaEventoTestHelper.ultimoEvento;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditoriaEventoMovimientosIT extends AbstractServiceIT {

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private EstanciaService estanciaService;

    @Autowired
    private PagoService pagoService;

    @Autowired
    private TarifaBaseAdminService tarifaBaseAdminService;

    @Autowired
    private AjusteTemporadaService ajusteTemporadaService;

    @Autowired
    private AuditoriaEventoRepository auditoriaEventoRepository;

    @Autowired
    private TarifaBaseRepository tarifaBaseRepository;

    @Autowired
    private AjusteTemporadaRepository ajusteTemporadaRepository;

    @Test
    void exitoAuditoriaCreacionReservaDesdeMovimiento_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        var cliente = crearCliente(clienteData());
        ReservaRequestDTO request = reservaRequestDTO(unidad.getTipo(), unidad.getCodigo(), cliente, LocalDate.now().plusDays(2));

        // ---------- WHEN ----------
        var reservaDto = reservaService.crearReserva(request);

        // ---------- THEN ----------
        AuditoriaEvento evento = ultimoEvento(auditoriaEventoRepository, TipoEntidad.RESERVA, reservaDto.getId());
        assertEventoBasico(evento, TipoEvento.CREACION_RESERVA, TipoEntidad.RESERVA, reservaDto.getId(), null, reservaDto.getCodigoReserva());

        Map<String, Object> detalle = parseDetalleNuevo(evento.getDetalle());
        assertThat(detalle).containsKeys("codigoReserva", "unidad", "fechaEntrada", "fechaSalida");
        assertThat(detalle.get("codigoReserva")).isEqualTo(reservaDto.getCodigoReserva());
        assertThat(detalle.get("unidad")).isEqualTo(unidad.getCodigo());
    }

    @Test
    void exitoAuditoriaActivacionEstanciaDesdeMovimiento_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        ActivarEstanciaDTO request = activarEstanciaRequestDTO(
                reserva.getId(),
                reserva.getCliente(),
                null,
                LocalDateTime.now().minusHours(2)
        );

        // ---------- WHEN ----------
        var estanciaDto = estanciaService.activarEstancia(request);

        // ---------- THEN ----------
        AuditoriaEvento evento = ultimoEvento(auditoriaEventoRepository, TipoEntidad.ESTANCIA, estanciaDto.getId());
        assertEventoBasico(
                evento,
                TipoEvento.ACTIVACION_ESTANCIA,
                TipoEntidad.ESTANCIA,
                estanciaDto.getId(),
                estanciaDto.getCodigoFolio(),
                reserva.getCodigo()
        );

        Map<String, Object> detalle = parseDetalleNuevo(evento.getDetalle());
        assertThat(detalle).containsKeys("codigoEstancia", "unidad", "fechaEntrada", "fechaSalida");
        assertThat(detalle.get("codigoEstancia")).isEqualTo(estanciaDto.getCodigoFolio());
    }

    @Test
    void exitoAuditoriaFinalizacionEstanciaDesdeMovimiento_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        PagoNuevoRequestDTO pagoRequest = pagoNuevoRequestDTO(TipoPago.ESTANCIA_COMPLETADA);
        pagoRequest.setEstado(EstadoPago.COMPLETADO);
        SalidaEstanciaDTO request = salidaEstanciaRequestDTO(estancia.getId(), LocalDateTime.now(), pagoRequest);

        // ---------- WHEN ----------
        estanciaService.finalizarEstancia(request);

        // ---------- THEN ----------
        AuditoriaEvento evento = auditoriaEventoRepository.findByEntidadAndIdEntidadOrderByFechaDesc(TipoEntidad.ESTANCIA, estancia.getId())
                .stream()
                .filter(e -> e.getTipoEvento() == TipoEvento.FINALIZACION_ESTANCIA)
                .findFirst()
                .orElseThrow();

        assertEventoBasico(
                evento,
                TipoEvento.FINALIZACION_ESTANCIA,
                TipoEntidad.ESTANCIA,
                estancia.getId(),
                estancia.getCodigoFolio(),
                null
        );

        Map<String, Object> detalle = parseDetalleNuevo(evento.getDetalle());
        assertThat(detalle).containsKeys("codigoEstancia", "unidad", "fechaEntrada", "fechaSalida", "precioTotal");
        assertThat(detalle.get("codigoEstancia")).isEqualTo(estancia.getCodigoFolio());
    }

    @Test
    void exitoAuditoriaCreacionPagoEnEstanciaReservadaUsaCodigoReserva_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        Reserva reserva = crearReservaExistente(unidad.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        PagoNuevoRequestDTO request = pagoNuevoRequestDTO(TipoPago.ANTICIPO_RESERVA);

        // ---------- WHEN ----------
        var pagoDto = pagoService.crearPago(request, reserva.getEstancia().getId(), true);

        // ---------- THEN ----------
        AuditoriaEvento evento = ultimoEvento(auditoriaEventoRepository, TipoEntidad.PAGO, pagoDto.getId());
        assertEventoBasico(
                evento,
                TipoEvento.CREACION_PAGO,
                TipoEntidad.PAGO,
                pagoDto.getId(),
                null,
                reserva.getCodigo()
        );

        Map<String, Object> detalle = parseDetalleNuevo(evento.getDetalle());
        assertThat(detalle).containsKeys("codigoEstancia", "tipoPago", "monto", "montoEstimado", "estado");
        assertThat(detalle.get("tipoPago")).isEqualTo("ANTICIPO_RESERVA");
    }

    @Test
    void exitoAuditoriaCreacionRecargoCambioUnidadDesdeMovimiento_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        // ---------- WHEN ----------
        var pagoRecargo = pagoService.crearPagoPorCambioUnidad(estancia, TipoUnidad.APARTAMENTO);

        // ---------- THEN ----------
        AuditoriaEvento evento = ultimoEvento(auditoriaEventoRepository, TipoEntidad.PAGO, pagoRecargo.getId());
        assertEventoBasico(
                evento,
                TipoEvento.CREACION_RECARGO,
                TipoEntidad.PAGO,
                pagoRecargo.getId(),
                estancia.getCodigoFolio(),
                null
        );

        Map<String, Object> detalle = parseDetalleNuevo(evento.getDetalle());
        assertThat(detalle).containsKeys("codigoEstancia", "monto", "montoEstimado", "estado", "motivo");
        assertThat(detalle.get("motivo")).isEqualTo("Cambio de unidad");
    }

    @Test
    void exitoAuditoriaModificacionPagoPendienteDesdeMovimiento_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        var pago = crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA_COMPLETADA);
        pago.setEstado(EstadoPago.PENDIENTE);
        pagoRepository.save(pago);

        // ---------- WHEN ----------
        pagoService.pagarPagoPendiente(pago.getId());

        // ---------- THEN ----------
        AuditoriaEvento evento = ultimoEvento(auditoriaEventoRepository, TipoEntidad.PAGO, pago.getId());
        assertEventoBasico(
                evento,
                TipoEvento.MODIFICACION_PAGO,
                TipoEntidad.PAGO,
                pago.getId(),
                estancia.getCodigoFolio(),
                null
        );

        Map<String, Map<String, Object>> detalle = parseDetalleCambios(evento.getDetalle());
        assertThat(detalle).containsKey("Estado");
        assertThat(detalle.get("Estado")).containsEntry("anterior", "PENDIENTE");
        assertThat(detalle.get("Estado")).containsEntry("nuevo", "COMPLETADO");
    }

    @Test
    void exitoAuditoriaModificacionTarifaBaseDesdeMovimiento_test() {

        // ---------- GIVEN ----------
        TarifaBase tarifa = tarifaBaseRepository.findByTipoUnidad(TipoUnidad.APARTAMENTO).orElseThrow();
        TarifaBaseActualizacionDTO request = new TarifaBaseActualizacionDTO();
        request.setTipoUnidad(TipoUnidad.APARTAMENTO);
        request.setPrecioDiaTemBaja(tarifa.getPrecioDiaTemBaja().add(BigDecimal.valueOf(1000)));
        request.setPrecioDiaTemAlta(tarifa.getPrecioDiaTemAlta());
        request.setPrecioEstadiaCorta(tarifa.getPrecioEstadiaCorta());
        request.setPrecioPersonaAdicionalTemBaja(tarifa.getPrecioPersonaAdicionalTemBaja());
        request.setPrecioPersonaAdicionalTemAlta(tarifa.getPrecioPersonaAdicionalTemAlta());
        request.setPrecioEstadiaPersonaAdicionalCorta(tarifa.getPrecioEstadiaPersonaAdicionalCorta());

        // ---------- WHEN ----------
        tarifaBaseAdminService.actualizarTarifasBase(request);

        // ---------- THEN ----------
        AuditoriaEvento evento = ultimoEvento(auditoriaEventoRepository, TipoEntidad.TARIFA_BASE, tarifa.getId());
        assertEventoBasico(
                evento,
                TipoEvento.MODIFICACION_TARIFA_BASE,
                TipoEntidad.TARIFA_BASE,
                tarifa.getId(),
                null,
                null
        );

        Map<String, Map<String, Object>> detalle = parseDetalleCambios(evento.getDetalle());
        assertThat(detalle).containsKey("precioDiaTemBaja");
        assertThat(detalle.get("precioDiaTemBaja")).containsKeys("anterior", "nuevo");
    }

    @Test
    void exitoAuditoriaModificacionTemporadaActivaDesdeMovimiento_test() {

        // ---------- GIVEN ----------
        Temporada actual = ajusteTemporadaService.obtenerTemporadaActiva();
        Temporada nueva = actual == Temporada.ALTA ? Temporada.BAJA : Temporada.ALTA;

        // ---------- WHEN ----------
        ajusteTemporadaService.cambiarTemporadaActiva(nueva);

        // ---------- THEN ----------
        AjusteTemporada ajuste = ajusteTemporadaRepository.findByTemporada(nueva).orElseThrow();
        AuditoriaEvento evento = ultimoEvento(auditoriaEventoRepository, TipoEntidad.AJUSTE_TEMPORADA, ajuste.getId());
        assertEventoBasico(
                evento,
                TipoEvento.MODIFICACION_TEMPORADA_ACTIVA,
                TipoEntidad.AJUSTE_TEMPORADA,
                ajuste.getId(),
                null,
                null
        );

        Map<String, Map<String, Object>> detalle = parseDetalleCambios(evento.getDetalle());
        assertThat(detalle).containsKey("temporadaActiva");
        assertThat(detalle.get("temporadaActiva")).containsEntry("anterior", actual.name());
        assertThat(detalle.get("temporadaActiva")).containsEntry("nuevo", nueva.name());
    }

    @Test
    void exitoAuditoriaCreacionEstanciaNuevaDesdeMovimiento_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.DISPONIBLE);
        var cliente = crearCliente(clienteData());
        EstanciaRequestDTO request = estanciaRequestDTO(
                unidad.getTipo(),
                unidad.getCodigo(),
                cliente,
                null,
                LocalDateTime.now().minusHours(1)
        );

        // ---------- WHEN ----------
        var estanciaDto = estanciaService.crearEstanciaNueva(request);

        // ---------- THEN ----------
        AuditoriaEvento evento = ultimoEvento(auditoriaEventoRepository, TipoEntidad.ESTANCIA, estanciaDto.getId());
        assertEventoBasico(
                evento,
                TipoEvento.CREACION_ESTANCIA,
                TipoEntidad.ESTANCIA,
                estanciaDto.getId(),
                estanciaDto.getCodigoFolio(),
                null
        );

        Map<String, Object> detalle = parseDetalleNuevo(evento.getDetalle());
        assertThat(detalle).containsKeys("codigoEstancia", "unidad", "fechaEntrada", "fechaSalida");
        assertThat(detalle.get("unidad")).isEqualTo(unidad.getCodigo());
    }
}
