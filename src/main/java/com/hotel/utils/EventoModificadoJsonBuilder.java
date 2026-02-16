package com.hotel.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.LinkedHashMap;
import java.util.Map;

public class EventoModificadoJsonBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // LinkedHashMap para mantener el orden en que agregas cambios
    private final Map<String, Map<String, Object>> cambios = new LinkedHashMap<>();

    public EventoModificadoJsonBuilder agregarCambio(String campo, Object anterior, Object nuevo) {
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("anterior", anterior);
        detalle.put("nuevo", nuevo);

        cambios.put(campo, detalle);
        return this;
    }

    public boolean tieneCambios() {
        return !cambios.isEmpty();
    }

    public String build() {
        try {
            return MAPPER.writeValueAsString(cambios);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error construyendo JSON de cambios", e);
        }
    }
}
