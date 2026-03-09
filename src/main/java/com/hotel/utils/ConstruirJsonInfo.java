package com.hotel.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConstruirJsonInfo {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final Map<String, Object> json = new LinkedHashMap<>();
    private Map<String, Object> objetoActual;

    public ConstruirJsonInfo agregarObjeto(String nombreObjeto) {
        Map<String, Object> nuevoObjeto = new LinkedHashMap<>();
        json.put(nombreObjeto, nuevoObjeto);
        objetoActual = nuevoObjeto;
        return this;
    }

    public ConstruirJsonInfo agregarPropiedades(String campo, Object valor) {
        if (objetoActual == null) {
            json.put(campo, valor);
            return this;
        }
        objetoActual.put(campo, valor);
        return this;
    }

    public boolean tieneContenido() {
        return !json.isEmpty();
    }

    public String build() {
        try {
            return MAPPER.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error construyendo JSON de info", e);
        }
    }
}
