package com.hotel.services;

import com.hotel.dtos.auditoria.AuditoriaEventoDTO;
import com.hotel.models.AuditoriaEvento;
import com.hotel.models.enums.TipoEntidad;
import com.hotel.models.enums.TipoEvento;
import com.hotel.repositories.AuditoriaEventoRepository;
import com.hotel.services.support.AbstractServiceIT;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditoriaEventoServiceIT extends AbstractServiceIT {

    @Autowired
    private AuditoriaEventoService auditoriaEventoService;

    @Autowired
    private AuditoriaEventoRepository auditoriaEventoRepository;

    @Test
    void falloCreandoEventoSinTipoEvento_test() {

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> auditoriaEventoService.crearEvento(
                null,
                TipoEntidad.RESERVA,
                1L,
                "{}",
                null,
                "RES-1"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tipoEvento es obligatorio");
    }

    @Test
    void falloCreandoEventoSinEntidad_test() {

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> auditoriaEventoService.crearEvento(
                TipoEvento.CREACION_RESERVA,
                null,
                1L,
                "{}",
                null,
                "RES-1"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("entidad es obligatoria");
    }

    @Test
    void falloCreandoEventoSinIdEntidad_test() {

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> auditoriaEventoService.crearEvento(
                TipoEvento.CREACION_RESERVA,
                TipoEntidad.RESERVA,
                null,
                "{}",
                null,
                "RES-1"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("idEntidad es obligatorio");
    }

    @Test
    void falloCreandoEventoSinCodigosEnEntidadOperativa_test() {

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> auditoriaEventoService.crearEvento(
                TipoEvento.CREACION_ESTANCIA,
                TipoEntidad.ESTANCIA,
                99L,
                "{}",
                null,
                null
        ))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("AuditoriaEvento requiere codigoEstancia o codigoReserva");
    }

    @Test
    void exitoCreandoEventoParaEntidadNoOperativaSinCodigos_test() {

        // ---------- WHEN ----------
        AuditoriaEvento evento = auditoriaEventoService.crearEvento(
                TipoEvento.MODIFICACION_TARIFA_BASE,
                TipoEntidad.TARIFA_BASE,
                10L,
                "{\"precio\":{\"anterior\":1,\"nuevo\":2}}",
                null,
                null
        );

        // ---------- THEN ----------
        assertThat(evento.getId()).isNotNull();
        assertThat(evento.getCodigoEstancia()).isNull();
        assertThat(evento.getCodigoReserva()).isNull();
        assertThat(evento.getEntidad()).isEqualTo(TipoEntidad.TARIFA_BASE);
        assertThat(evento.getTipoEvento()).isEqualTo(TipoEvento.MODIFICACION_TARIFA_BASE);
    }

    @Test
    void exitoBuscandoEventosConOrdenPorDefectoDesc_test() {

        // ---------- GIVEN ----------
        AuditoriaEvento viejo = auditoriaEventoService.crearEvento(
                TipoEvento.CREACION_RESERVA,
                TipoEntidad.RESERVA,
                1L,
                "{\"codigoReserva\":\"RES-1\"}",
                null,
                "RES-1"
        );

        AuditoriaEvento nuevo = auditoriaEventoService.crearEvento(
                TipoEvento.ELIMINACION_RESERVA,
                TipoEntidad.RESERVA,
                2L,
                "{\"codigoReserva\":\"RES-2\"}",
                null,
                "RES-2"
        );

        viejo.setFecha(LocalDateTime.of(2030, 1, 1, 10, 0));
        nuevo.setFecha(LocalDateTime.of(2030, 1, 1, 12, 0));
        auditoriaEventoRepository.save(viejo);
        auditoriaEventoRepository.save(nuevo);

        // ---------- WHEN ----------
        Page<AuditoriaEventoDTO> pagina = auditoriaEventoService.buscarEventos(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 10)
        );

        // ---------- THEN ----------
        assertThat(pagina.getContent()).isNotEmpty();
        assertThat(pagina.getContent().getFirst().getId()).isEqualTo(nuevo.getId());
    }

    @Test
    void exitoBuscandoEventosConFiltrosPorCodigoFechasYDetalle_test() {

        // ---------- GIVEN ----------
        AuditoriaEvento match = auditoriaEventoService.crearEvento(
                TipoEvento.CREACION_ESTANCIA,
                TipoEntidad.ESTANCIA,
                11L,
                "{\"unidad\":\"UN-ALFA\"}",
                "EST-FILTRO-1",
                null
        );
        match.setFecha(LocalDateTime.of(2031, 6, 10, 9, 0));
        auditoriaEventoRepository.save(match);

        AuditoriaEvento noMatch = auditoriaEventoService.crearEvento(
                TipoEvento.CREACION_ESTANCIA,
                TipoEntidad.ESTANCIA,
                12L,
                "{\"unidad\":\"UN-BETA\"}",
                "EST-OTRO-2",
                null
        );
        noMatch.setFecha(LocalDateTime.of(2031, 6, 20, 9, 0));
        auditoriaEventoRepository.save(noMatch);

        // ---------- WHEN ----------
        Page<AuditoriaEventoDTO> pagina = auditoriaEventoService.buscarEventos(
                List.of(TipoEvento.CREACION_ESTANCIA),
                null,
                null,
                "filtro",
                null,
                LocalDateTime.of(2031, 6, 1, 0, 0),
                LocalDateTime.of(2031, 6, 15, 23, 59),
                "alfa",
                PageRequest.of(0, 10)
        );

        // ---------- THEN ----------
        assertThat(pagina.getContent()).hasSize(1);
        assertThat(pagina.getContent().getFirst().getId()).isEqualTo(match.getId());
        assertThat(pagina.getContent().getFirst().getCodigoEstancia()).isEqualTo("EST-FILTRO-1");
    }

    @Test
    void exitoBuscandoEventosConTipoYEntidadAplicaOr_test() {

        // ---------- GIVEN ----------
        AuditoriaEvento porTipo = auditoriaEventoService.crearEvento(
                TipoEvento.CREACION_RESERVA,
                TipoEntidad.RESERVA,
                21L,
                "{}",
                null,
                "RES-OR-1"
        );

        AuditoriaEvento porEntidad = auditoriaEventoService.crearEvento(
                TipoEvento.MODIFICACION_ESTANCIA,
                TipoEntidad.ESTANCIA,
                22L,
                "{}",
                "EST-OR-2",
                null
        );

        // ---------- WHEN ----------
        Page<AuditoriaEventoDTO> pagina = auditoriaEventoService.buscarEventos(
                List.of(TipoEvento.CREACION_RESERVA),
                List.of(TipoEntidad.ESTANCIA),
                null,
                null,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 20)
        );

        // ---------- THEN ----------
        assertThat(pagina.getContent())
                .extracting(AuditoriaEventoDTO::getId)
                .contains(porTipo.getId(), porEntidad.getId());
    }

    @Test
    void exitoBuscandoEventosRespetaSortExplicito_test() {

        // ---------- GIVEN ----------
        AuditoriaEvento primero = auditoriaEventoService.crearEvento(
                TipoEvento.CREACION_RESERVA,
                TipoEntidad.RESERVA,
                31L,
                "{}",
                null,
                "RES-SORT-1"
        );
        primero.setFecha(LocalDateTime.of(2032, 1, 1, 10, 0));
        auditoriaEventoRepository.save(primero);

        AuditoriaEvento segundo = auditoriaEventoService.crearEvento(
                TipoEvento.CREACION_RESERVA,
                TipoEntidad.RESERVA,
                32L,
                "{}",
                null,
                "RES-SORT-2"
        );
        segundo.setFecha(LocalDateTime.of(2032, 1, 1, 12, 0));
        auditoriaEventoRepository.save(segundo);

        // ---------- WHEN ----------
        Page<AuditoriaEventoDTO> pagina = auditoriaEventoService.buscarEventos(
                null,
                null,
                null,
                null,
                "SORT",
                null,
                null,
                null,
                PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "fecha"))
        );

        // ---------- THEN ----------
        assertThat(pagina.getContent()).isNotEmpty();
        assertThat(pagina.getContent())
                .extracting(AuditoriaEventoDTO::getId)
                .contains(primero.getId(), segundo.getId());
    }
}
