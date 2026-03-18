package com.hotel.services;

import com.hotel.dtos.DetalleCalendarioUnidadDTO;
import com.hotel.dtos.dashboard.DashboardDistribucionFinancieraDTO;
import com.hotel.dtos.dashboard.DashboardResumenDTO;
import com.hotel.models.Estancia;
import com.hotel.models.Gasto;
import com.hotel.models.Habitacion;
import com.hotel.models.Pago;
import com.hotel.models.Reserva;
import com.hotel.models.Unidad;
import com.hotel.models.enums.CanalReserva;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.EstadoPago;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.models.enums.MedioPago;
import com.hotel.models.enums.TipoPago;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.GastoRepository;
import com.hotel.services.support.AbstractServiceIT;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DetalleServiceIT extends AbstractServiceIT {

    @Autowired
    private DetalleService detalleService;

    @Autowired
    private GastoRepository gastoRepository;

    @Test
    void falloObteniendoCalendarioSinDesdeOHasta_test() {

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> detalleService.obtenerCalendario(
                null,
                LocalDateTime.now(),
                null,
                null,
                List.of(EstadoReserva.CONFIRMADA),
                List.of(EstadoEstancia.ACTIVA)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("desde y hasta son obligatorios");
    }

    @Test
    void falloObteniendoCalendarioConRangoInvalido_test() {

        // ---------- GIVEN ----------
        LocalDateTime desde = LocalDateTime.now().plusDays(2);
        LocalDateTime hasta = LocalDateTime.now().plusDays(1);

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> detalleService.obtenerCalendario(
                desde,
                hasta,
                null,
                null,
                List.of(EstadoReserva.CONFIRMADA),
                List.of(EstadoEstancia.ACTIVA)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("desde no puede ser mayor que hasta");
    }

    @Test
    void exitoObteniendoCalendarioSinEstadosRetornaVacio_test() {

        // ---------- GIVEN ----------
        LocalDateTime desde = LocalDateTime.now().minusDays(1);
        LocalDateTime hasta = LocalDateTime.now().plusDays(5);

        // ---------- WHEN ----------
        List<DetalleCalendarioUnidadDTO> resultado = detalleService.obtenerCalendario(
                desde,
                hasta,
                null,
                null,
                List.of(),
                List.of()
        );

        // ---------- THEN ----------
        assertThat(resultado).isEmpty();
    }

    @Test
    void exitoObteniendoCalendarioTipoHabitacionConFiltroCodigo_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacionObjetivo = apartamento.getHabitaciones().getFirst();
        crearReservaExistente(List.of(habitacionObjetivo), false, EstadoReserva.CONFIRMADA);

        LocalDateTime desde = LocalDateTime.now().minusDays(1);
        LocalDateTime hasta = LocalDateTime.now().plusDays(10);

        // ---------- WHEN ----------
        List<DetalleCalendarioUnidadDTO> resultado = detalleService.obtenerCalendario(
                desde,
                hasta,
                TipoUnidad.HABITACION,
                habitacionObjetivo.getCodigo(),
                List.of(EstadoReserva.CONFIRMADA),
                List.of()
        );

        // ---------- THEN ----------
        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().getUnidad().getTipo()).isEqualTo(TipoUnidad.APARTAMENTO);
        assertThat(resultado.getFirst().getUnidad().getCodigo()).isEqualTo(apartamento.getCodigo());
        assertThat(resultado.getFirst().getReservas()).isNotEmpty();
    }

    @Test
    void exitoObteniendoCalendarioTipoApartaestudioSinCodigo_test() {

        // ---------- GIVEN ----------
        Unidad apartaestudio = crearApartaestudio(EstadoOperativo.DISPONIBLE);
        crearReservaExistente(apartaestudio.getHabitaciones(), false, EstadoReserva.CONFIRMADA);

        LocalDateTime desde = LocalDateTime.now().minusDays(1);
        LocalDateTime hasta = LocalDateTime.now().plusDays(10);

        // ---------- WHEN ----------
        List<DetalleCalendarioUnidadDTO> resultado = detalleService.obtenerCalendario(
                desde,
                hasta,
                TipoUnidad.APARTAESTUDIO,
                null,
                List.of(EstadoReserva.CONFIRMADA),
                List.of()
        );

        // ---------- THEN ----------
        assertThat(resultado).isNotEmpty();
        assertThat(resultado)
                .extracting(detalle -> detalle.getUnidad().getTipo())
                .containsOnly(TipoUnidad.APARTAESTUDIO);
        assertThat(resultado)
                .extracting(detalle -> detalle.getUnidad().getCodigo())
                .contains(apartaestudio.getCodigo());
    }

    @Test
    void exitoObteniendoCalendarioDeduplicaReservasPorUnidad_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.DISPONIBLE);
        Reserva reserva = crearReservaExistente(apartamento.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        String codigoFiltro = apartamento.getHabitaciones().getFirst().getCodigo();

        LocalDateTime desde = LocalDateTime.now().minusDays(1);
        LocalDateTime hasta = LocalDateTime.now().plusDays(10);

        // ---------- WHEN ----------
        List<DetalleCalendarioUnidadDTO> resultado = detalleService.obtenerCalendario(
                desde,
                hasta,
                TipoUnidad.APARTAMENTO,
                codigoFiltro,
                List.of(EstadoReserva.CONFIRMADA),
                List.of()
        );

        // ---------- THEN ----------
        assertThat(resultado).hasSize(1);
        DetalleCalendarioUnidadDTO detalle = resultado.getFirst();
        assertThat(detalle.getUnidad().getCodigo()).isEqualTo(apartamento.getCodigo());
        assertThat(detalle.getReservas())
                .extracting(r -> r.getId())
                .containsExactly(reserva.getId());
    }

    @Test
    void exitoObteniendoDashboardResumenConEstructuraEsperada_test() {

        // ---------- WHEN ----------
        DashboardResumenDTO resumen = detalleService.obtenerDashboardResumen();

        // ---------- THEN ----------
        assertThat(resumen).isNotNull();
        assertThat(resumen.getOperativo()).isNotNull();
        assertThat(resumen.getAlertas()).isNotNull();
        assertThat(resumen.getOperativo().getReservasPorCanal())
                .containsKeys(Arrays.stream(CanalReserva.values()).map(Enum::name).toArray(String[]::new));
        assertThat(resumen.getOperativo().getEstanciasUltimos12Meses()).hasSize(12);
        assertThat(resumen.getAlertas().getTotalAlertas())
                .isEqualTo(resumen.getAlertas().getReservasExpiradas() + resumen.getAlertas().getEstanciasExcedidas());
        assertThat(resumen.getOperativo().getOcupacionPorcentaje()).isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
        assertThat(resumen.getOperativo().getReservadasPorcentaje()).isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
    }

    @Test
    void exitoObteniendoDashboardResumenConConteoPorCanalYMeses_test() {

        // ---------- GIVEN ----------
        Unidad unidad1 = crearApartamento(EstadoOperativo.DISPONIBLE);
        Unidad unidad2 = crearApartamento(EstadoOperativo.DISPONIBLE);
        Unidad unidad3 = crearApartamento(EstadoOperativo.OCUPADO);

        Reserva r1 = crearReservaExistente(unidad1.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        r1.setCanalReserva(CanalReserva.WHATSAPP);
        reservaRepository.save(r1);

        Reserva r2 = crearReservaExistente(unidad2.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        r2.setCanalReserva(CanalReserva.WHATSAPP);
        reservaRepository.save(r2);

        Reserva r3 = crearReservaExistente(unidad2.getHabitaciones(), false, EstadoReserva.CONFIRMADA);
        r3.setCanalReserva(CanalReserva.EMAIL);
        reservaRepository.save(r3);

        Estancia e1 = crearEstanciaExistente(unidad3.getHabitaciones(), false, EstadoEstancia.ACTIVA);
        e1.setEntradaReal(LocalDateTime.now().minusMonths(2));
        estanciaRepository.save(e1);

        Estancia e2 = crearEstanciaExistente(unidad3.getHabitaciones(), false, EstadoEstancia.FINALIZADA);
        e2.setEntradaReal(LocalDateTime.now().minusMonths(2).minusDays(1));
        estanciaRepository.save(e2);

        // ---------- WHEN ----------
        DashboardResumenDTO resumen = detalleService.obtenerDashboardResumen();

        // ---------- THEN ----------
        assertThat(resumen.getOperativo().getReservasPorCanal().get(CanalReserva.WHATSAPP.name())).isEqualTo(2L);
        assertThat(resumen.getOperativo().getReservasPorCanal().get(CanalReserva.EMAIL.name())).isEqualTo(1L);
        assertThat(resumen.getOperativo().getReservasPorCanal().get(CanalReserva.TELEFONO.name())).isNotNull();

        long sumaMeses = resumen.getOperativo().getEstanciasUltimos12Meses().stream()
                .mapToLong(item -> item.getCantidad() == null ? 0L : item.getCantidad())
                .sum();
        assertThat(sumaMeses).isGreaterThanOrEqualTo(2L);
        assertThat(resumen.getOperativo().getEstanciasUltimos12Meses()).hasSize(12);
    }

    @Test
    void exitoObteniendoDashboardResumenSinHabitacionesGlobalesPorcentajeCero_test() {

        // ---------- GIVEN ----------
        habitacionRepository.deleteAll();
        unidadRepository.deleteAll();

        // ---------- WHEN ----------
        DashboardResumenDTO resumen = detalleService.obtenerDashboardResumen();

        // ---------- THEN ----------
        assertThat(resumen.getOperativo().getHabitacionesTotales()).isZero();
        assertThat(resumen.getOperativo().getOcupacionPorcentaje()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resumen.getOperativo().getReservadasPorcentaje()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void falloObteniendoDistribucionFinancieraConRangoInvalido_test() {

        // ---------- GIVEN ----------
        LocalDateTime desde = LocalDateTime.of(2099, 1, 10, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2099, 1, 1, 23, 59);

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> detalleService.obtenerDistribucionFinanciera(desde, hasta))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("desde no puede ser mayor que hasta");
    }

    @Test
    void exitoObteniendoDistribucionFinancieraConRangoCalculaTotales_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        Pago pagoCompletadoAnticipo = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        pagoCompletadoAnticipo.setMonto(BigDecimal.valueOf(100));
        pagoCompletadoAnticipo.setEstado(EstadoPago.COMPLETADO);
        pagoCompletadoAnticipo.setMedioPago(MedioPago.EFECTIVO);
        pagoCompletadoAnticipo.setFecha(LocalDateTime.of(2099, 1, 5, 8, 0));
        pagoRepository.save(pagoCompletadoAnticipo);

        Pago pagoCompletadoEstancia = crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA_COMPLETADA);
        pagoCompletadoEstancia.setMonto(BigDecimal.valueOf(60));
        pagoCompletadoEstancia.setEstado(EstadoPago.COMPLETADO);
        pagoCompletadoEstancia.setMedioPago(MedioPago.TARJETA_CREDITO);
        pagoCompletadoEstancia.setFecha(LocalDateTime.of(2099, 1, 5, 9, 0));
        pagoRepository.save(pagoCompletadoEstancia);

        Pago pagoPendienteValido = crearPagoInicialEnEstancia(estancia, TipoPago.ESTANCIA_COMPLETADA);
        pagoPendienteValido.setMonto(BigDecimal.valueOf(40));
        pagoPendienteValido.setEstado(EstadoPago.PENDIENTE);
        pagoPendienteValido.setMedioPago(MedioPago.TRANSFERENCIA_BANCARIA);
        pagoPendienteValido.setFecha(LocalDateTime.of(2099, 1, 5, 10, 0));
        pagoRepository.save(pagoPendienteValido);

        Pago pagoPendienteNoValido = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        pagoPendienteNoValido.setMonto(BigDecimal.valueOf(30));
        pagoPendienteNoValido.setEstado(EstadoPago.PENDIENTE);
        pagoPendienteNoValido.setMedioPago(MedioPago.PLATAFORMA);
        pagoPendienteNoValido.setFecha(LocalDateTime.of(2099, 1, 5, 11, 0));
        pagoRepository.save(pagoPendienteNoValido);

        Gasto gasto = new Gasto();
        gasto.setConcepto("Limpieza");
        gasto.setDescripcion("Gasto de limpieza");
        gasto.setMonto(BigDecimal.valueOf(50));
        gasto.setFecha(LocalDateTime.of(2099, 1, 5, 12, 0));
        gasto.setMetodoPago("EFECTIVO");
        gasto.setReferencia("G-2099-001");
        gasto.setEstado("ACTIVO");
        gastoRepository.save(gasto);

        LocalDateTime desde = LocalDateTime.of(2099, 1, 1, 0, 0);
        LocalDateTime hasta = LocalDateTime.of(2099, 1, 31, 23, 59);

        // ---------- WHEN ----------
        DashboardDistribucionFinancieraDTO dto = detalleService.obtenerDistribucionFinanciera(desde, hasta);

        // ---------- THEN ----------
        assertThat(dto.getIngresosPagos()).isEqualByComparingTo(BigDecimal.valueOf(160));
        assertThat(dto.getIngresosPendientesMonto()).isEqualByComparingTo(BigDecimal.valueOf(40));
        assertThat(dto.getIngresosPendientesCantidad()).isEqualTo(1L);
        assertThat(dto.getIngresosTotales()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(dto.getGastos()).isEqualByComparingTo(BigDecimal.valueOf(50));
        assertThat(dto.getNeto()).isEqualByComparingTo(BigDecimal.valueOf(110));
        assertThat(dto.getIngresosPorTipoPago())
                .anyMatch(item -> "ANTICIPO_ESTANCIA".equals(item.getCategoria())
                        && item.getMonto().compareTo(BigDecimal.valueOf(100)) == 0);
        assertThat(dto.getIngresosPorTipoPago())
                .anyMatch(item -> "ESTANCIA_COMPLETADA".equals(item.getCategoria())
                        && item.getMonto().compareTo(BigDecimal.valueOf(100)) == 0);
        assertThat(dto.getIngresosPorMedioPago())
                .anyMatch(item -> "TRANSFERENCIA_BANCARIA".equals(item.getCategoria())
                        && item.getMonto().compareTo(BigDecimal.valueOf(40)) == 0
                        && item.getConteoPagos() == 1);
    }

    @Test
    void exitoObteniendoDistribucionFinancieraSinRangoConFormulaConsistente_test() {

        // ---------- WHEN ----------
        DashboardDistribucionFinancieraDTO dto = detalleService.obtenerDistribucionFinanciera(null, null);

        // ---------- THEN ----------
        assertThat(dto).isNotNull();
        assertThat(dto.getIngresosTotales())
                .isEqualByComparingTo(dto.getIngresosPagos().add(dto.getIngresosPendientesMonto()));
        assertThat(dto.getNeto())
                .isEqualByComparingTo(dto.getIngresosPagos().subtract(dto.getGastos()));
        assertThat(dto.getIngresosPorTipoPago()).isNotNull();
        assertThat(dto.getIngresosPorMedioPago()).isNotNull();
    }

    @Test
    void exitoObteniendoCalendarioSinTipoConFiltroCodigoHabitacion_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.DISPONIBLE);
        Habitacion habitacionObjetivo = apartamento.getHabitaciones().getFirst();
        crearReservaExistente(List.of(habitacionObjetivo), false, EstadoReserva.CONFIRMADA);

        LocalDateTime desde = LocalDateTime.now().minusDays(1);
        LocalDateTime hasta = LocalDateTime.now().plusDays(10);

        // ---------- WHEN ----------
        List<DetalleCalendarioUnidadDTO> resultado = detalleService.obtenerCalendario(
                desde,
                hasta,
                null,
                habitacionObjetivo.getCodigo(),
                List.of(EstadoReserva.CONFIRMADA),
                List.of()
        );

        // ---------- THEN ----------
        assertThat(resultado).isNotEmpty();
        assertThat(resultado)
                .extracting(detalle -> detalle.getUnidad().getCodigo())
                .contains(apartamento.getCodigo());
    }

    @Test
    void exitoObteniendoCalendarioTipoApartamentoSinCodigo_test() {

        // ---------- GIVEN ----------
        Unidad apartamento = crearApartamento(EstadoOperativo.DISPONIBLE);
        crearReservaExistente(apartamento.getHabitaciones(), false, EstadoReserva.CONFIRMADA);

        LocalDateTime desde = LocalDateTime.now().minusDays(1);
        LocalDateTime hasta = LocalDateTime.now().plusDays(10);

        // ---------- WHEN ----------
        List<DetalleCalendarioUnidadDTO> resultado = detalleService.obtenerCalendario(
                desde,
                hasta,
                TipoUnidad.APARTAMENTO,
                null,
                List.of(EstadoReserva.CONFIRMADA),
                List.of()
        );

        // ---------- THEN ----------
        assertThat(resultado).isNotEmpty();
        assertThat(resultado)
                .extracting(detalle -> detalle.getUnidad().getTipo())
                .containsOnly(TipoUnidad.APARTAMENTO);
        assertThat(resultado)
                .extracting(detalle -> detalle.getUnidad().getCodigo())
                .contains(apartamento.getCodigo());
    }

    @Test
    void exitoObteniendoCalendarioSoloEstadosEstancia_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        LocalDateTime desde = LocalDateTime.now().minusDays(1);
        LocalDateTime hasta = LocalDateTime.now().plusDays(10);

        // ---------- WHEN ----------
        List<DetalleCalendarioUnidadDTO> resultado = detalleService.obtenerCalendario(
                desde,
                hasta,
                TipoUnidad.APARTAMENTO,
                unidad.getHabitaciones().getFirst().getCodigo(),
                List.of(),
                List.of(EstadoEstancia.ACTIVA)
        );

        // ---------- THEN ----------
        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().getEstancias())
                .extracting(e -> e.getId())
                .contains(estancia.getId());
        assertThat(resultado.getFirst().getReservas()).isEmpty();
    }

    @Test
    void exitoObteniendoCalendarioDeduplicaEstanciasPorUnidad_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        LocalDateTime desde = LocalDateTime.now().minusDays(1);
        LocalDateTime hasta = LocalDateTime.now().plusDays(10);

        // ---------- WHEN ----------
        List<DetalleCalendarioUnidadDTO> resultado = detalleService.obtenerCalendario(
                desde,
                hasta,
                TipoUnidad.APARTAMENTO,
                unidad.getHabitaciones().getFirst().getCodigo(),
                List.of(),
                List.of(EstadoEstancia.ACTIVA)
        );

        // ---------- THEN ----------
        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().getEstancias())
                .extracting(e -> e.getId())
                .containsExactly(estancia.getId());
    }

    @Test
    void exitoObteniendoDistribucionFinancieraSoloDesde_test() {

        // ---------- GIVEN ----------
        Unidad unidad = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estancia = crearEstanciaExistente(unidad.getHabitaciones(), false, EstadoEstancia.ACTIVA);

        Pago pagoViejo = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        pagoViejo.setMonto(BigDecimal.valueOf(10));
        pagoViejo.setEstado(EstadoPago.COMPLETADO);
        pagoViejo.setFecha(LocalDateTime.of(2098, 12, 31, 10, 0));
        pagoRepository.save(pagoViejo);

        Pago pagoNuevo = crearPagoInicialEnEstancia(estancia, TipoPago.ANTICIPO_ESTANCIA);
        pagoNuevo.setMonto(BigDecimal.valueOf(25));
        pagoNuevo.setEstado(EstadoPago.COMPLETADO);
        pagoNuevo.setFecha(LocalDateTime.of(2099, 1, 2, 10, 0));
        pagoRepository.save(pagoNuevo);

        // ---------- WHEN ----------
        DashboardDistribucionFinancieraDTO dto = detalleService.obtenerDistribucionFinanciera(
                LocalDateTime.of(2099, 1, 1, 0, 0),
                null
        );

        // ---------- THEN ----------
        assertThat(dto.getIngresosPagos()).isEqualByComparingTo(BigDecimal.valueOf(25));
    }

    @Test
    void exitoObteniendoDistribucionFinancieraRangoSinDatosRetornaCeros_test() {

        // ---------- WHEN ----------
        DashboardDistribucionFinancieraDTO dto = detalleService.obtenerDistribucionFinanciera(
                LocalDateTime.of(2200, 1, 1, 0, 0),
                LocalDateTime.of(2200, 1, 31, 23, 59)
        );

        // ---------- THEN ----------
        assertThat(dto.getIngresosPagos()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getIngresosPendientesMonto()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getIngresosPendientesCantidad()).isEqualTo(0L);
        assertThat(dto.getIngresosTotales()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getGastos()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(dto.getNeto()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
