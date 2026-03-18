package com.hotel.services;

import com.hotel.dtos.ocupante.ClienteTablaDTO;
import com.hotel.dtos.ocupante.OcupanteDTO;
import com.hotel.dtos.ocupante.OcupanteNuevoRequestDTO;
import com.hotel.models.Estancia;
import com.hotel.models.Ocupante;
import com.hotel.models.Reserva;
import com.hotel.models.Unidad;
import com.hotel.models.enums.EstadoEstancia;
import com.hotel.models.enums.EstadoOperativo;
import com.hotel.models.enums.EstadoReserva;
import com.hotel.models.enums.ModoOcupacion;
import com.hotel.models.enums.TipoDocumento;
import com.hotel.models.enums.TipoOcupante;
import com.hotel.services.support.AbstractServiceIT;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static com.hotel.testdata.EstanciaTestData.estanciaData;
import static com.hotel.testdata.OcupanteTestData.acompanantesData;
import static com.hotel.testdata.OcupanteTestData.clienteData;
import static com.hotel.testdata.OcupanteTestData.clienteEditarData;
import static com.hotel.testdata.ReservaTestData.reservaData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OcupanteServiceIT extends AbstractServiceIT {

    @Autowired
    private OcupanteService ocupanteService;

    @Test
    void exitoBuscandoOcupantePorId_test() {

        // ---------- GIVEN ----------
        Ocupante cliente = crearCliente(clienteData());

        // ---------- WHEN ----------
        Ocupante resultado = ocupanteService.buscarPorId(cliente.getId());

        // ---------- THEN ----------
        assertThat(resultado.getId()).isEqualTo(cliente.getId());
        assertThat(resultado.getNumeroDocumento()).isEqualTo(cliente.getNumeroDocumento());
    }

    @Test
    void falloBuscandoOcupantePorIdInexistente_test() {

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> ocupanteService.buscarPorId(Long.MAX_VALUE))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Ocupante no encontrado con id: " + Long.MAX_VALUE);
    }

    @Test
    void exitoBuscandoPorNumeroDocumentoParcial_test() {

        // ---------- GIVEN ----------
        Ocupante cliente = crearCliente(clienteData());

        // ---------- WHEN ----------
        List<OcupanteDTO> resultado = ocupanteService.buscarPorNumeroDocumento(cliente.getNumeroDocumento().substring(0, 5));

        // ---------- THEN ----------
        assertThat(resultado).isNotEmpty();
        assertThat(resultado).anyMatch(item -> item.getId().equals(cliente.getId()));
    }

    @Test
    void exitoBuscandoPorNumeroDocumentoNuloOVacioRetornaListaVacia_test() {

        // ---------- WHEN ----------
        List<OcupanteDTO> nulo = ocupanteService.buscarPorNumeroDocumento(null);
        List<OcupanteDTO> vacio = ocupanteService.buscarPorNumeroDocumento("   ");

        // ---------- THEN ----------
        assertThat(nulo).isEmpty();
        assertThat(vacio).isEmpty();
    }

    @Test
    void exitoCreandoOcupante_test() {

        // ---------- GIVEN ----------
        OcupanteNuevoRequestDTO request = requestOcupante("Laura", "Mora", "DOC-CREATE-01", TipoOcupante.CLIENTE);

        // ---------- WHEN ----------
        OcupanteDTO dto = ocupanteService.crearOcupante(request);

        // ---------- THEN ----------
        Ocupante ocupanteDb = ocupanteRepository.findById(dto.getId()).orElseThrow();
        assertThat(ocupanteDb.getNombres()).isEqualTo("Laura");
        assertThat(ocupanteDb.getNumeroDocumento()).isEqualTo("DOC-CREATE-01");
        assertThat(ocupanteDb.getTipoOcupante()).isEqualTo(TipoOcupante.CLIENTE);
    }

    @Test
    void exitoEditandoOcupanteActualizaCampos_test() {

        // ---------- GIVEN ----------
        Ocupante ocupante = crearCliente(clienteData());
        OcupanteNuevoRequestDTO request = requestOcupante("Mario", "Ruiz", "DOC-EDIT-99", TipoOcupante.ACOMPANANTE);

        // ---------- WHEN ----------
        OcupanteDTO dto = ocupanteService.editarOcupante(ocupante.getId(), request);

        // ---------- THEN ----------
        Ocupante ocupanteDb = ocupanteRepository.findById(dto.getId()).orElseThrow();
        assertThat(ocupanteDb.getNombres()).isEqualTo("Mario");
        assertThat(ocupanteDb.getApellidos()).isEqualTo("Ruiz");
        assertThat(ocupanteDb.getNumeroDocumento()).isEqualTo("DOC-EDIT-99");
        assertThat(ocupanteDb.getTipoOcupante()).isEqualTo(TipoOcupante.ACOMPANANTE);
    }

    @Test
    void exitoEditandoOcupantePropagaCambiosARelacionadosPorDocumentoYTipoDocumento_test() {

        // ---------- GIVEN ----------
        Ocupante base = crearCliente(clienteData());
        base.setTipoDocumento(TipoDocumento.CC);
        base.setNumeroDocumento("DOC-GRUPO-1");
        ocupanteRepository.save(base);

        Ocupante relacionado = crearCliente(clienteData());
        relacionado.setTipoDocumento(TipoDocumento.CC);
        relacionado.setNumeroDocumento("DOC-GRUPO-1");
        ocupanteRepository.save(relacionado);

        Ocupante otroTipoDocumento = crearCliente(clienteData());
        otroTipoDocumento.setTipoDocumento(TipoDocumento.CE);
        otroTipoDocumento.setNumeroDocumento("DOC-GRUPO-1");
        ocupanteRepository.save(otroTipoDocumento);

        OcupanteNuevoRequestDTO request = requestOcupante("Nombre Nuevo", "Apellido Nuevo", "DOC-GRUPO-2", TipoOcupante.CLIENTE);
        request.setTipoDocumento(TipoDocumento.CC);

        // ---------- WHEN ----------
        ocupanteService.editarOcupante(base.getId(), request);

        // ---------- THEN ----------
        Ocupante baseDb = ocupanteRepository.findById(base.getId()).orElseThrow();
        Ocupante relacionadoDb = ocupanteRepository.findById(relacionado.getId()).orElseThrow();
        Ocupante otroTipoDb = ocupanteRepository.findById(otroTipoDocumento.getId()).orElseThrow();

        assertThat(baseDb.getNumeroDocumento()).isEqualTo("DOC-GRUPO-2");
        assertThat(relacionadoDb.getNumeroDocumento()).isEqualTo("DOC-GRUPO-2");
        assertThat(otroTipoDb.getNumeroDocumento()).isEqualTo("DOC-GRUPO-1");
    }

    @Test
    void falloEditandoOcupanteInexistente_test() {

        // ---------- GIVEN ----------
        OcupanteNuevoRequestDTO request = requestOcupante("Ana", "Torres", "DOC-404", TipoOcupante.CLIENTE);

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> ocupanteService.editarOcupante(Long.MAX_VALUE, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Ocupante no encontrado con id: " + Long.MAX_VALUE);
    }

    @Test
    void exitoDeterminandoOcupantesConClienteYAcompanantesExistentes_test() {

        // ---------- GIVEN ----------
        Ocupante cliente = crearCliente(clienteData());
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());

        // ---------- WHEN ----------
        List<Ocupante> resultado = ocupanteService.determinarOcupantes(
                cliente.getId(),
                acompanantes.stream().map(Ocupante::getId).toList()
        );

        // ---------- THEN ----------
        assertThat(resultado).hasSize(3);
        assertThat(resultado).anyMatch(item -> item.getId().equals(cliente.getId()));
        assertThat(resultado.stream().filter(item -> item.getTipoOcupante() == TipoOcupante.ACOMPANANTE)).hasSize(2);
    }

    @Test
    void exitoDeterminandoOcupantesCreaClienteSiElRegistroEraAcompanante_test() {

        // ---------- GIVEN ----------
        Ocupante acompanante = crearAcompanantesSinCliente(List.of(acompanantesData().getFirst())).getFirst();
        long totalAntes = ocupanteRepository.count();

        // ---------- WHEN ----------
        List<Ocupante> resultado = ocupanteService.determinarOcupantes(acompanante.getId(), List.of());

        // ---------- THEN ----------
        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().getTipoOcupante()).isEqualTo(TipoOcupante.CLIENTE);
        assertThat(ocupanteRepository.count()).isEqualTo(totalAntes + 1);
    }

    @Test
    void exitoDeterminandoOcupantesCreaAcompananteSiElRegistroEraCliente_test() {

        // ---------- GIVEN ----------
        Ocupante cliente = crearCliente(clienteData());
        Ocupante acompananteCliente = crearCliente(clienteEditarData());
        long totalAntes = ocupanteRepository.count();

        // ---------- WHEN ----------
        List<Ocupante> resultado = ocupanteService.determinarOcupantes(cliente.getId(), List.of(acompananteCliente.getId()));

        // ---------- THEN ----------
        assertThat(resultado).hasSize(2);
        assertThat(resultado.stream().filter(item -> item.getTipoOcupante() == TipoOcupante.ACOMPANANTE)).hasSize(1);
        assertThat(ocupanteRepository.count()).isEqualTo(totalAntes + 1);
    }

    @Test
    void exitoDeterminandoOcupantesIgnoraIdsAcompanantesNulos_test() {

        // ---------- GIVEN ----------
        Ocupante cliente = crearCliente(clienteData());

        // ---------- WHEN ----------
        List<Ocupante> resultado = ocupanteService.determinarOcupantes(cliente.getId(), java.util.Arrays.asList((Long) null));

        // ---------- THEN ----------
        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().getId()).isEqualTo(cliente.getId());
    }

    @Test
    void falloDeterminandoOcupantesConIdInexistente_test() {

        // ---------- GIVEN ----------
        Ocupante cliente = crearCliente(clienteData());

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> ocupanteService.determinarOcupantes(cliente.getId(), List.of(Long.MAX_VALUE)))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Ocupante no encontrado con id: " + Long.MAX_VALUE);
    }

    @Test
    void exitoBuscandoClienteCuandoTipoEsCliente_test() {

        // ---------- GIVEN ----------
        Ocupante cliente = crearCliente(clienteData());

        // ---------- WHEN ----------
        Ocupante resultado = ocupanteService.buscarCliente(cliente.getId());

        // ---------- THEN ----------
        assertThat(resultado.getId()).isEqualTo(cliente.getId());
    }

    @Test
    void falloBuscandoClienteCuandoTipoNoEsCliente_test() {

        // ---------- GIVEN ----------
        Ocupante acompanante = crearAcompanantesSinCliente(List.of(acompanantesData().getFirst())).getFirst();

        // ---------- WHEN + THEN ----------
        assertThatThrownBy(() -> ocupanteService.buscarCliente(acompanante.getId()))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Cliente no encontrado con id: " + acompanante.getId());
    }

    @Test
    void exitoObteniendoNombresAcompanantesConcatenados_test() {

        // ---------- GIVEN ----------
        List<Ocupante> acompanantes = crearAcompanantesSinCliente(acompanantesData());

        // ---------- WHEN ----------
        String nombres = ocupanteService.obtenerNombresAcompanantes(
                acompanantes.stream().map(Ocupante::getId).collect(java.util.stream.Collectors.toSet())
        );

        // ---------- THEN ----------
        assertThat(nombres).contains(acompanantes.get(0).getNombres());
        assertThat(nombres).contains(acompanantes.get(1).getNombres());
        assertThat(nombres).contains(",");
    }

    @Test
    void exitoBuscandoClientesTablaConsolidandoPorDocumento_test() {

        // ---------- GIVEN ----------
        Ocupante cliente = crearCliente(clienteData());
        cliente.setNumeroDocumento("DOC-CONS-01");
        cliente.setTipoOcupante(TipoOcupante.CLIENTE);
        cliente.setCreadoEn(LocalDateTime.now().minusMinutes(1));
        ocupanteRepository.save(cliente);

        Ocupante acompanante = crearAcompanantesSinCliente(List.of(acompanantesData().getFirst())).getFirst();
        acompanante.setNumeroDocumento(" doc-cons-01 ");
        acompanante.setTipoDocumento(cliente.getTipoDocumento());
        acompanante.setTipoOcupante(TipoOcupante.ACOMPANANTE);
        acompanante.setCreadoEn(LocalDateTime.now());
        ocupanteRepository.save(acompanante);

        // ---------- WHEN ----------
        Page<ClienteTablaDTO> resultado = ocupanteService.buscarClientesTabla(
                null, null, null, "doc-cons-01", null, null,
                PageRequest.of(0, 10)
        );

        // ---------- THEN ----------
        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().getFirst().getTipoOcupante()).isEqualTo("CLIENTE_Y_ACOMPANANTE");
        assertThat(resultado.getContent().getFirst().getNumeroDocumento().trim().toLowerCase())
                .isEqualTo("doc-cons-01");
    }

    @Test
    void exitoBuscandoClientesTablaIncluyeReservasYExcluyeEstanciasReservadas_test() {

        // ---------- GIVEN ----------
        Ocupante cliente = crearCliente(clienteData());
        cliente.setNumeroDocumento("DOC-HIST-01");
        ocupanteRepository.save(cliente);

        Unidad unidadReserva = crearApartamento(EstadoOperativo.DISPONIBLE);
        Reserva reserva = reservaData(cliente, ModoOcupacion.COMPLETO, EstadoReserva.CONFIRMADA, unidadReserva.getHabitaciones(), null);
        reservaRepository.save(reserva);

        Unidad unidadActiva = crearApartamento(EstadoOperativo.OCUPADO);
        Estancia estanciaActiva = estanciaData(
                null,
                List.of(cliente),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.ACTIVA,
                unidadActiva.getHabitaciones(),
                null
        );
        estanciaRepository.save(estanciaActiva);

        Unidad unidadReservada = crearApartamento(EstadoOperativo.RESERVADO);
        Estancia estanciaReservada = estanciaData(
                null,
                List.of(cliente),
                ModoOcupacion.COMPLETO,
                EstadoEstancia.RESERVADA,
                unidadReservada.getHabitaciones(),
                null
        );
        estanciaRepository.save(estanciaReservada);

        // ---------- WHEN ----------
        Page<ClienteTablaDTO> resultado = ocupanteService.buscarClientesTabla(
                null, null, null, "DOC-HIST-01", null, null,
                PageRequest.of(0, 10)
        );

        // ---------- THEN ----------
        assertThat(resultado.getContent()).hasSize(1);
        ClienteTablaDTO fila = resultado.getContent().getFirst();
        assertThat(fila.getReservas()).extracting(item -> item.getId()).contains(reserva.getId());
        assertThat(fila.getEstancias()).extracting(item -> item.getId()).contains(estanciaActiva.getId());
        assertThat(fila.getEstancias()).extracting(item -> item.getId()).doesNotContain(estanciaReservada.getId());
    }

    @Test
    void exitoBuscandoClientesTablaConPaginacionPorGruposYSortExplicito_test() {

        // ---------- GIVEN ----------
        Ocupante c1 = crearCliente(clienteData());
        c1.setNumeroDocumento("DOC-PAGE-01");
        c1.setCreadoEn(LocalDateTime.now().minusHours(3));
        ocupanteRepository.save(c1);

        Ocupante c2 = crearCliente(clienteEditarData());
        c2.setNumeroDocumento("DOC-PAGE-02");
        c2.setCreadoEn(LocalDateTime.now().minusHours(2));
        ocupanteRepository.save(c2);

        Ocupante c3 = crearCliente(clienteData());
        c3.setNumeroDocumento("DOC-PAGE-03");
        c3.setCreadoEn(LocalDateTime.now().minusHours(1));
        ocupanteRepository.save(c3);

        // ---------- WHEN ----------
        Page<ClienteTablaDTO> pagina0 = ocupanteService.buscarClientesTabla(
                null, null, null, "DOC-PAGE", null, null,
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "creadoEn"))
        );
        Page<ClienteTablaDTO> pagina1 = ocupanteService.buscarClientesTabla(
                null, null, null, "DOC-PAGE", null, null,
                PageRequest.of(1, 2, Sort.by(Sort.Direction.ASC, "creadoEn"))
        );

        // ---------- THEN ----------
        assertThat(pagina0.getTotalElements()).isEqualTo(3);
        assertThat(pagina0.getContent()).hasSize(2);
        assertThat(pagina1.getContent()).hasSize(1);
        assertThat(pagina0.getContent().get(0).getNumeroDocumento()).contains("DOC-PAGE-01");
        assertThat(pagina1.getContent().getFirst().getNumeroDocumento()).contains("DOC-PAGE-03");
    }

    @Test
    void exitoBuscandoClientesTablaConFiltros_test() {

        // ---------- GIVEN ----------
        Ocupante cliente = crearCliente(clienteData());
        cliente.setNombres("Filtrado");
        cliente.setApellidos("Especial");
        cliente.setTipoDocumento(TipoDocumento.CE);
        cliente.setNumeroDocumento("DOC-FILT-01");
        cliente.setTelefono("3115550000");
        cliente.setEmail("filtrado@hotel.com");
        ocupanteRepository.save(cliente);

        // ---------- WHEN ----------
        Page<ClienteTablaDTO> resultado = ocupanteService.buscarClientesTabla(
                "filt",
                "espe",
                TipoDocumento.CE,
                "FILT-01",
                "555",
                "hotel.com",
                PageRequest.of(0, 10)
        );

        // ---------- THEN ----------
        assertThat(resultado.getContent()).isNotEmpty();
        assertThat(resultado.getContent())
                .anyMatch(item -> "DOC-FILT-01".equals(item.getNumeroDocumento()));
    }

    private OcupanteNuevoRequestDTO requestOcupante(String nombres, String apellidos, String documento, TipoOcupante tipo) {
        OcupanteNuevoRequestDTO request = new OcupanteNuevoRequestDTO();
        request.setNombres(nombres);
        request.setApellidos(apellidos);
        request.setTipoDocumento(TipoDocumento.CC);
        request.setNumeroDocumento(documento);
        request.setTelefono("3001112233");
        request.setEmail("test@hotel.com");
        request.setTipoOcupante(tipo);
        return request;
    }
}
