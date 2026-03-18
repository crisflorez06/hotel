package com.hotel.testutils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.models.AuditoriaEvento;
import com.hotel.models.enums.TipoEntidad;
import com.hotel.models.enums.TipoEvento;
import com.hotel.repositories.AuditoriaEventoRepository;
import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public final class AuditoriaEventoTestHelper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

    private AuditoriaEventoTestHelper() {
    }

    public static AuditoriaEvento ultimoEvento(
            AuditoriaEventoRepository repository,
            TipoEntidad entidad,
            Long idEntidad
    ) {
        return repository.findFirstByEntidadAndIdEntidadOrderByFechaDesc(entidad, idEntidad)
                .orElseThrow(() -> new AssertionError(
                        "No se encontró evento para entidad " + entidad + " e id " + idEntidad));
    }

    public static Map<String, Object> parseDetalleNuevo(String detalle) {
        try {
            return OBJECT_MAPPER.readValue(detalle, new TypeReference<>() {});
        } catch (IOException ex) {
            throw new AssertionError("No se pudo parsear detalle de evento nuevo: " + detalle, ex);
        }
    }

    public static Map<String, Map<String, Object>> parseDetalleCambios(String detalle) {
        try {
            return OBJECT_MAPPER.readValue(detalle, new TypeReference<>() {});
        } catch (IOException ex) {
            throw new AssertionError("No se pudo parsear detalle de cambios: " + detalle, ex);
        }
    }

    public static void assertEventoBasico(
            AuditoriaEvento evento,
            TipoEvento tipoEvento,
            TipoEntidad entidad,
            Long idEntidad,
            String codigoEstancia,
            String codigoReserva
    ) {
        assertThat(evento.getTipoEvento()).isEqualTo(tipoEvento);
        assertThat(evento.getEntidad()).isEqualTo(entidad);
        assertThat(evento.getIdEntidad()).isEqualTo(idEntidad);
        assertThat(evento.getCodigoEstancia()).isEqualTo(codigoEstancia);
        assertThat(evento.getCodigoReserva()).isEqualTo(codigoReserva);
        assertThat(evento.getDetalle()).isNotBlank();
        assertThat(evento.getFecha()).isNotNull();
    }
}
