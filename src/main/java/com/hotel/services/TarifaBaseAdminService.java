package com.hotel.services;

import com.hotel.dtos.tarifa.TarifaBaseActualizacionDTO;
import com.hotel.dtos.tarifa.TarifaBaseDTO;
import com.hotel.models.TarifaBase;
import com.hotel.models.enums.TipoUnidad;
import com.hotel.repositories.TarifaBaseRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TarifaBaseAdminService {

    private static final List<TipoUnidad> ORDEN_TIPOS_BASE = List.of(
            TipoUnidad.HABITACION,
            TipoUnidad.APARTAESTUDIO,
            TipoUnidad.APARTAMENTO
    );

    private static final Set<TipoUnidad> TIPOS_BASE = EnumSet.copyOf(ORDEN_TIPOS_BASE);

    private final TarifaBaseRepository tarifaBaseRepository;

    public TarifaBaseAdminService(TarifaBaseRepository tarifaBaseRepository) {
        this.tarifaBaseRepository = tarifaBaseRepository;
    }

    public List<TarifaBaseDTO> listarTarifasBase() {
        return ORDEN_TIPOS_BASE.stream()
                .map(this::buscarTarifaBasePorTipo)
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public List<TarifaBaseDTO> actualizarTarifasBase(List<TarifaBaseActualizacionDTO> actualizaciones) {
        validarActualizaciones(actualizaciones);

        Map<TipoUnidad, TarifaBaseActualizacionDTO> actualizacionesPorTipo = new EnumMap<>(TipoUnidad.class);
        for (TarifaBaseActualizacionDTO actualizacion : actualizaciones) {
            if (actualizacionesPorTipo.put(actualizacion.getTipoUnidad(), actualizacion) != null) {
                throw new IllegalArgumentException(
                        "Hay tipos de unidad repetidos en la actualización: " + actualizacion.getTipoUnidad());
            }
        }

        if (!actualizacionesPorTipo.keySet().equals(TIPOS_BASE)) {
            throw new IllegalArgumentException(
                    "Debes enviar exactamente las tarifas para: HABITACION, APARTAESTUDIO y APARTAMENTO.");
        }

        List<TarifaBase> tarifasActualizadas = new ArrayList<>();
        for (TipoUnidad tipoUnidad : ORDEN_TIPOS_BASE) {
            TarifaBase tarifaBase = buscarTarifaBasePorTipo(tipoUnidad);
            TarifaBaseActualizacionDTO actualizacion = actualizacionesPorTipo.get(tipoUnidad);

            tarifaBase.setPrecioDiaTemBaja(actualizacion.getPrecioDiaTemBaja());
            tarifaBase.setPrecioDiaTemAlta(actualizacion.getPrecioDiaTemAlta());
            tarifaBase.setPrecioEstadiaCorta(actualizacion.getPrecioEstadiaCorta());
            tarifaBase.setPrecioPersonaAdicionalTemBaja(actualizacion.getPrecioPersonaAdicionalTemBaja());
            tarifaBase.setPrecioPersonaAdicionalTemAlta(actualizacion.getPrecioPersonaAdicionalTemAlta());
            tarifaBase.setPrecioEstadiaPersonaAdicionalCorta(actualizacion.getPrecioEstadiaPersonaAdicionalCorta());

            tarifasActualizadas.add(tarifaBase);
        }

        tarifaBaseRepository.saveAll(tarifasActualizadas);
        return listarTarifasBase();
    }

    private void validarActualizaciones(List<TarifaBaseActualizacionDTO> actualizaciones) {
        if (actualizaciones == null || actualizaciones.isEmpty()) {
            throw new IllegalArgumentException(
                    "Debes enviar una lista con las tarifas base a actualizar.");
        }
    }

    private TarifaBase buscarTarifaBasePorTipo(TipoUnidad tipoUnidad) {
        return tarifaBaseRepository.findByTipoUnidad(tipoUnidad)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No existe la tarifa base para tipo de unidad: " + tipoUnidad));
    }

    private TarifaBaseDTO toDto(TarifaBase tarifaBase) {
        TarifaBaseDTO dto = new TarifaBaseDTO();
        dto.setTipoUnidad(tarifaBase.getTipoUnidad());
        dto.setPrecioDiaTemBaja(tarifaBase.getPrecioDiaTemBaja());
        dto.setPrecioDiaTemAlta(tarifaBase.getPrecioDiaTemAlta());
        dto.setPrecioEstadiaCorta(tarifaBase.getPrecioEstadiaCorta());
        dto.setPrecioPersonaAdicionalTemBaja(tarifaBase.getPrecioPersonaAdicionalTemBaja());
        dto.setPrecioPersonaAdicionalTemAlta(tarifaBase.getPrecioPersonaAdicionalTemAlta());
        dto.setPrecioEstadiaPersonaAdicionalCorta(tarifaBase.getPrecioEstadiaPersonaAdicionalCorta());
        return dto;
    }
}
