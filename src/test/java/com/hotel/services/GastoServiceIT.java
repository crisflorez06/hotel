package com.hotel.services;

import com.hotel.dtos.gasto.GastoDTO;
import com.hotel.dtos.gasto.GastoNuevoRequestDTO;
import com.hotel.repositories.GastoRepository;
import com.hotel.services.support.AbstractServiceIT;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
class GastoServiceIT extends AbstractServiceIT {

    @Autowired
    private GastoService gastoService;

    @Autowired
    private GastoRepository gastoRepository;

    @Test
    void exitoCreandoGasto_test() {

        // ---------- GIVEN ----------
        GastoNuevoRequestDTO request = gastoRequest(
                "Mantenimiento",
                "Cambio de cerradura",
                BigDecimal.valueOf(120000),
                LocalDateTime.of(2030, 1, 10, 9, 30),
                "EFECTIVO",
                "REF-001",
                "ACTIVO"
        );

        // ---------- WHEN ----------
        GastoDTO dto = gastoService.crear(request);

        // ---------- THEN ----------
        assertThat(dto.getId()).isNotNull();
        assertThat(dto.getConcepto()).isEqualTo("Mantenimiento");
        assertThat(dto.getMonto()).isEqualByComparingTo(BigDecimal.valueOf(120000));
        assertThat(gastoRepository.findById(dto.getId())).isPresent();
    }

    @Test
    void exitoListandoGastos_test() {

        // ---------- GIVEN ----------
        gastoService.crear(gastoRequest(
                "Limpieza",
                "Productos de limpieza",
                BigDecimal.valueOf(50000),
                LocalDateTime.of(2030, 2, 1, 8, 0),
                "TRANSFERENCIA",
                "REF-002",
                "ACTIVO"
        ));
        gastoService.crear(gastoRequest(
                "Servicios",
                "Pago internet",
                BigDecimal.valueOf(90000),
                LocalDateTime.of(2030, 2, 2, 10, 0),
                "TARJETA",
                "REF-003",
                "ACTIVO"
        ));

        // ---------- WHEN ----------
        List<GastoDTO> gastos = gastoService.listar();

        // ---------- THEN ----------
        assertThat(gastos).hasSize(2);
        assertThat(gastos).extracting(GastoDTO::getConcepto)
                .containsExactlyInAnyOrder("Limpieza", "Servicios");
    }

    @Test
    void exitoObteniendoGastoPorId_test() {

        // ---------- GIVEN ----------
        GastoDTO creado = gastoService.crear(gastoRequest(
                "Papeleria",
                "Compra de hojas",
                BigDecimal.valueOf(30000),
                LocalDateTime.of(2030, 3, 5, 11, 0),
                "EFECTIVO",
                "REF-004",
                "ACTIVO"
        ));

        // ---------- WHEN ----------
        GastoDTO obtenido = gastoService.obtenerPorId(creado.getId());

        // ---------- THEN ----------
        assertThat(obtenido.getId()).isEqualTo(creado.getId());
        assertThat(obtenido.getConcepto()).isEqualTo("Papeleria");
    }

    @Test
    void falloObteniendoGastoPorIdInexistente_test() {

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> gastoService.obtenerPorId(Long.MAX_VALUE))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Gasto no encontrado con id: " + Long.MAX_VALUE);
    }

    @Test
    void exitoActualizandoGasto_test() {

        // ---------- GIVEN ----------
        GastoDTO creado = gastoService.crear(gastoRequest(
                "Mantenimiento",
                "Inicial",
                BigDecimal.valueOf(100000),
                LocalDateTime.of(2030, 4, 1, 8, 0),
                "EFECTIVO",
                "REF-005",
                "ACTIVO"
        ));

        GastoNuevoRequestDTO update = gastoRequest(
                "Mantenimiento Correctivo",
                "Cambio de aire acondicionado",
                BigDecimal.valueOf(250000),
                LocalDateTime.of(2030, 4, 3, 15, 0),
                "TRANSFERENCIA",
                "REF-005-A",
                "APROBADO"
        );

        // ---------- WHEN ----------
        GastoDTO actualizado = gastoService.actualizar(creado.getId(), update);

        // ---------- THEN ----------
        assertThat(actualizado.getId()).isEqualTo(creado.getId());
        assertThat(actualizado.getConcepto()).isEqualTo("Mantenimiento Correctivo");
        assertThat(actualizado.getMonto()).isEqualByComparingTo(BigDecimal.valueOf(250000));
        assertThat(actualizado.getEstado()).isEqualTo("APROBADO");
    }

    @Test
    void falloActualizandoGastoInexistente_test() {

        // ---------- GIVEN ----------
        GastoNuevoRequestDTO update = gastoRequest(
                "No existe",
                "No existe",
                BigDecimal.valueOf(1),
                LocalDateTime.of(2030, 1, 1, 0, 0),
                "EFECTIVO",
                "X",
                "ACTIVO"
        );

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> gastoService.actualizar(Long.MAX_VALUE, update))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Gasto no encontrado con id: " + Long.MAX_VALUE);
    }

    @Test
    void exitoEliminandoGasto_test() {

        // ---------- GIVEN ----------
        GastoDTO creado = gastoService.crear(gastoRequest(
                "Consumo",
                "Agua",
                BigDecimal.valueOf(70000),
                LocalDateTime.of(2030, 5, 1, 7, 0),
                "EFECTIVO",
                "REF-006",
                "ACTIVO"
        ));

        // ---------- WHEN ----------
        gastoService.eliminar(creado.getId());

        // ---------- THEN ----------
        assertThat(gastoRepository.findById(creado.getId())).isNotPresent();
    }

    @Test
    void falloEliminandoGastoInexistente_test() {

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> gastoService.eliminar(Long.MAX_VALUE))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Gasto no encontrado con id: " + Long.MAX_VALUE);
    }

    private GastoNuevoRequestDTO gastoRequest(
            String concepto,
            String descripcion,
            BigDecimal monto,
            LocalDateTime fecha,
            String metodoPago,
            String referencia,
            String estado
    ) {
        GastoNuevoRequestDTO request = new GastoNuevoRequestDTO();
        request.setConcepto(concepto);
        request.setDescripcion(descripcion);
        request.setMonto(monto);
        request.setFecha(fecha);
        request.setMetodoPago(metodoPago);
        request.setReferencia(referencia);
        request.setEstado(estado);
        return request;
    }
}
