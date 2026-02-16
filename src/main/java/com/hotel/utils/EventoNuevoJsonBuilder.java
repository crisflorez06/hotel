package com.hotel.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.LinkedHashMap;
import java.util.Map;

public class EventoNuevoJsonBuilder {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Mapa simple: "campo" -> valor
    private final Map<String, Object> props = new LinkedHashMap<>();

    public EventoNuevoJsonBuilder agregarProp(String campo, Object valor) {
        props.put(campo, valor);
        return this;
    }

    public boolean tieneProps() {
        return !props.isEmpty();
    }

    public String build() {
        try {
            return MAPPER.writeValueAsString(props);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error construyendo JSON de creación", e);
        }
    }
}
